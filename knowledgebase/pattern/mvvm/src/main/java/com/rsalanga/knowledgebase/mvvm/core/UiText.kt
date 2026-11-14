package com.rsalanga.knowledgebase.mvvm.core

import androidx.annotation.StringRes

/**
 * MVVM-M5: ViewModels never resolve strings. They expose a resource reference (or raw
 * server text) and the UI resolves it with [com.rsalanga.knowledgebase.mvvm.core.ui.asString].
 *
 * Both cases are data classes, so tests can compare them with `assertEquals`.
 */
sealed interface UiText {
    data class Raw(val value: String) : UiText

    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
}
