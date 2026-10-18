package com.dailyassistant

import com.dailyassistant.data.Bill
import com.dailyassistant.data.Budget
import com.dailyassistant.data.Category
import com.dailyassistant.data.Debt
import com.dailyassistant.data.Entry
import com.dailyassistant.data.Goal
import com.dailyassistant.data.Insights
import com.dailyassistant.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightsTest {

    private val today = LocalDate.of(2026, 10, 10)
    private val settings = Settings(salaryMinor = 300_000L, paydayDay = 1, savingsPercent = 10)

    private val categories = listOf(
        Category(id = 1, name = "Food", emoji = "F"),
        Category(id = 2, name = "Transport", emoji = "T")
    )

    private val entries = listOf(
        Entry(amountMinor = 40_000L, categoryId = 1, epochDay = today.minusDays(1).toEpochDay()),
        Entry(amountMinor = 5_000L, categoryId = 2, epochDay = today.toEpochDay(), note = "bus"),
        Entry(amountMinor = 2_000L, categoryId = 1, epochDay = today.toEpochDay())
    )

    private val bills = listOf(Bill(id = 1, name = "Rent", amountMinor = 100_000L, dueDayOfMonth = 12))
    private val goals = listOf(
        Goal(id = 1, name = "Laptop", targetMinor = 200_000L, savedMinor = 50_000L,
            deadlineEpochDay = today.plusDays(100).toEpochDay())
    )
    private val debts = listOf(Debt(id = 1, person = "Ali", amountMinor = 10_000L, iOwe = true))

    @Test
    fun `reports on the things worth knowing`() {
        val budget = Budget.compute(settings, entries, bills, today)
        val insights = Insights.build(settings, budget, entries, entries, categories, bills, goals, debts, today)

        assertTrue(insights.size >= 6)
        assertTrue(insights.any { it.text.contains("Food") })
        assertTrue(insights.any { it.text.contains("bill(s)") })
        assertTrue(insights.any { it.text.contains("Laptop") })
        assertTrue(insights.any { it.text.contains("You owe") })
        assertTrue(insights.any { it.text.contains("until payday") })
    }

    @Test
    fun `without a salary it asks for one and says nothing else`() {
        val budget = Budget.compute(Settings(), emptyList(), emptyList(), today)
        val insights = Insights.build(
            Settings(), budget, emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList(), today
        )
        assertEquals(1, insights.size)
        assertTrue(insights.first().text.contains("salary"))
    }

    @Test
    fun `daily totals fill every day oldest first`() {
        val totals = Insights.dailyTotals(entries, 7, today)
        assertEquals(7, totals.size)
        assertEquals(today.minusDays(6), totals.first().first)
        assertEquals(today, totals.last().first)
        assertEquals(7_000L, totals.last().second)
        assertEquals(40_000L, totals[5].second)
        assertEquals(0L, totals.first().second)
    }

    @Test
    fun `income never shows up as spending in the chart`() {
        val income = listOf(
            Entry(amountMinor = 9_000L, categoryId = null, epochDay = today.toEpochDay(), isIncome = true)
        )
        assertEquals(0L, Insights.dailyTotals(income, 1, today).last().second)
    }
}
