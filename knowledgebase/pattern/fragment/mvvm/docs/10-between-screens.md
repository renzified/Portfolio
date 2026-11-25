# 10. Between screens — passing data and notifying events

Chapters 1–9 are about one destination. This chapter is about what crosses the boundary between
destinations: the input a screen opens with, the answer it gives back, and things that happen on
one screen that other screens need to know about.

## Tree 4 — How does this reach the other screen?

Run this whenever a value or an "it happened" has to leave the screen that produced it.

```
Is it data the app keeps (persisted, cached, or shown on more than one screen)?
├── YES → Write it to the repository; other screens observe it. Send nothing.   (FMVVM-N5, V4)
└── NO  → Is it what the next screen needs in order to open?
          ├── YES → Put the ID / small value in the nav key                      (FMVVM-N1, N2)
          └── NO  → Is it an answer for the screen that opened this one
                    (picker choice, confirm result, "deleted, offer undo")?
                    ├── YES → Fragment result, consumed once by the opener       (FMVVM-N4)
                    └── NO  → Must it outlive the current screen, or does it come from
                              outside any screen (sync, worker, session)?
                              ├── Message for the user → App-level message queue (FMVVM-N6)
                              └── App-wide condition   → Session/app state in a repository (FMVVM-N7)

Shared, not-yet-saved draft across the steps of one flow? → Nested graph + flow ViewModel (FMVVM-V5), not this chapter.
```

There is **no branch that ends in an event bus** (FMVVM-N8).

---

## Passing data forward

### FMVVM-N1 — Keys carry IDs and small parameters, never data (MUST)
A nav key is a `@Serializable` class. The Navigation component writes its fields into the
destination's **arguments Bundle**, which is saved with the back stack. Whatever you put in it is
stored in saved state, frozen at the moment you navigated, and becomes a second copy of the data
(S7). Large arguments also count toward the Binder limit and can crash with
`TransactionTooLargeException` when the Activity saves state.

| Allowed in a key | Not allowed in a key |
|---|---|
| IDs (`taskId: Long`) and other primitives, `String`, enums | Domain objects (`Task`), UI models (`TaskUi`), entities/DTOs |
| Small parameters that configure the screen: mode enum, pre-selected filter, initial query, `selectedLabelId` for a picker | Lists, bitmaps, byte arrays, anything unbounded |
| Not-yet-saved values that are **small** and **are the input** (a search query typed on the previous screen) | Data the destination can load by ID; `Parcelable`s that need a custom `NavType` |

```kotlin
// ❌ Stale snapshot of the task; bloats saved state; needs a custom NavType; a second source of truth
@Serializable data class TaskDetailKey(val task: TaskUi)

// ✅ The destination loads the current task by ID and observes it
@Serializable data class TaskDetailKey(val taskId: Long)
```

Creating something and then opening it: the repository saves and returns the new ID, the VM
exposes it as an outcome (`createdTaskId = id`, E3), the Fragment navigates with that ID.

