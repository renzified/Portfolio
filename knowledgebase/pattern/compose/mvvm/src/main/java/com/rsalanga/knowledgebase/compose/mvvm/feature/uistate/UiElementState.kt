package com.rsalanga.knowledgebase.compose.mvvm.feature.uistate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.RuleNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val SCROLL_TO_TOP_THRESHOLD = 2
private const val FILLER_ROWS = 40

data class FaqUi(val id: Int, val question: String, val answer: String)

private val Faqs = listOf(
    FaqUi(1, "Why no ViewModel here?", "Nothing on this screen touches business logic (S2)."),
    FaqUi(2, "Why rememberSaveable?", "Losing an expanded section on rotation is annoying (S5)."),
    FaqUi(3, "Why hoist expandedIds?", "\"Expand all\" needs it, so it lives in the parent (S4).")
)

/**
 * MVVM-S3: UI logic that grew past a couple of lines, packaged as a plain state holder.
 * It may hold UI objects ([LazyListState], a composition [CoroutineScope]) — exactly what a
 * ViewModel can't (MVVM-L4).
 */
@Stable
class FeedListState(val listState: LazyListState, private val scope: CoroutineScope) {
    val showScrollToTop: Boolean by derivedStateOf {
        listState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD
    }

    fun scrollToTop() {
        scope.launch { listState.animateScrollToItem(0) }
    }
}

@Composable
fun rememberFeedListState(
    listState: LazyListState = rememberLazyListState(),
    scope: CoroutineScope = rememberCoroutineScope()
): FeedListState = remember(listState, scope) { FeedListState(listState, scope) }

/** Chapter 3: all of this is UI element state, so there is deliberately no ViewModel. */
@Composable
fun UiElementStateRoute(onBack: () -> Unit) {
    val feed = rememberFeedListState()
    // MVVM-I1 table: text only the UI cares about → rememberTextFieldState in the composable.
    val password = rememberTextFieldState()
    // MVVM-S4: hoisted to the lowest common ancestor — the "Expand all" button needs it.
    // MVVM-S5: rememberSaveable, so it survives rotation and process death.
    var expandedIds by rememberSaveable { mutableStateOf(setOf<Int>()) }

    DemoScaffold(
        title = "UI element state",
        onBack = onBack,
        floatingActionButton = {
            if (feed.showScrollToTop) {
                FloatingActionButton(onClick = feed::scrollToTop) { Text("Top") }
            }
        }
    ) { padding ->
        LazyColumn(state = feed.listState, modifier = Modifier.padding(padding).fillMaxSize()) {
            item { RuleNote("S2: password visibility is local remember state.") }
            item { PasswordField(password, Modifier.padding(horizontal = 16.dp)) }
            item { RuleNote("S2: this FAQ keeps its own expanded state.") }
            item { StatefulFaqItem(Faqs.first()) }
            item {
                Column {
                    RuleNote("S4: these share hoisted state so \"Expand all\" can drive them.")
                    TextButton(onClick = { expandedIds = Faqs.map { it.id }.toSet() }) {
                        Text("Expand all")
                    }
                }
            }
            items(items = Faqs, key = { it.id }) { faq ->
                FaqItem(
                    faq = faq,
                    expanded = faq.id in expandedIds,
                    onToggle = { expandedIds = expandedIds.toggle(faq.id) }
                )
            }
            item { RuleNote("S3: scroll down — the Top button comes from FeedListState.") }
            items(count = FILLER_ROWS, key = { "row-$it" }) { index ->
                Text("Row ${index + 1}", Modifier.padding(16.dp))
            }
        }
    }
}

/** MVVM-S2: nobody else cares whether the password is visible. */
@Composable
fun PasswordField(state: TextFieldState, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val mode = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped
    OutlinedSecureTextField(
        state = state,
        label = { Text("Password") },
        textObfuscationMode = mode,
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide" else "Show") }
        },
        modifier = modifier.fillMaxWidth()
    )
}

/** MVVM-S2 + S5: state that only this item uses stays in this item. */
@Composable
fun StatefulFaqItem(faq: FaqUi, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    FaqItem(
        faq = faq,
        expanded = expanded,
        onToggle = { expanded = !expanded },
        modifier = modifier
    )
}

/** Stateless variant: `expanded` is a parameter once a parent needs to control it (MVVM-S4). */
@Composable
fun FaqItem(faq: FaqUi, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(16.dp)
    ) {
        Text(faq.question, style = MaterialTheme.typography.titleSmall)
        AnimatedVisibility(expanded) { Text(faq.answer) }
    }
}

private fun Set<Int>.toggle(id: Int): Set<Int> = if (id in this) this - id else this + id
