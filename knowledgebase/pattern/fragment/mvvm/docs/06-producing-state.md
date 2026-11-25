# 6. Producing state

## FMVVM-P1 — Streams become state with `stateIn` (MUST)
```kotlin
val uiState: StateFlow<XUiState> = source
    .map { … }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),   // survives rotation, stops 5 s after UI leaves
        initialValue = XUiState.Loading,
    )
```
- `WhileSubscribed(5_000)` is the only allowed `started` for UI state. `Eagerly`/`Lazily` keep
  upstream (DB, sockets, location) alive while the app is in the background.
- Define a constant once: `private val WhileUiSubscribed = SharingStarted.WhileSubscribed(5_000)`.

## FMVVM-P2 — User inputs are private flows, combined with data (MUST)
The canonical screen: a data stream from the repository + user-controlled inputs + transient UI
outcomes.

```kotlin
class TaskListViewModel(
    private val repository: TaskRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, TaskFilter.All)  // P5: user input
    private val isRefreshing = MutableStateFlow(false)                               // transient
    private val userMessage = MutableStateFlow<UiText?>(null)                       // transient (07)

    val uiState: StateFlow<TaskListUiState> =
        combine(
            filter.flatMapLatest { repository.observeTasks(it) },
            filter, isRefreshing, userMessage,
        ) { tasks, filter, refreshing, message ->
            TaskListUiState.Content(
                tasks = tasks.map { it.toUi() },
                filter = filter,
                isRefreshing = refreshing,
                userMessage = message,
            ) as TaskListUiState
        }
            .catch { emit(TaskListUiState.Error(UiText.Res(R.string.error_generic))) }   // P7
            .stateIn(viewModelScope, WhileUiSubscribed, TaskListUiState.Loading)

    fun selectFilter(f: TaskFilter) { savedStateHandle[KEY_FILTER] = f }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.refresh()                                       // writes DB → stream re-emits
                .onFailure { userMessage.value = UiText.Res(R.string.refresh_failed) }
            isRefreshing.value = false
        }
    }

    fun toggleDone(id: TaskId) { viewModelScope.launch { repository.toggleDone(id) } }
    fun messageShown() { userMessage.value = null }

    private companion object { const val KEY_FILTER = "filter" }
}
```

`flatMapLatest` needs `@OptIn(ExperimentalCoroutinesApi::class)`.

### When to use `MutableStateFlow<XUiState>` + `update {}` instead
When the screen is driven mostly by user actions rather than data streams (forms, editors, games),
the "private `_uiState` + `update {}`" form from chapter 2 is fine. Pick per VM:

| VM mostly… | Shape |
|---|---|
| displays repository streams | `combine(...).stateIn(...)` (P1/P2) |
| reacts to user actions / local logic | `private val _uiState = MutableStateFlow(…)` + `update {}` (C2) |

Don't do both for the same fields.

## FMVVM-P3 — No work in `init {}` (MUST NOT)
```kotlin
// ❌ Runs on construction even if the UI never subscribes; hard to test; loads again
//    on nothing; can't be retried cleanly.
init { viewModelScope.launch { _uiState.value = Content(repository.load()) } }
```

One-shot loads become a cold flow inside the `stateIn` chain, with a retry trigger:

```kotlin
private val retries = MutableStateFlow(0)

@OptIn(ExperimentalCoroutinesApi::class)
val uiState: StateFlow<ProfileUiState> = retries
    .flatMapLatest {
        flow {
            emit(ProfileUiState.Loading)
            emit(
                repository.loadProfile().fold(
                    onSuccess = { ProfileUiState.Content(it.toUi()) },
                    onFailure = { ProfileUiState.Error(UiText.Res(R.string.error_generic)) },
                )
            )
        }
    }
    .stateIn(viewModelScope, WhileUiSubscribed, ProfileUiState.Loading)

fun retry() = retries.update { it + 1 }
```

Note: with `WhileSubscribed(5_000)`, if the user is away > 5 s the upstream restarts and the load
runs again on return. That's usually desired (fresh data). If not, cache in the repository.

## FMVVM-P4 — Public VM functions are non-suspend and main-safe (MUST)
```kotlin
fun save() { viewModelScope.launch { … } }          // ✅ UI calls it from a click
suspend fun save() { … }                             // ❌ UI would need a scope; work dies with the view
```
Repositories/use cases are main-safe (they switch dispatchers themselves), so the VM never wraps
calls in `withContext(Dispatchers.IO)` for repository calls. Heavy CPU work in the VM uses an injected
dispatcher (P8).

