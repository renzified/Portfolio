package com.rsalanga.knowledgebase.compose.mvvm.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.rsalanga.knowledgebase.compose.mvvm.catalog.CatalogRoute
import com.rsalanga.knowledgebase.compose.mvvm.catalog.ChapterRoute
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.EmptyState
import com.rsalanga.knowledgebase.compose.mvvm.feature.account.AccountRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.account.SignInRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.checkout.CheckoutRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.checkoutflow.CheckoutFlowRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.editor.NoteEditorRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.feed.FeedRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.game.snapshot.LiveGameRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.game.stateflow.GameRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.labels.LabelPickerRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.orders.OrdersRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.profile.ProfileRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.report.ReportRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.search.SearchRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.signup.SignUpRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.store.StoreRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.detail.TaskDetailRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.edit.EditTaskRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.edit.EditTaskViewModel
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list.TaskListRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.rename.RenameTaskRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.uistate.UiElementStateRoute
import com.rsalanga.knowledgebase.compose.mvvm.feature.volume.VolumeRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The only code that touches the app back stack (MVVM-E5). Routes receive navigation as lambdas.
 *
 * - MVVM-L9: the ViewModel decorator scopes every Route's VM to its own entry.
 * - MVVM-N4: the ResultEventBus decorator gives entries a bus for returning results.
 * - MVVM-N2: entry blocks unpack keys and pass values to VM constructors via `parametersOf`.
 * - MVVM-N3: nothing is handed forward any other way — no holders, no shared VMs.
 * - MVVM-N9: on wide screens the list–detail strategy shows TaskList and TaskDetail side by side.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavDisplay(backStack: NavBackStack<NavKey>, modifier: Modifier = Modifier) {
    val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
    val dialogs = remember { DialogSceneStrategy<NavKey>() }
    val listDetail = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        backStack = backStack,
        onBack = goBack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(), // MVVM-L9 — required
            rememberResultEventBusNavEntryDecorator() // MVVM-N4 — entries exchange results
        ),
        sceneStrategies = listOf(dialogs, listDetail),
        entryProvider = entryProvider {
            entry<CatalogKey> {
                CatalogRoute(onOpenChapter = { number -> backStack.add(ChapterKey(number)) })
            }
            entry<ChapterKey> { key ->
                ChapterRoute(
                    number = key.number,
                    onOpenDemo = { demo -> backStack.add(demo) },
                    onBack = goBack
                )
            }

            // ---- Tasks ----------------------------------------------------------------------

            entry<TaskListKey>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { EmptyState("Pick a task") }
                )
            ) {
                TaskListRoute(
                    // MVVM-N9: the selection IS the detail key on the back stack.
                    selectedTaskId = (backStack.lastOrNull() as? TaskDetailKey)?.taskId,
                    onOpenTask = { id -> backStack.showTaskDetail(id) },
                    onAddTask = { backStack.add(EditTaskKey(taskId = null)) },
                    onRenameTask = { id -> backStack.add(RenameTaskKey(id)) },
                    onBack = goBack
                )
            }
            entry<TaskDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                TaskDetailRoute(
                    onEdit = { backStack.add(EditTaskKey(key.taskId)) },
                    onBack = goBack,
                    // MVVM-P5 / N2: key → constructor.
                    viewModel = koinViewModel { parametersOf(key.taskId) }
                )
            }
            entry<EditTaskKey> { key ->
                val viewModel = koinViewModel<EditTaskViewModel> { parametersOf(key.taskId) }
                // MVVM-N4, step 3: receive in the opener's entry block, forward to a VM intent,
                // then remove so it isn't delivered again when this entry re-enters composition.
                val resultBus = LocalResultEventBus.current
                ResultEffect<Long>(resultKey = LABEL_PICKED_RESULT) { labelId ->
                    viewModel.selectLabel(labelId)
                    resultBus.removeResult(resultKey = LABEL_PICKED_RESULT)
                }
                EditTaskRoute(
                    viewModel = viewModel,
                    onPickLabel = { current -> backStack.add(LabelPickerKey(current)) },
                    onBack = goBack
                )
            }
            entry<LabelPickerKey> { key ->
                val resultBus = LocalResultEventBus.current
                LabelPickerRoute(
                    selectedLabelId = key.selectedLabelId,
                    // MVVM-N4, step 2: the back-stack owner sends the result, then pops.
                    onLabelPicked = { labelId ->
                        resultBus.sendResult(resultKey = LABEL_PICKED_RESULT, result = labelId)
                        backStack.removeLastOrNull()
                    },
                    onBack = goBack
                )
            }
            // MVVM-V7: a dialog destination with its own VM.
            entry<RenameTaskKey>(metadata = DialogSceneStrategy.dialog()) { key ->
                RenameTaskRoute(
                    onDismiss = goBack,
                    viewModel = koinViewModel { parametersOf(key.taskId) }
                )
            }

            // ---- Chapters 2–4 -----------------------------------------------------------------

            entry<GameKey> { GameRoute(onBack = goBack) }
            entry<LiveGameKey> { LiveGameRoute(onBack = goBack) }
            entry<UiElementStateKey> { UiElementStateRoute(onBack = goBack) }
            entry<OrdersKey> { OrdersRoute(onBack = goBack) }
            entry<StoreKey> {
                StoreRoute(onOpenCheckout = { backStack.add(CheckoutKey) }, onBack = goBack)
            }
            entry<CheckoutKey> {
                CheckoutRoute(onContinue = { backStack.add(CheckoutFlowKey) }, onBack = goBack)
            }
            entry<CheckoutFlowKey> { CheckoutFlowRoute(onExit = goBack) }

            // ---- Chapters 6–8 -----------------------------------------------------------------

            entry<ProfileKey> { ProfileRoute(onBack = goBack) }
            entry<FeedKey> { FeedRoute(onBack = goBack) }
            entry<ReportKey> { ReportRoute(onBack = goBack) }
            entry<NoteEditorKey> { NoteEditorRoute(onBack = goBack) }
            entry<SignUpKey> { SignUpRoute(onBack = goBack) }
            entry<SearchKey> { SearchRoute(onBack = goBack) }
            entry<VolumeKey> { VolumeRoute(onBack = goBack) }

            // ---- Chapter 10 -------------------------------------------------------------------

            entry<AccountKey> { AccountRoute(onBack = goBack) }
            entry<SignInKey> { SignInRoute() }
        }
    )
}

/** MVVM-N9: selecting another task replaces the detail key instead of stacking details. */
private fun NavBackStack<NavKey>.showTaskDetail(taskId: Long) {
    if (lastOrNull() is TaskDetailKey) removeLastOrNull()
    add(TaskDetailKey(taskId))
}
