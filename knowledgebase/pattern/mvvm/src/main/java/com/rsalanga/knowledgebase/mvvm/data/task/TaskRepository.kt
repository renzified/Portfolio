package com.rsalanga.knowledgebase.mvvm.data.task

import kotlinx.coroutines.flow.Flow

/**
 * MVVM-V4 / N5: the single source of truth for tasks. Every screen that shows tasks observes
 * these flows, so an edit on one screen reaches the others without any event being sent.
 *
 * Suspend functions are main-safe (MVVM-P4): callers never wrap them in `withContext`.
 */
interface TaskRepository {
    fun observeTasks(filter: TaskFilter): Flow<List<Task>>

    fun observeTask(id: TaskId): Flow<Task?>

    suspend fun refresh(): Result<Unit>

    suspend fun toggleDone(id: TaskId)

    suspend fun save(draft: TaskDraft): Result<TaskId>

    suspend fun rename(id: TaskId, title: String): Result<Unit>

    suspend fun delete(id: TaskId)
}
