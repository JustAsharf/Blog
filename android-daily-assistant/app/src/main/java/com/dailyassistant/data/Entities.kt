package com.dailyassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * All money is stored as minor units (e.g. cents / piastres) in a Long so that
 * arithmetic never drifts the way floating point does.
 * All calendar dates are stored as epoch-days, all timestamps as epoch-millis.
 */

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "💸",
    val colorArgb: Long = 0xFF4C8BF5,
    val monthlyBudgetMinor: Long = 0,
    val archived: Boolean = false
)

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val categoryId: Long?,
    val note: String = "",
    val epochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
    /** true = money in (salary, bonus, refund), false = money out (spending). */
    val isIncome: Boolean = false,
    val method: String = "cash"
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val dueDayOfMonth: Int,
    val categoryId: Long? = null,
    val remindDaysBefore: Int = 2,
    /** "yyyy-MM" of the last month this bill was marked paid. */
    val lastPaidYearMonth: String? = null,
    val active: Boolean = true
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🎯",
    val targetMinor: Long,
    val savedMinor: Long = 0,
    val deadlineEpochDay: Long? = null,
    val archived: Boolean = false
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** Minutes since midnight. */
    val minuteOfDay: Int = 20 * 60,
    val startEpochDay: Long,
    /** ONCE | DAILY | WEEKLY | MONTHLY */
    val repeatMode: String = RepeatMode.ONCE,
    /** Bit 0 = Monday ... bit 6 = Sunday. Only used by WEEKLY. */
    val daysOfWeekMask: Int = 0,
    val enabled: Boolean = true,
    val lastFiredAt: Long? = null,
    /** System reminders (the daily spending nudge) cannot be deleted, only disabled. */
    val system: Boolean = false
)

object RepeatMode {
    const val ONCE = "ONCE"
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    val all = listOf(ONCE, DAILY, WEEKLY, MONTHLY)
}

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val amountMinor: Long,
    /** true = I owe them, false = they owe me. */
    val iOwe: Boolean,
    val note: String = "",
    val dueEpochDay: Long? = null,
    val settled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
