package com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.edit

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.Priority
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.LABEL_PICKED_ID
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.LABEL_PICKED_RESULT
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.LabelPickerKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import org.koin.androidx.viewmodel.ext.android.viewModel

@Immutable
data class EditTaskActions(
    val onPickLabelClick: (Long?) -> Unit,
    val onPrioritySelect: (Priority) -> Unit,
    val onAttachPhotoClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onBack: () -> Unit
)

/**
 * Chapter 7 in one destination: E3 (outcomes are state), E5 (the Fragment navigates), E6
 * (effects keyed on the outcome; the permission launcher is registered by the Fragment).
 * Also N4: this Fragment is the opener of the label picker and receives its result.
 */
class EditTaskFragment : Fragment() {

    // FMVVM-P5: EditTaskKey.taskId reaches the constructor through the Koin definition.
    private val viewModel: EditTaskViewModel by viewModel()

    /**
     * FMVVM-E6: launchers are UI effects owned by the Fragment. `registerForActivityResult` must
     * be called before the Fragment is created, so it is a property, not a call inside a click
     * handler. The result goes back to the VM as an intent.
     */
    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.cameraPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FMVVM-N4, step 3: the opener listens with an explicit request key, forwards the payload
        // to a VM intent, and is done. The Fragment Result API delivers each result once, only
        // while this Fragment is STARTED, and keeps it across rotation and process death until
        // then. Registered in onCreate, so it survives the view being destroyed on the back stack.
        setFragmentResultListener(LABEL_PICKED_RESULT) { _, bundle ->
            viewModel.selectLabel(bundle.getLong(LABEL_PICKED_ID))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        // FMVVM-E3 + E5: the outcome is state; navigation runs here, in the Fragment.
        LaunchedEffect(state.navigateBack) {
            if (state.navigateBack) {
                viewModel.navigationHandled()
                navigateUp()
            }
        }

        // FMVVM-E3: message outcome.
        state.userMessage?.let { message ->
            val text = message.asString()
            LaunchedEffect(message) {
                snackbarHostState.showSnackbar(text)
                viewModel.messageShown()
            }
        }

        // FMVVM-E6: the VM set the outcome; the Fragment launches the launcher it registered.
        LaunchedEffect(state.requestCameraPermission) {
            if (state.requestCameraPermission) cameraPermission.launch(Manifest.permission.CAMERA)
        }

        val actions = remember(viewModel) {
            EditTaskActions(
                // FMVVM-N1: the picker gets a small parameter (the current selection) in its key.
                onPickLabelClick = { current ->
                    findNavController().navigate(LabelPickerKey(current))
                },
                onPrioritySelect = viewModel::selectPriority,
                onAttachPhotoClick = viewModel::attachPhoto,
                onSaveClick = viewModel::save,
                onBack = { navigateUp() }
            )
        }

        EditTaskScreen(
            state = state,
            title = viewModel.title, // FMVVM-I1: TextFieldState passed down as-is
            actions = actions,
            snackbarHostState = snackbarHostState
        )
    }
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
            // FMVVM-I1 / S7: the draft lives in the VM, not in a remember copy of VM state.
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
                "E3/E5: Save sets navigateBack; the Fragment navigates and calls " +
                    "navigationHandled(). E6: Attach photo uses the permission launcher the " +
                    "Fragment registered. N4: Pick label returns a Fragment result. N6: \"Task " +
                    "saved\" is shown by the Activity shell after this screen closes."
            )
        }
    }
}
