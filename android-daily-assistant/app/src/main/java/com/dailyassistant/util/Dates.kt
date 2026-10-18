package com.dailyassistant.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Dates {

    fun today(): LocalDate = LocalDate.now()

    fun epochDay(date: LocalDate): Long = date.toEpochDay()

    fun of(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun monthRange(month: YearMonth): LongRange =
        month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()

    fun clampDay(month: YearMonth, day: Int): LocalDate =
        month.atDay(day.coerceIn(1, month.lengthOfMonth()))

    /**
     * The salary period that contains [date]: starts on the payday of this month
     * (or of the previous month when payday has not happened yet) and ends the
     * day before the next payday.
     */
    fun payPeriod(date: LocalDate, paydayDay: Int): ClosedRange<LocalDate> {
        val thisMonth = YearMonth.from(date)
        val thisPayday = clampDay(thisMonth, paydayDay)
        val start = if (!date.isBefore(thisPayday)) thisPayday else clampDay(thisMonth.minusMonths(1), paydayDay)
        val end = clampDay(YearMonth.from(start).plusMonths(1), paydayDay).minusDays(1)
        return start..end
    }

    fun nextPayday(date: LocalDate, paydayDay: Int): LocalDate =
        payPeriod(date, paydayDay).endInclusive.plusDays(1)

    fun daysUntil(from: LocalDate, to: LocalDate): Long = to.toEpochDay() - from.toEpochDay()

    fun yearMonthKey(date: LocalDate): String = YearMonth.from(date).toString()

    fun minuteOfDayToText(minuteOfDay: Int): String {
        val time = LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60)
        return time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }

    fun dayLabel(date: LocalDate): String {
        val today = today()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.US))
        }
    }

    fun shortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))

    fun monthLabel(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.FULL, Locale.US) + " " + month.year

    fun weekdayInitial(day: DayOfWeek): String =
        day.getDisplayName(TextStyle.SHORT, Locale.US).take(3)

    /** Epoch-millis for [date] at [minuteOfDay] in the device time zone. */
    fun millisAt(date: LocalDate, minuteOfDay: Int): Long {
        val time = LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60)
        return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun nowMillis(): Long = Instant.now().toEpochMilli()
}
