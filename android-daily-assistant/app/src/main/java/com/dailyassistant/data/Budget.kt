package com.dailyassistant.data

import com.dailyassistant.util.Dates
import java.time.LocalDate
import java.time.YearMonth

/**
 * Everything the dashboard needs, derived from the salary, the pay period and
 * what has actually been spent. Pure arithmetic — no I/O, so it is easy to trust.
 */
data class BudgetSnapshot(
    val periodStart: LocalDate = Dates.today(),
    val periodEnd: LocalDate = Dates.today(),
    val today: LocalDate = Dates.today(),
    val salaryMinor: Long = 0,
    val extraIncomeMinor: Long = 0,
    val savingsTargetMinor: Long = 0,
    val billsUnpaidMinor: Long = 0,
    val spendableMinor: Long = 0,
    val spentPeriodMinor: Long = 0,
    val spentTodayMinor: Long = 0,
    val dailyAllowanceMinor: Long = 0,
    val projectedPeriodMinor: Long = 0
) {
    val daysInPeriod: Int get() = (periodEnd.toEpochDay() - periodStart.toEpochDay() + 1).toInt().coerceAtLeast(1)
    val dayIndex: Int get() = (today.toEpochDay() - periodStart.toEpochDay() + 1).toInt().coerceIn(1, daysInPeriod)
    val daysLeft: Int get() = (periodEnd.toEpochDay() - today.toEpochDay() + 1).toInt().coerceAtLeast(1)
    val daysToPayday: Long get() = Dates.daysUntil(today, periodEnd.plusDays(1))

    val incomeMinor: Long get() = salaryMinor + extraIncomeMinor
    val remainingMinor: Long get() = spendableMinor - spentPeriodMinor
    val todayRemainingMinor: Long get() = dailyAllowanceMinor - spentTodayMinor
    val spentFraction: Float
        get() = if (spendableMinor <= 0) 0f else (spentPeriodMinor.toDouble() / spendableMinor).toFloat()
    val todayFraction: Float
        get() = if (dailyAllowanceMinor <= 0) 0f else (spentTodayMinor.toDouble() / dailyAllowanceMinor).toFloat()
    val onTrack: Boolean get() = spendableMinor <= 0 || projectedPeriodMinor <= spendableMinor
    val overspentToday: Boolean get() = dailyAllowanceMinor > 0 && spentTodayMinor > dailyAllowanceMinor
}

object Budget {

    fun compute(
        settings: Settings,
        periodEntries: List<Entry>,
        bills: List<Bill>,
        today: LocalDate = Dates.today()
    ): BudgetSnapshot {
        val period = Dates.payPeriod(today, settings.paydayDay)
        val start = period.start
        val end = period.endInclusive
        val todayEpoch = today.toEpochDay()

        val spentPeriod = periodEntries.filter { !it.isIncome }.sumOf { it.amountMinor }
        val spentBeforeToday = periodEntries.filter { !it.isIncome && it.epochDay < todayEpoch }.sumOf { it.amountMinor }
        val spentToday = periodEntries.filter { !it.isIncome && it.epochDay == todayEpoch }.sumOf { it.amountMinor }
        val extraIncome = periodEntries.filter { it.isIncome }.sumOf { it.amountMinor }

        val savingsTarget = settings.salaryMinor * settings.savingsPercent.coerceIn(0, 90) / 100

        // Bills that fall inside this pay period and have not been paid for their own month yet.
        val billsUnpaid = bills.filter { it.active }.sumOf { bill ->
            val dueDate = dueDateInPeriod(bill, start, end) ?: return@sumOf 0L
            val paid = bill.lastPaidYearMonth == Dates.yearMonthKey(dueDate)
            if (paid) 0L else bill.amountMinor
        }

        val spendable = (settings.salaryMinor + extraIncome - savingsTarget - billsUnpaid).coerceAtLeast(0)

        val daysInPeriod = (end.toEpochDay() - start.toEpochDay() + 1).coerceAtLeast(1)
        val daysLeft = (end.toEpochDay() - todayEpoch + 1).coerceAtLeast(1)
        val dayIndex = (todayEpoch - start.toEpochDay() + 1).coerceIn(1, daysInPeriod)

        val dailyAllowance = if (settings.carryOver) {
            ((spendable - spentBeforeToday).coerceAtLeast(0)) / daysLeft
        } else {
            spendable / daysInPeriod
        }

        val projected = if (dayIndex <= 0) spentPeriod else spentPeriod * daysInPeriod / dayIndex

        return BudgetSnapshot(
            periodStart = start,
            periodEnd = end,
            today = today,
            salaryMinor = settings.salaryMinor,
            extraIncomeMinor = extraIncome,
            savingsTargetMinor = savingsTarget,
            billsUnpaidMinor = billsUnpaid,
            spendableMinor = spendable,
            spentPeriodMinor = spentPeriod,
            spentTodayMinor = spentToday,
            dailyAllowanceMinor = dailyAllowance,
            projectedPeriodMinor = projected
        )
    }

    /** The date this bill is due inside the given window, if it falls in it at all. */
    fun dueDateInPeriod(bill: Bill, start: LocalDate, end: LocalDate): LocalDate? {
        var month = YearMonth.from(start)
        val lastMonth = YearMonth.from(end)
        while (!month.isAfter(lastMonth)) {
            val candidate = Dates.clampDay(month, bill.dueDayOfMonth)
            if (!candidate.isBefore(start) && !candidate.isAfter(end)) return candidate
            month = month.plusMonths(1)
        }
        return null
    }

    /** Next due date from today onwards, used by the bills screen. */
    fun nextDueDate(bill: Bill, today: LocalDate = Dates.today()): LocalDate {
        val thisMonth = Dates.clampDay(YearMonth.from(today), bill.dueDayOfMonth)
        return if (!thisMonth.isBefore(today)) thisMonth
        else Dates.clampDay(YearMonth.from(today).plusMonths(1), bill.dueDayOfMonth)
    }

    fun isPaidThisCycle(bill: Bill, today: LocalDate = Dates.today()): Boolean =
        bill.lastPaidYearMonth == Dates.yearMonthKey(today)
}
