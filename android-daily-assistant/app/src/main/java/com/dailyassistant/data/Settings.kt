package com.dailyassistant.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

data class Settings(
    val currency: String = "$",
    /** Net monthly salary in minor units. */
    val salaryMinor: Long = 0,
    /** Day of month the salary lands. */
    val paydayDay: Int = 1,
    /** Share of the salary to park into savings before any daily budget is computed. */
    val savingsPercent: Int = 10,
    /** 0 = follow system, 1 = light, 2 = dark. */
    val themeMode: Int = 0,
    val pinHash: String? = null,
    val dailyNudgeEnabled: Boolean = true,
    val dailyNudgeMinute: Int = 21 * 60,
    /** Roll unspent days forward into today's allowance. */
    val carryOver: Boolean = true,
    val seeded: Boolean = false
) {
    val hasSalary: Boolean get() = salaryMinor > 0
}

val CURRENCIES = listOf("$", "€", "£", "EGP", "SAR", "AED", "KWD", "QAR", "₹", "₺", "¥", "R", "MAD", "TND", "DZD")

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state

    val current: Settings get() = _state.value

    private fun read() = Settings(
        currency = prefs.getString(KEY_CURRENCY, "$") ?: "$",
        salaryMinor = prefs.getLong(KEY_SALARY, 0L),
        paydayDay = prefs.getInt(KEY_PAYDAY, 1),
        savingsPercent = prefs.getInt(KEY_SAVINGS_PCT, 10),
        themeMode = prefs.getInt(KEY_THEME, 0),
        pinHash = prefs.getString(KEY_PIN, null),
        dailyNudgeEnabled = prefs.getBoolean(KEY_NUDGE_ON, true),
        dailyNudgeMinute = prefs.getInt(KEY_NUDGE_MIN, 21 * 60),
        carryOver = prefs.getBoolean(KEY_CARRY_OVER, true),
        seeded = prefs.getBoolean(KEY_SEEDED, false)
    )

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_state.value)
        prefs.edit()
            .putString(KEY_CURRENCY, next.currency)
            .putLong(KEY_SALARY, next.salaryMinor)
            .putInt(KEY_PAYDAY, next.paydayDay)
            .putInt(KEY_SAVINGS_PCT, next.savingsPercent)
            .putInt(KEY_THEME, next.themeMode)
            .putString(KEY_PIN, next.pinHash)
            .putBoolean(KEY_NUDGE_ON, next.dailyNudgeEnabled)
            .putInt(KEY_NUDGE_MIN, next.dailyNudgeMinute)
            .putBoolean(KEY_CARRY_OVER, next.carryOver)
            .putBoolean(KEY_SEEDED, next.seeded)
            .apply()
        _state.value = next
    }

    companion object {
        private const val KEY_CURRENCY = "currency"
        private const val KEY_SALARY = "salary_minor"
        private const val KEY_PAYDAY = "payday_day"
        private const val KEY_SAVINGS_PCT = "savings_pct"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_PIN = "pin_hash"
        private const val KEY_NUDGE_ON = "nudge_on"
        private const val KEY_NUDGE_MIN = "nudge_minute"
        private const val KEY_CARRY_OVER = "carry_over"
        private const val KEY_SEEDED = "seeded"

        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(("daily-assistant:" + pin).toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
