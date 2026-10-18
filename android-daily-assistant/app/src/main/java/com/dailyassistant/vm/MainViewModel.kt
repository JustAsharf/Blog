package com.dailyassistant.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyassistant.DailyApp
import com.dailyassistant.data.Bill
import com.dailyassistant.data.Budget
import com.dailyassistant.data.BudgetSnapshot
import com.dailyassistant.data.Category
import com.dailyassistant.data.Debt
import com.dailyassistant.data.Entry
import com.dailyassistant.data.Goal
import com.dailyassistant.data.Insight
import com.dailyassistant.data.Insights
import com.dailyassistant.data.Reminder
import com.dailyassistant.data.RepeatMode
import com.dailyassistant.data.Settings
import com.dailyassistant.data.SettingsStore
import com.dailyassistant.util.Dates
import com.dailyassistant.work.Scheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DailyApp
    private val repo = app.repo

    val settings: StateFlow<Settings> = repo.settings.state

    private val eager = SharingStarted.WhileSubscribed(5_000)

    val categories: StateFlow<List<Category>> =
        repo.categories.stateIn(viewModelScope, eager, emptyList())

    val bills: StateFlow<List<Bill>> =
        repo.bills.stateIn(viewModelScope, eager, emptyList())

    val goals: StateFlow<List<Goal>> =
        repo.goals.stateIn(viewModelScope, eager, emptyList())

    val reminders: StateFlow<List<Reminder>> =
        repo.reminders.stateIn(viewModelScope, eager, emptyList())

    val debts: StateFlow<List<Debt>> =
        repo.debts.stateIn(viewModelScope, eager, emptyList())

    val recentEntries: StateFlow<List<Entry>> =
        repo.recentEntries(400).stateIn(viewModelScope, eager, emptyList())

    /** Entries inside the current salary cycle. */
    val periodEntries: StateFlow<List<Entry>> = settings
        .flatMapLatest { s ->
            val period = Dates.payPeriod(Dates.today(), s.paydayDay)
            repo.entriesBetween(period.start.toEpochDay(), period.endInclusive.toEpochDay())
        }
        .stateIn(viewModelScope, eager, emptyList())

    val budget: StateFlow<BudgetSnapshot> =
        combine(settings, periodEntries, bills) { s, entries, billList ->
            Budget.compute(s, entries, billList)
        }.stateIn(viewModelScope, eager, BudgetSnapshot())

    val insights: StateFlow<List<Insight>> =
        combine(budget, periodEntries, recentEntries, categories, bills) { b, period, recent, cats, billList ->
            Insights.build(
                settings = settings.value,
                budget = b,
                periodEntries = period,
                allRecentEntries = recent,
                categories = cats,
                bills = billList,
                goals = goals.value,
                debts = debts.value
            )
        }.stateIn(viewModelScope, eager, emptyList())

    /** Month shown by the history and report screens. */
    val selectedMonth = MutableStateFlow(YearMonth.now())

    val monthEntries: StateFlow<List<Entry>> = selectedMonth
        .flatMapLatest { month ->
            val range = Dates.monthRange(month)
            repo.entriesBetween(range.first, range.last)
        }
        .stateIn(viewModelScope, eager, emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    fun say(text: String) { _message.value = text }
    fun messageShown() { _message.value = null }

    fun shiftMonth(delta: Long) {
        selectedMonth.value = selectedMonth.value.plusMonths(delta)
    }

    fun categoryById(id: Long?): Category? = categories.value.firstOrNull { it.id == id }

    // ------------------------------------------------------------- app lock

    fun tryUnlock(pin: String): Boolean {
        val expected = settings.value.pinHash ?: return true.also { _unlocked.value = true }
        val ok = SettingsStore.hashPin(pin) == expected
        if (ok) _unlocked.value = true
        return ok
    }

    fun markUnlocked() { _unlocked.value = true }

    fun setPin(pin: String?) {
        repo.settings.update { it.copy(pinHash = pin?.let { raw -> SettingsStore.hashPin(raw) }) }
        _unlocked.value = true
        say(if (pin == null) "App lock removed" else "App lock set")
    }

    // --------------------------------------------------------------- entries

    fun addEntry(
        amountMinor: Long,
        categoryId: Long?,
        note: String,
        date: LocalDate,
        isIncome: Boolean,
        method: String = "cash"
    ) = viewModelScope.launch {
        repo.saveEntry(
            Entry(
                amountMinor = amountMinor,
                categoryId = categoryId,
                note = note.trim(),
                epochDay = date.toEpochDay(),
                isIncome = isIncome,
                method = method
            )
        )
        say(if (isIncome) "Income added" else "Expense added")
    }

    fun updateEntry(entry: Entry) = viewModelScope.launch {
        repo.saveEntry(entry)
        say("Saved")
    }

    fun deleteEntry(entry: Entry) = viewModelScope.launch {
        repo.deleteEntry(entry)
        say("Deleted")
    }

    // ------------------------------------------------------------ categories

    fun saveCategory(category: Category) = viewModelScope.launch {
        repo.saveCategory(category)
        say("Category saved")
    }

    fun archiveCategory(category: Category) = viewModelScope.launch {
        repo.deleteCategory(category)
        say("Category removed")
    }

    // ----------------------------------------------------------------- bills

    fun saveBill(bill: Bill) = viewModelScope.launch {
        repo.saveBill(bill)
        Scheduler.scheduleBillCheck(app)
        say("Bill saved")
    }

    fun deleteBill(bill: Bill) = viewModelScope.launch {
        repo.deleteBill(bill)
        say("Bill deleted")
    }

    fun payBill(bill: Bill) = viewModelScope.launch {
        repo.payBill(bill)
        say(bill.name + " marked paid")
    }

    // ----------------------------------------------------------------- goals

    fun saveGoal(goal: Goal) = viewModelScope.launch {
        repo.saveGoal(goal)
        say("Goal saved")
    }

    fun deleteGoal(goal: Goal) = viewModelScope.launch {
        repo.deleteGoal(goal)
        say("Goal deleted")
    }

    fun contribute(goal: Goal, amountMinor: Long) = viewModelScope.launch {
        repo.contributeToGoal(goal, amountMinor)
        say("Added to " + goal.name)
    }

    // ------------------------------------------------------------- reminders

    fun saveReminder(reminder: Reminder) = viewModelScope.launch {
        val id = repo.saveReminder(reminder)
        val stored = reminder.copy(id = if (reminder.id == 0L) id else reminder.id)
        Scheduler.schedule(app, stored)
        if (stored.system) {
            repo.settings.update {
                it.copy(dailyNudgeEnabled = stored.enabled, dailyNudgeMinute = stored.minuteOfDay)
            }
        }
        say(if (stored.enabled) "Reminder set for " + Dates.minuteOfDayToText(stored.minuteOfDay) else "Reminder off")
    }

    fun toggleReminder(reminder: Reminder, enabled: Boolean) = saveReminder(
        reminder.copy(
            enabled = enabled,
            // A one-off that already fired needs a fresh date when switched back on.
            startEpochDay = if (enabled && reminder.repeatMode == RepeatMode.ONCE &&
                reminder.startEpochDay < Dates.today().toEpochDay()
            ) Dates.today().toEpochDay() else reminder.startEpochDay
        )
    )

    fun deleteReminder(reminder: Reminder) = viewModelScope.launch {
        Scheduler.cancel(app, reminder.id)
        repo.deleteReminder(reminder)
        say("Reminder deleted")
    }

    // ----------------------------------------------------------------- debts

    fun saveDebt(debt: Debt) = viewModelScope.launch {
        repo.saveDebt(debt)
        say("Saved")
    }

    fun deleteDebt(debt: Debt) = viewModelScope.launch {
        repo.deleteDebt(debt)
        say("Deleted")
    }

    fun settleDebt(debt: Debt) = viewModelScope.launch {
        repo.saveDebt(debt.copy(settled = !debt.settled))
    }

    // -------------------------------------------------------------- settings

    fun updateSettings(transform: (Settings) -> Settings) {
        repo.settings.update(transform)
    }

    fun setDailyNudge(enabled: Boolean, minuteOfDay: Int) = viewModelScope.launch {
        repo.settings.update { it.copy(dailyNudgeEnabled = enabled, dailyNudgeMinute = minuteOfDay) }
        val system = repo.systemReminder() ?: return@launch
        val updated = system.copy(enabled = enabled, minuteOfDay = minuteOfDay)
        repo.saveReminder(updated)
        Scheduler.schedule(app, updated)
    }

    // ------------------------------------------------------- backup / export

    suspend fun backupJson(): String = repo.exportJson()

    suspend fun csv(): String = repo.exportCsv()

    fun restoreJson(text: String) = viewModelScope.launch {
        repo.importJson(text)
            .onSuccess { count ->
                Scheduler.rescheduleAll(app, repo)
                say("Restored $count records")
            }
            .onFailure { say("Could not restore: " + (it.message ?: "invalid file")) }
    }
}
