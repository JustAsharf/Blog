package com.dailyassistant.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailyassistant.DailyApp
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import java.time.YearMonth

/**
 * Runs once a day and warns about bills that are due soon or already late,
 * then books itself for tomorrow.
 */
class BillWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DailyApp
        val repo = app.repo
        val today = Dates.today()
        val currency = repo.settings.current.currency

        val due = repo.activeBills().mapNotNull { bill ->
            val dueDate = Dates.clampDay(YearMonth.from(today), bill.dueDayOfMonth)
            val paidThisMonth = bill.lastPaidYearMonth == Dates.yearMonthKey(today)
            val daysLeft = Dates.daysUntil(today, dueDate)
            when {
                paidThisMonth -> null
                daysLeft < 0 -> bill to daysLeft
                daysLeft <= bill.remindDaysBefore -> bill to daysLeft
                else -> null
            }
        }

        if (due.isNotEmpty()) {
            val total = due.sumOf { it.first.amountMinor }
            val text = due.joinToString("\n") { (bill, daysLeft) ->
                val whenText = when {
                    daysLeft < 0 -> "late by ${-daysLeft}d"
                    daysLeft == 0L -> "due today"
                    daysLeft == 1L -> "due tomorrow"
                    else -> "in ${daysLeft}d"
                }
                "• ${bill.name} — ${Money.format(bill.amountMinor, currency)} ($whenText)"
            }
            Notifications.show(
                context = applicationContext,
                notificationId = 777,
                title = "Bills to pay — ${Money.format(total, currency)}",
                text = text,
                route = "bills"
            )
        }

        Scheduler.scheduleBillCheck(applicationContext)
        return Result.success()
    }
}
