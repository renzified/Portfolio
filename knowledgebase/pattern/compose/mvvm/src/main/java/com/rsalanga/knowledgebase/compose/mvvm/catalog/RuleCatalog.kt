package com.rsalanga.knowledgebase.compose.mvvm.catalog

import androidx.navigation3.runtime.NavKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.AccountKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.CheckoutFlowKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.CheckoutKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.EditTaskKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.FeedKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.GameKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.LiveGameKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.NoteEditorKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.OrdersKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.ProfileKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.ReportKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.SearchKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.SignUpKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.StoreKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.TaskListKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.UiElementStateKey
import com.rsalanga.knowledgebase.compose.mvvm.navigation.VolumeKey

/**
 * One demo = one destination (or, for chapter 9, a test file). [key] is null when there is
 * nothing to open on the device.
 */
data class Demo(
    val title: String,
    val rules: String,
    val description: String,
    val key: NavKey? = null
)

data class Chapter(val number: Int, val title: String, val demos: List<Demo>)

/**
 * The catalog shown on the home screen. Static data, so the catalog screens have no ViewModel
 * (MVVM-S2 / E1). The README rule index links every rule ID to the file that implements it.
 */
val RuleCatalog: List<Chapter> = listOf(
    Chapter(
        number = 1,
        title = "Layers and naming",
        demos = listOf(
            Demo(
                title = "Tasks",
                rules = "L1 L2 L3 L5 L6 L8",
                description = "TaskListRoute owns the VM and collects state; TaskListScreen is " +
                    "stateless and only gets state + lambdas.",
                key = TaskListKey
            ),
            Demo(
                title = "Task detail (Nav3 scoping)",
                rules = "L4 L7 L9",
                description = "Open any task: its VM is scoped to its back-stack entry by " +
                    "rememberViewModelStoreNavEntryDecorator() in AppNavDisplay.",
                key = TaskListKey
            )
        )
    ),
    Chapter(
        number = 2,
        title = "Consistency vs granularity",
        demos = listOf(
            Demo(
                title = "Game — StateFlow<data class>",
                rules = "C1 C2 C3 C5 C6 C7 C8",
                description = "Default style. Children get only the fields they read.",
                key = GameKey
            ),
            Demo(
                title = "Game — snapshot-state class",
                rules = "C4 C4a C4b C4c C4d C8 I3",
                description = "High-frequency ticker. Children get the state object and read " +
                    "inside; the progress bar reads in the draw phase.",
                key = LiveGameKey
            )
        )
    ),
    Chapter(
        number = 3,
        title = "Where state lives",
        demos = listOf(
            Demo(
                title = "UI element state",
                rules = "S2 S3 S4 S5",
                description = "Password visibility, FAQ expansion (single and hoisted), and a " +
                    "scroll-to-top state holder. No ViewModel.",
                key = UiElementStateKey
            ),
            Demo(
                title = "Orders tab",
                rules = "S1 S6 P5",
                description = "The selected tab drives the query, so the VM owns it in " +
                    "SavedStateHandle.",
                key = OrdersKey
            ),
            Demo(
                title = "Edit task draft",
                rules = "S7",
                description = "The title draft is a VM-owned TextFieldState, not a remember " +
                    "copy of VM state.",
                key = EditTaskKey(taskId = 2L)
            )
        )
    ),
    Chapter(
        number = 4,
        title = "Splitting ViewModels",
        demos = listOf(
            Demo(
                title = "Store + cart badge",
                rules = "V1 V3 V4",
                description = "Two VMs in one Route: the screen's own and a reusable cart " +
                    "badge. Both observe CartRepository.",
                key = StoreKey
            ),
            Demo(
                title = "Checkout with promo delegate",
                rules = "V2 V3",
                description = "PromoCodeController is a plain class inside CheckoutViewModel.",
                key = CheckoutKey
            ),
            Demo(
                title = "Checkout wizard",
                rules = "V5",
                description = "One flow entry owns the shared VM and an inner back stack.",
                key = CheckoutFlowKey
            ),
            Demo(
                title = "Dialogs (rename / confirm delete)",
                rules = "V7",
                description = "In Tasks: Rename is a dialog destination with its own VM; " +
                    "Delete confirmation is UI state with the result sent to the list VM.",
                key = TaskListKey
            ),
            Demo(
                title = "App shell ViewModel",
                rules = "V6",
                description = "AppViewModel is the only Activity-scoped VM: session gate and " +
                    "message host, never screen data. See shell/AppViewModel.kt."
            )
        )
    ),
    Chapter(
        number = 5,
        title = "Modeling UI state",
        demos = listOf(
            Demo(
                title = "Tasks",
                rules = "M1 M2 M3 M4 M5 M6 M7 M8",
                description = "Sealed Loading/Error/Content, derived isEmpty, keyed lazy list, " +
                    "UiText messages, TaskUi mapping, priority colour chosen in the UI.",
                key = TaskListKey
            ),
            Demo(
                title = "Checkout totals",
                rules = "M2",
                description = "Subtotal and total are computed properties, never stored.",
                key = CheckoutKey
            )
        )
    ),
    Chapter(
        number = 6,
        title = "Producing state",
        demos = listOf(
            Demo(
                title = "Tasks",
                rules = "P1 P2 P4 P7",
                description = "Repository stream + filter/refresh/message inputs combined with " +
                    "stateIn(WhileUiSubscribed). Errors become state.",
                key = TaskListKey
            ),
            Demo(
                title = "Profile with retry",
                rules = "P3",
                description = "No init {}: the load is a cold flow with a retry trigger. The " +
                    "first load fails on purpose.",
                key = ProfileKey
            ),
            Demo(
                title = "Task detail",
                rules = "P5",
                description = "The task id comes from the key into the VM constructor.",
                key = TaskListKey
            ),
            Demo(
                title = "Paged feed",
                rules = "P6",
                description = "Flow<PagingData> next to the UiState, cachedIn(viewModelScope).",
                key = FeedKey
            ),
            Demo(
                title = "Report export",
                rules = "P7 P8",
                description = "CSV built on an injected default dispatcher.",
                key = ReportKey
            )
        )
    ),
    Chapter(
        number = 7,
        title = "Events",
        demos = listOf(
            Demo(
                title = "Tasks",
                rules = "E1",
                description = "Row click navigates without touching the VM.",
                key = TaskListKey
            ),
            Demo(
                title = "Note editor",
                rules = "E2",
                description = "Seven actions grouped in a stable NoteEditorActions holder.",
                key = NoteEditorKey
            ),
            Demo(
                title = "Edit task",
                rules = "E3 E4 E5 E6",
                description = "Save → navigateBack outcome; errors → userMessage; camera " +
                    "permission requested by the Route and reported back to the VM.",
                key = EditTaskKey(taskId = null)
            )
        )
    ),
    Chapter(
        number = 8,
        title = "Input and high frequency",
        demos = listOf(
            Demo(
                title = "Sign up",
                rules = "I1",
                description = "VM-owned TextFieldState; validation derived with snapshotFlow.",
                key = SignUpKey
            ),
            Demo(
                title = "Volume slider",
                rules = "I2",
                description = "Drag stays local; only the final value is committed to the VM.",
                key = VolumeKey
            ),
            Demo(
                title = "Live game progress bar",
                rules = "I3",
                description = "Progress read inside drawBehind via a provider lambda.",
                key = LiveGameKey
            ),
            Demo(
                title = "Search",
                rules = "I4",
                description = "debounce + flatMapLatest in the VM. Type \"error\" to fail.",
                key = SearchKey
            )
        )
    ),
    Chapter(
        number = 9,
        title = "Testing",
        demos = listOf(
            Demo(
                title = "Fakes + MainDispatcherRule",
                rules = "T1",
                description = "src/test/…/testing/MainDispatcherRule.kt and testing/Fakes.kt"
            ),
            Demo(
                title = "Collector in backgroundScope",
                rules = "T2",
                description = "src/test/…/feature/tasks/list/TaskListViewModelTest.kt"
            ),
            Demo(
                title = "Snapshot-style VM",
                rules = "T3",
                description = "src/test/…/feature/game/GameViewModelTests.kt"
            ),
            Demo(
                title = "Stateless Screen UI test + previews",
                rules = "T4",
                description = "src/androidTest/…/feature/tasks/list/TaskListScreenTest.kt and " +
                    "TaskListPreviews.kt"
            )
        )
    ),
    Chapter(
        number = 10,
        title = "Between screens",
        demos = listOf(
            Demo(
                title = "Task list → detail (two panes on wide screens)",
                rules = "N1 N2 N3 N5 N9",
                description = "Keys carry ids. Edits on any screen reach the list through the " +
                    "repository. On tablets, selection is the detail key on the back stack.",
                key = TaskListKey
            ),
            Demo(
                title = "Pick a label",
                rules = "N4",
                description = "Edit task → Pick label. The result goes through ResultEventBus " +
                    "in the entry blocks and is forwarded to the VM once.",
                key = EditTaskKey(taskId = 1L)
            ),
            Demo(
                title = "App messages, session, sync",
                rules = "N6 N7 N8",
                description = "Sync in the background, sign out, or expire the session. The " +
                    "shell shows messages and rewrites the back stack.",
                key = AccountKey
            )
        )
    )
)
