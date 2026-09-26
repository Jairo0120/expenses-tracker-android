package com.jldev.expensestracker.android.ui.format

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

// Ports of the Expo app's helpers/utils.js.

private val esCO: Locale = Locale.forLanguageTag("es-CO")

/** `$ 1.234.567`: Colombian pesos, no decimals. */
fun formatMoney(value: Double): String =
    NumberFormat.getCurrencyInstance(esCO).apply {
        currency = Currency.getInstance("COP")
        maximumFractionDigits = 0
    }.format(value)

/** `1.23M`, `450K`, `-12.5K`: rounded up to two decimals. */
fun formatShortMoney(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val absValue = abs(value)
    val (divisor, suffix) = when {
        absValue >= 1_000_000 -> 1_000_000.0 to "M"
        absValue >= 1_000 -> 1_000.0 to "K"
        else -> 1.0 to ""
    }
    val ceiled = ceil(absValue / divisor * 100) / 100
    val text = String.format(Locale.US, "%.2f", ceiled).trimEnd('0').trimEnd('.')
    return "$sign$text$suffix"
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", esCO)

/** A record's timestamp in the device's time zone, e.g. `05 sept 2026, 08:30 p. m.`. */
fun formatDate(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    dateTimeFormatter.format(instant.atZone(zone))

private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM yyyy", esCO)

/** A cycle's month, e.g. `Sept 2026`. */
fun formatShortDate(date: LocalDate): String =
    shortDateFormatter.format(date).replaceFirstChar { it.titlecase(esCO) }

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", esCO)

/** A cycle's month name, e.g. `Septiembre`. */
fun formatMonth(date: LocalDate): String =
    monthFormatter.format(date).replaceFirstChar { it.titlecase(esCO) }
