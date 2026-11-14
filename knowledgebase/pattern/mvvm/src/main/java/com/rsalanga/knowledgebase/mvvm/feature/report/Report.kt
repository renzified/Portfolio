package com.rsalanga.knowledgebase.mvvm.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.mvvm.R
import com.rsalanga.knowledgebase.mvvm.core.AppDispatchers
import com.rsalanga.knowledgebase.mvvm.core.UiText
import com.rsalanga.knowledgebase.mvvm.core.suspendRunCatching
import com.rsalanga.knowledgebase.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.mvvm.data.ReportRepository
import com.rsalanga.knowledgebase.mvvm.data.ReportRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

private const val PREVIEW_LINES = 5

data class ReportUiState(
    val isExporting: Boolean = false,
    val csv: String? = null,
    val userMessage: UiText? = null
) {
    // MVVM-M2: derived from csv, not stored.
    val rowCount: Int get() = csv?.lines()?.drop(1)?.size ?: 0
    val preview: String get() = csv?.lines()?.take(PREVIEW_LINES)?.joinToString("\n").orEmpty()
}

/**
 * MVVM-P8: CPU work runs on an injected dispatcher; the VM never names `Dispatchers.Default`.
 * Tests pass a TestDispatcher through [AppDispatchers].
 * MVVM-P7: the launch handles its own failure (suspendRunCatching keeps cancellation intact) and
 * writes it into state.
 */
class ReportViewModel(
    private val repository: ReportRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun export() {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val result = suspendRunCatching {
                val rows = repository.rows() // main-safe repository: no withContext needed
                withContext(dispatchers.default) { buildCsv(rows) }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { csv -> current.copy(isExporting = false, csv = csv) },
                    onFailure = { _ ->
                        current.copy(
                            isExporting = false,
                            userMessage = UiText.Res(R.string.export_failed)
                        )
                    }
                )
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(userMessage = null) }
}

private fun buildCsv(rows: List<ReportRow>): String = buildString {
    appendLine("month,revenue_cents")
    rows.forEach { row -> appendLine("${row.month},${row.revenueCents}") }
}.trimEnd()

@Composable
fun ReportRoute(onBack: () -> Unit, viewModel: ReportViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    state.userMessage?.let { message ->
        val text = message.asString()
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(text)
            viewModel.messageShown()
        }
    }
    ReportScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onExportClick = viewModel::export,
        onBack = onBack
    )
}

@Composable
fun ReportScreen(
    state: ReportUiState,
    snackbarHostState: SnackbarHostState,
    onExportClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(
        title = "Report",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onExportClick, enabled = !state.isExporting) {
                Text(if (state.isExporting) "Exporting…" else "Export CSV")
            }
            if (state.csv != null) {
                Text("${state.rowCount} rows")
                Text(state.preview, fontFamily = FontFamily.Monospace)
            }
            RuleNote("P8: buildCsv runs on AppDispatchers.default, injected by Koin.")
        }
    }
}
