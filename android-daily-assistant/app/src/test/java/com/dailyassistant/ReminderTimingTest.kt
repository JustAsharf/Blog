package com.dailyassistant

import com.dailyassistant.data.Reminder
import com.dailyassistant.data.RepeatMode
import com.dailyassistant.util.ReminderTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderTimingTest {

    /** A Sunday afternoon. */
    private val from = LocalDateTime.of(2026, 10, 18, 14, 30)

    private fun reminder(
        minuteOfDay: Int,
        start: LocalDate,
        mode: String,
        mask: Int = 0
    ) = Reminder(
        title = "test",
        minuteOfDay = minuteOfDay,
        startEpochDay = start.toEpochDay(),
        repeatMode = mode,
        daysOfWeekMask = mask
    )

    @Test
    fun `a one-off fires once and then never again`() {
        val later = reminder(18 * 60, LocalDate.of(2026, 10, 18), RepeatMode.ONCE)
        assertEquals(LocalDateTime.of(2026, 10, 18, 18, 0), ReminderTiming.next(later, from))

        val past = reminder(9 * 60, LocalDate.of(2026, 10, 18), RepeatMode.ONCE)
        assertNull(ReminderTiming.next(past, from))
    }

    @Test
    fun `a daily reminder rolls to tomorrow once today's time has gone`() {
        val later = reminder(21 * 60, LocalDate.of(2026, 10, 1), RepeatMode.DAILY)
        assertEquals(LocalDateTime.of(2026, 10, 18, 21, 0), ReminderTiming.next(later, from))

        val past = reminder(8 * 60, LocalDate.of(2026, 10, 1), RepeatMode.DAILY)
        assertEquals(LocalDateTime.of(2026, 10, 19, 8, 0), ReminderTiming.next(past, from))
    }

    @Test
    fun `a reminder that starts in the future waits for its start date`() {
        val future = reminder(21 * 60, LocalDate.of(2026, 11, 1), RepeatMode.DAILY)
        assertEquals(LocalDateTime.of(2026, 11, 1, 21, 0), ReminderTiming.next(future, from))
    }

    @Test
    fun `a weekly reminder picks the next selected weekday`() {
        // Monday and Wednesday.
        val weekly = reminder(7 * 60, LocalDate.of(2026, 10, 1), RepeatMode.WEEKLY, (1 shl 0) or (1 shl 2))
        assertEquals(LocalDateTime.of(2026, 10, 19, 7, 0), ReminderTiming.next(weekly, from))
        assertEquals(
            LocalDateTime.of(2026, 10, 21, 7, 0),
            ReminderTiming.next(weekly, LocalDateTime.of(2026, 10, 19, 20, 0))
        )
    }

    @Test
    fun `a weekly reminder with no weekday selected uses the start date's weekday`() {
        val weekly = reminder(7 * 60, LocalDate.of(2026, 10, 1), RepeatMode.WEEKLY)
        assertEquals("THURSDAY", ReminderTiming.next(weekly, from)?.dayOfWeek?.toString())
    }

    @Test
    fun `a monthly reminder clamps to the last day of a short month`() {
        val monthly = reminder(10 * 60, LocalDate.of(2026, 1, 31), RepeatMode.MONTHLY)
        assertEquals(
            LocalDateTime.of(2026, 2, 28, 10, 0),
            ReminderTiming.next(monthly, LocalDateTime.of(2026, 2, 1, 0, 0))
        )
        assertEquals(LocalDateTime.of(2026, 10, 31, 10, 0), ReminderTiming.next(monthly, from))
        assertEquals(
            LocalDateTime.of(2026, 11, 30, 10, 0),
            ReminderTiming.next(monthly, LocalDateTime.of(2026, 10, 31, 23, 0))
        )
    }
}
