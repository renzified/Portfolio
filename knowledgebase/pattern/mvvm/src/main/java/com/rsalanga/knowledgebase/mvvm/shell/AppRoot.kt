package com.rsalanga.knowledgebase.mvvm.shell

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import com.rsalanga.knowledgebase.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.mvvm.navigation.AppNavDisplay
import com.rsalanga.knowledgebase.mvvm.navigation.CatalogKey
import com.rsalanga.knowledgebase.mvvm.navigation.backStackFor
import org.koin.compose.viewmodel.koinViewModel

/**
 * Owns the app back stack. Obtained outside the NavDisplay, so [AppViewModel] is Activity-scoped
 * (the MVVM-V6 exception).
 */
@Composable
fun AppRoot(viewModel: AppViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(CatalogKey)
    val snackbarHostState = remember { SnackbarHostState() }

    // MVVM-N7: session changes are state; only the back-stack owner reacts, by rewriting the
    // stack. Screens never listen for "logged out".
    LaunchedEffect(state.session) {
        backStackFor(state.session, backStack.toList())?.let { required ->
            backStack.clear()
            backStack.addAll(required)
        }
    }

    // MVVM-N6: the one host for app-level messages. Same pattern as E3, at app scope.
    state.message?.let { message ->
        val text = message.text.asString()
        LaunchedEffect(message.id) {
            snackbarHostState.showSnackbar(text)
            viewModel.messageShown(message.id)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AppNavDisplay(backStack = backStack, modifier = Modifier.padding(padding))
    }
}
