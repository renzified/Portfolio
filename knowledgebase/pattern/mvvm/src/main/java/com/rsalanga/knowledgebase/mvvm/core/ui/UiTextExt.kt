package com.rsalanga.knowledgebase.mvvm.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rsalanga.knowledgebase.mvvm.core.UiText
import java.util.Locale

/** MVVM-M5: strings are resolved in the UI, so they follow locale changes. */
@Suppress("SpreadOperator") // stringResource takes the format args as varargs.
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> stringResource(id, *args.toTypedArray())
}

private const val CENTS_PER_UNIT = 100

/** MVVM-M5: ViewModels expose raw money (cents); formatting happens in the UI layer. */
fun Long.asPrice(): String =
    String.format(Locale.US, "$%d.%02d", this / CENTS_PER_UNIT, this % CENTS_PER_UNIT)
