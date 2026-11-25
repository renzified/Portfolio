package com.rsalanga.knowledgebase.fragment.mvvm.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content

/**
 * FMVVM-F1: the only way a Fragment shows Compose UI. Return it from `onCreateView`.
 *
 * `content { }` (fragment-compose) builds a [ComposeView] with
 * `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed`, so the composition lives exactly
 * as long as the Fragment's *view*: it is disposed when the Fragment goes onto the back stack
 * (view destroyed, Fragment kept) and rebuilt when it comes back. `rememberSaveable` state is
 * saved and restored across that, like any View state.
 *
 * Inside the composition, `LocalLifecycleOwner` is the Fragment's `viewLifecycleOwner`, so
 * `collectAsStateWithLifecycle()` stops collecting below STARTED (FMVVM-L6 / F3).
 */
fun Fragment.composeScreen(screen: @Composable () -> Unit): ComposeView = content {
    MvvmTheme(content = screen)
}
