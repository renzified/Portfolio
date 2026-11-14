package com.rsalanga.knowledgebase.mvvm.data.feed

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

private const val PAGE_SIZE = 20
private const val PAGE_COUNT = 10
private const val PAGE_DELAY_MS = 600L

data class Post(val id: Long, val title: String)

interface FeedRepository {
    fun pagedPosts(): Flow<PagingData<Post>>
}

class InMemoryFeedRepository : FeedRepository {
    override fun pagedPosts(): Flow<PagingData<Post>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
            PostPagingSource()
        }.flow
}

private class PostPagingSource : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 0
        delay(PAGE_DELAY_MS)
        val first = page * PAGE_SIZE
        val posts = (first until first + PAGE_SIZE).map { index ->
            Post(id = index.toLong(), title = "Post #${index + 1}")
        }
        return LoadResult.Page(
            data = posts,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (page + 1 < PAGE_COUNT) page + 1 else null
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
        }
}
