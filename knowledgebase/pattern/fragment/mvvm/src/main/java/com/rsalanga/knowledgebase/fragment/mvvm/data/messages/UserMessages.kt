package com.rsalanga.knowledgebase.fragment.mvvm.data.messages

import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UserMessage(val id: Long, val text: UiText)

/**
 * FMVVM-N6: the app-level message queue. Screen VMs and background work **post**; the app
 * shell (`MainActivity` → `shell/AppMessageHost.kt`) is the only place that **shows**, then
 * calls [shown].
 *
 * It is state, not a stream (FMVVM-N8 / E4): a message posted while no one is looking waits
 * in the list until it is displayed.
 */
class UserMessages {
    private val _messages = MutableStateFlow<List<UserMessage>>(emptyList())
    val messages: StateFlow<List<UserMessage>> = _messages.asStateFlow()
    private val nextId = AtomicLong()

    fun post(text: UiText) {
        _messages.update { queue -> queue + UserMessage(nextId.incrementAndGet(), text) }
    }

    fun shown(id: Long) {
        _messages.update { queue -> queue.filterNot { message -> message.id == id } }
    }
}