## FMVVM-P5 — Nav args via constructor; SavedStateHandle for user input only (MUST)
The Navigation component writes a destination's `@Serializable` key into its arguments, and a
Fragment's default `SavedStateHandle` starts with those arguments. The key is decoded **once, in
the Koin definition**, with `SavedStateHandle.toRoute<XKey>()`, and the VM receives plain values
in its constructor. What a key may contain: see 10 – FMVVM-N1.

```kotlin
@Serializable data class TaskDetailKey(val taskId: Long)

// di/AppModules.kt
private inline fun <reified K : Any> Scope.navArgs(): K = get<SavedStateHandle>().toRoute<K>()

viewModel { TaskDetailViewModel(navArgs<TaskDetailKey>().taskId, get(), get()) }

// The Fragment just asks for its VM. It never reads its own arguments.
class TaskDetailFragment : Fragment() {
    private val viewModel: TaskDetailViewModel by viewModel()
}

class TaskDetailViewModel(
    val taskId: Long,                                   // nav arg – immutable for the VM's lifetime
    private val repository: TaskRepository,
    labelRepository: LabelRepository,
) : ViewModel()
```

Why decode in the definition, not in the VM (`savedStateHandle.toRoute<…>()` in a field) and not
in the Fragment:
- The VM has no Navigation dependency, and tests construct it with `TaskDetailViewModel(7, …)` (T1).
- The Fragment does not touch `arguments`/`Bundle` and has nothing to forget (F2).
- `findNavController().getBackStackEntry<TaskDetailKey>()` returns the **topmost** entry of that
  type, which is the wrong one when the destination is on the back stack twice (detail → detail).
  The Fragment's own `SavedStateHandle` is always its own.

A destination with no other logic still gets a small VM when its screen needs a key value
(`ChapterViewModel(number)`, `LabelPickerViewModel(selectedLabelId, …)`). Key values reach the
Screen through the VM's state, like everything else.

The arguments are saved with the back stack, so they survive process death without your help.
Don't also write them into `SavedStateHandle` yourself, and don't reuse their names for your own
saved keys: they share the same handle.

| Put in SavedStateHandle | Don't |
|---|---|
| Selected filter / tab / sort | Nav arguments (constructor, above) |
| Draft text the user typed | Lists/data you can reload |
| | Loading flags, errors |
| | Large objects (Bundle limit ~1 MB for the whole app) |

```kotlin
private val sort = savedStateHandle.getStateFlow("sort", SortOrder.Newest)        // StateFlow style

@OptIn(SavedStateHandleSaveableApi::class)
val title: TextFieldState = savedStateHandle.saveable(
    key = "title", saver = TextFieldState.Saver,
) { TextFieldState() }                                                            // draft text (I1)
```

## FMVVM-P6 — Paging is a separate flow (MUST — explicit exception to C1)
`PagingData` is a stream, not a value; it can't sit inside a `data class` UiState.

```kotlin
class FeedViewModel(repository: FeedRepository) : ViewModel() {
    val posts: Flow<PagingData<PostUi>> = repository.pagedPosts()
        .map { page -> page.map { it.toUi() } }
        .cachedIn(viewModelScope)                       // MUST: survives rotation, single upstream

    val uiState: StateFlow<FeedUiState> = …            // everything else (header, filters)
}

// in FeedFragment
@Composable
private fun Content() {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    FeedScreen(state = state, posts = posts)
}
```
Loading/error of the list itself comes from `posts.loadState`, not from UiState.
`cachedIn` also matters for the back stack: the Fragment's view (and its `LazyPagingItems`) is
destroyed when you navigate away, and on return the new view re-collects the cached pages instead
of reloading from page 0.

## FMVVM-P7 — Errors are state (MUST)
- Every flow that reaches `stateIn` has a `.catch { emit(Error…) }` (or the repository returns
  `Result`).
- Every `viewModelScope.launch` that can fail handles the failure and writes it into state
  (`userMessage`, `Error` phase). Never let an exception escape `viewModelScope`.
- `CancellationException` is never swallowed — `runCatching` around a suspend call must rethrow it
  (or use a `suspendRunCatching` helper).

## FMVVM-P8 — Injected dispatchers (MUST)
```kotlin
class ReportViewModel(
    private val repository: ReportRepository,
    @DefaultDispatcher private val default: CoroutineDispatcher,
) : ViewModel() {
    fun export() {
        viewModelScope.launch {
            val csv = withContext(default) { buildCsv(repository.rows()) }   // CPU work
            …
        }
    }
}
```
Tests pass a `TestDispatcher`.
