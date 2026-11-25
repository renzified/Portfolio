# MVVM — House Rules (Fragments · Navigation component · Compose UI · Koin)

A rulebook, not a tutorial. It is the Fragment counterpart of the
[Compose / Navigation 3 rulebook](../../compose/mvvm/README.md): the same chapters and the same
rule IDs where the rule is the same, prefixed `FMVVM-` so a review comment says which book it
cites (`"breaks FMVVM-N4"`). Words are used strictly:

| Word | Meaning |
|---|---|
| **MUST / MUST NOT** | No exceptions unless the rule itself lists one. |
| **SHOULD** | Default. Deviating requires a comment in code explaining why. |
| **MAY** | Allowed when the linked decision tree says so. |

## The stack

```
MainActivity (FragmentActivity, thin shell: F4)
 ├── NavHostFragment ── graph built in Kotlin DSL with @Serializable keys (AppNavGraph.kt)
 │    ├── XFragment ── by viewModel() ─▶ XViewModel        (L2, L9)
 │    │    └── composeScreen { } ─▶ XScreen (stateless)   (F1)
 │    ├── XDialogFragment (dialog<…> destination)         (V7)
 │    └── navigation<FlowKey> { step Fragments }          (V5: flow VM scoped to the graph)
 └── ComposeView ── AppMessageHost (app-level messages)   (N6)
```

Every screen is drawn by Compose. The Fragment is the destination: it takes over everything the
`XRoute` composable does in the Compose rulebook (gets the ViewModel, collects state, runs
effects, navigates).

### What differs from the Compose rulebook

| Topic | Compose + Navigation 3 | Fragments + Navigation component | Rules |
|---|---|---|---|
| Stateful half of a destination | `XRoute` composable | `XFragment` | L2 |
| Getting the VM | `koinViewModel()` in the Route | `by viewModel()` property on the Fragment | L9 |
| VM scoped to a destination | ViewModel entry decorator | Automatic: the Fragment's ViewModelStore | L9 |
| Hosting Compose | — | `content { }` with `DisposeOnViewTreeLifecycleDestroyed` | F1–F3 |
| App shell | `AppRoot` composable | `MainActivity` + `NavHostFragment` | F4, N6, N7 |
| Nav args → VM | `parametersOf(key.x)` in the entry block | `SavedStateHandle.toRoute<XKey>()` in the Koin definition | P5, N2 |
| Multi-step flow | One entry + nested `NavDisplay` | Nested graph; VM on the graph's back-stack entry | V5 |
| Dialog destination | `DialogSceneStrategy` | `DialogFragment` via `dialog<…>()` | V7 |
| Results | Nav3 `ResultEventBus` in entry blocks | Fragment Result API | N4, N8 |
| Permission launchers | `rememberLauncherForActivityResult` | `registerForActivityResult` as a Fragment property | E6 |
| Two panes | List–detail scene strategy (N9) | Not covered | — |
| Fragment-level tests | — | `launchFragmentInContainer` + `TestNavHostController` | T5 |

Chapters 2, 3, 5 and 8 (state shape, ownership, modeling, input) are the same as in the Compose
rulebook, with Fragment notes where the view lifecycle matters.

## Runnable examples

Every rule has an implementation in this module: the **Example** column of the rule index below
links to it, and the code is tagged `// FMVVM-XX` where the rule applies. The app is a catalog:
one screen per chapter, listing the demo destinations for that chapter's rules.

```sh
./gradlew :knowledgebase:pattern:fragment:mvvm:installDebug              # run the catalog
./gradlew :knowledgebase:pattern:fragment:mvvm:testDebugUnitTest         # ViewModel tests (T1–T3, chapter 10)
./gradlew :knowledgebase:pattern:fragment:mvvm:connectedDebugAndroidTest # Screen UI test (T4), Fragment nav test (T5)
```

Data is in-memory (`data/`), with deliberate failures so error paths can be tried: every second
task refresh fails, the first profile load fails, searching "error" fails, and
`taken@example.com` is already registered.

## Chapters

