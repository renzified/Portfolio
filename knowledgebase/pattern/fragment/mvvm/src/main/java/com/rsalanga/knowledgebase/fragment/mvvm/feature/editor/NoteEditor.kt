package com.rsalanga.knowledgebase.fragment.mvvm.feature.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.R
import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.androidx.viewmodel.ext.android.viewModel

/** FMVVM-M8: the VM says *which* formats are on; the UI turns that into a TextStyle. */
data class NoteFormat(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)

data class NoteEditorUiState(
    val format: NoteFormat = NoteFormat(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val closed: Boolean = false
)

class NoteEditorViewModel(private val userMessages: UserMessages) : ViewModel() {
    val body = TextFieldState()
    private val history = MutableStateFlow(History())

    val uiState: StateFlow<NoteEditorUiState> = history
        .map { h ->
            NoteEditorUiState(
                format = h.current,
                canUndo = h.past.isNotEmpty(),
                canRedo = h.future.isNotEmpty(),
                closed = h.closed
            )
        }
        .stateIn(viewModelScope, WhileUiSubscribed, NoteEditorUiState())

    // FMVVM-E2: one function per intent, named for the intent — the VM doesn't know about buttons.

    fun toggleBold() = applyFormat { it.copy(bold = !it.bold) }

    fun toggleItalic() = applyFormat { it.copy(italic = !it.italic) }

    fun toggleUnderline() = applyFormat { it.copy(underline = !it.underline) }

    fun undo() = history.update { h ->
        val previous = h.past.lastOrNull() ?: return@update h
        h.copy(past = h.past.dropLast(1), current = previous, future = listOf(h.current) + h.future)
    }

    fun redo() = history.update { h ->
        val next = h.future.firstOrNull() ?: return@update h
        h.copy(past = h.past + h.current, current = next, future = h.future.drop(1))
    }

    fun clear() {
        body.clearText()
        history.value = History()
    }

    fun save() {
        userMessages.post(UiText.Res(R.string.note_saved)) // FMVVM-N6: outlives this screen
        history.update { it.copy(closed = true) }
    }

    fun closeHandled() = history.update { it.copy(closed = false) }

    private fun applyFormat(change: (NoteFormat) -> NoteFormat) = history.update { h ->
        h.copy(past = h.past + h.current, current = change(h.current), future = emptyList())
    }

    private data class History(
        val past: List<NoteFormat> = emptyList(),
        val current: NoteFormat = NoteFormat(),
        val future: List<NoteFormat> = emptyList(),
        val closed: Boolean = false
    )
}

/**
 * FMVVM-E2: more than six actions → a stable holder, built once per VM in the Fragment. Not an
 * MVI-style `Action` sealed class.
 */
@Suppress("LongParameterList") // A holder of lambdas is the point of this class.
@Immutable
data class NoteEditorActions(
    val onBold: () -> Unit,
    val onItalic: () -> Unit,
    val onUnderline: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onClear: () -> Unit,
    val onSave: () -> Unit
)

class NoteEditorFragment : Fragment() {
    private val viewModel: NoteEditorViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val actions = remember(viewModel) {
            NoteEditorActions(
                onBold = viewModel::toggleBold,
                onItalic = viewModel::toggleItalic,
                onUnderline = viewModel::toggleUnderline,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onClear = viewModel::clear,
                onSave = viewModel::save
            )
        }
        LaunchedEffect(state.closed) {
            if (state.closed) {
                viewModel.closeHandled()
                navigateUp()
            }
        }
        NoteEditorScreen(
            state = state,
            body = viewModel.body,
            actions = actions,
            onBack = { navigateUp() }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow, on Compose versions where it is still marked
@Composable
fun NoteEditorScreen(
    state: NoteEditorUiState,
    body: TextFieldState,
    actions: NoteEditorActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Note editor", onBack = onBack, modifier = modifier) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.format.bold, actions.onBold, label = { Text("Bold") })
                FilterChip(state.format.italic, actions.onItalic, label = { Text("Italic") })
                FilterChip(
                    state.format.underline,
                    actions.onUnderline,
                    label = { Text("Underline") }
                )
                OutlinedButton(onClick = actions.onUndo, enabled = state.canUndo) { Text("Undo") }
                OutlinedButton(onClick = actions.onRedo, enabled = state.canRedo) { Text("Redo") }
                OutlinedButton(onClick = actions.onClear) { Text("Clear") }
                OutlinedButton(onClick = actions.onSave) { Text("Save") }
            }
            OutlinedTextField(
                state = body,
                textStyle = state.format.toTextStyle(),
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth()
            )
            RuleNote("E2: seven actions travel as one NoteEditorActions holder.")
        }
    }
}

private fun NoteFormat.toTextStyle() = TextStyle(
    fontWeight = if (bold) FontWeight.Bold else null,
    fontStyle = if (italic) FontStyle.Italic else null,
    textDecoration = if (underline) TextDecoration.Underline else null
)
