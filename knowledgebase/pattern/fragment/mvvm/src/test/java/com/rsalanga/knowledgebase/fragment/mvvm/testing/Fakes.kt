package com.rsalanga.knowledgebase.fragment.mvvm.testing

import com.rsalanga.knowledgebase.fragment.mvvm.data.Profile
import com.rsalanga.knowledgebase.fragment.mvvm.data.ProfileRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.Label
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.LabelRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.Task
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskDraft
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * FMVVM-T1: hand-written fakes of the repository interfaces, not mocks, so tests drive real
 * flows. Each exposes knobs for the paths the test wants.
 */
class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    var refreshResult: Result<Unit> = Result.success(Unit)
    var saveResult: Result<Unit> = Result.success(Unit)

    /** When set, observeTasks() fails with it — drives the Error phase. */
    var observeError: Throwable? = null
    val savedDrafts = mutableListOf<TaskDraft>()

    fun emit(list: List<Task>) {
        tasks.value = list
    }

    override fun observeTasks(filter: TaskFilter): Flow<List<Task>> {
        val error = observeError
        return if (error != null) {
            flow { throw error }
        } else {
            tasks.map { list -> list.filter(filter::matches) }
        }
    }

    override fun observeTask(id: TaskId): Flow<Task?> =
        tasks.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refresh(): Result<Unit> = refreshResult

    override suspend fun toggleDone(id: TaskId) {
        tasks.update { list -> list.map { if (it.id == id) it.copy(done = !it.done) else it } }
    }

    override suspend fun save(draft: TaskDraft): Result<TaskId> {
        savedDrafts += draft
        return saveResult.map { draft.id ?: TaskId(NEW_ID) }
    }

    override suspend fun rename(id: TaskId, title: String): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: TaskId) {
        tasks.update { list -> list.filterNot { it.id == id } }
    }

    companion object {
        const val NEW_ID = 99L
    }
}

class FakeLabelRepository(labels: List<Label> = listOf(Label(1, "Home"), Label(2, "Work"))) :
    LabelRepository {
    private val labels = MutableStateFlow(labels)

    override fun observeLabels(): Flow<List<Label>> = labels
}

class FakeProfileRepository : ProfileRepository {
    var nextResult: Result<Profile> = Result.failure(IOException("offline"))
    var calls = 0
        private set

    override suspend fun loadProfile(): Result<Profile> {
        calls++
        return nextResult
    }
}

fun task(id: Long, title: String = "Task $id", done: Boolean = false, labelId: Long? = null) =
    Task(id = TaskId(id), title = title, done = done, labelId = labelId)
