package com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list

import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.Priority
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.Task
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskId

/**
 * FMVVM-M1: exclusive phases are sealed; things shown on top of the content (refreshing,
 * snackbar) are flags inside [Content].
 * FMVVM-M7: the VM starts from [Loading], never from `null`.
 */
sealed interface TaskListUiState {
    data object Loading : TaskListUiState

    data class Error(val message: UiText) : TaskListUiState

    data class Content(
        // FMVVM-M3: read-only List, never mutated after it is placed here.
        val tasks: List<TaskUi>,
        val filter: TaskFilter,
        val isRefreshing: Boolean = false,
        // FMVVM-E3: one-off outcome, cleared by messageShown().
        val userMessage: UiText? = null
    ) : TaskListUiState {
        // FMVVM-M2: derived, not stored.
        val isEmpty: Boolean get() = tasks.isEmpty()
    }
}

/**
 * FMVVM-M6: the row needs the label *name*, which the domain [Task] doesn't have, so the VM maps
 * domain → UI model.
 * FMVVM-M8: [priority] is meaning, not a colour. The row picks the colour.
 */
data class TaskUi(
    val id: TaskId,
    val title: String,
    val done: Boolean,
    val priority: Priority,
    val labelName: String?
)

fun Task.toUi(labelNames: Map<Long, String>): TaskUi = TaskUi(
    id = id,
    title = title,
    done = done,
    priority = priority,
    labelName = labelId?.let(labelNames::get)
)
