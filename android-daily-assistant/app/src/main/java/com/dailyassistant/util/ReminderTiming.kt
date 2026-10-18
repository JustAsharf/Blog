package com.dailyassistant.util

import com.dailyassistant.data.Reminder
import com.dailyassistant.data.RepeatMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/**
 * When a reminder should next fire. Kept free of Android types so the rules can
 * be reasoned about — and tested — on their own.
 */
object ReminderTiming {

    fun next(reminder: Reminder, from: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val time = LocalTime.of((reminder.minuteOfDay / 60) % 24, reminder.minuteOfDay % 60)
        val start = LocalDate.ofEpochDay(reminder.startEpochDay)

        return when (reminder.repeatMode) {
            RepeatMode.ONCE -> {
                val at = LocalDateTime.of(start, time)
                if (at.isAfter(from)) at else null
            }

            RepeatMode.DAILY -> {
                var day = if (start.isAfter(from.toLocalDate())) start else from.toLocalDate()
                if (LocalDateTime.of(day, time).isBefore(from)) day = day.plusDays(1)
                LocalDateTime.of(day, time)
            }

            RepeatMode.WEEKLY -> {
                val mask = if (reminder.daysOfWeekMask == 0) {
                    1 shl (start.dayOfWeek.value - 1)
                } else {
                    reminder.daysOfWeekMask
                }
                var day = if (start.isAfter(from.toLocalDate())) start else from.toLocalDate()
                repeat(8) {
                    val bit = 1 shl (day.dayOfWeek.value - 1)
                    val candidate = LocalDateTime.of(day, time)
                    if (mask and bit != 0 && !candidate.isBefore(from)) return candidate
                    day = day.plusDays(1)
                }
                null
            }

            RepeatMode.MONTHLY -> {
                var month = YearMonth.from(if (start.isAfter(from.toLocalDate())) start else from.toLocalDate())
                repeat(3) {
                    val candidate = LocalDateTime.of(Dates.clampDay(month, start.dayOfMonth), time)
                    if (!candidate.isBefore(from) && !candidate.toLocalDate().isBefore(start)) return candidate
                    month = month.plusMonths(1)
                }
                null
            }

            else -> null
        }
    }
}
