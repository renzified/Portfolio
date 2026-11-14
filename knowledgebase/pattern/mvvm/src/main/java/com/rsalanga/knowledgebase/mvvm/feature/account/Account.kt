package com.rsalanga.knowledgebase.mvvm.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.mvvm.data.session.Session
import com.rsalanga.knowledgebase.mvvm.data.session.SessionRepository
import com.rsalanga.knowledgebase.mvvm.data.session.SyncService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel

data class AccountUiState(val userName: String? = null)

/**
 * MVVM-N7: signing out only changes session **state**. This VM does not navigate and nobody
 * broadcasts "logged out"; the app shell sees the new state and rewrites the back stack, which
 * also clears this VM (its entry leaves the stack).
 *
 * MVVM-N6: background sync reports back through the app message queue after this screen may be
 * long gone.
 */
class AccountViewModel(
    private val sessionRepository: SessionRepository,
    private val syncService: SyncService
) : ViewModel() {

    val uiState: StateFlow<AccountUiState> = sessionRepository.session
        .map { AccountUiState(userName = (it as? Session.SignedIn)?.userName) }
        .stateIn(viewModelScope, WhileUiSubscribed, AccountUiState())

    fun signOut() = sessionRepository.signOut()

    fun expireSession() = sessionRepository.expire()

    fun syncInBackground() = syncService.syncInBackground()
}

@Composable
fun AccountRoute(onBack: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AccountScreen(
        state = state,
        onSyncClick = viewModel::syncInBackground,
        onSignOutClick = viewModel::signOut,
        onExpireClick = viewModel::expireSession,
        onBack = onBack
    )
}

@Composable
fun AccountScreen(
    state: AccountUiState,
    onSyncClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onExpireClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Account", onBack = onBack, modifier = modifier) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Signed in as ${state.userName.orEmpty()}",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(onClick = onSyncClick, modifier = Modifier.fillMaxWidth()) {
                Text("Sync in background, then go Back")
            }
            RuleNote(
                "N6: the result arrives 2 s later as an app-level snackbar, even if you've " +
                    "left this screen. No event bus (N8): it's a queue in UserMessages."
            )
            Button(onClick = onSignOutClick, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
            OutlinedButton(onClick = onExpireClick, modifier = Modifier.fillMaxWidth()) {
                Text("Simulate session expiry")
            }
            RuleNote(
                "N7: both only change Session state. AppRoot (the back-stack owner) maps it to " +
                    "a new back stack with backStackFor()."
            )
        }
    }
}

data class SignInUiState(val canSignIn: Boolean = false)

class SignInViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    val name = TextFieldState()

    val uiState: StateFlow<SignInUiState> = snapshotFlow { name.text.isNotBlank() }
        .map { SignInUiState(canSignIn = it) }
        .stateIn(viewModelScope, WhileUiSubscribed, SignInUiState())

    /** MVVM-N7: just state. The shell navigates back to the catalog when it sees SignedIn. */
    fun signIn() = sessionRepository.signIn(name.text.toString())
}

/** The SignIn destination has no Back: it is the whole back stack while signed out. */
@Composable
fun SignInRoute(viewModel: SignInViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SignInScreen(state = state, name = viewModel.name, onSignInClick = viewModel::signIn)
}

@Composable
fun SignInScreen(
    state: SignInUiState,
    name: TextFieldState,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Sign in", onBack = null, modifier = modifier) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                state = name,
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onSignInClick, enabled = state.canSignIn) { Text("Sign in") }
            RuleNote("N7: signing in changes Session; AppRoot swaps this stack for the catalog.")
        }
    }
}
