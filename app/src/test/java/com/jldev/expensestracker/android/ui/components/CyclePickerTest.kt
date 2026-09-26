package com.jldev.expensestracker.android.ui.components

import com.jldev.expensestracker.android.data.cycle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CyclePickerTest {

    private val cycles = listOf(cycle("sep", "2026-09-01"), cycle("aug", "2026-08-01"))

    @Test
    fun `menu lists the newest cycles unless showing all`() {
        val many = (0 until 30).map { month ->
            val start = LocalDate.of(2026, 9, 1).minusMonths(month.toLong())
            cycle("c$month", start.toString())
        }
        assertEquals(many.take(RECENT_CYCLES), visibleCycles(many, showAll = false))
        assertEquals(many, visibleCycles(many, showAll = true))
        assertEquals(cycles, visibleCycles(cycles, showAll = false))
    }
}
