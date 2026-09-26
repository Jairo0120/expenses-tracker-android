package com.jldev.expensestracker.android.ui.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PagedListTest {

    private val errors = mutableListOf<Exception>()

    /** Serves the numbers 0 until [total], [PAGE] at a time. */
    private fun TestScope.numbers(total: Int, fail: (skip: Int) -> Boolean = { false }) =
        PagedList(scope = this, pageSize = PAGE, onError = { errors += it }) { skip, limit ->
            if (fail(skip)) throw IOException("offline")
            (skip until minOf(skip + limit, total)).toList()
        }

    @Test
    fun `pages accumulate until a short page`() = runTest {
        val list = numbers(total = 7)
        list.refresh()
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 2), list.state.value.items)
        assertFalse(list.state.value.endReached)

        list.loadMore()
        advanceUntilIdle()
        list.loadMore()
        advanceUntilIdle()
        assertEquals((0 until 7).toList(), list.state.value.items)
        assertTrue(list.state.value.endReached)

        list.loadMore() // no-op at the end
        advanceUntilIdle()
        assertEquals(7, list.state.value.items.size)
    }

    @Test
    fun `load more is ignored before the first page and while loading`() = runTest {
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val list = PagedList(this, PAGE, onError = { errors += it }) { skip, _ ->
            calls++
            if (skip > 0) gate.await()
            List(PAGE) { skip + it }
        }
        list.loadMore()
        assertEquals(0, calls)

        list.refresh()
        advanceUntilIdle()
        list.loadMore()
        list.loadMore()
        advanceUntilIdle()
        assertEquals(2, calls)
        assertTrue(list.state.value.isLoadingMore)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(6, list.state.value.items.size)
        assertFalse(list.state.value.isLoadingMore)
    }

    @Test
    fun `refresh discards a stale load in flight`() = runTest {
        var filter = "old"
        val gate = CompletableDeferred<Unit>()
        val list = PagedList(this, PAGE, onError = { errors += it }) { _, _ ->
            val requested = filter
            if (requested == "old") gate.await()
            listOf(requested)
        }
        list.refresh()
        advanceUntilIdle()
        filter = "new"
        list.refresh()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("new"), list.state.value.items)
    }

    @Test
    fun `failed refresh clears items and reports`() = runTest {
        var failing = false
        val list = numbers(total = 10, fail = { failing })
        list.refresh()
        advanceUntilIdle()
        failing = true
        list.refresh()
        advanceUntilIdle()
        assertEquals(PagedState<Int>(), list.state.value)
        assertEquals(1, errors.size)
    }

    @Test
    fun `failed load more keeps loaded items`() = runTest {
        val list = numbers(total = 10, fail = { it > 0 })
        list.refresh()
        advanceUntilIdle()
        list.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 2), list.state.value.items)
        assertFalse(list.state.value.isLoadingMore)
        assertEquals(1, errors.size)
    }

    private companion object {
        const val PAGE = 3
    }
}
