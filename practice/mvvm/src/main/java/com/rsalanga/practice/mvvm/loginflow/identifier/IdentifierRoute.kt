package com.rsalanga.practice.mvvm.loginflow.identifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import org.koin.compose.viewmodel.koinViewModel

@Stable
internal interface IdentifierTextFieldState {

    val fieldState: TextFieldState
    val error: Error?

    enum class Error {
        REQUIRED,
        INVALID
    }
}

@Stable
internal sealed interface ScreenState {

    data object Initial : ScreenState

    data object Loading : ScreenState

    data class Error(val error: Type) : ScreenState {

        enum class Type {
            NO_INTERNET_CONNECTION,
            SOMETHING_WENT_WRONG
        }
    }
}

private class MutableIdentifierTextFieldState : IdentifierTextFieldState {
    override val fieldState by mutableStateOf(TextFieldState())
    override val error by mutableStateOf<IdentifierTextFieldState.Error?>(null)
}

internal class IdentifierViewModel : ViewModel() {

    val screenState by mutableStateOf<ScreenState>(ScreenState.Initial)
    val identifierState: IdentifierTextFieldState = MutableIdentifierTextFieldState()
}

@Composable
internal fun IdentifierRoute(viewModel: IdentifierViewModel = koinViewModel()) {
    IdentifierScreen(
        screenState = viewModel.screenState,
        identifierState = viewModel.identifierState
    )
}

@Composable
private fun IdentifierScreen(screenState: ScreenState, identifierState: IdentifierTextFieldState) {
    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Sign in") }
                )
            }
        ) { contentPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(state = rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(100.dp))
                OutlinedTextField(
                    state = identifierState.fieldState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    label = { Text(text = "Email or Username") }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { Text(text = "Next") }
            }
        }

        if (screenState is ScreenState.Loading) {
            LoadingDialog()
        }
    }
}

@Composable
private fun LoadingDialog() {
    Dialog(onDismissRequest = {}) {
        LoadingDialogContent()
    }
}

@Preview
@Composable
private fun LoadingDialogContent() {
    Surface(shape = RoundedCornerShape(12.dp)) {
        CircularWavyProgressIndicator(modifier = Modifier.padding(12.dp))
    }
}
