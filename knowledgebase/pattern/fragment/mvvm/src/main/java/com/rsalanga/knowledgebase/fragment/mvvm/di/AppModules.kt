package com.rsalanga.knowledgebase.fragment.mvvm.di

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.rsalanga.knowledgebase.fragment.mvvm.catalog.ChapterViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.core.AppDispatchers
import com.rsalanga.knowledgebase.fragment.mvvm.core.ApplicationScope
import com.rsalanga.knowledgebase.fragment.mvvm.data.AuthRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.InMemoryAuthRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.InMemoryOrdersRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.InMemoryProfileRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.InMemoryReportRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.InMemorySearchRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.InMemorySettingsRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.OrdersRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.ProfileRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.ReportRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.SearchRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.SettingsRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.feed.FeedRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.feed.InMemoryFeedRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.InMemoryLabelRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.LabelRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.fragment.mvvm.data.session.InMemorySessionRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.session.SessionRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.session.SyncService
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.CartRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.InMemoryCartRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.InMemoryProductRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.InMemoryPromoRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.ProductRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.PromoRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.InMemoryTaskRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskRepository
import com.rsalanga.knowledgebase.fragment.mvvm.feature.account.AccountViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.account.SignInViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.cartbadge.CartBadgeViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.checkout.CheckoutViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.checkoutflow.CheckoutFlowViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.checkoutflow.PaymentViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.editor.NoteEditorViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.feed.FeedViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.game.snapshot.LiveGameViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.game.stateflow.GameViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.labels.LabelPickerViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.orders.OrdersViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.profile.ProfileViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.report.ReportViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.search.SearchViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.signup.SignUpViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.store.StoreViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.detail.TaskDetailViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.edit.EditTaskViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list.TaskListViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.rename.RenameTaskViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.feature.volume.VolumeViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.ChapterKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.EditTaskKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.LabelPickerKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.RenameTaskKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.TaskDetailKey
import com.rsalanga.knowledgebase.fragment.mvvm.shell.AppViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.core.scope.Scope
import org.koin.dsl.module

/**
 * Repositories are singletons: they are the shared source of truth (FMVVM-V4). In-memory data
 * lives in a `MutableStateFlow` inside them.
 */
val dataModule = module {
    // FMVVM-P8: the only place in the app that names real dispatchers.
    single { AppDispatchers(default = Dispatchers.Default, io = Dispatchers.IO) }
    single { ApplicationScope(get()) }
    single { UserMessages() }
    single { SyncService(get(), get()) }

    single<TaskRepository> { InMemoryTaskRepository() }
    single<LabelRepository> { InMemoryLabelRepository() }
    single<ProductRepository> { InMemoryProductRepository() }
    single<CartRepository> { InMemoryCartRepository(get()) }
    single<PromoRepository> { InMemoryPromoRepository() }
    single<ProfileRepository> { InMemoryProfileRepository() }
    single<FeedRepository> { InMemoryFeedRepository() }
    single<OrdersRepository> { InMemoryOrdersRepository() }
    single<SearchRepository> { InMemorySearchRepository() }
    single<AuthRepository> { InMemoryAuthRepository() }
    single<ReportRepository> { InMemoryReportRepository() }
    single<SettingsRepository> { InMemorySettingsRepository() }
    single<SessionRepository> { InMemorySessionRepository(get()) }
}

/**
 * FMVVM-P5 / N2: VMs that need a nav argument take it as the first constructor parameter, as a
 * plain value. The definition decodes the destination's key with `navArgs<XKey>()`, so the VM
 * never sees Navigation and its tests construct it with the value directly (FMVVM-T1).
 * `SavedStateHandle` itself is passed only for user input.
 *
 * FMVVM-L9: the definitions don't decide the scope; the Fragment's delegate does
 * (`viewModel()` = this destination, `viewModel(ownerProducer = graphOwner<…>())` = a nested
 * graph, `viewModel()` in MainActivity = the Activity).
 */
val viewModelModule = module {
    viewModel { AppViewModel(get(), get()) }

    viewModel { ChapterViewModel(navArgs<ChapterKey>().number) }

    viewModel { TaskListViewModel(get(), get(), get()) }
    viewModel { TaskDetailViewModel(navArgs<TaskDetailKey>().taskId, get(), get()) }
    viewModel { EditTaskViewModel(navArgs<EditTaskKey>().taskId, get(), get(), get(), get()) }
    viewModel { RenameTaskViewModel(navArgs<RenameTaskKey>().taskId, get()) }
    viewModel { LabelPickerViewModel(navArgs<LabelPickerKey>().selectedLabelId, get()) }

    viewModel { GameViewModel() }
    viewModel { LiveGameViewModel() }
    viewModel { OrdersViewModel(get(), get()) }

    viewModel { StoreViewModel(get(), get()) }
    viewModel { CartBadgeViewModel(get()) }
    viewModel { CheckoutViewModel(get(), get()) }
    viewModel { CheckoutFlowViewModel(get(), get()) }
    viewModel { PaymentViewModel() }

    viewModel { ProfileViewModel(get()) }
    viewModel { FeedViewModel(get()) }
    viewModel { ReportViewModel(get(), get()) }

    viewModel { NoteEditorViewModel(get()) }
    viewModel { SignUpViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { VolumeViewModel(get()) }

    viewModel { AccountViewModel(get(), get()) }
    viewModel { SignInViewModel(get()) }
}

/**
 * FMVVM-N2 / P5: the destination's nav key, decoded from the ViewModel's own SavedStateHandle.
 * A Fragment's default SavedStateHandle starts with the Fragment's arguments, which are exactly
 * the key's fields, so this is the documented `SavedStateHandle.toRoute()` and never the topmost
 * entry of that type (wrong when a destination is on the back stack twice).
 */
private inline fun <reified K : Any> Scope.navArgs(): K = get<SavedStateHandle>().toRoute<K>()
