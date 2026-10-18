package com.dailyassistant.data

import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import java.time.LocalDate

enum class Tone { GOOD, INFO, WARN, BAD }

data class Insight(val emoji: String, val text: String, val tone: Tone)

/**
 * Plain rules over the local data — no model, no network, no account.
 * Everything here runs in a few microseconds on the device.
 */
object Insights {

    fun build(
        settings: Settings,
        budget: BudgetSnapshot,
        periodEntries: List<Entry>,
        allRecentEntries: List<Entry>,
        categories: List<Category>,
        bills: List<Bill>,
        goals: List<Goal>,
        debts: List<Debt>,
        today: LocalDate = Dates.today()
    ): List<Insight> {
        val currency = settings.currency
        val out = ArrayList<Insight>()

        if (!settings.hasSalary) {
            out += Insight("👋", "Add your monthly salary in Settings and the daily budget starts working.", Tone.INFO)
            return out
        }

        // 1. Today's allowance.
        out += if (budget.todayRemainingMinor >= 0) {
            Insight("✅", "You can still spend ${Money.format(budget.todayRemainingMinor, currency)} today.", Tone.GOOD)
        } else {
            Insight("⚠️", "You are ${Money.format(-budget.todayRemainingMinor, currency)} over today's allowance.", Tone.WARN)
        }

        // 2. Where the money is going this cycle.
        val byCategory = periodEntries.filter { !it.isIncome && it.categoryId != null }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
        val topCategory = byCategory.maxByOrNull { it.value }
        if (topCategory != null && budget.spentPeriodMinor > 0) {
            val name = categories.firstOrNull { it.id == topCategory.key }?.let { it.emoji + " " + it.name } ?: "Uncategorised"
            val share = topCategory.value * 100 / budget.spentPeriodMinor
            out += Insight("📊", "$name is your biggest spend this cycle: ${Money.format(topCategory.value, currency)} ($share%).", Tone.INFO)
        }

        // 3. Burn-rate projection to the end of the cycle.
        if (budget.spentPeriodMinor > 0 && budget.spendableMinor > 0) {
            val diff = budget.projectedPeriodMinor - budget.spendableMinor
            out += if (diff > 0) {
                Insight("📉", "At this pace you will end the cycle ${Money.format(diff, currency)} over budget. Cutting ${Money.format(diff / budget.daysLeft, currency)} a day fixes it.", Tone.BAD)
            } else {
                Insight("📈", "At this pace you will finish the cycle with ${Money.format(-diff, currency)} to spare.", Tone.GOOD)
            }
        }

        // 4. This week against last week.
        val thisWeekStart = today.minusDays(6).toEpochDay()
        val lastWeekStart = today.minusDays(13).toEpochDay()
        val thisWeek = allRecentEntries.filter { !it.isIncome && it.epochDay in thisWeekStart..today.toEpochDay() }.sumOf { it.amountMinor }
        val lastWeek = allRecentEntries.filter { !it.isIncome && it.epochDay in lastWeekStart until thisWeekStart }.sumOf { it.amountMinor }
        if (lastWeek > 0 && thisWeek > 0) {
            val change = (thisWeek - lastWeek) * 100 / lastWeek
            out += when {
                change >= 15 -> Insight("🔺", "This week you spent $change% more than last week (${Money.format(thisWeek, currency)} vs ${Money.format(lastWeek, currency)}).", Tone.WARN)
                change <= -15 -> Insight("🔽", "This week you spent ${-change}% less than last week. Nicely done.", Tone.GOOD)
                else -> Insight("⚖️", "Your weekly spending is steady (${Money.format(thisWeek, currency)} this week).", Tone.INFO)
            }
        }

        // 5. No-spend days, a habit worth seeing.
        val daysWithSpend = periodEntries.filter { !it.isIncome }.map { it.epochDay }.toSet()
        val noSpendDays = (0 until budget.dayIndex).count { offset ->
            !daysWithSpend.contains(budget.periodStart.toEpochDay() + offset)
        }
        if (budget.dayIndex >= 3) {
            out += Insight("🌱", "$noSpendDays no-spend day(s) out of ${budget.dayIndex} so far this cycle.", if (noSpendDays > 0) Tone.GOOD else Tone.INFO)
        }

        // 6. Biggest single expense of the cycle.
        periodEntries.filter { !it.isIncome }.maxByOrNull { it.amountMinor }?.let { biggest ->
            if (biggest.amountMinor > 0) {
                val label = biggest.note.ifBlank {
                    categories.firstOrNull { it.id == biggest.categoryId }?.name ?: "an expense"
                }
                out += Insight("🧾", "Biggest single expense: ${Money.format(biggest.amountMinor, currency)} on $label (${Dates.dayLabel(Dates.of(biggest.epochDay))}).", Tone.INFO)
            }
        }

        // 7. Bills landing in the next week.
        val soon = bills.filter { it.active && !Budget.isPaidThisCycle(it, today) }
            .map { it to Budget.nextDueDate(it, today) }
            .filter { Dates.daysUntil(today, it.second) <= 7 }
        if (soon.isNotEmpty()) {
            val total = soon.sumOf { it.first.amountMinor }
            out += Insight("📅", "${soon.size} bill(s) worth ${Money.format(total, currency)} are due within a week.", Tone.WARN)
        }

        // 8. Savings goals.
        goals.firstOrNull { it.targetMinor > 0 && it.savedMinor < it.targetMinor }?.let { goal ->
            val percent = goal.savedMinor * 100 / goal.targetMinor
            val left = goal.targetMinor - goal.savedMinor
            val perDay = goal.deadlineEpochDay?.let { deadline ->
                val days = Dates.daysUntil(today, Dates.of(deadline))
                if (days > 0) left / days else null
            }
            val tail = perDay?.let { " Put aside ${Money.format(it, currency)}/day to make the deadline." } ?: ""
            out += Insight(goal.emoji, "${goal.name}: $percent% saved, ${Money.format(left, currency)} to go.$tail", Tone.INFO)
        }

        // 9. Debts.
        val owed = debts.filter { !it.settled }
        if (owed.isNotEmpty()) {
            val iOwe = owed.filter { it.iOwe }.sumOf { it.amountMinor }
            val owedToMe = owed.filter { !it.iOwe }.sumOf { it.amountMinor }
            if (iOwe > 0) out += Insight("🔻", "You owe ${Money.format(iOwe, currency)} in total.", Tone.WARN)
            if (owedToMe > 0) out += Insight("💰", "People owe you ${Money.format(owedToMe, currency)}.", Tone.INFO)
        }

        // 10. Average day so far.
        if (budget.dayIndex > 1 && budget.spentPeriodMinor > 0) {
            out += Insight("🧮", "Average so far: ${Money.format(budget.spentPeriodMinor / budget.dayIndex, currency)} a day against an allowance of ${Money.format(budget.dailyAllowanceMinor, currency)}.", Tone.INFO)
        }

        // 11. Days until payday.
        out += Insight("📆", "${budget.daysToPayday} day(s) until payday, ${Money.format(budget.remainingMinor.coerceAtLeast(0), currency)} left to spend.", if (budget.remainingMinor > 0) Tone.GOOD else Tone.BAD)

        return out
    }

    /** Totals per day for the last [days] days, oldest first — feeds the bar chart. */
    fun dailyTotals(entries: List<Entry>, days: Int, today: LocalDate = Dates.today()): List<Pair<LocalDate, Long>> {
        val byDay = entries.filter { !it.isIncome }.groupBy { it.epochDay }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            date to (byDay[date.toEpochDay()] ?: 0L)
        }
    }
}
