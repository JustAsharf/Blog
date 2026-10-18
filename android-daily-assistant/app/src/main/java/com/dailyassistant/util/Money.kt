package com.dailyassistant.util

import java.util.Locale
import kotlin.math.abs

object Money {

    /**
     * Reads a typed amount into minor units, accepting either separator:
     * "1234.5" and "1234,50" are decimals, "1,234.50" is grouped with decimals,
     * and "1,500" / "1.500" — exactly three trailing digits — is grouping, so it
     * reads as 1500. Four or more trailing digits are decimals, rounded to two.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(" ", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        val normalised = normaliseSeparators(cleaned)
        val value = normalised.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return Math.round(value * 100.0)
    }

    private fun normaliseSeparators(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
        val lastDot = filtered.lastIndexOf('.')
        val lastComma = filtered.lastIndexOf(',')
        val decimalAt = maxOf(lastDot, lastComma)
        if (decimalAt < 0) return filtered
        val decimals = filtered.length - decimalAt - 1
        // Exactly three digits after the separator means it was a thousands
        // separator, not a decimal point: "1,500" and "1.500" are both 1500.
        if (decimals == 3) return filtered.filter { it.isDigit() || it == '-' }
        val whole = filtered.substring(0, decimalAt).filter { it.isDigit() || it == '-' }
        val fraction = filtered.substring(decimalAt + 1).filter { it.isDigit() }
        return if (whole.isEmpty() || whole == "-") whole + "0." + fraction else "$whole.$fraction"
    }

    /** 123450 -> "1,234.50" (no currency). */
    fun plain(minor: Long): String =
        String.format(Locale.US, "%,.2f", minor / 100.0)

    /** 123450 -> "1,234" when it is a whole amount, otherwise "1,234.50". */
    fun compact(minor: Long): String =
        if (minor % 100L == 0L) String.format(Locale.US, "%,d", minor / 100)
        else String.format(Locale.US, "%,.2f", minor / 100.0)

    fun format(minor: Long, currency: String): String {
        val sign = if (minor < 0) "-" else ""
        val body = compact(abs(minor))
        return if (currency.length <= 2) "$sign$currency$body" else "$sign$body $currency"
    }

    fun formatSigned(minor: Long, currency: String): String =
        (if (minor > 0) "+" else "") + format(minor, currency)
}