| # | File | Covers |
|---|---|---|
| 1 | [01-layers-and-naming.md](docs/01-layers-and-naming.md) | Fragment/Screen split, who may touch the ViewModel, VM scoping, naming, Fragment hosting (F1–F4), folder layout |
| 2 | [02-consistency-vs-granularity.md](docs/02-consistency-vs-granularity.md) | `StateFlow<UiState>` vs snapshot state, atomic writes, invalidation scope |
| 3 | [03-where-state-lives.md](docs/03-where-state-lives.md) | ViewModel vs `remember` vs plain state holder class (never a Fragment field) |
| 4 | [04-splitting-viewmodels.md](docs/04-splitting-viewmodels.md) | One vs many ViewModels, delegates, nested-graph flows, DialogFragments, sharing data |
| 5 | [05-modeling-ui-state.md](docs/05-modeling-ui-state.md) | Sealed vs flags, derived values, lists, strings, UI models |
| 6 | [06-producing-state.md](docs/06-producing-state.md) | `stateIn`, `combine`, no `init {}`, retry/refresh, nav args via `toRoute()`, Paging, errors |
| 7 | [07-events.md](docs/07-events.md) | UI → VM actions, VM → UI one-off outcomes, navigation from the Fragment, launchers |
| 8 | [08-input-and-high-frequency.md](docs/08-input-and-high-frequency.md) | Text fields, search debounce, gestures, animations, phase deferral |
| 9 | [09-testing.md](docs/09-testing.md) | Testing both state styles, stateless screens, and Fragment navigation |
| 10 | [10-between-screens.md](docs/10-between-screens.md) | Nav key contents, Fragment results, notifying other screens, app-level messages, session changes |
| 11 | [11-review-checklist.md](docs/11-review-checklist.md) | Anti-patterns + PR checklist |

---

## The four decision trees

Run trees 1–3 in order whenever you add a new piece of state. Run tree 4 whenever a value or an
"it happened" has to leave the screen that produced it.

### Tree 1 — Where does this state live? (chapter 3)

```
Is it read or changed by business logic, or does it come from / go to the data layer?
├── YES → ViewModel                                                    (FMVVM-S1)
└── NO  → It is UI element state.
          Is the logic around it more than trivial, or shared by several composables?
          ├── YES → Plain state holder class + rememberXState()        (FMVVM-S3)
          └── NO  → remember { } / rememberSaveable { } in the composable (FMVVM-S2)

Would the user be annoyed to lose it on rotation / process death / the back stack?
                                                                    → rememberSaveable (FMVVM-S5)
Does it change every frame (gesture, animation)?                    → never in the VM  (FMVVM-I2)
Tempted to keep it in a Fragment field?                             → never            (FMVVM-F2)
```

### Tree 2 — How does the ViewModel expose it? (chapter 2)

```
Does any field update continuously (≥ several times per second for a while:
ticker, game loop, upload progress, playback position, sensor)?
├── NO  → StateFlow<XUiState> (one immutable data class)                (FMVVM-C1)  ← DEFAULT
└── YES → Is recomposing the screen root on each tick measurably expensive,
          or are there many independent readers of different fields?
          ├── NO  → StateFlow<XUiState> (stay on the default)
          └── YES → Snapshot-state class (read-only interface + private mutable impl) (FMVVM-C4)
```

### Tree 3 — Which ViewModel owns it? (chapter 4)

```
Is the state for a section that is self-contained (own data source) AND reused on several screens?
├── YES → That section's own ViewModel, obtained by the host Fragment           (FMVVM-V3)
└── NO  → Is it shared by several destinations of one multi-step flow?
          ├── YES → Nested graph; flow ViewModel scoped to the graph's entry    (FMVVM-V5)
          └── NO  → Is it shared by unrelated screens?
                    ├── YES → Repository is the source of truth; each VM observes it (FMVVM-V4)
                    └── NO  → The destination's ViewModel (`by viewModel()`)     (FMVVM-V1)
                              Getting big? Split logic into delegates inside it (FMVVM-V2)
Never activityViewModel().                                                      (FMVVM-V6)
```

### Tree 4 — How does this reach the other screen? (chapter 10)

```
Is it data the app keeps (persisted, cached, or shown on more than one screen)?
├── YES → Write it to the repository; other screens observe it. Send nothing.   (FMVVM-N5)
└── NO  → Is it what the next screen needs in order to open?
          ├── YES → ID / small value in the nav key                             (FMVVM-N1, N2)
          └── NO  → Is it an answer for the screen that opened this one?
                    ├── YES → Fragment result, consumed once by the opener      (FMVVM-N4)
                    └── NO  → Message for the user → app-level message queue    (FMVVM-N6)
                              App-wide condition   → session/app state in a repo (FMVVM-N7)
Never an event bus.                                                             (FMVVM-N8)
```

---

## Rule index

