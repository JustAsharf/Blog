package com.dailyassistant

import com.dailyassistant.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DatesTest {

    @Test
    fun `pay cycle starts at the previous payday when payday has not arrived`() {
        val period = Dates.payPeriod(LocalDate.of(2026, 10, 18), 25)
        assertEquals(LocalDate.of(2026, 9, 25), period.start)
        assertEquals(LocalDate.of(2026, 10, 24), period.endInclusive)
    }

    @Test
    fun `pay cycle rolls over on payday itself`() {
        val period = Dates.payPeriod(LocalDate.of(2026, 10, 25), 25)
        assertEquals(LocalDate.of(2026, 10, 25), period.start)
        assertEquals(LocalDate.of(2026, 11, 24), period.endInclusive)
    }

    @Test
    fun `payday after the end of a short month is clamped`() {
        val period = Dates.payPeriod(LocalDate.of(2026, 3, 5), 31)
        assertEquals(LocalDate.of(2026, 2, 28), period.start)
        assertEquals(LocalDate.of(2026, 3, 30), period.endInclusive)
        assertEquals(LocalDate.of(2026, 2, 28), Dates.clampDay(YearMonth.of(2026, 2), 31))
    }

    @Test
    fun `next payday is the day after the cycle ends`() {
        assertEquals(LocalDate.of(2026, 10, 25), Dates.nextPayday(LocalDate.of(2026, 10, 18), 25))
    }

    @Test
    fun `month range covers every day of the month`() {
        assertEquals(31, Dates.monthRange(YearMonth.of(2026, 1)).count())
        assertEquals(28, Dates.monthRange(YearMonth.of(2026, 2)).count())
    }

    @Test
    fun `labels relative days and formats times`() {
        assertEquals("Today", Dates.dayLabel(Dates.today()))
        assertEquals("Yesterday", Dates.dayLabel(Dates.today().minusDays(1)))
        assertEquals("Tomorrow", Dates.dayLabel(Dates.today().plusDays(1)))
        assertEquals("9:05 PM", Dates.minuteOfDayToText(21 * 60 + 5))
        assertEquals("12:00 AM", Dates.minuteOfDayToText(0))
    }
}
