package app.lumadocs.kmp.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay

/**
 * A [PagingSource] that pages through an already-in-memory list, [pageSize] items at a time.
 *
 * The Documents screen filters/sorts/searches the full file list on the client, so a
 * server-side paging source would break those features. This source keeps that behaviour
 * while still loading the visible list incrementally: the first page appears instantly and
 * each following page shows a short loading indicator before it appears (see [loadDelayMs]).
 */
class SnapshotPagingSource<T : Any>(
    private val items: List<T>,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val loadDelayMs: Long = NEXT_PAGE_DELAY_MS,
) : PagingSource<Int, T>() {

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        val anchor = state.anchorPosition ?: return null
        return anchor / pageSize
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 0
        // Give pages after the first a brief loading moment so the loader is visible.
        if (page > 0 && loadDelayMs > 0) delay(loadDelayMs)

        val from = page * pageSize
        if (from >= items.size) {
            return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        }
        val to = minOf(from + pageSize, items.size)
        return LoadResult.Page(
            data = items.subList(from, to),
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (to >= items.size) null else page + 1,
        )
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 10
        const val NEXT_PAGE_DELAY_MS = 600L
    }
}
