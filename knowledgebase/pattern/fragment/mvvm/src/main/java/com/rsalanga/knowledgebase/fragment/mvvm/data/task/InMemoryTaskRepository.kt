package com.rsalanga.knowledgebase.fragment.mvvm.data.task

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private const val NETWORK_DELAY_MS = 600L

class InMemoryTaskRepository : TaskRepository {

    private val tasks = MutableStateFlow(SeedTasks)
    private val nextId = AtomicLong(tasks.value.maxOf { it.id.value })
    private val refreshCount = AtomicInteger()

    override fun observeTasks(filter: TaskFilter): Flow<List<Task>> =
        tasks.map { list -> list.filter(filter::matches) }

    override fun observeTask(id: TaskId): Flow<Task?> =
        tasks.map { list -> list.firstOrNull { task -> task.id == id } }

    /** Every second refresh fails, so the error path (FMVVM-E3 message) is visible in the demo. */
    override suspend fun refresh(): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        return if (refreshCount.incrementAndGet() % 2 == 0) {
            Result.failure(IOException("Simulated network failure"))
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun toggleDone(id: TaskId) {
        // FMVVM-M3: never mutate a list that was handed out; always build a new one.
        tasks.update { list ->
            list.map { task -> if (task.id == id) task.copy(done = !task.done) else task }
        }
    }

    override suspend fun save(draft: TaskDraft): Result<TaskId> {
        delay(NETWORK_DELAY_MS)
        if (draft.title.isBlank()) return Result.failure(IllegalArgumentException("Empty title"))

        val id = draft.id ?: TaskId(nextId.incrementAndGet())
        tasks.update { list ->
            val existing = list.firstOrNull { task -> task.id == id }
            val saved = Task(
                id = id,
                title = draft.title.trim(),
                done = existing?.done ?: false,
                priority = draft.priority,
                labelId = draft.labelId
            )
            if (existing == null) list + saved else list.map { if (it.id == id) saved else it }
        }
        return Result.success(id)
    }

    override suspend fun rename(id: TaskId, title: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Empty title"))

        tasks.update { list ->
            list.map { task -> if (task.id == id) task.copy(title = title.trim()) else task }
        }
        return Result.success(Unit)
    }

    override suspend fun delete(id: TaskId) {
        tasks.update { list -> list.filterNot { task -> task.id == id } }
    }
}

private val SeedTasks: List<Task> = listOf(
    Task(TaskId(1), "Buy milk", priority = Priority.Low, labelId = 3),
    Task(TaskId(2), "Write MVVM rules", priority = Priority.High, labelId = 2),
    Task(TaskId(3), "Water the plants", done = true, labelId = 1),
    Task(TaskId(4), "Review pull request", priority = Priority.High, labelId = 2),
    Task(TaskId(5), "Book dentist appointment", labelId = 3)
)