> Why not "just pass the object to skip a DB read": the detail screen must observe the repository
> anyway (the task can change while it's open, V4). The read is not saved, only duplicated.

### FMVVM-N2 — Key → SavedStateHandle → VM constructor; nothing else reads the key (MUST)
- Keys are `@Serializable` `data class`/`data object`s named `XKey`, with only `val`s, declared in
  `navigation/Keys.kt`. One key type per destination, registered once in `AppNavGraph.kt`
  (`fragment<XFragment, XKey>()`).
- The Koin definition decodes the key from the VM's `SavedStateHandle` with
  `toRoute<XKey>()` and passes its values to the VM constructor (P5).
- The Fragment does not read `arguments`, does not call `getBackStackEntry<XKey>()` for its own
  key, and does not pass key values to the Screen. If the Screen needs one, it comes from the VM's
  state (`LabelPickerUiState.selectedLabelId`).
- `XScreen` and its children never receive a key.
- The VM never calls `vm.load(id)` after creation (E5).

### FMVVM-N3 — No side channels for forward data (MUST NOT)
All of these skip the key, so the data is gone after process death and the destination
depends on whatever the previous screen did before it:

```kotlin
// ❌ Static/singleton "holder" set right before navigating
SelectedTask.current = task; findNavController().navigate(TaskDetailKey(task.id))
// ❌ Shared Activity-scoped VM used as a handoff (V6)
activityViewModel<SharedViewModel>().selected = task
// ❌ Constructor arguments or setters on the Fragment (lost on recreation, F2)
TaskDetailFragment(task)
// ❌ Building the arguments Bundle by hand, or navigating by destination id + bundleOf(...)
findNavController().navigate(R.id.detail, bundleOf("task" to task))
// ❌ setTargetFragment / reaching into parentFragmentManager.fragments to find the next screen
// ❌ Writing into the next destination's SavedStateHandle
```

If the data is too big for a key, it belongs in the data layer: save it (or put it in an in-memory
repository) and pass the ID.

---

## Returning a result to the opener

### FMVVM-N4 — Results go through the Fragment Result API and are consumed once (MUST)
Use this for answers that are **not data the app keeps**: the item chosen in a picker, the answer
to a confirm dialog destination, "task 42 was deleted, offer undo".

1. The **result Fragment** doesn't know who opened it. It calls
   `setFragmentResult(REQUEST_KEY, bundleOf(…))`, then pops itself.
2. The **opener Fragment** registers `setFragmentResultListener(REQUEST_KEY)` (the fragment-ktx
   extension, which listens on the shared parent FragmentManager) in **`onCreate`**, once per
   Fragment instead of once per view (F3).
3. The listener forwards the payload to a VM intent (`viewModel.selectLabel(id)`) and does
   nothing else.
4. The VM treats it as normal input: writes it into its state, or into `SavedStateHandle` if the
   user would miss it after process death (S5).

```kotlin
// navigation/Keys.kt — the request key is an explicit constant, declared next to the result screen's key
@Serializable data class LabelPickerKey(val selectedLabelId: Long?)
const val LABEL_PICKED_RESULT = "label_picked"
const val LABEL_PICKED_ID = "labelId"                       // payload: Long

// The result screen — steps 1
class LabelPickerFragment : Fragment() {
    private fun pick(labelId: Long) {
        setFragmentResult(LABEL_PICKED_RESULT, bundleOf(LABEL_PICKED_ID to labelId))
        findNavController().popBackStack()
    }
}

// The opener — steps 2 and 3
class EditTaskFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(LABEL_PICKED_RESULT) { _, bundle ->
            viewModel.selectLabel(bundle.getLong(LABEL_PICKED_ID))
        }
    }
}
```

Why the Fragment Result API:
- Every destination of one `NavHostFragment` (including `dialog<…>` destinations) shares its child
  FragmentManager, so result and opener meet there without knowing each other.
- A result is held until the opener is **STARTED**, delivered **once**, then cleared. It survives
  rotation and process death while it waits. That is the "consumed once" guarantee, with no
  `removeResult` step to forget.
- Payloads follow N1: IDs and small values only. The VM never sees the result mechanism. Two VMs
  never talk to each other (L7).

Notes:
- One request key per kind of answer. Two pickers that both return a `Long` get two keys.
- Only the latest result per key is kept. That is right for "the user's choice"; if every value
  matters, it is data, and it goes in a repository (N5).
- Use one mechanism for the codebase. The Navigation component also supports results through
  `previousBackStackEntry?.savedStateHandle`; this rulebook does not, because a handle value isn't
  cleared after it is read and so needs a manual "consumed" step.
- Result keys and payload keys are strings in a `Bundle`, which is not type-safe. Keep both
  constants next to the result screen's nav key, and read the payload in exactly one place.

---

## Notifying other screens

### FMVVM-N5 — A data change is not an event (MUST)
When screen B edits, creates or deletes something that screen A shows, A finds out by **observing
the repository** (V4), never by being told.

```kotlin
// ❌ Telling the list to refresh
setFragmentResult("task_changed", bundleOf("id" to taskId))
override fun onResume() { super.onResume(); viewModel.refresh() }     // "refresh on resume"
// ❌ Repository emitting "something changed" pings that VMs react to by reloading
val changes = MutableSharedFlow<Unit>()

// ✅ The list VM already observes repository.observeTasks(); B's write makes it re-emit
```

A Fragment on the back stack keeps its ViewModel, and its `stateIn(WhileSubscribed(5_000))` flow
stops 5 s after the view is destroyed. When the user returns, the new view subscribes again and
gets the repository's current data. No refresh is needed.

If the data layer can't be observed (network-only API), make it observable: cache in the
repository (`MutableStateFlow` or Room) and have writes update the cache. Don't move the problem
into the UI.

### FMVVM-N6 — App-level messages are state in one queue (MUST)
Use this for user-visible messages that must **outlive the screen that caused them**, or that come
from **outside any screen**: "Saved" right before the editor closes, "Sync finished",
"Upload failed".

```kotlin
data class UserMessage(val id: Long, val text: UiText)

class UserMessages {                               // Koin `single`, data/ui-shell layer
    private val _messages = MutableStateFlow<List<UserMessage>>(emptyList())
    val messages: StateFlow<List<UserMessage>> = _messages.asStateFlow()
    private val nextId = AtomicLong()

    fun post(text: UiText) = _messages.update { it + UserMessage(nextId.incrementAndGet(), text) }
    fun shown(id: Long) = _messages.update { list -> list.filterNot { it.id == id } }
}
```

- Screen VMs and workers **post**. One host **shows**: `MainActivity`, in a `ComposeView` layered
  over the `NavHostFragment` (F4). Its `AppViewModel` is Activity-scoped, which is the "truly about
  the Activity" case allowed by V6. It exposes the head of the queue and `messageShown(id)`,
  exactly like E3.
- This is E3 lifted to app scope: messages wait in state until shown, then are removed by id. No
  `SharedFlow`/`Channel` (E4), and no `Snackbar.make(requireActivity().findViewById(…))` from a
  Fragment that is about to be popped.

```xml
<!-- activity_main.xml -->
<FrameLayout ...>
    <androidx.fragment.app.FragmentContainerView android:id="@+id/nav_host"
        android:name="androidx.navigation.fragment.NavHostFragment" app:defaultNavHost="true" ... />
    <androidx.compose.ui.platform.ComposeView android:id="@+id/message_host"
        android:layout_height="wrap_content" android:layout_gravity="bottom" ... />
</FrameLayout>
```

**Screen message (E3) or app message (N6)?**

| The message… | Use |
|---|---|
| is about this screen and only matters while it is visible | `userMessage` in the screen's UiState (E3) |
| is caused by this screen, but the screen closes right after | App queue (N6) |
| answers the screen that opened this one, which then decides what to show | Result (N4); the opener's VM sets its own `userMessage` |
| comes from a worker, sync, or another non-screen source | App queue (N6) |

### FMVVM-N7 — App-wide conditions are state; the NavController's owner reacts (MUST)
Logout, session expired, account switched, forced update, onboarding not done: these are **state**
in a repository (`SessionRepository.session: StateFlow<Session>`), not broadcasts.

- Only the Activity (the owner of the `NavHostFragment`) observes it and rewrites the back stack
  (E5): navigate to `SignInKey` with `popUpTo(graph.id) { inclusive = true }` when the session
  becomes `SignedOut`.
- Fragments don't each listen for "logged out". Their VMs are cleared because their destinations
  are popped (L9).
- Repositories holding user data reset themselves from the same session state, not from a screen.

```kotlin
// MainActivity.onCreate
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {                      // L6, outside Compose
        viewModel.uiState.map { it.session }.distinctUntilChanged().collect(::applySession)
    }
}

private fun applySession(session: Session) {
    val isOnSignIn = navController.currentDestination?.hasRoute<SignInKey>() == true
    val target = sessionRedirect(session, isOnSignIn) ?: return        // pure, unit-tested
    navController.navigate(target) {
        popUpTo(navController.graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
```

Collecting at STARTED matters: a `navigate()` after the Activity has saved its state can throw.
The session state waits, and the collector applies it when the app comes back.

### FMVVM-N8 — No event bus between screens or ViewModels (MUST NOT)
No app-wide `MutableSharedFlow`, `Channel`, `EventBus`, `LiveData` "event" singleton,
`LocalBroadcastManager`, or static listener lists for screen-to-screen communication. They drop
events when nobody is collecting, deliver them twice on re-subscription, hide who depends on whom,
and can't be asserted as state in tests. This is E4 applied across screens.

**Only exception:** the Fragment Result API, used exactly as N4 describes.

> The Compose rulebook's N9 (two panes, selection is the back stack) has no counterpart here:
> this module has no list–detail layout. With Fragments, two panes would be a
> `SlidingPaneLayout` with its own child `NavHostFragment` for the detail pane, and the same idea
> applies: the selection is the detail pane's back stack, not shared VM state.

---

## Testing these

- **N1/N2:** VM tests construct the VM with the ID directly; no navigation needed (T1). The
  Koin definition is the only place the key is decoded.
- **N4:** The VM side is a plain intent (`selectLabel(id)`): test it directly. The result API
  lives only in Fragments.
- **N5:** Write through the fake repository, assert the *other* VM's state updates.
- **N6:** Fake `UserMessages`. Assert the VM posted the expected `UiText`; test the shell VM's
  `messageShown(id)` separately.
- **N7:** Test the session → destination mapping as a pure function
  (`sessionRedirect(session, isOnSignIn)`).
- **Wiring:** that a Fragment navigates with the right key is a Fragment test (T5).

## Summary

| Situation | Do | Rule |
|---|---|---|
| Open detail for an item | `TaskDetailKey(taskId)`; VM loads by ID | N1, N2 |
| "I'll pass the whole object to save a read" | Pass the ID | N1 |
| Data too big for a key | Save it to a repository, pass the ID | N1, N3 |
| Picker returns a choice | `setFragmentResult` + pop; opener's listener forwards to its VM | N4 |
| Detail edited a task the list shows | Nothing to send; list observes repository | N5 |
| "Saved" after the editor closes | App message queue, shown by the Activity | N6 |
| Background sync finished | App message queue | N6 |
| Session expired | Session state; the Activity rewrites the back stack | N7 |
| `activityViewModels()` to hand data to the next screen | Never | N3, V6 |
| Global `SharedFlow<AppEvent>` | Never | N8 |
