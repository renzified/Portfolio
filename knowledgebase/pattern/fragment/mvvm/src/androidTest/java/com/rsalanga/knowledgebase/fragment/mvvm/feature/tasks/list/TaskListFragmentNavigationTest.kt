package com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rsalanga.knowledgebase.fragment.mvvm.R
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.EditTaskKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.TaskListKey
import com.rsalanga.knowledgebase.fragment.mvvm.testing.attachTestNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FMVVM-T5: the Fragment is tested only for what the Screen test can't see — that its lambdas
 * reach the NavController with the right key. A [TestNavHostController] carries the real app
 * graph (`buildAppGraph()`, see [attachTestNavController]), so the test also proves the key is a
 * registered destination.
 *
 * The Fragment's ViewModel is the real one from Koin (started by `MvvmApplication`) with the
 * in-memory data; its logic is covered by the JVM tests (FMVVM-T1–T3), not here.
 */
@RunWith(AndroidJUnit4::class)
class TaskListFragmentNavigationTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun newTaskClick_navigatesToTheEditorForANewTask() {
        lateinit var navController: TestNavHostController
        launchFragmentInContainer<TaskListFragment>(themeResId = R.style.Theme_Portfolio)
            .onFragment { fragment ->
                navController = attachTestNavController(fragment, startAt = TaskListKey)
            }

        compose.onNodeWithText("New task").performClick()

        val entry = checkNotNull(navController.currentBackStackEntry)
        assertEquals(EditTaskKey(taskId = null), entry.toRoute<EditTaskKey>())
    }
}
