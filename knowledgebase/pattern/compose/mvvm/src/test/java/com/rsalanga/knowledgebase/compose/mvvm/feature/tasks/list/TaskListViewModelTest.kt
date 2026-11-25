package com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list

import androidx.lifecycle.SavedStateHandle
import com.rsalanga.knowledgebase.compose.mvvm.R
import com.rsalanga.knowledgebase.compose.mvvm.core.UiText
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.compose.mvvm.testing.FakeLabelRepository
import com.rsalanga.knowledgebase.compose.mvvm.testing.FakeTaskRepository
import com.rsalanga.knowledgebase.compose.mvvm.testing.MainDispatcherRule
import com.rsalanga.knowledgebase.compose.mvvm.testing.task
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeTaskRepository()

    // Lazy: created inside the test, after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy {
        TaskListViewModel(repository, FakeLabelRepository(), SavedStateHandle())
    }

    private val content get() = viewModel.uiState.value as TaskListUiState.Content

    @Test
    fun `starts in Loading before anyone collects`() {
        // MVVM-M7 + P3: a real initial value, and nothing ran on construction.
        assertEquals(TaskListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `shows tasks from the repository mapped to UI models`() = runTest {
        // MVVM-T2: WhileSubscribed does nothing until collected.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        repository.emit(listOf(task(1, "Buy milk", labelId = 1)))

        val row = content.tasks.single()
        assertEquals("Buy milk", row.title)
        assertEquals("Home", row.labelName) // MVVM-M6: domain → UI mapping
    }

    @Test
    fun `filter is applied and survives in SavedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        val vm = TaskListViewModel(repository, FakeLabelRepository(), handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        repository.emit(listOf(task(1, done = true), task(2, done = false)))

        vm.selectFilter(TaskFilter.Done)

        val state = vm.uiState.value as TaskListUiState.Content
        assertEquals(listOf(TaskId(1)), state.tasks.map { it.id })
        assertEquals(TaskFilter.Done, handle.get<TaskFilter>("filter")) // MVVM-P5
    }

    @Test
    fun `refresh failure shows a message that clears after it is shown`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        repository.refreshResult = Result.failure(IOException())

        viewModel.refresh()
        // MVVM-E3: the outcome is plain data in the state.
        assertEquals(UiText.Res(R.string.refresh_failed), content.userMessage)

        viewModel.messageShown()
        assertNull(content.userMessage)
    }

    @Test
    fun `stream failure becomes the Error phase and retry recovers`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        repository.observeError = IOException("boom")
        viewModel.retry()
        assertTrue(viewModel.uiState.value is TaskListUiState.Error) // MVVM-P7

        repository.observeError = null
        viewModel.retry()
        assertTrue(viewModel.uiState.value is TaskListUiState.Content)
    }
}
