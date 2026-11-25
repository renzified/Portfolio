# 1. Layers, naming, and Fragment hosting

```
 ┌──────────────┐  state (read-only)   ┌──────────────┐  state + lambdas  ┌────────────────────┐
 │  Repository  │ ───────────────────▶ │  ViewModel   │ ────────────────▶ │ XFragment          │
 │  / UseCase   │ ◀─────────────────── │              │ ◀──────────────── │  └ ComposeView     │
 └──────────────┘    suspend / Flow    └──────────────┘   intent calls    │     └ XScreen      │
                                                                          │        └ parts     │
                          NavController ◀─── navigate / popBackStack ──── └────────────────────┘
```

The Fragment is the destination. It plays the part the `XRoute` composable plays in the Compose
rulebook: it owns the ViewModel, collects state, wires lambdas, runs effects and navigates. All UI
below it is Compose.

## FMVVM-L1 — Unidirectional data flow
State flows **down** as read-only values. Events flow **up** as lambdas. A composable never
writes into state it received as a parameter.

## FMVVM-L2 — Fragment / Screen split (MUST)
Every navigation destination has exactly two parts:

- `XFragment` — stateful. Obtains the ViewModel, collects state inside its `ComposeView`, wires
  lambdas, runs effects, and performs navigation with `findNavController()`.
- `XScreen` — a stateless composable. Takes `XUiState` + lambdas. Previewable and testable
  without a Fragment or a VM.

```kotlin
class TaskListFragment : Fragment() {
    private val viewModel: TaskListViewModel by viewModel()           // Koin, scoped to this Fragment (L9)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        composeScreen { Content() }                                   // F1

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()  // L6
        TaskListScreen(
            state = state,
            onTaskClick = { id -> findNavController().navigate(TaskDetailKey(id.value)) }, // E1 + E5
            onToggleDone = viewModel::toggleDone,                     // intent verb on VM (L8)
            onRetryClick = viewModel::retry,
        )
    }
}

@Composable
fun TaskListScreen(
    state: TaskListUiState,
    onTaskClick: (TaskId) -> Unit,
    onToggleDone: (TaskId) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) { /* no ViewModel, no Fragment, no NavController, no repository here */ }
```

A `@Composable private fun Content()` member keeps `onCreateView` one line and lets the content
call the Fragment's own members (`viewModel`, `findNavController()`). A destination without a
ViewModel (the catalog) still has the split.

## FMVVM-L3 — The ViewModel stops at the Fragment (MUST NOT)
```kotlin
// ❌ Child reaches into the VM – untestable, unpreviewable, recomposes on everything
@Composable fun TaskRow(task: TaskUi, viewModel: TaskListViewModel) { ... }

// ✅ Child gets data + lambdas
@Composable fun TaskRow(task: TaskUi, onToggleDone: () -> Unit, modifier: Modifier = Modifier) { ... }
```
The same holds for the Activity: the shell passes `state.message` and `viewModel::messageShown`
to `AppMessageHost`, never the `AppViewModel`.

## FMVVM-L4 — No framework objects in a ViewModel (MUST NOT)
Forbidden in a VM: `Context`, `Activity`, `Fragment`, `FragmentManager`, `NavController`,
`NavBackStackEntry`, `View`, `Bundle`, `Resources`, `LazyListState`, `ScrollState`,
`SnackbarHostState`, `FocusRequester`, `Painter`, `Color` derived from theme, any `@Composable`.

**Only exception:** `TextFieldState` (see FMVVM-I1). It is a plain snapshot-state object with no
Android references, and keeping it in the VM is the recommended way to own text input.

If you need an Android service (clipboard, location, connectivity), wrap it in a data-layer class
built with the application `Context` and depend on its interface.

## FMVVM-L5 — Read-only exposure (MUST)
```kotlin
// StateFlow style
private val _uiState = MutableStateFlow(GameUiState())
val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

// Snapshot style – see 02 (FMVVM-C4a)
private val _uiState = MutableGameUiState()
val uiState: GameUiState = _uiState
```

## FMVVM-L6 — Lifecycle-aware collection (MUST)
- Inside the Fragment's composition: always `collectAsStateWithLifecycle()`. Its
  `LocalLifecycleOwner` is the Fragment's **view** lifecycle (F3), so collection stops below
  STARTED and ends with the view.
- Outside Compose (rare — the Activity shell): `lifecycleScope.launch { repeatOnLifecycle(STARTED) { … } }`.
  In a Fragment, that is `viewLifecycleOwner.lifecycleScope` / `viewLifecycleOwner.repeatOnLifecycle`.
- Never `collectAsState()`, `launchWhenStarted`, or a bare `lifecycleScope.launch { flow.collect {} }`
  for ViewModel flows.

## FMVVM-L7 — No VM ↔ VM references (MUST NOT)
A ViewModel never receives another ViewModel (constructor, function arg, or lambda). Shared
data goes through the data layer — see FMVVM-V4.

## FMVVM-L8 — Naming (MUST)

| Thing | Name | Example |
|---|---|---|
| ViewModel | `XViewModel` | `TaskListViewModel` |
| UI state | `XUiState` | `TaskListUiState` |
| UI model for one item | `XUi` | `TaskUi` |
| Destination | `XFragment` / `XDialogFragment` | `TaskListFragment`, `RenameTaskDialogFragment` |
| Stateless screen | `XScreen` | `TaskListScreen` |
| Navigation key (type-safe route) | `XKey` | `TaskDetailKey` (see 10 – FMVVM-N2) |
| Fragment result request key | `X_RESULT` const | `LABEL_PICKED_RESULT` (see 10 – FMVVM-N4) |
| UI state holder (non-VM) | `XState` + `rememberXState()` | `FeedListState` |
| VM delegate | `XController` | `PromoCodeController` |
| VM public functions | intent verb, no `on` prefix | `retry()`, `toggleDone(id)`, `save()` |
| UI lambdas | `onNounVerb` | `onRetryClick`, `onToggleDone` |
| "Consumed" callbacks | `xHandled()` | `navigationHandled()`, `messageShown()` |

