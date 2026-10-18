package com.dailyassistant.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailyassistant.DailyApp
import com.dailyassistant.data.RepeatMode
import com.dailyassistant.util.Dates

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_REMINDER_ID, -1L)
        if (id <= 0L) return Result.success()

        val repo = (applicationContext as DailyApp).repo
        val reminder = repo.reminderById(id) ?: return Result.success()
        if (!reminder.enabled) return Result.success()

        Notifications.show(
            context = applicationContext,
            notificationId = (1000 + id).toInt(),
            title = reminder.title,
            text = reminder.note.ifBlank { "Reminder from Daily Assistant" },
            route = if (reminder.system) "add" else "reminders"
        )

        val fired = reminder.copy(
            lastFiredAt = Dates.nowMillis(),
            enabled = reminder.repeatMode != RepeatMode.ONCE
        )
        repo.saveReminder(fired)
        if (fired.enabled) Scheduler.schedule(applicationContext, fired)

        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
    }
}
