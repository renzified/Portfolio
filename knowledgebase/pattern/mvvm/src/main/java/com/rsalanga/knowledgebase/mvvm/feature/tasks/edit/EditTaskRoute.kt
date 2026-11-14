package com.rsalanga.knowledgebase.mvvm.feature.tasks.edit

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rsalanga.knowledgebase.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.mvvm.data.task.Priority

@Immutable
data class EditTaskActions(
    val onPickLabelClick: (Long?) -> Unit,
    val onPrioritySelect: (Priority) -> Unit,
    val onAttachPhotoClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onBack: () -> Unit
)

/**
 * The VM is created in the entry block (it also needs it for the MVVM-N4 ResultEffect) and passed
 * in. Passing a VM *into a Route* is allowed; passing it below the Route is not (MVVM-L3).
 */
@Composable
fun EditTaskRoute(viewModel: EditTaskViewModel, onPickLabel: (Long?) -> Unit, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnBack by rememberUpdatedState(onBack) // MVVM-E6

    // MVVM-E3 + E5: the outcome is state; navigation runs here, in the Route.
    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) {
            viewModel.navigationHandled()
            currentOnBack()
        }
    }

    // MVVM-E3: message outcome.
    state.userMessage?.let { message ->
        val text = message.asString()
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(text)
            viewModel.messageShown()
        }
    }

    // MVVM-E6: permission launchers are UI effects. The VM set the outcome; the Route launches
    // and reports the result back through an intent.
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.cameraPermissionResult(granted) }
    LaunchedEffect(state.requestCameraPermission) {
        if (state.requestCameraPermission) cameraPermission.launch(Manifest.permission.CAMERA)
    }

    val actions = remember(viewModel, onPickLabel, onBack) {
        EditTaskActions(
            onPickLabelClick = onPickLabel,
            onPrioritySelect = viewModel::selectPriority,
            onAttachPhotoClick = viewModel::attachPhoto,
            onSaveClick = viewModel::save,
            onBack = onBack
        )
    }

    EditTaskScreen(
        state = state,
        title = viewModel.title, // MVVM-I1: TextFieldState passed down as-is
        actions = actions,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun EditTaskScreen(
    state: EditTaskUiState,
    title: TextFieldState,
    actions: EditTaskActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    DemoScaffold(
        title = if (state.isNew) "New task" else "Edit task",
        onBack = actions.onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // MVVM-I1 / S7: the draft lives in the VM, not in a remember copy of VM state.
            OutlinedTextField(
                state = title,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { priority ->
                    FilterChip(
                        selected = priority == state.priority,
                        onClick = { actions.onPrioritySelect(priority) },
                        label = { Text(priority.name) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Label: ${state.labelName ?: "none"}", Modifier.weight(1f))
                OutlinedButton(onClick = { actions.onPickLabelClick(state.selectedLabelId) }) {
                    Text("Pick label")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.hasPhoto) "Photo attached" else "No photo", Modifier.weight(1f))
                OutlinedButton(onClick = actions.onAttachPhotoClick) { Text("Attach photo") }
            }
            Button(
                onClick = actions.onSaveClick,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
            RuleNote(
                "E3/E5: Save sets navigateBack; the Route navigates and calls " +
                    "navigationHandled(). E6: Attach photo asks for camera permission from the " +
                    "Route. N4: Pick label returns a result. N6: \"Task saved\" is shown by the " +
                    "app shell after this screen closes."
            )
        }
    }
}
