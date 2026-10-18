package com.dailyassistant

import com.dailyassistant.data.Doors
import com.dailyassistant.util.ArabicText
import com.dailyassistant.util.SpendingParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendingParserTest {

    @Test
    fun `reads the sentence from the plan`() {
        val r = SpendingParser.parse("مصروف ألبان بـ 10,000 ومصرف أولاد بـ 15,000")
        assertEquals(2, r.entries.size)
        assertEquals("tariya", r.entries[0].groupId)
        assertEquals("ألبان", r.entries[0].sub)
        assertEquals(1_000_000L, r.entries[0].amountMinor)
        assertEquals("masarif", r.entries[1].groupId)
        assertEquals("مصرف الأولاد", r.entries[1].sub)
        assertEquals(1_500_000L, r.entries[1].amountMinor)
        assertEquals(2_500_000L, r.totalMinor)
    }

    @Test
    fun `understands Arabic-Indic digits and the word alf`() {
        assertEquals(1_200_000L, SpendingParser.parse("دجاج 12 الف").totalMinor)
        assertEquals(2_000_000L, SpendingParser.parse("بنزين ٢٠٠٠٠").totalMinor)
        assertEquals(500_000L, SpendingParser.parse("خبز ٥ الف").totalMinor)
    }

    @Test
    fun `both separators mean thousands`() {
        assertEquals(1_000_000L, SpendingParser.parse("ألبان 10,000").totalMinor)
        assertEquals(1_000_000L, SpendingParser.parse("ألبان 10.000").totalMinor)
    }

    @Test
    fun `the longest category name wins`() {
        val r = SpendingParser.parse("لحم مفروم 12000")
        assertEquals(1, r.entries.size)
        assertEquals("لحم مفروم", r.entries[0].sub)
    }

    @Test
    fun `fuel stays outside the monthly tree`() {
        val r = SpendingParser.parse("بنزين 25,000")
        assertEquals(Doors.GROUP_FUEL, r.entries[0].groupId)
        assertEquals(false, Doors.group(Doors.GROUP_FUEL)!!.insideMonthly)
    }

    @Test
    fun `spelling variants still match`() {
        assertEquals("مصرف الزوجة", SpendingParser.parse("مصرف الزوجه 20000").entries[0].sub)
        assertEquals("مصرف السيارة", SpendingParser.parse("السياره 30000").entries[0].sub)
        assertEquals("مراجعة طبيب", SpendingParser.parse("دكتور 15000").entries[0].sub)
    }

    @Test
    fun `four items in one long sentence`() {
        val r = SpendingParser.parse("مصروف ألبان بـ 10,000 ومصرف أولاد بـ 15,000 ودجاج 12 الف وبنزين ٢٠٠٠٠")
        assertEquals(4, r.entries.size)
        assertEquals(5_700_000L, r.totalMinor)
    }

    @Test
    fun `an amount with no category is reported, not guessed`() {
        val r = SpendingParser.parse("50000")
        assertTrue(r.isEmpty)
        assertEquals(1, r.orphanAmounts)
    }

    @Test
    fun `a category with no amount adds nothing`() {
        assertTrue(SpendingParser.parse("ألبان").isEmpty)
    }

    @Test
    fun `empty input is empty, not an error`() {
        assertTrue(SpendingParser.parse("").isEmpty)
        assertTrue(SpendingParser.parse("   ").isEmpty)
    }

    @Test
    fun `normalisation folds the spellings people mix`() {
        assertEquals("ا", ArabicText.normalise("أ"))
        assertEquals("الاولاد", ArabicText.normalise("الأولاد"))
        assertEquals("مراجعه", ArabicText.normalise("مُراجعة"))
        assertEquals("2026", ArabicText.latinDigits("٢٠٢٦"))
    }

    @Test
    fun `every group and sub in the four-door tree is reachable by name`() {
        Doors.all.forEach { group ->
            group.subs.forEach { sub ->
                val r = SpendingParser.parse("$sub 1000")
                assertTrue("لم يُتعرَّف على $sub", r.entries.any { it.sub == sub && it.groupId == group.id })
            }
        }
    }
}
