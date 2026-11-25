package com.rsalanga.knowledgebase.compose.mvvm.data.session

import com.rsalanga.knowledgebase.compose.mvvm.R
import com.rsalanga.knowledgebase.compose.mvvm.core.ApplicationScope
import com.rsalanga.knowledgebase.compose.mvvm.core.UiText
import com.rsalanga.knowledgebase.compose.mvvm.data.messages.UserMessages
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SYNC_DURATION_MS = 2_000L

sealed interface Session {
    /** Before the shell has read the session. Navigation ignores it. */
    data object Unknown : Session

    data class SignedIn(val userName: String) : Session

    data object SignedOut : Session
}

/**
 * MVVM-N7: logout / session expiry are **state** here, not broadcasts. Only the code that owns
 * the back stack observes it (`shell/AppRoot.kt`).
 */
interface SessionRepository {
    val session: StateFlow<Session>

    fun signIn(userName: String)

    fun signOut()

    /** Simulates the server rejecting the token: a non-screen source of state change. */
    fun expire()
}

class InMemorySessionRepository(private val userMessages: UserMessages) : SessionRepository {
    private val _session = MutableStateFlow<Session>(Session.SignedIn(userName = "Demo user"))
    override val session: StateFlow<Session> = _session.asStateFlow()

    override fun signIn(userName: String) {
        _session.value = Session.SignedIn(userName.trim())
    }

    override fun signOut() {
        _session.value = Session.SignedOut
    }

    override fun expire() {
        _session.value = Session.SignedOut
        userMessages.post(UiText.Res(R.string.session_expired)) // MVVM-N6: not from a screen
    }
}

/**
 * MVVM-N6: work that finishes after the screen that started it is gone. It reports through the
 * app message queue, never through a screen's state or an event bus.
 */
class SyncService(
    private val userMessages: UserMessages,
    private val applicationScope: ApplicationScope
) {
    fun syncInBackground() {
        applicationScope.launch {
            delay(SYNC_DURATION_MS)
            userMessages.post(UiText.Res(R.string.sync_finished))
        }
    }
}
