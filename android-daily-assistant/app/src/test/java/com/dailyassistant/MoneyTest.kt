package com.dailyassistant

import com.dailyassistant.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `parses plain and decimal amounts`() {
        assertEquals(10_000L, Money.parse("100"))
        assertEquals(12_350L, Money.parse("123.50"))
        assertEquals(12_300L, Money.parse("123.0"))
        assertEquals(12_350L, Money.parse("123,50"))
        assertEquals(50_000L, Money.parse(" 500 "))
    }

    @Test
    fun `treats three trailing digits as grouping`() {
        assertEquals(123_450L, Money.parse("1,234.50"))
        assertEquals(123_400L, Money.parse("1,234"))
        assertEquals(150_000L, Money.parse("1.500"))
        assertEquals(1_234_500L, Money.parse("12.345"))
        assertEquals(100_000_000L, Money.parse("1,000,000"))
        assertEquals(123_450L, Money.parse("1.234,50"))
    }

    @Test
    fun `rounds anything longer than two decimals`() {
        assertEquals(1_235L, Money.parse("12.3456"))
    }

    @Test
    fun `rejects what is not a number`() {
        assertNull(Money.parse("abc"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("   "))
    }

    @Test
    fun `formats with the symbol in the right place`() {
        assertEquals("$1,234", Money.format(123_400L, "$"))
        assertEquals("$1,234.50", Money.format(123_450L, "$"))
        assertEquals("-$12.50", Money.format(-1_250L, "$"))
        assertEquals("1,234 EGP", Money.format(123_400L, "EGP"))
        assertTrue(Money.formatSigned(500L, "$").startsWith("+"))
    }

    @Test
    fun `survives a round trip through its own output`() {
        assertEquals(123_450L, Money.parse(Money.plain(123_450L)))
        assertEquals(123_400L, Money.parse(Money.compact(123_400L)))
        assertEquals(1L, Money.parse(Money.plain(1L)))
    }
}
