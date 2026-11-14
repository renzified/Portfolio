package com.rsalanga.knowledgebase.mvvm.data.task

@JvmInline
value class TaskId(val value: Long)

enum class Priority {
    Low,
    Normal,
    High
}

data class Task(
    val id: TaskId,
    val title: String,
    val done: Boolean = false,
    val priority: Priority = Priority.Normal,
    val labelId: Long? = null
)

/** What an editor sends to the repository. `id == null` creates a new task. */
data class TaskDraft(val id: TaskId?, val title: String, val priority: Priority, val labelId: Long?)

enum class TaskFilter {
    All,
    Active,
    Done;

    fun matches(task: Task): Boolean = when (this) {
        All -> true
        Active -> !task.done
        Done -> task.done
    }
}
