package com.rsalanga.knowledgebase.mvvm.feature

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import com.rsalanga.knowledgebase.mvvm.R
import com.rsalanga.knowledgebase.mvvm.core.UiText
import com.rsalanga.knowledgebase.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.mvvm.feature.tasks.detail.TaskDetailUiState
import com.rsalanga.knowledgebase.mvvm.feature.tasks.detail.TaskDetailViewModel
import com.rsalanga.knowledgebase.mvvm.feature.tasks.edit.EditTaskViewModel
import com.rsalanga.knowledgebase.mvvm.feature.tasks.list.TaskListUiState
import com.rsalanga.knowledgebase.mvvm.feature.tasks.list.TaskListViewModel
import com.rsalanga.knowledgebase.mvvm.testing.FakeLabelRepository
import com.rsalanga.knowledgebase.mvvm.testing.FakeTaskRepository
import com.rsalanga.knowledgebase.mvvm.testing.MainDispatcherRule
import com.rsalanga.knowledgebase.mvvm.testing.task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Chapter 10: each test drives the VM side of one rule; navigation itself stays out. */
@OptIn(ExperimentalCoroutinesApi::class)
class BetweenScreensTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeTaskRepository()
    private val labels = FakeLabelRepository()
    private val userMessages = UserMessages()

    private fun editViewModel(taskId: Long?) =
        EditTaskViewModel(taskId, repository, labels, userMessages, SavedStateHandle())

    @Test
    fun `N1 N2 - the detail VM gets only the id and loads the task itself`() = runTest {
        repository.emit(listOf(task(7, "From the repository")))
        val detail = TaskDetailViewModel(7, repository, labels) // constructed with the id only
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            detail.uiState.collect {}
        }

        val state = detail.uiState.value as TaskDetailUiState.Content
        assertEquals("From the repository", state.task.title)
    }

    @Test
    fun `N4 - a picked label is a plain intent on the opener's VM`() = runTest {
        val edit = editViewModel(taskId = null)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { edit.uiState.collect {} }

        edit.selectLabel(2)

        assertEquals(2L, edit.uiState.value.selectedLabelId)
        assertEquals("Work", edit.uiState.value.labelName)
    }

    @Test
    fun `N5 - a change made on one screen reaches another through the repository`() = runTest {
        repository.emit(listOf(task(1, done = false)))
        val list = TaskListViewModel(repository, labels, SavedStateHandle())
        val detail = TaskDetailViewModel(1, repository, labels)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { list.uiState.collect {} }

        detail.toggleDone() // no event sent to the list VM

        val row = (list.uiState.value as TaskListUiState.Content).tasks.single()
        assertEquals(TaskId(1), row.id)
        assertTrue(row.done)
    }

    @Test
    fun `N6 - saving posts an app-level message and sets the navigation outcome`() = runTest {
        val edit = editViewModel(taskId = null)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { edit.uiState.collect {} }
        edit.title.setTextAndPlaceCursorAtEnd("Buy milk")

        edit.save()

        assertEquals("Buy milk", repository.savedDrafts.single().title)
        assertEquals(
            listOf(UiText.Res(R.string.task_saved)),
            userMessages.messages.value.map { it.text }
        )
        assertTrue(edit.uiState.value.navigateBack) // MVVM-E3

        edit.navigationHandled()
        assertEquals(false, edit.uiState.value.navigateBack)
    }
}
