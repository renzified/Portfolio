package com.rsalanga.knowledgebase.fragment.mvvm.feature.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.paging.map
import com.rsalanga.knowledgebase.fragment.mvvm.R
import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.ErrorState
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.feed.FeedRepository
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.viewModel

data class PostUi(val id: Long, val title: String)

/**
 * FMVVM-P6 (explicit exception to C1): PagingData is a stream, not a value, so it can't sit in a
 * UiState data class. It is a separate flow, `cachedIn(viewModelScope)` so it survives rotation
 * with a single upstream. This screen needs no other state, so there is no `uiState`.
 */
class FeedViewModel(repository: FeedRepository) : ViewModel() {
    val posts: Flow<PagingData<PostUi>> = repository.pagedPosts()
        .map { page -> page.map { PostUi(it.id, it.title) } }
        .cachedIn(viewModelScope)
}

class FeedFragment : Fragment() {
    private val viewModel: FeedViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val posts = viewModel.posts.collectAsLazyPagingItems()
        FeedScreen(posts = posts, onBack = { navigateUp() })
    }
}

@Composable
fun FeedScreen(posts: LazyPagingItems<PostUi>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    DemoScaffold(title = "Paged feed", onBack = onBack, modifier = modifier) { padding ->
        // Loading / error of the list itself come from loadState, not from a UiState.
        when (posts.loadState.refresh) {
            is LoadState.Loading -> Box(Modifier.padding(padding).fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            is LoadState.Error -> ErrorState(
                message = UiText.Res(R.string.error_generic),
                onRetryClick = posts::retry,
                modifier = Modifier.padding(padding)
            )

            is LoadState.NotLoading -> LazyColumn(Modifier.padding(padding)) {
                item { RuleNote("P6: Flow<PagingData> + cachedIn(viewModelScope).") }
                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id },
                    contentType = posts.itemContentType { "post" }
                ) { index ->
                    posts[index]?.let { post -> Text(post.title, Modifier.padding(16.dp)) }
                }
                if (posts.loadState.append is LoadState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp)) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }
}