Navigation keys are named `XKey`, not `XRoute`, even though the Navigation component calls them
routes. It keeps the names the same as the Compose rulebook, where `XRoute` is the stateful
composable.

## FMVVM-L9 — ViewModel scope is chosen by the Fragment's delegate (MUST)
A Fragment's ViewModelStore lives as long as the Fragment is in the FragmentManager: through
rotation, and while the Fragment sits on the back stack with its view destroyed. It is cleared
when the destination is popped. Scope is decided **only** by which Koin delegate the Fragment uses:

| Delegate | Scope | Use for |
|---|---|---|
| `by viewModel()` | This destination | The default (V1) |
| `by viewModel(ownerProducer = graphOwner<FlowKey>())` | A nested graph's back-stack entry | A flow ViewModel shared by the graph's steps (V5) |
| `by viewModel()` in `MainActivity` | The Activity | The shell only (V6) |
| `by activityViewModel()` in a Fragment | The Activity | ❌ Never (V6) |

- Obtain ViewModels as Fragment **properties** (`by viewModel()`), never inside composables, and
  never with `ViewModelProvider(requireActivity())`.
- Lazy delegates are first touched after `onCreate`, so the ViewModel always sees the Fragment's
  arguments in its `SavedStateHandle` (P5).

---

## Fragment hosting

### FMVVM-F1 — Compose goes in through `content { }` (MUST)
`onCreateView` returns `composeScreen { … }` (this module's wrapper around fragment-compose's
`Fragment.content { }`, adding the theme). It sets
`ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed`, so the composition lives exactly
as long as the Fragment's view.

```kotlin
// ✅
override fun onCreateView(...): View = composeScreen { Content() }

// ❌ The default strategy disposes on window detach, which does not match the Fragment's view
//    lifecycle: the composition can outlive the view on the back stack or die during transitions.
override fun onCreateView(...) = ComposeView(requireContext()).apply { setContent { Content() } }
```

When a destination goes onto the back stack its view (and composition) is destroyed, but the
Fragment, its ViewModel and its `rememberSaveable` state survive. Returning rebuilds the view from
the same ViewModel.

### FMVVM-F2 — Fragments are stateless shells (MUST)
- No constructor parameters. The system recreates Fragments with the no-arg constructor after
  process death. Input comes from the nav key (P5); dependencies come from Koin.
- No screen state in Fragment fields (`var selectedId`, `var items`, a cached `TaskUi`). Fields
  are lost on recreation and survive on the back stack in ways that surprise. State lives in the
  ViewModel or in `rememberSaveable` (S1, S2, S5).
- No `View` references in fields. Compose has none to hold. Fields are limited to Koin delegates
  and Activity Result launchers (E6).

### FMVVM-F3 — View-bound work uses `viewLifecycleOwner` (MUST)
A Fragment has two lifecycles: the Fragment's (`this`) and its view's (`viewLifecycleOwner`).
On the back stack the Fragment stays CREATED while its view is destroyed. Anything that touches
the UI is bound to the **view** lifecycle:

- Compose content already is (F1, L6).
- A non-Compose observer started in `onViewCreated` uses `viewLifecycleOwner`, never `this`
  (with `this` it keeps running and duplicates every time the view is recreated).
- Things that must outlive the view are registered with `this` in `onCreate`: Fragment result
  listeners (N4) and Activity Result launchers (E6).

### FMVVM-F4 — The Activity is a thin shell (MUST)
One `FragmentActivity` hosts one `NavHostFragment` (`app:defaultNavHost="true"`) and sets the
graph built with the Kotlin DSL (`buildAppGraph()`). Besides that it only does the two app-wide
jobs that belong to the owner of the back stack: the session gate (N7) and the app-level message
host (N6). No screen UI, no screen logic.

```kotlin
class MainActivity : FragmentActivity(R.layout.activity_main) {
    private val viewModel: AppViewModel by viewModel()                // V6: the only Activity VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController.graph = navController.buildAppGraph()           // restores the saved back stack
        ...
    }
}
```

Libraries this rulebook needs (in `libs.versions.toml`, declared in the module's own build file):

| Artifact | For |
|---|---|
| `androidx.navigation:navigation-fragment` ≥ 2.8 | `NavHostFragment`, Kotlin DSL graph, type-safe keys, `toRoute()` |
| `androidx.fragment:fragment-compose` | `Fragment.content { }` (F1) |
| `androidx.lifecycle:lifecycle-runtime-compose` | `collectAsStateWithLifecycle()` (L6) |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `SavedStateHandle.saveable` for `TextFieldState` (I1) |
| `io.insert-koin:koin-android` | `Fragment.viewModel()`, `ownerProducer` (L9) |
| `org.jetbrains.kotlin.plugin.serialization` | `@Serializable` keys (N1) |
| `androidx.fragment:fragment-testing`, `androidx.navigation:navigation-testing` | Fragment navigation tests (T5) |

## Folder layout (per feature)
```
feature/tasks/
├── list/
│   ├── TaskListViewModel.kt
│   ├── TaskListUiState.kt        ← UiState + UI models (TaskUi)
│   ├── TaskListFragment.kt       ← Fragment + Screen
│   └── components/               ← stateless pieces (TaskRow, EmptyState…)
└── detail/ ...
navigation/
├── Keys.kt                       ← every @Serializable XKey + result request keys
└── AppNavGraph.kt                ← the graph (declares destinations; navigates nowhere)
```
