package com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.EmptyState
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.compose.mvvm.data.task.Priority
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list.TaskListActions
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list.TaskListUiState
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list.TaskUi

@Composable
internal fun TaskListContent(
    state: TaskListUiState.Content,
    selectedTaskId: TaskId?,
    actions: TaskListActions,
    modifier: Modifier = Modifier
) {
    // MVVM-V7 / S2: which task is awaiting confirmation is UI state. The delete itself is a
    // VM action, sent through the lambda.
    var confirmDelete by remember { mutableStateOf<TaskId?>(null) }

    Column(modifier.fillMaxSize()) {
        RuleNote("Tap a task to open it. On a wide screen the detail opens beside the list.")
        // MVVM-C8: children get the fields they read, not the whole UiState.
        FilterRow(selected = state.filter, onSelect = actions.onFilterSelect)
        TextButton(
            onClick = actions.onRefreshClick,
            enabled = !state.isRefreshing,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(if (state.isRefreshing) "Refreshing…" else "Refresh (fails every 2nd time)")
        }
        if (state.isEmpty) {
            EmptyState("Nothing here")
        } else {
            LazyColumn(Modifier.weight(1f)) {
                // MVVM-M4: stable key + content type.
                items(
                    items = state.tasks,
                    key = { it.id.value },
                    contentType = { "task" }
                ) { task ->
                    TaskRow(
                        task = task,
                        selected = task.id == selectedTaskId,
                        onClick = { actions.onTaskClick(task.id) },
                        onToggleDone = { actions.onToggleDone(task.id) },
                        onDeleteClick = { confirmDelete = task.id },
                        onRenameClick = { actions.onRename(task.id) }
                    )
                }
            }
        }
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.onDelete(id)
                        confirmDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
            text = { Text("Delete this task?") }
        )
    }
}

@Composable
private fun FilterRow(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.name) }
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskUi,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val background =
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggleDone() })
        Box(
            Modifier
                .size(10.dp)
                .background(task.priority.color(), CircleShape)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge)
            task.labelName?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        TextButton(onClick = onRenameClick) { Text("Rename") }
        TextButton(onClick = onDeleteClick) { Text("Delete") }
    }
}

/** MVVM-M8: the UiState says *what* (priority); the UI decides *how* it looks. */
@Composable
private fun Priority.color(): Color = when (this) {
    Priority.High -> MaterialTheme.colorScheme.error
    Priority.Normal -> MaterialTheme.colorScheme.primary
    Priority.Low -> MaterialTheme.colorScheme.outline
}
