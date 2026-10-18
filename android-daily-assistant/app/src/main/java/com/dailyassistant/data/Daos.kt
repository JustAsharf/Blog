package com.dailyassistant.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun all(): List<Category>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay DESC, createdAt DESC")
    fun observeRange(from: Long, to: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY epochDay DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY epochDay, createdAt")
    suspend fun all(): List<Entry>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM entries WHERE isIncome = 0 AND epochDay BETWEEN :from AND :to")
    fun observeSpentBetween(from: Long, to: Long): Flow<Long>

    @Upsert
    suspend fun upsert(entry: Entry): Long

    @Insert
    suspend fun insertAll(entries: List<Entry>)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("DELETE FROM entries")
    suspend fun clear()
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDayOfMonth")
    fun observeAll(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE active = 1")
    suspend fun activeBills(): List<Bill>

    @Query("SELECT * FROM bills")
    suspend fun all(): List<Bill>

    @Upsert
    suspend fun upsert(bill: Bill): Long

    @Insert
    suspend fun insertAll(bills: List<Bill>)

    @Delete
    suspend fun delete(bill: Bill)

    @Query("DELETE FROM bills")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY id DESC")
    fun observeActive(): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun all(): List<Goal>

    @Upsert
    suspend fun upsert(goal: Goal): Long

    @Insert
    suspend fun insertAll(goals: List<Goal>)

    @Delete
    suspend fun delete(goal: Goal)

    @Query("DELETE FROM goals")
    suspend fun clear()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY minuteOfDay")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun enabled(): List<Reminder>

    @Query("SELECT * FROM reminders")
    suspend fun all(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun byId(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE system = 1 LIMIT 1")
    suspend fun systemReminder(): Reminder?

    @Upsert
    suspend fun upsert(reminder: Reminder): Long

    @Insert
    suspend fun insertAll(reminders: List<Reminder>)

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders")
    suspend fun clear()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY settled, createdAt DESC")
    fun observeAll(): Flow<List<Debt>>

    @Query("SELECT * FROM debts")
    suspend fun all(): List<Debt>

    @Upsert
    suspend fun upsert(debt: Debt): Long

    @Insert
    suspend fun insertAll(debts: List<Debt>)

    @Delete
    suspend fun delete(debt: Debt)

    @Query("DELETE FROM debts")
    suspend fun clear()
}
