package com.rsalanga.knowledgebase.mvvm.catalog

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.rsalanga.knowledgebase.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.mvvm.core.ui.EmptyState

// The catalog is static data: there is no business logic, so no ViewModel (MVVM-S2 / E1).

@Composable
fun CatalogRoute(onOpenChapter: (Int) -> Unit) {
    CatalogScreen(chapters = RuleCatalog, onChapterClick = onOpenChapter)
}

@Composable
fun CatalogScreen(
    chapters: List<Chapter>,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "MVVM house rules", onBack = null, modifier = modifier) { padding ->
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

@Composable
fun ChapterRoute(number: Int, onOpenDemo: (NavKey) -> Unit, onBack: () -> Unit) {
    ChapterScreen(
        chapter = RuleCatalog.firstOrNull { it.number == number },
        onDemoClick = onOpenDemo,
        onBack = onBack
    )
}

@Composable
fun ChapterScreen(
    chapter: Chapter?,
    onDemoClick: (NavKey) -> Unit,
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
