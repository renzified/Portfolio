package com.rsalanga.knowledgebase.fragment.mvvm.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.data.messages.UserMessage
import com.rsalanga.knowledgebase.fragment.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.fragment.mvvm.data.session.Session
import com.rsalanga.knowledgebase.fragment.mvvm.data.session.SessionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AppUiState(
    val session: Session = Session.Unknown,
    /** FMVVM-N6: head of the app-level queue. */
    val message: UserMessage? = null
)

/**
 * The app shell's ViewModel, obtained by MainActivity with `by viewModel()`. It is
 * Activity-scoped, which FMVVM-V6 allows only for state that is truly about the Activity: the
 * session gate (N7) and the app-level message host (N6). It is not a store for screen data, and
 * no Fragment reaches it with `activityViewModel()`.
 */
class AppViewModel(sessionRepository: SessionRepository, private val userMessages: UserMessages) :
    ViewModel() {

    val uiState: StateFlow<AppUiState> =
        combine(sessionRepository.session, userMessages.messages) { session, queue ->
            AppUiState(session = session, message = queue.firstOrNull())
        }.stateIn(viewModelScope, WhileUiSubscribed, AppUiState())

    fun messageShown(id: Long) = userMessages.shown(id)
}
