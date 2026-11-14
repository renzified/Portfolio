package com.rsalanga.knowledgebase.mvvm.core

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * MVVM-P1: the only `started` policy for UI state. Survives rotation, stops the upstream
 * 5 s after the last collector leaves.
 */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS)

/**
 * MVVM-P8: dispatchers are injected, never hardcoded in a ViewModel. Koin builds the one
 * production instance (see `di/AppModules.kt`); tests pass test dispatchers.
 */
data class AppDispatchers(val default: CoroutineDispatcher, val io: CoroutineDispatcher)

/**
 * App-lifetime scope for work that must outlive any screen (MVVM-N6 background sync).
 * Never used by ViewModels; they use `viewModelScope`.
 */
class ApplicationScope(dispatchers: AppDispatchers) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + dispatchers.default
}

/**
 * MVVM-P7: like [runCatching], but never swallows coroutine cancellation.
 * Inline, so the block may call suspend functions when used inside a coroutine.
 */
@Suppress("TooGenericExceptionCaught", "RethrowCaughtException")
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
