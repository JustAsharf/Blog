package com.dailyassistant.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dailyassistant.data.Reminder
import com.dailyassistant.data.RepeatMode
import com.dailyassistant.data.Repo
import com.dailyassistant.util.Dates
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Reminders run on WorkManager, so they survive reboots and app kills without
 * any server, push service or account.
 */
object Scheduler {

    const val BILL_CHECK_WORK = "bill_check"
    const val BILL_CHECK_MINUTE = 9 * 60

    fun reminderWorkName(reminderId: Long) = "reminder_$reminderId"

    fun schedule(context: Context, reminder: Reminder) {
        val manager = WorkManager.getInstance(context)
        if (!reminder.enabled) {
            manager.cancelUniqueWork(reminderWorkName(reminder.id))
            return
        }
        val next = nextOccurrence(reminder) ?: run {
            manager.cancelUniqueWork(reminderWorkName(reminder.id))
            return
        }
        val delayMillis = (next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - Dates.nowMillis())
            .coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_REMINDER_ID to reminder.id))
            .addTag("reminder")
            .build()
        manager.enqueueUniqueWork(reminderWorkName(reminder.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, reminderId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(reminderWorkName(reminderId))
    }

    suspend fun rescheduleAll(context: Context, repo: Repo) {
        repo.enabledReminders().forEach { schedule(context, it) }
        scheduleBillCheck(context)
    }

    fun scheduleBillCheck(context: Context) {
        val now = LocalDateTime.now()
        val todayRun = LocalDateTime.of(LocalDate.now(), LocalTime.of(BILL_CHECK_MINUTE / 60, BILL_CHECK_MINUTE % 60))
        val next = if (todayRun.isAfter(now)) todayRun else todayRun.plusDays(1)
        val delayMillis = (next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - Dates.nowMillis())
            .coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<BillWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag("bills")
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(BILL_CHECK_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** The next date-and-time this reminder should fire, or null when it is finished. */
    fun nextOccurrence(reminder: Reminder, from: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
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
                val mask = if (reminder.daysOfWeekMask == 0) 1 shl (start.dayOfWeek.value - 1) else reminder.daysOfWeekMask
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
