package com.jldev.expensestracker.android.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FormattersTest {

    @Test
    fun `short money matches the Expo helper`() {
        assertEquals("0", formatShortMoney(0.0))
        assertEquals("999", formatShortMoney(999.0))
        assertEquals("1.5K", formatShortMoney(1_500.0))
        assertEquals("1.24M", formatShortMoney(1_234_567.0)) // rounded up
        assertEquals("-12K", formatShortMoney(-12_000.0))
    }

    @Test
    fun `money is COP without decimals`() {
        val text = formatMoney(1_234_567.0)
        assertEquals("1.234.567", text.filter { it.isDigit() || it == '.' })
        assertEquals(true, text.contains("$"))
    }

    @Test
    fun `dates are shown in the local zone`() {
        val bogota = ZoneId.of("America/Bogota")
        // 01:30 UTC is 20:30 the previous day in Bogotá.
        val text = formatDate(Instant.parse("2026-09-05T01:30:00Z"), bogota)
        assertEquals(true, text.startsWith("04 "))
        assertEquals(true, text.contains("08:30"))
    }
}
