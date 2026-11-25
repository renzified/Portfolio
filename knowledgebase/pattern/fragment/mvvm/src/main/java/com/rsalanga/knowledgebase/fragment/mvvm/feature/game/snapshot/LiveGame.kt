package com.rsalanga.knowledgebase.fragment.mvvm.feature.game.snapshot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val FRAME_MS = 50L
private const val POINTS_PER_TICK = 10.0
private const val TIME_BONUS = 0.01
private const val TICKS_PER_LEVEL = 100

/**
 * FMVVM-C4 (Style B): chosen because the ticker runs ~20×/s and many independent readers each
 * need a different field (Tree 2).
 *
 * FMVVM-C4a: public read-only `@Stable` interface…
 */
@Stable
interface LiveGameUiState {
    val levelName: String
    val timeSurvived: Int
    val score: Double

    /** 0..1 within the current level; changes every tick, so it is read in draw (FMVVM-I3). */
    val levelProgress: Float

    /** FMVVM-C4d: changes element by element. */
    val milestones: List<Int>
    val isRunning: Boolean
}

/** …FMVVM-C4a: private mutable implementation. FMVVM-C4c: primitive specialisations. */
private class MutableLiveGameUiState : LiveGameUiState {
    override var levelName by mutableStateOf("Level 1 - Stay Alive!")
    override var timeSurvived by mutableIntStateOf(0)
    override var score by mutableDoubleStateOf(0.0)
    override var levelProgress by mutableFloatStateOf(0f)
    override val milestones = mutableStateListOf<Int>()
    override var isRunning by mutableStateOf(false)
}

class LiveGameViewModel : ViewModel() {
    private val _uiState = MutableLiveGameUiState()
    val uiState: LiveGameUiState = _uiState // FMVVM-L5: read-only face only
    private var ticker: Job? = null

    fun start() {
        if (ticker?.isActive == true) return
        _uiState.isRunning = true // single-property write: may be bare (C4b)
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(FRAME_MS)
                tick()
            }
        }
    }

    fun pause() {
        ticker?.cancel()
        _uiState.isRunning = false
    }

    /**
     * FMVVM-C4b: a write touching several properties goes in `withMutableSnapshot`, so readers
     * never see time updated without score (isolation) and all fields apply together
     * (atomicity). FMVVM-C7: computed into locals first.
     */
    fun tick() {
        Snapshot.withMutableSnapshot {
            val time = _uiState.timeSurvived + 1
            val score = _uiState.score + POINTS_PER_TICK + TIME_BONUS * time
            _uiState.timeSurvived = time
            _uiState.score = score
            _uiState.levelProgress = (time % TICKS_PER_LEVEL) / TICKS_PER_LEVEL.toFloat()
            if (time % TICKS_PER_LEVEL == 0) _uiState.milestones += time
        }
    }
}

class LiveGameFragment : Fragment() {
    private val viewModel: LiveGameViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        LiveGameScreen(
            state = viewModel.uiState, // NO read here — just a reference, so this never recomposes
            onStartClick = viewModel::start,
            onPauseClick = viewModel::pause,
            onBack = { navigateUp() }
        )
    }
}

@Composable
fun LiveGameScreen(
    state: LiveGameUiState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(
        title = "Game · snapshot state",
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FMVVM-C8 (Style B): pass the state object; each child reads inside itself.
            Header(state)
            ScoreBadge(state)
            // FMVVM-I3: pass a provider; the bar reads it in the draw phase.
            LevelProgressBar(progress = { state.levelProgress })
            Milestones(state)
            Controls(state, onStart = onStartClick, onPause = onPauseClick)
            RuleNote(
                "Each tick invalidates only ScoreBadge; the progress bar only redraws. " +
                    "The Fragment's content and Header never rerun."
            )
        }
    }
}

@Composable
private fun Header(state: LiveGameUiState) {
    Text(state.levelName, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ScoreBadge(state: LiveGameUiState) {
    Text("${state.timeSurvived} ticks · ${String.format(Locale.US, "%.2f", state.score)}")
}

@Composable
private fun LevelProgressBar(progress: () -> Float, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Spacer(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .drawBehind {
                drawRect(track)
                drawRect(color, size = size.copy(width = size.width * progress()))
            }
    )
}

@Composable
private fun Milestones(state: LiveGameUiState) {
    Text("Levels cleared at tick: ${state.milestones.joinToString().ifEmpty { "–" }}")
}

@Composable
private fun Controls(state: LiveGameUiState, onStart: () -> Unit, onPause: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onStart, enabled = !state.isRunning) { Text("Start") }
        OutlinedButton(onClick = onPause, enabled = state.isRunning) { Text("Pause") }
    }
}
