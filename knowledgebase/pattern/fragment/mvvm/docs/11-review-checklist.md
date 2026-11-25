# 11. Review checklist & anti-patterns

Copy into the PR description for any screen/ViewModel change.

## PR checklist

**Structure**
- [ ] Destination has `XFragment` (stateful) + `XScreen` (stateless) — L2
- [ ] ViewModel is only referenced in the Fragment (or the Activity for the shell) — L3
- [ ] ViewModels obtained as Fragment properties with the right delegate; no `activityViewModel()` — L9, V6
- [ ] No `Context`/`Fragment`/`NavController`/`Bundle`/`LazyListState`/`SnackbarHostState`/UI types in the VM — L4, M8
- [ ] Mutable state is private; public is read-only — L5, C4a
- [ ] `collectAsStateWithLifecycle()` (or `repeatOnLifecycle` outside Compose) — L6
- [ ] No VM references another VM — L7

**Fragment hosting**
- [ ] `onCreateView` returns `composeScreen { }` / `content { }` — F1
- [ ] No constructor args, no screen state or View references in Fragment fields — F2
- [ ] View-bound observers use `viewLifecycleOwner`; result listeners and launchers are registered with the Fragment — F3, N4, E6
- [ ] New destination registered in `AppNavGraph.kt`; the Activity stays a shell — F4

**State shape**
- [ ] One `XUiState` in one `StateFlow` (or a justified snapshot-state class) — C1, C4
- [ ] Not mixing StateFlow and `mutableStateOf` UI state in one VM (except `TextFieldState`) — C5
- [ ] Multi-field writes are atomic: `update {}` / `withMutableSnapshot {}` — C2, C3, C4b
- [ ] New values computed into locals before writing — C7
- [ ] Exclusive phases are sealed; no impossible flag combos — M1
- [ ] No stored derived values — M2
- [ ] Lazy lists use `key` — M4
- [ ] No resolved strings in the VM — M5
- [ ] Non-null initial state — M7

**Ownership**
- [ ] Pure UI state isn't in the VM; business state isn't in `remember` — S1, S2, S6
- [ ] State hoisted no higher than needed — S4
- [ ] Rotation/back stack/process death behaviour is intentional — S5, P5
- [ ] Shared data goes through a repository; flow drafts through a nested-graph VM — V4, V5
- [ ] Dialog with its own logic is a `DialogFragment` destination with its own VM — V7

**Producing & events**
- [ ] Streams use `stateIn(viewModelScope, WhileSubscribed(5_000), …)` — P1
- [ ] No work in `init {}` — P3
- [ ] Nav args reach the constructor via `toRoute()` in the Koin definition — P5
- [ ] Errors caught and turned into state — P7
- [ ] VM → UI outcomes are state + `xHandled()`; no SharedFlow/Channel — E3, E4
- [ ] Navigation executed in the Fragment; NavController looked up when navigating — E5
- [ ] Text input via `TextFieldState` — I1
- [ ] Per-frame values stay in the UI — I2

**Between screens**
- [ ] Nav keys hold IDs / small parameters only — N1
- [ ] The Fragment and Screen never read the key; key values come through the VM — N2
- [ ] No static holders / shared Activity VMs / Fragment constructor args / hand-built Bundles — N3
- [ ] Results use the Fragment Result API with explicit request keys, listener in `onCreate`, forwarded to a VM intent — N4
- [ ] Other screens learn about data changes by observing the repository, not by refresh calls — N5
- [ ] Messages that outlive the screen go to the app message queue — N6
- [ ] Session/app-wide changes handled by the Activity from state — N7
- [ ] No `SharedFlow`/`Channel`/event bus between screens or VMs — N8

**Tests**
- [ ] VM test with fakes covering each UiState phase + each outcome — T1–T3
- [ ] Screen previews/tests per phase — T4
- [ ] Fragment navigation test for new navigation wiring — T5

## Anti-pattern gallery

