package com.jldev.expensestracker.android.ui.components

import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CyclePickerTest {

    private val cycles = listOf(
        CycleListItem(3, "Sept", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
        CycleListItem(2, "Aug", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
    )

    @Test
    fun `active cycle is the one containing today, bounds included`() {
        assertEquals(2, activeCycle(cycles, LocalDate.of(2026, 8, 31))?.id)
        assertEquals(3, activeCycle(cycles, LocalDate.of(2026, 9, 1))?.id)
    }

    @Test
    fun `falls back to the newest cycle`() {
        assertEquals(3, activeCycle(cycles, LocalDate.of(2026, 10, 2))?.id)
        assertNull(activeCycle(emptyList()))
    }
}
