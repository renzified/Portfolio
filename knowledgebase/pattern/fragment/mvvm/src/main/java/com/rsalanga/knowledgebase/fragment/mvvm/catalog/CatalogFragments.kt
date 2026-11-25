package com.rsalanga.knowledgebase.fragment.mvvm.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.EmptyState
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.ChapterKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

// The catalog is static data and takes no key: no business logic, so no ViewModel
// (FMVVM-S2 / E1). A Fragment without a ViewModel is still split into Fragment + stateless
// Screen (FMVVM-L2).

class CatalogFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen {
        CatalogScreen(
            chapters = RuleCatalog,
            onChapterClick = { number -> findNavController().navigate(ChapterKey(number)) }
        )
    }
}

@Composable
fun CatalogScreen(
    chapters: List<Chapter>,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(
        title = "Fragment MVVM house rules",
        onBack = null,
        modifier = modifier
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(items = chapters, key = { it.number }) { chapter ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onChapterClick(chapter.number) }
                        .padding(16.dp)
                ) {
                    Text("Chapter ${chapter.number}", style = MaterialTheme.typography.labelMedium)
                    Text(chapter.title, style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
            }
        }
    }
}

/** `chapter == null` means the key named a chapter that doesn't exist. */
data class ChapterUiState(val chapter: Chapter?)

/**
 * FMVVM-N2: the chapter is static data, but the Fragment still doesn't read its own arguments.
 * Every key value reaches the screen through the destination's VM, which gets it in its
 * constructor from the Koin definition (`SavedStateHandle.toRoute<ChapterKey>()`).
 */
class ChapterViewModel(number: Int) : ViewModel() {
    val uiState: StateFlow<ChapterUiState> =
        MutableStateFlow(ChapterUiState(RuleCatalog.firstOrNull { it.number == number }))
            .asStateFlow()
}

class ChapterFragment : Fragment() {
    private val viewModel: ChapterViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        ChapterScreen(
            chapter = state.chapter,
            onDemoClick = { demoKey -> findNavController().navigate(demoKey) },
            onBack = { navigateUp() }
        )
    }
}

@Composable
fun ChapterScreen(
    chapter: Chapter?,
    onDemoClick: (Any) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(
        title = chapter?.let { "${it.number}. ${it.title}" } ?: "Chapter",
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        if (chapter == null) {
            EmptyState("Unknown chapter", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(items = chapter.demos, key = { it.title }) { demo ->
                    DemoRow(demo = demo, onClick = demo.key?.let { key -> { onDemoClick(key) } })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DemoRow(demo: Demo, onClick: (() -> Unit)?) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        Modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(16.dp)
    ) {
        Text(demo.title, style = MaterialTheme.typography.titleMedium)
        Text(
            demo.rules,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(demo.description, style = MaterialTheme.typography.bodySmall)
        if (onClick == null) {
            Text("Not a screen: see the file above.", style = MaterialTheme.typography.labelSmall)
        }
    }
}
