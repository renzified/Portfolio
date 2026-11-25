package com.rsalanga.knowledgebase.compose.mvvm.feature.profile

import com.rsalanga.knowledgebase.compose.mvvm.data.Profile
import com.rsalanga.knowledgebase.compose.mvvm.testing.FakeProfileRepository
import com.rsalanga.knowledgebase.compose.mvvm.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeProfileRepository()

    @Test
    fun `nothing loads until the UI subscribes`() {
        ProfileViewModel(repository)
        assertEquals(0, repository.calls) // MVVM-P3: no work in init {}
    }

    @Test
    fun `retry reloads after a failure`() = runTest {
        val viewModel = ProfileViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        assertTrue(viewModel.uiState.value is ProfileUiState.Error)

        repository.nextResult = Result.success(Profile("Ana", "ana@example.com"))
        viewModel.retry()

        assertEquals(
            ProfileUiState.Content(ProfileUi("Ana", "ana@example.com")),
            viewModel.uiState.value
        )
        assertEquals(2, repository.calls)
    }
}