| ❌ Smell | Why it's wrong | ✅ Instead | Rule |
|---|---|---|---|
| `fun Child(vm: XViewModel)` | Untestable, couples UI to VM | Pass state + lambdas | L3 |
| `ComposeView(ctx).apply { setContent { } }` with the default strategy | Composition lifetime ≠ Fragment view lifetime | `composeScreen { }` / `content { }` | F1 |
| `class DetailFragment(val task: Task) : Fragment()` | Crashes or loses data on recreation | Key + VM | F2, N3 |
| `var selectedId: Long?` in a Fragment | Lost on recreation, stale on the back stack | VM / `rememberSaveable` | F2 |
| `lifecycleScope.launch { vm.state.collect { … } }` in `onViewCreated` | Keeps collecting without a view; duplicates on return | `viewLifecycleOwner.repeatOnLifecycle` / Compose | F3, L6 |
| `by activityViewModel()` / `activityViewModels()` | Global coupling, never cleared | Repository or nested-graph VM | V6, V5 |
| `DialogFragment().show(parentFragmentManager, …)` next to Navigation | Not on the back stack, no key | `dialog<…>()` destination | V7 |
| `_state.value = _state.value.copy(…)` | Lost updates under concurrency | `_state.update { it.copy(…) }` | C2 |
| `_items` + `_isLoading` as separate flows written together | Torn frames | One UiState | C3 |
| Public `var x by mutableStateOf()` in VM | Anyone can write | Read-only interface + private impl | C4a |
| `uiState.map { it.score }.stateIn()` per field | Fake granularity, breaks consistency | Style B | C6 |
| `val isLoading; val error; val data` all in one flat class | Impossible states | Sealed phases | M1 |
| `val total` stored next to `items` | Drifts | Computed property | M2 |
| `context.getString()` / `requireContext()` in VM | Leaks, ignores locale change | `UiText` | M5, L4 |
| `StateFlow<XUiState?>` | Hides loading/empty/error | Real initial value | M7 |
| `init { viewModelScope.launch { load() } }` | Eager, untestable, no retry | Cold flow in `stateIn` + retry trigger | P3 |
| `SharingStarted.Eagerly` for UI state | Upstream runs in background | `WhileSubscribed(5_000)` | P1 |
| `requireArguments().getLong("taskId")` in a Fragment | Untyped, key leaks into the UI layer | `toRoute()` in the Koin definition | P5, N2 |
| `suspend fun save()` called from UI | Work dies with the view | Non-suspend, launch in VM | P4 |
| `PagingData` inside UiState | Not a value | Separate flow + `cachedIn` | P6 |
| `MutableSharedFlow<Event>()` for navigation | Events lost | State + `navigationHandled()` | E3, E4 |
| `NavController` in a VM / `vm.load(id)` from `onViewCreated` | Navigation in VM, args after creation | Fragment navigates, args via constructor | E5, P5 |
| `val nav = findNavController()` during composition | Fails before the view is attached; breaks T5 | Look it up inside the lambda | E5 |
| `registerForActivityResult` inside a click handler | Crashes: must register before CREATED | Fragment property | E6 |
| `TextField(value = vm.query.collectAsState().value, …)` | Async → cursor bugs | `TextFieldState` | I1 |
| `onValueChange = vm::setDragOffset` | VM round-trip per frame | Local state, commit at end | I2 |
| `Modifier.offset(y = state.value.dp)` for scroll-driven UI | Recompose per frame | `Modifier.offset { }` | I3 |
| `expanded` set in VM for an FAQ row | Plumbing for pure UI state | `rememberSaveable` | S2 |
| `var name by remember { mutableStateOf(state.name) }` | Stale duplicate | VM-owned draft | S7 |
| `TaskDetailKey(task: TaskUi)` | Stale copy in saved state, size limits | `TaskDetailKey(taskId)` | N1 |
| `SelectedTask.current = task` before navigating | Lost on process death, hidden dependency | ID in the key | N3 |
| `previousBackStackEntry?.savedStateHandle?.set(…)` for results | Not cleared after reading; re-applied | Fragment Result API | N4 |
| `childFragmentManager.setFragmentResultListener(…)` in the opener | Never called: the result is set on the NavHost's FragmentManager | `setFragmentResultListener(…)` (fragment-ktx, parent FM) in `onCreate` | N4 |
| `onResume { vm.refresh() }` after returning | Polling for changes | Observe the repository | N5 |
| `object AppEvents { val bus = MutableSharedFlow<Event>() }` | Dropped/duplicated events, hidden coupling | Repository state / message queue / result | N8 |
| Each Fragment collecting a "logged out" event | Duplicated, racy navigation | Session state; the Activity rewrites the back stack | N7 |
