package com.dailyassistant.data

import android.content.Context
import com.dailyassistant.util.Dates
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class Repo(context: Context) {

    private val db = AppDatabase.get(context)
    val settings = SettingsStore(context)

    private val categoryDao = db.categoryDao()
    private val entryDao = db.entryDao()
    private val billDao = db.billDao()
    private val goalDao = db.goalDao()
    private val reminderDao = db.reminderDao()
    private val debtDao = db.debtDao()

    val categories: Flow<List<Category>> = categoryDao.observeActive()
    val bills: Flow<List<Bill>> = billDao.observeAll()
    val goals: Flow<List<Goal>> = goalDao.observeActive()
    val reminders: Flow<List<Reminder>> = reminderDao.observeAll()
    val debts: Flow<List<Debt>> = debtDao.observeAll()

    fun entriesBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<Entry>> =
        entryDao.observeRange(fromEpochDay, toEpochDay)

    fun recentEntries(limit: Int = 200): Flow<List<Entry>> = entryDao.observeRecent(limit)

    // ---------------------------------------------------------------- writes

    suspend fun saveEntry(entry: Entry): Long = entryDao.upsert(entry)
    suspend fun deleteEntry(entry: Entry) = entryDao.delete(entry)

    suspend fun saveCategory(category: Category): Long = categoryDao.upsert(category)
    suspend fun deleteCategory(category: Category) = categoryDao.upsert(category.copy(archived = true))

    suspend fun saveBill(bill: Bill): Long = billDao.upsert(bill)
    suspend fun deleteBill(bill: Bill) = billDao.delete(bill)

    suspend fun saveGoal(goal: Goal): Long = goalDao.upsert(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.delete(goal)

    suspend fun saveReminder(reminder: Reminder): Long = reminderDao.upsert(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.delete(reminder)
    suspend fun reminderById(id: Long): Reminder? = reminderDao.byId(id)
    suspend fun enabledReminders(): List<Reminder> = reminderDao.enabled()
    suspend fun systemReminder(): Reminder? = reminderDao.systemReminder()

    suspend fun saveDebt(debt: Debt): Long = debtDao.upsert(debt)
    suspend fun deleteDebt(debt: Debt) = debtDao.delete(debt)

    suspend fun activeBills(): List<Bill> = billDao.activeBills()

    /** Marks a bill paid for the month of [on] and books the matching expense. */
    suspend fun payBill(bill: Bill, on: LocalDate = Dates.today()) {
        billDao.upsert(bill.copy(lastPaidYearMonth = Dates.yearMonthKey(on)))
        entryDao.upsert(
            Entry(
                amountMinor = bill.amountMinor,
                categoryId = bill.categoryId,
                note = bill.name,
                epochDay = on.toEpochDay(),
                isIncome = false,
                method = "bill"
            )
        )
    }

    /** Adds [amountMinor] to a goal and books it as spending so the budget stays honest. */
    suspend fun contributeToGoal(goal: Goal, amountMinor: Long, on: LocalDate = Dates.today()) {
        goalDao.upsert(goal.copy(savedMinor = (goal.savedMinor + amountMinor).coerceAtLeast(0)))
        if (amountMinor > 0) {
            entryDao.upsert(
                Entry(
                    amountMinor = amountMinor,
                    categoryId = null,
                    note = "Saved for " + goal.name,
                    epochDay = on.toEpochDay(),
                    isIncome = false,
                    method = "savings"
                )
            )
        }
    }

    // ----------------------------------------------------------------- seed

    suspend fun seedIfNeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(defaultCategories())
        }
        if (reminderDao.systemReminder() == null) {
            reminderDao.upsert(
                Reminder(
                    title = "Log today's spending",
                    note = "Takes ten seconds. Tap to add what you spent today.",
                    minuteOfDay = settings.current.dailyNudgeMinute,
                    startEpochDay = Dates.today().toEpochDay(),
                    repeatMode = RepeatMode.DAILY,
                    enabled = settings.current.dailyNudgeEnabled,
                    system = true
                )
            )
        }
        if (!settings.current.seeded) {
            settings.update { it.copy(seeded = true) }
        }
    }

    private fun defaultCategories() = listOf(
        Category(name = "Food", emoji = "🍔", colorArgb = 0xFFE4572E),
        Category(name = "Groceries", emoji = "🛒", colorArgb = 0xFF2E933C),
        Category(name = "Transport", emoji = "🚕", colorArgb = 0xFF2F6FED),
        Category(name = "Coffee", emoji = "☕", colorArgb = 0xFF8D6346),
        Category(name = "Rent", emoji = "🏠", colorArgb = 0xFF6A4C93),
        Category(name = "Bills", emoji = "💡", colorArgb = 0xFFF0A202),
        Category(name = "Phone & Internet", emoji = "📱", colorArgb = 0xFF00A6A6),
        Category(name = "Health", emoji = "💊", colorArgb = 0xFFD64550),
        Category(name = "Family", emoji = "👪", colorArgb = 0xFFEF6F6C),
        Category(name = "Clothes", emoji = "👕", colorArgb = 0xFF9A48D0),
        Category(name = "Fun", emoji = "🎮", colorArgb = 0xFF3DA5D9),
        Category(name = "Other", emoji = "💸", colorArgb = 0xFF6C757D)
    )

    // --------------------------------------------------------------- backup

    /** Everything the app knows, as a single JSON document. No network involved. */
    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("format", "daily-assistant-backup")
        root.put("version", 1)
        root.put("exportedAt", Dates.nowMillis())

        val s = settings.current
        root.put(
            "settings",
            JSONObject()
                .put("currency", s.currency)
                .put("salaryMinor", s.salaryMinor)
                .put("paydayDay", s.paydayDay)
                .put("savingsPercent", s.savingsPercent)
                .put("themeMode", s.themeMode)
                .put("dailyNudgeEnabled", s.dailyNudgeEnabled)
                .put("dailyNudgeMinute", s.dailyNudgeMinute)
                .put("carryOver", s.carryOver)
        )

        root.put("categories", JSONArray().apply {
            categoryDao.all().forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("emoji", it.emoji)
                        .put("colorArgb", it.colorArgb)
                        .put("monthlyBudgetMinor", it.monthlyBudgetMinor)
                        .put("archived", it.archived)
                )
            }
        })

        root.put("entries", JSONArray().apply {
            entryDao.all().forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("amountMinor", it.amountMinor)
                        .put("categoryId", it.categoryId ?: JSONObject.NULL)
                        .put("note", it.note)
                        .put("epochDay", it.epochDay)
                        .put("createdAt", it.createdAt)
                        .put("isIncome", it.isIncome)
                        .put("method", it.method)
                )
            }
        })

        root.put("bills", JSONArray().apply {
            billDao.all().forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("amountMinor", it.amountMinor)
                        .put("dueDayOfMonth", it.dueDayOfMonth)
                        .put("categoryId", it.categoryId ?: JSONObject.NULL)
                        .put("remindDaysBefore", it.remindDaysBefore)
                        .put("lastPaidYearMonth", it.lastPaidYearMonth ?: JSONObject.NULL)
                        .put("active", it.active)
                )
            }
        })

        root.put("goals", JSONArray().apply {
            goalDao.all().forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("emoji", it.emoji)
                        .put("targetMinor", it.targetMinor)
                        .put("savedMinor", it.savedMinor)
                        .put("deadlineEpochDay", it.deadlineEpochDay ?: JSONObject.NULL)
                        .put("archived", it.archived)
                )
            }
        })

        root.put("reminders", JSONArray().apply {
            reminderDao.all().forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("title", it.title)
                        .put("note", it.note)
                        .put("minuteOfDay", it.minuteOfDay)
                        .put("startEpochDay", it.startEpochDay)
                        .put("repeatMode", it.repeatMode)
                        .put("daysOfWeekMask", it.daysOfWeekMask)
                        .put("enabled", it.enabled)
                        .put("system", it.system)
                )
            }
        })

        root.put("debts", JSONArray().apply {
            debtDao.all().forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("person", it.person)
                        .put("amountMinor", it.amountMinor)
                        .put("iOwe", it.iOwe)
                        .put("note", it.note)
                        .put("dueEpochDay", it.dueEpochDay ?: JSONObject.NULL)
                        .put("settled", it.settled)
                        .put("createdAt", it.createdAt)
                )
            }
        })

        return root.toString(2)
    }

    /** Replaces everything with the contents of a backup produced by [exportJson]. */
    suspend fun importJson(text: String): Result<Int> = runCatching {
        val root = JSONObject(text)
        require(root.optString("format") == "daily-assistant-backup") { "Not a Daily Assistant backup file" }

        root.optJSONObject("settings")?.let { o ->
            settings.update { current ->
                current.copy(
                    currency = o.optString("currency", current.currency),
                    salaryMinor = o.optLong("salaryMinor", current.salaryMinor),
                    paydayDay = o.optInt("paydayDay", current.paydayDay),
                    savingsPercent = o.optInt("savingsPercent", current.savingsPercent),
                    themeMode = o.optInt("themeMode", current.themeMode),
                    dailyNudgeEnabled = o.optBoolean("dailyNudgeEnabled", current.dailyNudgeEnabled),
                    dailyNudgeMinute = o.optInt("dailyNudgeMinute", current.dailyNudgeMinute),
                    carryOver = o.optBoolean("carryOver", current.carryOver)
                )
            }
        }

        entryDao.clear(); billDao.clear(); goalDao.clear(); reminderDao.clear(); debtDao.clear(); categoryDao.clear()

        var restored = 0

        root.optJSONArray("categories")?.let { arr ->
            val list = ArrayList<Category>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Category(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    emoji = o.optString("emoji", "💸"),
                    colorArgb = o.optLong("colorArgb", 0xFF4C8BF5),
                    monthlyBudgetMinor = o.optLong("monthlyBudgetMinor"),
                    archived = o.optBoolean("archived")
                )
            }
            categoryDao.insertAll(list); restored += list.size
        }

        root.optJSONArray("entries")?.let { arr ->
            val list = ArrayList<Entry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Entry(
                    id = o.optLong("id"),
                    amountMinor = o.optLong("amountMinor"),
                    categoryId = if (o.isNull("categoryId")) null else o.optLong("categoryId"),
                    note = o.optString("note"),
                    epochDay = o.optLong("epochDay"),
                    createdAt = o.optLong("createdAt", Dates.nowMillis()),
                    isIncome = o.optBoolean("isIncome"),
                    method = o.optString("method", "cash")
                )
            }
            entryDao.insertAll(list); restored += list.size
        }

        root.optJSONArray("bills")?.let { arr ->
            val list = ArrayList<Bill>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Bill(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    amountMinor = o.optLong("amountMinor"),
                    dueDayOfMonth = o.optInt("dueDayOfMonth", 1),
                    categoryId = if (o.isNull("categoryId")) null else o.optLong("categoryId"),
                    remindDaysBefore = o.optInt("remindDaysBefore", 2),
                    lastPaidYearMonth = if (o.isNull("lastPaidYearMonth")) null else o.optString("lastPaidYearMonth"),
                    active = o.optBoolean("active", true)
                )
            }
            billDao.insertAll(list); restored += list.size
        }

        root.optJSONArray("goals")?.let { arr ->
            val list = ArrayList<Goal>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Goal(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    emoji = o.optString("emoji", "🎯"),
                    targetMinor = o.optLong("targetMinor"),
                    savedMinor = o.optLong("savedMinor"),
                    deadlineEpochDay = if (o.isNull("deadlineEpochDay")) null else o.optLong("deadlineEpochDay"),
                    archived = o.optBoolean("archived")
                )
            }
            goalDao.insertAll(list); restored += list.size
        }

        root.optJSONArray("reminders")?.let { arr ->
            val list = ArrayList<Reminder>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Reminder(
                    id = o.optLong("id"),
                    title = o.optString("title"),
                    note = o.optString("note"),
                    minuteOfDay = o.optInt("minuteOfDay", 20 * 60),
                    startEpochDay = o.optLong("startEpochDay", Dates.today().toEpochDay()),
                    repeatMode = o.optString("repeatMode", RepeatMode.ONCE),
                    daysOfWeekMask = o.optInt("daysOfWeekMask"),
                    enabled = o.optBoolean("enabled", true),
                    system = o.optBoolean("system")
                )
            }
            reminderDao.insertAll(list); restored += list.size
        }

        root.optJSONArray("debts")?.let { arr ->
            val list = ArrayList<Debt>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Debt(
                    id = o.optLong("id"),
                    person = o.optString("person"),
                    amountMinor = o.optLong("amountMinor"),
                    iOwe = o.optBoolean("iOwe"),
                    note = o.optString("note"),
                    dueEpochDay = if (o.isNull("dueEpochDay")) null else o.optLong("dueEpochDay"),
                    settled = o.optBoolean("settled"),
                    createdAt = o.optLong("createdAt", Dates.nowMillis())
                )
            }
            debtDao.insertAll(list); restored += list.size
        }

        seedIfNeeded()
        restored
    }

    /** Spreadsheet-friendly dump of every entry. */
    suspend fun exportCsv(): String {
        val cats = categoryDao.all().associateBy { it.id }
        val sb = StringBuilder("date,type,amount,category,note,method\n")
        entryDao.all().forEach { e ->
            val date = Dates.of(e.epochDay)
            val category = e.categoryId?.let { cats[it]?.name } ?: ""
            sb.append(date).append(',')
                .append(if (e.isIncome) "income" else "expense").append(',')
                .append(String.format(java.util.Locale.US, "%.2f", e.amountMinor / 100.0)).append(',')
                .append(csvCell(category)).append(',')
                .append(csvCell(e.note)).append(',')
                .append(csvCell(e.method)).append('\n')
        }
        return sb.toString()
    }

    private fun csvCell(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
}