### Layers & naming — ch. 1
| ID | Rule | Example |
|---|---|---|
| L1 | State flows down, events flow up (UDF). | [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt) |
| L2 | Every destination = `XFragment` (stateful: owns VM, navigates) + `XScreen` (stateless composable). | [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt) |
| L3 | MUST NOT pass a ViewModel below the Fragment (or the Activity). | [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt), [MainActivity.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/MainActivity.kt) |
| L4 | MUST NOT hold Android/UI framework objects (`Context`, `Fragment`, `NavController`, `Bundle`…) in a ViewModel (one listed exception). | [TaskListViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListViewModel.kt) |
| L5 | Expose read-only state; keep mutable state `private`. | [Game.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/stateflow/Game.kt) |
| L6 | Collect with `collectAsStateWithLifecycle()` in Compose, `repeatOnLifecycle` outside it. | [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt), [MainActivity.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/MainActivity.kt) |
| L7 | ViewModels never reference other ViewModels. | [CheckoutFlowFragments.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/checkoutflow/CheckoutFlowFragments.kt) |
| L8 | Naming: `XViewModel`, `XUiState`, `XFragment`, `XScreen`, `XKey`; VM functions are intent verbs, UI lambdas are `onXxx`. | [TaskListViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListViewModel.kt) |
| L9 | ViewModel scope is chosen by the Fragment's Koin delegate: `viewModel()` (destination), `ownerProducer = graphOwner<…>()` (nested graph), Activity for the shell only. | [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt), [FragmentNavigation.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/navigation/FragmentNavigation.kt), [AppModules.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/di/AppModules.kt) |

### Fragment hosting — ch. 1
| ID | Rule | Example |
|---|---|---|
| F1 | Compose goes in through `content { }` (`composeScreen { }`): `DisposeOnViewTreeLifecycleDestroyed`. | [FragmentCompose.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/core/ui/FragmentCompose.kt), [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt) |
| F2 | Fragments are stateless shells: no constructor args, no screen state or View references in fields. | [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt) |
| F3 | View-bound work uses `viewLifecycleOwner`; listeners and launchers that outlive the view are registered with the Fragment. | [FragmentCompose.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/core/ui/FragmentCompose.kt), [EditTaskFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/edit/EditTaskFragment.kt) |
| F4 | The Activity is a thin shell: one NavHostFragment, the Kotlin DSL graph, session gate and message host. | [MainActivity.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/MainActivity.kt), [activity_main.xml](src/main/res/layout/activity_main.xml) |

### Consistency vs granularity — ch. 2
| ID | Rule | Example |
|---|---|---|
| C1 | Default: one immutable `XUiState` data class in a `StateFlow`. | [Game.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/stateflow/Game.kt) |
| C2 | Write with `_uiState.update { it.copy(...) }`, never `value = value.copy(...)`. | [Game.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/stateflow/Game.kt) |
| C3 | Fields written by the same event live in the same state unit. | [Game.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/stateflow/Game.kt) |
| C4 | Snapshot-state class only via Tree 2; then C4a–C4d apply. | [LiveGame.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/snapshot/LiveGame.kt) |
| C5 | One ViewModel = one style. Don't mix `StateFlow` UI state and `mutableStateOf` UI state (one listed exception). | [Game.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/stateflow/Game.kt), [SignUp.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/signup/SignUp.kt) |
| C6 | MUST NOT split a UiState into per-field flows to "gain granularity". | [Game.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/stateflow/Game.kt) |
| C7 | Compute new values into locals first, then write. | [Game.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/stateflow/Game.kt), [LiveGame.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/snapshot/LiveGame.kt) |
| C8 | StateFlow style: pass children only the fields they read. Snapshot style: pass the state object to defer reads. | [Game.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/stateflow/Game.kt), [LiveGame.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/snapshot/LiveGame.kt) |

### Where state lives — ch. 3
| ID | Rule | Example |
|---|---|---|
| S1 | Business-relevant state → ViewModel. | [Orders.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/orders/Orders.kt) |
| S2 | Pure UI element state → `remember`/`rememberSaveable`. | [UiElementState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/uistate/UiElementState.kt) |
| S3 | Non-trivial UI logic → plain state holder class + `rememberXState()`. | [UiElementState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/uistate/UiElementState.kt) |
| S4 | Hoist to the lowest common ancestor, no higher. | [UiElementState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/uistate/UiElementState.kt) |
| S5 | User-visible choices that must survive rotation/process death → `rememberSaveable` or `SavedStateHandle`, never a Fragment field. | [UiElementState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/uistate/UiElementState.kt) |
| S6 | The moment business logic needs a UI value, move it to the VM. | [Orders.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/orders/Orders.kt) |
| S7 | Single source of truth — never mirror the same state in VM and UI. | [EditTaskViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/edit/EditTaskViewModel.kt) |

