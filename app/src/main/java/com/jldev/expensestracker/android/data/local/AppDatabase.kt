package com.jldev.expensestracker.android.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** The local copy of the user's data; screens read only from here. */
@Database(
    entities = [
        CycleEntity::class,
        SavingTypeEntity::class,
        RecurrentBudgetEntity::class,
        RecurrentExpenseEntity::class,
        RecurrentIncomeEntity::class,
        RecurrentSavingEntity::class,
        BudgetEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class,
        SavingEntity::class,
        PendingChangeEntity::class,
    ],
    version = 2,
    autoMigrations = [
        // 2: the pending_changes queue for offline writes.
        AutoMigration(from = 1, to = 2),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncDao(): SyncDao
    abstract fun cycleDao(): CycleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingDao(): SavingDao
    abstract fun pendingChangeDao(): PendingChangeDao
}
