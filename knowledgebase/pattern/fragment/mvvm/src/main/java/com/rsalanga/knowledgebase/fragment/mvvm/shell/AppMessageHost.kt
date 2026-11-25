package com.rsalanga.knowledgebase.fragment.mvvm.shell

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.fragment.mvvm.data.messages.UserMessage

/**
 * FMVVM-N6: the one host for app-level messages, in the Activity's ComposeView above the
 * NavHostFragment. It outlives every destination, so "Task saved" still shows after the editor
 * has been popped. Same pattern as FMVVM-E3, at app scope: show the head, then report it shown.
 */
@Composable
fun AppMessageHost(
    message: UserMessage?,
    onMessageShown: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnMessageShown by rememberUpdatedState(onMessageShown) // FMVVM-E6

    if (message != null) {
        val text = message.text.asString()
        LaunchedEffect(message.id) {
            snackbarHostState.showSnackbar(text)
            currentOnMessageShown(message.id)
        }
    }

    SnackbarHost(snackbarHostState, modifier.navigationBarsPadding())
}