### Splitting ViewModels — ch. 4
| ID | Rule | Example |
|---|---|---|
| V1 | Default: one ViewModel per destination (Fragment). | [Store.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/store/Store.kt) |
| V2 | Large VM → split into plain delegate classes owned by the VM, not more VMs. | [PromoCodeController.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/checkout/PromoCodeController.kt), [Checkout.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/checkout/Checkout.kt) |
| V3 | Extra VM on one screen only for a self-contained, reused section, obtained in the Fragment. | [CartBadge.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/cartbadge/CartBadge.kt), [Store.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/store/Store.kt) |
| V4 | Cross-screen shared data → repository as source of truth, never VM-to-VM. | [Store.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/data/store/Store.kt), [CartBadge.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/cartbadge/CartBadge.kt) |
| V5 | Multi-step flow with in-progress data → nested graph; the flow VM is scoped to the graph's back-stack entry. | [CheckoutFlowFragments.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/checkoutflow/CheckoutFlowFragments.kt), [AppNavGraph.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/navigation/AppNavGraph.kt) |
| V6 | MUST NOT use `activityViewModel()` as a global store; the shell's VM is the only Activity-scoped one. | [AppViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/shell/AppViewModel.kt) |
| V7 | Dialog with its own logic → `DialogFragment` destination with its own VM; otherwise Compose dialog state in the parent. | [RenameTask.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/rename/RenameTask.kt), [TaskListContent.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/components/TaskListContent.kt) |

### Modeling UI state — ch. 5
| ID | Rule | Example |
|---|---|---|
| M1 | Mutually exclusive phases → sealed interface. Overlays → flags inside the content state. | [TaskListUiState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListUiState.kt) |
| M2 | Don't store derived values; compute them. | [TaskListUiState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListUiState.kt), [Checkout.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/checkout/Checkout.kt) |
| M3 | Collections in UiState are read-only `List`/`Map`, never mutated after creation. | [TaskListUiState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListUiState.kt), [InMemoryTaskRepository.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/data/task/InMemoryTaskRepository.kt) |
| M4 | Lazy list items MUST have a stable `key`. | [TaskListContent.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/components/TaskListContent.kt) |
| M5 | ViewModels don't resolve strings; expose `UiText`/`@StringRes`/raw data. | [UiText.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/core/UiText.kt), [UiTextExt.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/core/ui/UiTextExt.kt) |
| M6 | Map domain → UI models in the VM when the shape differs. | [TaskListUiState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListUiState.kt) |
| M7 | UiState always has a meaningful initial value; never `StateFlow<XUiState?>`. | [TaskListUiState.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListUiState.kt) |
| M8 | No UI types (`Color`, `Dp`, `Painter`, composables) in UiState. | [TaskListContent.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/components/TaskListContent.kt) |

### Producing state — ch. 6
| ID | Rule | Example |
|---|---|---|
| P1 | Streams → `.stateIn(viewModelScope, WhileSubscribed(5_000), initial)`. | [Coroutines.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/core/Coroutines.kt) |
| P2 | User-driven inputs → private `MutableStateFlow`s combined with streams. | [TaskListViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListViewModel.kt) |
| P3 | No work in `init {}`. | [Profile.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/profile/Profile.kt) |
| P4 | Public VM functions are non-suspend, main-safe, fire-and-forget. | [TaskListViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListViewModel.kt) |
| P5 | Nav args via constructor, decoded with `SavedStateHandle.toRoute()` in the Koin definition; `SavedStateHandle` itself only for user input. | [TaskDetail.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/detail/TaskDetail.kt), [AppModules.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/di/AppModules.kt) |
| P6 | Paging: separate `Flow<PagingData>` with `cachedIn(viewModelScope)` (explicit exception to C1). | [Feed.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/feed/Feed.kt) |
| P7 | Errors are state; catch them in the flow. | [TaskListViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListViewModel.kt), [Report.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/report/Report.kt) |
| P8 | Inject dispatchers; never hardcode `Dispatchers.IO`. | [Report.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/report/Report.kt), [AppModules.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/di/AppModules.kt) |

