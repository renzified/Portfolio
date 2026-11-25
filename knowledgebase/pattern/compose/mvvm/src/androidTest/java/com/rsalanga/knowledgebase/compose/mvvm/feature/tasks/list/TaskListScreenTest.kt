package com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rsalanga.knowledgebase.compose.mvvm.core.UiText
import com.rsalanga.knowledgebase.compose.mvvm.data.task.Priority
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MVVM-T4: UI tests target the stateless Screen with hand-built state — no ViewModel, no Koin,
 * no fake repository. Possible only because of the Route/Screen split (MVVM-L2).
 */
@RunWith(AndroidJUnit4::class)
class TaskListScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun actions(onTaskClick: (TaskId) -> Unit = {}, onRetryClick: () -> Unit = {}) =
        TaskListActions(
            onTaskClick = onTaskClick,
            onAddClick = {},
            onToggleDone = {},
            onDelete = {},
            onRename = {},
            onFilterSelect = {},
            onRefreshClick = {},
            onRetryClick = onRetryClick
        )

    @Test
    fun errorState_retryCallsLambda() {
        var retried = false
        compose.setContent {
            TaskListScreen(
                state = TaskListUiState.Error(UiText.Raw("Boom")),
                selectedTaskId = null,
                actions = actions(onRetryClick = { retried = true }),
                snackbarHostState = SnackbarHostState(),
                onBack = {}
            )
        }

        compose.onNodeWithText("Boom").assertExists()
        compose.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }

    @Test
    fun content_rowClickReportsTheTaskId() {
        var clicked: TaskId? = null
        val task = TaskUi(
            id = TaskId(5),
            title = "Buy milk",
            done = false,
            priority = Priority.Normal,
            labelName = null
        )
        compose.setContent {
            TaskListScreen(
                state = TaskListUiState.Content(tasks = listOf(task), filter = TaskFilter.All),
                selectedTaskId = null,
                actions = actions(onTaskClick = { clicked = it }),
                snackbarHostState = SnackbarHostState(),
                onBack = {}
            )
        }

        compose.onNodeWithText("Buy milk").performClick()
        assertEquals(TaskId(5), clicked)
    }
}
