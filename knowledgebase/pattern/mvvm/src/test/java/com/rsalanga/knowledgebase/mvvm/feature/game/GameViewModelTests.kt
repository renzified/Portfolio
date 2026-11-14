package com.rsalanga.knowledgebase.mvvm.feature.game

import com.rsalanga.knowledgebase.mvvm.feature.game.snapshot.LiveGameViewModel
import com.rsalanga.knowledgebase.mvvm.feature.game.stateflow.GameViewModel
import com.rsalanga.knowledgebase.mvvm.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GameViewModelTests {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `StateFlow style - tick updates time and score atomically`() {
        val viewModel = GameViewModel()

        viewModel.tick()

        val state = viewModel.uiState.value
        assertEquals(1, state.timeSurvived)
        assertEquals(10.01, state.score, 1e-9) // MVVM-C7: score uses the NEW time
    }

    @Test
    fun `snapshot style - read properties directly, no collector needed`() {
        // MVVM-T3: withMutableSnapshot applies to the global snapshot when it returns.
        val viewModel = LiveGameViewModel()

        viewModel.tick()

        assertEquals(1, viewModel.uiState.timeSurvived)
        assertEquals(10.01, viewModel.uiState.score, 1e-9) // same result as Style A (C7)
    }

    @Test
    fun `snapshot style - milestone list grows element by element`() {
        val viewModel = LiveGameViewModel()

        repeat(200) { viewModel.tick() }

        assertEquals(listOf(100, 200), viewModel.uiState.milestones) // MVVM-C4d
        assertEquals(0f, viewModel.uiState.levelProgress, 0f)
    }
}
