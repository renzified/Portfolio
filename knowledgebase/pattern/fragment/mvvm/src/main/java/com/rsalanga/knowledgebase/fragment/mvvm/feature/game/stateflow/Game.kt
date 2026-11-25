package com.rsalanga.knowledgebase.fragment.mvvm.feature.game.stateflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val TICK_MS = 1_000L
private const val POINTS_PER_TICK = 10.0
private const val TIME_BONUS = 0.01

/**
 * FMVVM-C1 (Style A, the default): one immutable data class in one StateFlow. Every field written
 * by the same event lives here, so a frame can never show new time with old score (FMVVM-C3).
 */
data class GameUiState(
    val levelName: String = "Level 1 - Stay Alive!",
    val timeSurvived: Int = 0,
    val score: Double = 0.0,
    val isRunning: Boolean = false
)

/**
 * FMVVM-C5: this VM uses StateFlow only — no `mutableStateOf` next to it.
 * FMVVM-C6: it does NOT expose per-field flows (`uiState.map { it.score }.stateIn(…)`) to fake
 * granularity. If that were needed, the answer is Style B (`../snapshot`).
 */
class GameViewModel : ViewModel() {
    // FMVVM-L5: mutable state is private; the public face is read-only.
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    private var ticker: Job? = null

    fun start() {
        if (ticker?.isActive == true) return
        _uiState.update { it.copy(isRunning = true) }
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    fun pause() {
        ticker?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    /**
     * FMVVM-C2: `update {}` is an atomic compare-and-set.
     * FMVVM-C7: new values are computed into locals first, so the score uses the NEW time — the
     * same result as the snapshot version.
     */
    fun tick() = _uiState.update { current ->
        val time = current.timeSurvived + 1
        val score = current.score + POINTS_PER_TICK + TIME_BONUS * time
        current.copy(timeSurvived = time, score = score)
    }
}

class GameFragment : Fragment() {
    private val viewModel: GameViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle() // the read happens here
        GameScreen(
            state = state,
            onStartClick = viewModel::start,
            onPauseClick = viewModel::pause,
            onBack = { navigateUp() }
        )
    }
}

@Composable
fun GameScreen(
    state: GameUiState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Game · StateFlow", onBack = onBack, modifier = modifier) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FMVVM-C8 (Style A): pass each child only the fields it reads. Header's param
            // doesn't change on tick, so it is skipped.
            Header(levelName = state.levelName)
            ScoreBadge(timeSurvived = state.timeSurvived, score = state.score)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartClick, enabled = !state.isRunning) { Text("Start") }
                OutlinedButton(onClick = onPauseClick, enabled = state.isRunning) {
                    Text("Pause")
                }
            }
            RuleNote(
                "Every tick invalidates the Fragment's content (it reads `state`). Header is " +
                    "skipped because levelName is unchanged. Fine for 1 update/second."
            )
        }
    }
}

@Composable
private fun Header(levelName: String) {
    Text(levelName, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ScoreBadge(timeSurvived: Int, score: Double) {
    Text("${timeSurvived}s · ${String.format(Locale.US, "%.2f", score)}")
}
