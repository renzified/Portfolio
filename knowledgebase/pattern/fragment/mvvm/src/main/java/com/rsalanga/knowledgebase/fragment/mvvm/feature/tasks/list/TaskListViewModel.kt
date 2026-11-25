package com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.R
import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.Label
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.LabelRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.Task
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The canonical screen ViewModel (FMVVM-P2): a repository stream + user inputs + transient
 * outcomes, combined into one `StateFlow<TaskListUiState>` (FMVVM-C1).
 *
 * - No `init {}` (FMVVM-P3); nothing runs until the Fragment collects.
 * - No Android/UI types (FMVVM-L4); no other ViewModel (FMVVM-L7).
 * - Mutable state is private (FMVVM-L5).
 */
class TaskListViewModel(
    private val repository: TaskRepository,
    private val labelRepository: LabelRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // FMVVM-P5 / S5: the filter is user input that should survive process death.
    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, TaskFilter.All)
    private val isRefreshing = MutableStateFlow(false)
    private val userMessage = MutableStateFlow<UiText?>(null)
    private val retries = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TaskListUiState> = retries
        .flatMapLatest {
            content()
                .onStart { emit(TaskListUiState.Loading) }
                // FMVVM-P7: errors are state.
                .catch { emit(TaskListUiState.Error(UiText.Res(R.string.error_generic))) }
        }
        // FMVVM-P1
        .stateIn(viewModelScope, WhileUiSubscribed, TaskListUiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun content(): Flow<TaskListUiState> = combine(
        filter.flatMapLatest { selected -> repository.observeTasks(selected) },
        labelRepository.observeLabels(),
        filter,
        isRefreshing,
        userMessage
    ) { tasks, labels, selected, refreshing, message ->
        toUiState(tasks, labels, selected, refreshing, message)
    }

    // FMVVM-L8 / P4: intent verbs, non-suspend, fire-and-forget.

    fun selectFilter(filter: TaskFilter) {
        savedStateHandle[KEY_FILTER] = filter
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.refresh()
                .onFailure { userMessage.value = UiText.Res(R.string.refresh_failed) }
            isRefreshing.value = false
        }
    }

    fun toggleDone(id: TaskId) {
        viewModelScope.launch { repository.toggleDone(id) }
    }

    fun delete(id: TaskId) {
        viewModelScope.launch {
            repository.delete(id)
            userMessage.value = UiText.Res(R.string.task_deleted)
        }
    }

    fun retry() = retries.update { it + 1 }

    // FMVVM-E3: the UI reports that it showed the message.
    fun messageShown() {
        userMessage.value = null
    }

    private companion object {
        const val KEY_FILTER = "filter"
    }
}

private fun toUiState(
    tasks: List<Task>,
    labels: List<Label>,
    filter: TaskFilter,
    isRefreshing: Boolean,
    userMessage: UiText?
): TaskListUiState {
    val labelNames = labels.associate { it.id to it.name }
    return TaskListUiState.Content(
        tasks = tasks.map { it.toUi(labelNames) },
        filter = filter,
        isRefreshing = isRefreshing,
        userMessage = userMessage
    )
}
