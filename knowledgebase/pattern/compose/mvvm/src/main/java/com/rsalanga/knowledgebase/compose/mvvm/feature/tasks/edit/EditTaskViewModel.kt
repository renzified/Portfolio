package com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.edit

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.rsalanga.knowledgebase.compose.mvvm.R
import com.rsalanga.knowledgebase.compose.mvvm.core.UiText
import com.rsalanga.knowledgebase.compose.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.compose.mvvm.data.label.LabelRepository
import com.rsalanga.knowledgebase.compose.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.compose.mvvm.data.task.Priority
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskDraft
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A form: no exclusive phases after load, so a flat data class is right (MVVM-M1). */
data class EditTaskUiState(
    val isNew: Boolean = true,
    val selectedLabelId: Long? = null,
    val labelName: String? = null,
    val priority: Priority = Priority.Normal,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val hasPhoto: Boolean = false,
    // MVVM-E3: one-off outcomes are state; each has an `xHandled()`/`messageShown()` callback.
    val navigateBack: Boolean = false,
    val requestCameraPermission: Boolean = false,
    val userMessage: UiText? = null
)

/**
 * Chapter 7 in one screen:
 * - E3: save → `navigateBack = true` / `userMessage`, cleared by the Route's callbacks.
 * - E4: no SharedFlow / Channel anywhere.
 * - E5: the VM says *that* it's done, never *where* to go.
 * - E6: camera permission is requested by the Route; the result comes back as an intent.
 *
 * Also: I1 / S7 (title draft is a VM-owned TextFieldState), N4 ([selectLabel] receives the
 * picker result), N6 (the "Task saved" message must outlive this screen, so it goes to
 * [UserMessages]).
 */
class EditTaskViewModel(
    private val taskId: Long?,
    private val repository: TaskRepository,
    labelRepository: LabelRepository,
    private val userMessages: UserMessages,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // MVVM-I1 + P5: text the VM submits, saved across process death.
    @OptIn(SavedStateHandleSaveableApi::class)
    val title: TextFieldState =
        savedStateHandle.saveable(KEY_TITLE, saver = TextFieldState.Saver) { TextFieldState() }

    private val selectedLabelId = savedStateHandle.getStateFlow<Long?>(KEY_LABEL, null)
    private val priority = savedStateHandle.getStateFlow(KEY_PRIORITY, Priority.Normal)
    private val outcomes = MutableStateFlow(Outcomes())

    /**
     * Copies the stored task into the form once, when the UI first subscribes (no `init {}`,
     * MVVM-P3). Skipped after process death, because the form was restored already.
     */
    private val prefill: Flow<Unit> = flow {
        if (taskId != null && savedStateHandle.get<Boolean>(KEY_PREFILLED) != true) {
            repository.observeTask(TaskId(taskId)).first()?.let { task ->
                title.setTextAndPlaceCursorAtEnd(task.title)
                savedStateHandle[KEY_LABEL] = task.labelId
                savedStateHandle[KEY_PRIORITY] = task.priority
            }
            savedStateHandle[KEY_PREFILLED] = true
        }
        emit(Unit)
    }

    private val form: Flow<Form> = combine(
        selectedLabelId,
        priority,
        snapshotFlow { title.text.isNotBlank() }
    ) { labelId, selectedPriority, hasTitle -> Form(labelId, selectedPriority, hasTitle) }

    val uiState: StateFlow<EditTaskUiState> = combine(
        prefill,
        labelRepository.observeLabels(),
        form,
        outcomes
    ) { _, labels, current, outcome ->
        EditTaskUiState(
            isNew = taskId == null,
            selectedLabelId = current.labelId,
            labelName = labels.firstOrNull { it.id == current.labelId }?.name,
            priority = current.priority,
            canSave = current.hasTitle && !outcome.isSaving,
            isSaving = outcome.isSaving,
            hasPhoto = outcome.hasPhoto,
            navigateBack = outcome.navigateBack,
            requestCameraPermission = outcome.requestCameraPermission,
            userMessage = outcome.userMessage
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, EditTaskUiState(isNew = taskId == null))

    /** MVVM-N4: the picker's result, forwarded once by the entry block. */
    fun selectLabel(labelId: Long) {
        savedStateHandle[KEY_LABEL] = labelId
    }

    fun selectPriority(value: Priority) {
        savedStateHandle[KEY_PRIORITY] = value
    }

    fun save() {
        // MVVM-C7: compute first, then write.
        val draft = TaskDraft(
            id = taskId?.let { TaskId(it) },
            title = title.text.toString(),
            priority = priority.value,
            labelId = selectedLabelId.value
        )
        outcomes.update { it.copy(isSaving = true) } // MVVM-C2
        viewModelScope.launch {
            repository.save(draft)
                .onSuccess { _ ->
                    userMessages.post(UiText.Res(R.string.task_saved)) // MVVM-N6
                    outcomes.update { it.copy(isSaving = false, navigateBack = true) }
                }
                .onFailure { _ ->
                    outcomes.update {
                        it.copy(isSaving = false, userMessage = UiText.Res(R.string.save_failed))
                    }
                }
        }
    }

    fun navigationHandled() = outcomes.update { it.copy(navigateBack = false) }

    fun messageShown() = outcomes.update { it.copy(userMessage = null) }

    /** MVVM-E6: the VM decides a permission is needed; the Route asks for it. */
    fun attachPhoto() = outcomes.update { it.copy(requestCameraPermission = true) }

    fun cameraPermissionResult(granted: Boolean) = outcomes.update {
        it.copy(
            requestCameraPermission = false,
            hasPhoto = it.hasPhoto || granted,
            userMessage = if (granted) it.userMessage else UiText.Res(R.string.camera_denied)
        )
    }

    private data class Form(val labelId: Long?, val priority: Priority, val hasTitle: Boolean)

    private data class Outcomes(
        val isSaving: Boolean = false,
        val hasPhoto: Boolean = false,
        val navigateBack: Boolean = false,
        val requestCameraPermission: Boolean = false,
        val userMessage: UiText? = null
    )

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_LABEL = "labelId"
        const val KEY_PRIORITY = "priority"
        const val KEY_PREFILLED = "prefilled"
    }
}
