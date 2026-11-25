package com.rsalanga.knowledgebase.fragment.mvvm.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.createGraph
import androidx.navigation.fragment.dialog
import androidx.navigation.fragment.fragment
import androidx.navigation.navigation
import com.rsalanga.knowledgebase.fragment.mvvm.catalog.CatalogFragment
import com.rsalanga.knowledgebase.fragment.mvvm.catalog.ChapterFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.account.AccountFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.account.SignInFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.checkout.CheckoutFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.checkoutflow.AddressStepFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.checkoutflow.PaymentStepFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.checkoutflow.ReviewStepFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.editor.NoteEditorFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.feed.FeedFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.game.snapshot.LiveGameFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.game.stateflow.GameFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.labels.LabelPickerFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.orders.OrdersFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.profile.ProfileFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.report.ReportFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.search.SearchFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.signup.SignUpFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.store.StoreFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.detail.TaskDetailFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.edit.EditTaskFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list.TaskListFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.rename.RenameTaskDialogFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.uistate.UiElementStateFragment
import com.rsalanga.knowledgebase.fragment.mvvm.feature.volume.VolumeFragment

/**
 * The app's navigation graph, in the Kotlin DSL with type-safe `@Serializable` keys (FMVVM-N1).
 * One key type ↔ one destination ↔ one Fragment (FMVVM-L2).
 *
 * The graph only *declares* destinations. Navigating is done by the Fragment that owns the
 * outcome, through `findNavController()` (FMVVM-E5). Nothing is handed between destinations
 * except keys (FMVVM-N2/N3) and Fragment results (FMVVM-N4).
 *
 * A function of [NavController] (not a top-level `val`) so tests can build the same graph on a
 * `TestNavHostController` (FMVVM-T5).
 */
fun NavController.buildAppGraph(): NavGraph = createGraph(startDestination = CatalogKey) {
    fragment<CatalogFragment, CatalogKey>()
    fragment<ChapterFragment, ChapterKey>()

    // ---- Tasks ----------------------------------------------------------------------------
    fragment<TaskListFragment, TaskListKey>()
    fragment<TaskDetailFragment, TaskDetailKey>()
    fragment<EditTaskFragment, EditTaskKey>()
    fragment<LabelPickerFragment, LabelPickerKey>()
    // FMVVM-V7: a dialog destination is a DialogFragment with its own ViewModel.
    dialog<RenameTaskDialogFragment, RenameTaskKey>()

    // ---- Chapters 2–4 ---------------------------------------------------------------------
    fragment<GameFragment, GameKey>()
    fragment<LiveGameFragment, LiveGameKey>()
    fragment<UiElementStateFragment, UiElementStateKey>()
    fragment<OrdersFragment, OrdersKey>()
    fragment<StoreFragment, StoreKey>()
    fragment<CheckoutFragment, CheckoutKey>()
    // FMVVM-V5: a multi-step flow is a nested graph. Its back-stack entry owns the flow VM and
    // is popped (clearing the VM) when the user leaves the flow.
    navigation<CheckoutFlowKey>(startDestination = AddressStepKey) {
        fragment<AddressStepFragment, AddressStepKey>()
        fragment<PaymentStepFragment, PaymentStepKey>()
        fragment<ReviewStepFragment, ReviewStepKey>()
    }

    // ---- Chapters 6–8 ---------------------------------------------------------------------
    fragment<ProfileFragment, ProfileKey>()
    fragment<FeedFragment, FeedKey>()
    fragment<ReportFragment, ReportKey>()
    fragment<NoteEditorFragment, NoteEditorKey>()
    fragment<SignUpFragment, SignUpKey>()
    fragment<SearchFragment, SearchKey>()
    fragment<VolumeFragment, VolumeKey>()

    // ---- Chapter 10 -----------------------------------------------------------------------
    fragment<AccountFragment, AccountKey>()
    fragment<SignInFragment, SignInKey>()
}
