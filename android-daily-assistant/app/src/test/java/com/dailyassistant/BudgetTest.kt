package com.dailyassistant

import com.dailyassistant.data.Bill
import com.dailyassistant.data.Budget
import com.dailyassistant.data.Entry
import com.dailyassistant.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetTest {

    private val today = LocalDate.of(2026, 10, 10)

    private val settings = Settings(
        salaryMinor = 300_000L,
        paydayDay = 1,
        savingsPercent = 10,
        carryOver = true
    )

    private val bills = listOf(
        Bill(id = 1, name = "Rent", amountMinor = 100_000L, dueDayOfMonth = 5),
        Bill(id = 2, name = "Net", amountMinor = 20_000L, dueDayOfMonth = 8, lastPaidYearMonth = "2026-10")
    )

    private val entries = listOf(
        Entry(amountMinor = 20_000L, categoryId = null, epochDay = LocalDate.of(2026, 10, 8).toEpochDay()),
        Entry(amountMinor = 5_000L, categoryId = null, epochDay = LocalDate.of(2026, 10, 9).toEpochDay()),
        Entry(amountMinor = 3_000L, categoryId = null, epochDay = today.toEpochDay()),
        Entry(amountMinor = 50_000L, categoryId = null, epochDay = today.toEpochDay(), isIncome = true)
    )

    @Test
    fun `splits the salary cycle into days`() {
        val budget = Budget.compute(settings, entries, bills, today)
        assertEquals(LocalDate.of(2026, 10, 1), budget.periodStart)
        assertEquals(LocalDate.of(2026, 10, 31), budget.periodEnd)
        assertEquals(31, budget.daysInPeriod)
        assertEquals(10, budget.dayIndex)
        assertEquals(22, budget.daysLeft)
        assertEquals(22L, budget.daysToPayday)
    }

    @Test
    fun `spendable money excludes savings and unpaid bills but counts extra income`() {
        val budget = Budget.compute(settings, entries, bills, today)
        assertEquals(50_000L, budget.extraIncomeMinor)
        assertEquals(30_000L, budget.savingsTargetMinor)
        // Only the rent is still outstanding; the internet bill is already paid this month.
        assertEquals(100_000L, budget.billsUnpaidMinor)
        assertEquals(220_000L, budget.spendableMinor)
    }

    @Test
    fun `daily allowance divides what is left by the days that remain`() {
        val budget = Budget.compute(settings, entries, bills, today)
        assertEquals(28_000L, budget.spentPeriodMinor)
        assertEquals(3_000L, budget.spentTodayMinor)
        assertEquals((220_000L - 25_000L) / 22, budget.dailyAllowanceMinor)
        assertEquals(budget.dailyAllowanceMinor - 3_000L, budget.todayRemainingMinor)
        assertEquals(192_000L, budget.remainingMinor)
    }

    @Test
    fun `carry-over off gives a flat allowance for the whole cycle`() {
        val budget = Budget.compute(settings.copy(carryOver = false), entries, bills, today)
        assertEquals(220_000L / 31, budget.dailyAllowanceMinor)
    }

    @Test
    fun `projects the cycle from the pace so far`() {
        val budget = Budget.compute(settings, entries, bills, today)
        assertEquals(28_000L * 31 / 10, budget.projectedPeriodMinor)
        assertTrue(budget.onTrack)
        assertFalse(budget.overspentToday)
    }

    @Test
    fun `reports overspending instead of hiding it`() {
        val heavy = entries + Entry(amountMinor = 250_000L, categoryId = null, epochDay = today.toEpochDay())
        val budget = Budget.compute(settings, heavy, bills, today)
        assertFalse(budget.onTrack)
        assertTrue(budget.remainingMinor < 0)
        assertTrue(budget.overspentToday)
    }

    @Test
    fun `no salary means no budget and no division by zero`() {
        val budget = Budget.compute(Settings(), emptyList(), emptyList(), today)
        assertEquals(0L, budget.spendableMinor)
        assertEquals(0L, budget.dailyAllowanceMinor)
        assertEquals(0f, budget.spentFraction, 0f)
        assertEquals(0f, budget.todayFraction, 0f)
    }

    @Test
    fun `finds the due date inside a window and rolls forward after it`() {
        val rent = bills[0]
        assertEquals(
            LocalDate.of(2026, 10, 5),
            Budget.dueDateInPeriod(rent, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        )
        assertNull(Budget.dueDateInPeriod(rent, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 31)))
        assertEquals(LocalDate.of(2026, 11, 5), Budget.nextDueDate(rent, today))
        assertEquals(LocalDate.of(2026, 10, 5), Budget.nextDueDate(rent, LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun `a bill due on the 31st still lands in a 30-day month`() {
        val card = Bill(name = "Card", amountMinor = 1_000L, dueDayOfMonth = 31)
        assertEquals(
            LocalDate.of(2026, 4, 30),
            Budget.dueDateInPeriod(card, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))
        )
    }

    @Test
    fun `knows which bills are already paid this cycle`() {
        assertTrue(Budget.isPaidThisCycle(bills[1], today))
        assertFalse(Budget.isPaidThisCycle(bills[0], today))
    }
}
