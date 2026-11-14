package com.rsalanga.knowledgebase.mvvm.di

import com.rsalanga.knowledgebase.mvvm.core.AppDispatchers
import com.rsalanga.knowledgebase.mvvm.core.ApplicationScope
import com.rsalanga.knowledgebase.mvvm.data.AuthRepository
import com.rsalanga.knowledgebase.mvvm.data.InMemoryAuthRepository
import com.rsalanga.knowledgebase.mvvm.data.InMemoryOrdersRepository
import com.rsalanga.knowledgebase.mvvm.data.InMemoryProfileRepository
import com.rsalanga.knowledgebase.mvvm.data.InMemoryReportRepository
import com.rsalanga.knowledgebase.mvvm.data.InMemorySearchRepository
import com.rsalanga.knowledgebase.mvvm.data.InMemorySettingsRepository
import com.rsalanga.knowledgebase.mvvm.data.OrdersRepository
import com.rsalanga.knowledgebase.mvvm.data.ProfileRepository
import com.rsalanga.knowledgebase.mvvm.data.ReportRepository
import com.rsalanga.knowledgebase.mvvm.data.SearchRepository
import com.rsalanga.knowledgebase.mvvm.data.SettingsRepository
import com.rsalanga.knowledgebase.mvvm.data.feed.FeedRepository
import com.rsalanga.knowledgebase.mvvm.data.feed.InMemoryFeedRepository
import com.rsalanga.knowledgebase.mvvm.data.label.InMemoryLabelRepository
import com.rsalanga.knowledgebase.mvvm.data.label.LabelRepository
import com.rsalanga.knowledgebase.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.mvvm.data.session.InMemorySessionRepository
import com.rsalanga.knowledgebase.mvvm.data.session.SessionRepository
import com.rsalanga.knowledgebase.mvvm.data.session.SyncService
import com.rsalanga.knowledgebase.mvvm.data.store.CartRepository
import com.rsalanga.knowledgebase.mvvm.data.store.InMemoryCartRepository
import com.rsalanga.knowledgebase.mvvm.data.store.InMemoryProductRepository
import com.rsalanga.knowledgebase.mvvm.data.store.InMemoryPromoRepository
import com.rsalanga.knowledgebase.mvvm.data.store.ProductRepository
import com.rsalanga.knowledgebase.mvvm.data.store.PromoRepository
import com.rsalanga.knowledgebase.mvvm.data.task.InMemoryTaskRepository
import com.rsalanga.knowledgebase.mvvm.data.task.TaskRepository
import com.rsalanga.knowledgebase.mvvm.feature.account.AccountViewModel
import com.rsalanga.knowledgebase.mvvm.feature.account.SignInViewModel
import com.rsalanga.knowledgebase.mvvm.feature.cartbadge.CartBadgeViewModel
import com.rsalanga.knowledgebase.mvvm.feature.checkout.CheckoutViewModel
import com.rsalanga.knowledgebase.mvvm.feature.checkoutflow.CheckoutFlowViewModel
import com.rsalanga.knowledgebase.mvvm.feature.checkoutflow.PaymentViewModel
import com.rsalanga.knowledgebase.mvvm.feature.editor.NoteEditorViewModel
import com.rsalanga.knowledgebase.mvvm.feature.feed.FeedViewModel
import com.rsalanga.knowledgebase.mvvm.feature.game.snapshot.LiveGameViewModel
import com.rsalanga.knowledgebase.mvvm.feature.game.stateflow.GameViewModel
import com.rsalanga.knowledgebase.mvvm.feature.labels.LabelPickerViewModel
import com.rsalanga.knowledgebase.mvvm.feature.orders.OrdersViewModel
import com.rsalanga.knowledgebase.mvvm.feature.profile.ProfileViewModel
import com.rsalanga.knowledgebase.mvvm.feature.report.ReportViewModel
import com.rsalanga.knowledgebase.mvvm.feature.search.SearchViewModel
import com.rsalanga.knowledgebase.mvvm.feature.signup.SignUpViewModel
import com.rsalanga.knowledgebase.mvvm.feature.store.StoreViewModel
import com.rsalanga.knowledgebase.mvvm.feature.tasks.detail.TaskDetailViewModel
import com.rsalanga.knowledgebase.mvvm.feature.tasks.edit.EditTaskViewModel
import com.rsalanga.knowledgebase.mvvm.feature.tasks.list.TaskListViewModel
import com.rsalanga.knowledgebase.mvvm.feature.tasks.rename.RenameTaskViewModel
import com.rsalanga.knowledgebase.mvvm.feature.volume.VolumeViewModel
import com.rsalanga.knowledgebase.mvvm.shell.AppViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Repositories are singletons: they are the shared source of truth (MVVM-V4). In-memory data
 * lives in a `MutableStateFlow` inside them.
 */
val dataModule = module {
    // MVVM-P8: the only place in the app that names real dispatchers.
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
 * MVVM-P5 / N2: VMs that need a nav argument take it as the first constructor parameter, passed
 * with `koinViewModel { parametersOf(key.x) }` in the entry block. `SavedStateHandle` comes from
 * `get()`, for user input only.
 */
val viewModelModule = module {
    viewModel { AppViewModel(get(), get()) }

    viewModel { TaskListViewModel(get(), get(), get()) }
    viewModel { (taskId: Long) -> TaskDetailViewModel(taskId, get(), get()) }
    viewModel { params -> EditTaskViewModel(params.getOrNull<Long>(), get(), get(), get(), get()) }
    viewModel { (taskId: Long) -> RenameTaskViewModel(taskId, get()) }
    viewModel { LabelPickerViewModel(get()) }

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
