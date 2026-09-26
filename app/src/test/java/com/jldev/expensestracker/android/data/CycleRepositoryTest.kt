package com.jldev.expensestracker.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CycleRepositoryTest {

    private val cycles = listOf(cycle("sep", "2026-09-01"), cycle("aug", "2026-08-01"))

    @Test
    fun `the cycle the server flags is the active one`() {
        val flagged = listOf(cycle("sep", "2026-09-01"), cycle("aug", "2026-08-01", active = true))
        assertEquals("aug", activeCycle(flagged, LocalDate.of(2026, 9, 15))?.uuid)
    }

    @Test
    fun `without a flag, the cycle containing today, bounds included`() {
        assertEquals("aug", activeCycle(cycles, LocalDate.of(2026, 8, 31))?.uuid)
        assertEquals("sep", activeCycle(cycles, LocalDate.of(2026, 9, 1))?.uuid)
    }

    @Test
    fun `falls back to the newest cycle`() {
        assertEquals("sep", activeCycle(cycles, LocalDate.of(2026, 10, 2))?.uuid)
        assertNull(activeCycle(emptyList()))
    }

    @Test
    fun `saving types are named like the backend capitalizes them`() {
        assertEquals("Ahorro carro", savingTypeName("aHORRO CARRO"))
    }
}