### Events — ch. 7
| ID | Rule | Example |
|---|---|---|
| E1 | UI-only events (no business logic) are handled in the UI. | [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt) |
| E2 | Actions needing logic → VM function. | [NoteEditor.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/editor/NoteEditor.kt) |
| E3 | VM → UI one-off outcomes are state + a "consumed" callback. | [EditTaskFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/edit/EditTaskFragment.kt), [EditTaskViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/edit/EditTaskViewModel.kt) |
| E4 | MUST NOT use `SharedFlow`/`Channel` for VM → UI events. | [EditTaskViewModel.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/edit/EditTaskViewModel.kt) |
| E5 | Navigation executes in the Fragment (NavController looked up when navigating), never in the VM. | [EditTaskFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/edit/EditTaskFragment.kt), [TaskListFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragment.kt) |
| E6 | State-triggered effects live in the Fragment, keyed on the outcome; launchers are Fragment properties. | [EditTaskFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/edit/EditTaskFragment.kt) |

### Input & high frequency — ch. 8
| ID | Rule | Example |
|---|---|---|
| I1 | Text input owned by the VM uses `TextFieldState`, never a `StateFlow` round-trip. | [SignUp.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/signup/SignUp.kt) |
| I2 | Per-frame gesture/animation values never go to the VM; commit the final value only. | [Volume.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/volume/Volume.kt) |
| I3 | Read fast-changing values in layout/draw phase via lambda modifiers. | [LiveGame.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/snapshot/LiveGame.kt) |
| I4 | Debounce/throttle in the VM, not in the UI. | [Search.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/search/Search.kt) |

### Testing — ch. 9
| ID | Rule | Example |
|---|---|---|
| T1 | VM tests use fakes + `MainDispatcherRule`. | [MainDispatcherRule.kt](src/test/java/com/rsalanga/knowledgebase/fragment/mvvm/testing/MainDispatcherRule.kt), [Fakes.kt](src/test/java/com/rsalanga/knowledgebase/fragment/mvvm/testing/Fakes.kt) |
| T2 | `WhileSubscribed` flows need a collector in `backgroundScope`. | [TaskListViewModelTest.kt](src/test/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListViewModelTest.kt) |
| T3 | Snapshot-style VMs are asserted by reading properties directly. | [GameViewModelTests.kt](src/test/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/game/GameViewModelTests.kt) |
| T4 | UI tests target `XScreen` with fake state, no Fragment or ViewModel. | [TaskListScreenTest.kt](src/androidTest/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListScreenTest.kt), [TaskListPreviews.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListPreviews.kt) |
| T5 | Fragment tests cover navigation wiring only: `launchFragmentInContainer` + `TestNavHostController` with the real graph. | [TaskListFragmentNavigationTest.kt](src/androidTest/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/list/TaskListFragmentNavigationTest.kt), [TestNavigation.kt](src/androidTest/java/com/rsalanga/knowledgebase/fragment/mvvm/testing/TestNavigation.kt) |

### Between screens — ch. 10
| ID | Rule | Example |
|---|---|---|
| N1 | Nav keys (`@Serializable` routes) carry IDs and small parameters, never domain/UI objects or lists. | [Keys.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/navigation/Keys.kt) |
| N2 | Key → `toRoute()` in the Koin definition → VM constructor; the Fragment and Screen never read the key. | [AppModules.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/di/AppModules.kt), [LabelPicker.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/labels/LabelPicker.kt) |
| N3 | No side channels for forward data (static holders, `activityViewModel()`, Fragment constructor args, hand-built Bundles). | [AppNavGraph.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/navigation/AppNavGraph.kt) |
| N4 | Results go through the Fragment Result API with explicit request keys; the opener's listener (in `onCreate`) forwards to a VM intent. | [LabelPicker.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/labels/LabelPicker.kt), [EditTaskFragment.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/edit/EditTaskFragment.kt), [Keys.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/navigation/Keys.kt) |
| N5 | A data change is not an event: other screens observe the repository; no "refresh" pings. | [TaskDetail.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/tasks/detail/TaskDetail.kt), [BetweenScreensTest.kt](src/test/java/com/rsalanga/knowledgebase/fragment/mvvm/feature/BetweenScreensTest.kt) |
| N6 | Messages that outlive a screen go into one app-level queue (state + `shown(id)`), shown by the Activity. | [UserMessages.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/data/messages/UserMessages.kt), [AppMessageHost.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/shell/AppMessageHost.kt) |
| N7 | App-wide conditions (logout, session expired) are repository state; only the Activity (NavController owner) reacts. | [MainActivity.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/MainActivity.kt), [SessionNavigation.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/navigation/SessionNavigation.kt) |
| N8 | MUST NOT use an event bus between screens or VMs (only exception: the Fragment Result API per N4). | [UserMessages.kt](src/main/java/com/rsalanga/knowledgebase/fragment/mvvm/data/messages/UserMessages.kt) |
