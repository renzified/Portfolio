# 9. Testing

The rules in chapters 1–8 exist partly so that each layer can be tested alone:
the VM without UI, the Screen without a VM or a Fragment, and the Fragment only for the wiring
that is left.

| Layer | Test | Runs on | Rule |
|---|---|---|---|
| ViewModel | Plain JVM test with fakes | `testDebugUnitTest` | T1–T3 |
| Screen | Compose UI test, hand-built state | device (`connectedDebugAndroidTest`) | T4 |
| Fragment | Fragment + `TestNavHostController` | device | T5 |

## FMVVM-T1 — Fakes + MainDispatcherRule (MUST)
- Use hand-written **fakes** of repository interfaces (not mocks) so tests drive real flows.
- `viewModelScope` uses `Dispatchers.Main`; replace it in every VM test.

```kotlin
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    var refreshResult: Result<Unit> = Result.success(Unit)
    fun emit(list: List<Task>) { tasks.value = list }
    override fun observeTasks(filter: TaskFilter) = tasks.map { it.filter(filter::matches) }
    override suspend fun refresh() = refreshResult
    override suspend fun toggleDone(id: TaskId) =
        tasks.update { l -> l.map { if (it.id == id) it.copy(done = !it.done) else it } }
}
```

## FMVVM-T2 — `WhileSubscribed` flows need a collector (MUST)
`stateIn(WhileSubscribed)` does nothing until someone collects. Start a collector in
`backgroundScope` (auto-cancelled at the end of the test).

```kotlin
class TaskListViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val repo = FakeTaskRepository()
    private val vm = TaskListViewModel(repo, SavedStateHandle())

    @Test
    fun `shows tasks from repository`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        repo.emit(listOf(task(id = 1, title = "Buy milk")))

        val state = vm.uiState.value as TaskListUiState.Content
        assertEquals(listOf("Buy milk"), state.tasks.map { it.title })
    }

    @Test
    fun `refresh failure shows message and clears after shown`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        repo.refreshResult = Result.failure(IOException())

        vm.refresh()
        assertEquals(UiText.Res(R.string.refresh_failed),
            (vm.uiState.value as TaskListUiState.Content).userMessage)   // E3 – outcome is plain data

        vm.messageShown()
        assertNull((vm.uiState.value as TaskListUiState.Content).userMessage)
    }
}
```
(Turbine's `vm.uiState.test { awaitItem() … }` is an equally valid way to collect; pick one per
project and stick to it.)

## FMVVM-T3 — Snapshot-style VMs: read properties directly (MUST)
No collector needed; `withMutableSnapshot` applies to the global snapshot when it returns.

```kotlin
class GameViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    @Test
    fun `tick increments time and score atomically`() {
        val vm = GameViewModel()

        vm.tick()

        assertEquals(1, vm.uiState.timeSurvived)
        assertEquals(10.01, vm.uiState.score, 1e-9)       // C7: score uses the NEW time
    }
}
```
To test reactions to snapshot state (e.g. `snapshotFlow { field.text }` in I1/I4), change the state
then advance: `vm.query.setTextAndPlaceCursorAtEnd("kot"); advanceTimeBy(301)`. If changes made
outside composition don't propagate, call `Snapshot.sendApplyNotifications()` after the write.

## FMVVM-T4 — UI tests hit the stateless Screen (MUST)
Because of L2, screens take plain state — no Fragment, no VM, no Koin, no fake repository in UI
tests.

```kotlin
class TaskListScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun errorState_retryCallsLambda() {
        var retried = false
        compose.setContent {
            TaskListScreen(
                state = TaskListUiState.Error(UiText.Raw("Boom")),
                onTaskClick = {}, onToggleDone = {}, onRetryClick = { retried = true },
            )
        }
        compose.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }
}
```

Previews follow the same pattern: one `@Preview` per sealed phase (Loading / Error / Content /
Empty).

## FMVVM-T5 — Fragment tests cover navigation wiring only (SHOULD)
What a Screen test can't see is whether the Fragment's lambdas reach the NavController with the
right key. Test that with `launchFragmentInContainer` and a `TestNavHostController` carrying the
real graph, so the test also proves the key is a registered destination:

```kotlin
@get:Rule val compose = createEmptyComposeRule()

@Test
fun newTaskClick_navigatesToTheEditorForANewTask() {
    lateinit var navController: TestNavHostController
    launchFragmentInContainer<TaskListFragment>(themeResId = R.style.Theme_Portfolio)
        .onFragment { fragment -> navController = attachTestNavController(fragment, startAt = TaskListKey) }

    compose.onNodeWithText("New task").performClick()

    assertEquals(EditTaskKey(taskId = null), navController.currentBackStackEntry!!.toRoute<EditTaskKey>())
}
```

A plain `TestNavHostController` can't hold a Kotlin-DSL graph with `fragment<…>()` /
`dialog<…>()` destinations: the builders ask the navigator provider for a `FragmentNavigator` /
`DialogFragmentNavigator`, and the test controller only has stand-ins (`ClassCastException`).
`testing/TestNavigation.kt` registers a `FragmentNavigator` subclass that records navigation on
the back stack without running Fragment transactions, plus a real `DialogFragmentNavigator`.
That is also why the graph is a function of `NavController` (`buildAppGraph()`), not a `val`.

- Don't re-test VM logic here (that's T1–T3). The Fragment gets its real VM from Koin with the
  in-memory data, so keep assertions to navigation.
- `findNavController()` is called only inside lambdas and effects (E5), never in `onCreateView`
  itself, which is what makes it possible to attach the test controller after the view exists.
