package com.rsalanga.knowledgebase.fragment.mvvm.feature.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.R
import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.AuthRepository
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_EMAIL_LENGTH = 64
private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

private fun String.isValidEmail() = EmailPattern.matches(this)

data class SignUpUiState(
    val emailError: UiText? = null,
    val canSubmit: Boolean = false,
    val isSubmitting: Boolean = false,
    val userMessage: UiText? = null
)

/**
 * FMVVM-I1: text the VM validates and submits is a `TextFieldState` owned by the VM — the field
 * edits it synchronously, so there is no StateFlow round-trip per keystroke (no cursor jumps,
 * IME composition stays intact). Validation is derived from it with `snapshotFlow`.
 * This is the one allowed exception to FMVVM-L4 / C5.
 */
class SignUpViewModel(private val repository: AuthRepository) : ViewModel() {
    val email = TextFieldState()
    val password = TextFieldState()

    private val isSubmitting = MutableStateFlow(false)
    private val userMessage = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<SignUpUiState> = combine(
        snapshotFlow { email.text.toString() to password.text.toString() },
        isSubmitting,
        userMessage
    ) { (emailText, passwordText), submitting, message ->
        SignUpUiState(
            emailError = if (emailText.isNotEmpty() && !emailText.isValidEmail()) {
                UiText.Res(R.string.invalid_email)
            } else {
                null
            },
            canSubmit = emailText.isValidEmail() &&
                passwordText.length >= MIN_PASSWORD_LENGTH &&
                !submitting,
            isSubmitting = submitting,
            userMessage = message
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, SignUpUiState())

    fun submit() {
        val emailText = email.text.toString()
        val passwordText = password.text.toString()
        viewModelScope.launch {
            isSubmitting.value = true
            repository.signUp(emailText, passwordText)
                .onSuccess { _ ->
                    // Programmatic changes go through the TextFieldState API.
                    email.clearText()
                    password.clearText()
                    userMessage.value = UiText.Res(R.string.signed_up)
                }
                .onFailure { _ -> userMessage.value = UiText.Res(R.string.signup_failed) }
            isSubmitting.value = false
        }
    }

    fun messageShown() {
        userMessage.value = null
    }
}

class SignUpFragment : Fragment() {
    private val viewModel: SignUpViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        state.userMessage?.let { message ->
            val text = message.asString()
            LaunchedEffect(message) {
                snackbarHostState.showSnackbar(text)
                viewModel.messageShown()
            }
        }
        SignUpScreen(
            state = state,
            email = viewModel.email, // TextFieldState passed down as-is
            password = viewModel.password,
            onSubmitClick = viewModel::submit,
            onBack = { navigateUp() },
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
fun SignUpScreen(
    state: SignUpUiState,
    email: TextFieldState,
    password: TextFieldState,
    onSubmitClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null
) {
    val emailError = state.emailError
    DemoScaffold(
        title = "Sign up",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                state = email,
                label = { Text("Email") },
                isError = emailError != null,
                supportingText = if (emailError != null) {
                    { Text(emailError.asString()) }
                } else {
                    null
                },
                // Input rules are an InputTransformation, not the VM rewriting text.
                inputTransformation = InputTransformation.maxLength(MAX_EMAIL_LENGTH),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedSecureTextField(
                state = password,
                label = { Text("Password (8+ characters)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onSubmitClick, enabled = state.canSubmit) {
                Text(if (state.isSubmitting) "Signing up…" else "Sign up")
            }
            RuleNote("I1: both fields are VM-owned TextFieldState. Try taken@example.com.")
        }
    }
}
