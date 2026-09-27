package com.jldev.expensestracker.android.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jldev.expensestracker.android.data.model.BudgetTotal
import com.jldev.expensestracker.android.data.model.CycleStatus
import com.jldev.expensestracker.android.data.model.ExpenseItem
import com.jldev.expensestracker.android.data.model.GroupedSaving
import com.jldev.expensestracker.android.data.model.RecurrentSavingItem
import com.jldev.expensestracker.android.data.model.SavingItem
import kotlinx.coroutines.flow.Flow

/** Writes coming from sync: upserts by uuid, deletes by uuid. */
@Dao
interface SyncDao {
    @Upsert suspend fun upsertCycles(records: List<CycleEntity>)
    @Upsert suspend fun upsertSavingTypes(records: List<SavingTypeEntity>)
    @Upsert suspend fun upsertRecurrentBudgets(records: List<RecurrentBudgetEntity>)
    @Upsert suspend fun upsertRecurrentExpenses(records: List<RecurrentExpenseEntity>)
    @Upsert suspend fun upsertRecurrentIncomes(records: List<RecurrentIncomeEntity>)
    @Upsert suspend fun upsertRecurrentSavings(records: List<RecurrentSavingEntity>)
    @Upsert suspend fun upsertBudgets(records: List<BudgetEntity>)
    @Upsert suspend fun upsertExpenses(records: List<ExpenseEntity>)
    @Upsert suspend fun upsertIncomes(records: List<IncomeEntity>)
    @Upsert suspend fun upsertSavings(records: List<SavingEntity>)

    @Query("DELETE FROM cycles WHERE uuid IN (:uuids)") suspend fun deleteCycles(uuids: List<String>)
    @Query("DELETE FROM saving_types WHERE uuid IN (:uuids)") suspend fun deleteSavingTypes(uuids: List<String>)
    @Query("DELETE FROM recurrent_budgets WHERE uuid IN (:uuids)")
    suspend fun deleteRecurrentBudgets(uuids: List<String>)
    @Query("DELETE FROM recurrent_expenses WHERE uuid IN (:uuids)")
    suspend fun deleteRecurrentExpenses(uuids: List<String>)
    @Query("DELETE FROM recurrent_incomes WHERE uuid IN (:uuids)")
    suspend fun deleteRecurrentIncomes(uuids: List<String>)
    @Query("DELETE FROM recurrent_savings WHERE uuid IN (:uuids)")
    suspend fun deleteRecurrentSavings(uuids: List<String>)
    @Query("DELETE FROM budgets WHERE uuid IN (:uuids)") suspend fun deleteBudgets(uuids: List<String>)
    @Query("DELETE FROM expenses WHERE uuid IN (:uuids)") suspend fun deleteExpenses(uuids: List<String>)
    @Query("DELETE FROM incomes WHERE uuid IN (:uuids)") suspend fun deleteIncomes(uuids: List<String>)
    @Query("DELETE FROM savings WHERE uuid IN (:uuids)") suspend fun deleteSavings(uuids: List<String>)

    /** The server deletes a budget's link from its expenses; do the same locally right away. */
    @Query("UPDATE expenses SET budgetUuid = NULL WHERE budgetUuid IN (:budgetUuids)")
    suspend fun unlinkBudgets(budgetUuids: List<String>)

    // After the server merges one of our records into another, point every reference at the survivor.
    @Query("UPDATE budgets SET cycleUuid = :new WHERE cycleUuid = :old")
    suspend fun repointBudgetCycles(old: String, new: String)
    @Query("UPDATE expenses SET cycleUuid = :new WHERE cycleUuid = :old")
    suspend fun repointExpenseCycles(old: String, new: String)
    @Query("UPDATE incomes SET cycleUuid = :new WHERE cycleUuid = :old")
    suspend fun repointIncomeCycles(old: String, new: String)
    @Query("UPDATE savings SET cycleUuid = :new WHERE cycleUuid = :old")
    suspend fun repointSavingCycles(old: String, new: String)
    @Query("UPDATE expenses SET budgetUuid = :new WHERE budgetUuid = :old")
    suspend fun repointExpenseBudgets(old: String, new: String)
    @Query("UPDATE savings SET savingTypeUuid = :new WHERE savingTypeUuid = :old")
    suspend fun repointSavingTypes(old: String, new: String)
    @Query("UPDATE recurrent_savings SET savingTypeUuid = :new WHERE savingTypeUuid = :old")
    suspend fun repointRecurrentSavingTypes(old: String, new: String)
    @Query("UPDATE budgets SET recurrentBudgetUuid = :new WHERE recurrentBudgetUuid = :old")
    suspend fun repointRecurrentBudgets(old: String, new: String)
    @Query("UPDATE expenses SET recurrentExpenseUuid = :new WHERE recurrentExpenseUuid = :old")
    suspend fun repointRecurrentExpenses(old: String, new: String)
    @Query("UPDATE incomes SET recurrentIncomeUuid = :new WHERE recurrentIncomeUuid = :old")
    suspend fun repointRecurrentIncomes(old: String, new: String)
    @Query("UPDATE savings SET recurrentSavingUuid = :new WHERE recurrentSavingUuid = :old")
    suspend fun repointRecurrentSavings(old: String, new: String)

    /** Every reference to [old], in any table, becomes [new] (uuids are unique across tables). */
    suspend fun repoint(old: String, new: String) {
        repointBudgetCycles(old, new)
        repointExpenseCycles(old, new)
        repointIncomeCycles(old, new)
        repointSavingCycles(old, new)
        repointExpenseBudgets(old, new)
        repointSavingTypes(old, new)
        repointRecurrentSavingTypes(old, new)
        repointRecurrentBudgets(old, new)
        repointRecurrentExpenses(old, new)
        repointRecurrentIncomes(old, new)
        repointRecurrentSavings(old, new)
    }

    // The 6-month window: older cycles' budgets, expenses and incomes don't stay on the phone.
    @Query(
        """DELETE FROM budgets WHERE cycleUuid IN (SELECT uuid FROM cycles WHERE startDate < :windowStart)
            AND uuid NOT IN (SELECT uuid FROM pending_changes)""",
    )
    suspend fun pruneBudgets(windowStart: String)
    @Query(
        """DELETE FROM expenses WHERE cycleUuid IN (SELECT uuid FROM cycles WHERE startDate < :windowStart)
            AND uuid NOT IN (SELECT uuid FROM pending_changes)""",
    )
    suspend fun pruneExpenses(windowStart: String)
    @Query(
        """DELETE FROM incomes WHERE cycleUuid IN (SELECT uuid FROM cycles WHERE startDate < :windowStart)
            AND uuid NOT IN (SELECT uuid FROM pending_changes)""",
    )
    suspend fun pruneIncomes(windowStart: String)
}

@Dao
interface CycleDao {
    /** Newest first, like GET /cycles/list-cycles. */
    @Query("SELECT * FROM cycles ORDER BY startDate DESC")
    fun cycles(): Flow<List<CycleEntity>>

    @Query(
        """
        SELECT
            (SELECT COALESCE(SUM(valExpense), 0) FROM expenses
                WHERE cycleUuid = :cycleUuid AND isRecurrentExpense = 1) AS totalRecurrentExpenses,
            (SELECT COALESCE(SUM(valExpense), 0) FROM expenses
                WHERE cycleUuid = :cycleUuid AND isRecurrentExpense = 0) AS totalExpenses,
            (SELECT COALESCE(SUM(valIncome), 0) FROM incomes WHERE cycleUuid = :cycleUuid) AS totalIncomes,
            (SELECT COALESCE(SUM(valSaving), 0) FROM savings WHERE cycleUuid = :cycleUuid) AS totalSavings
        """,
    )
    fun status(cycleUuid: String): Flow<CycleStatus>
}

@Dao
interface ExpenseDao {
    /**
     * A cycle's expenses, newest first. [budgetUuid] filters by budget; [unbudgetedOnly] keeps the ones
     * without a budget.
     */
    @Query(
        """
        SELECT e.*, b.description AS budgetDescription FROM expenses e
        LEFT JOIN budgets b ON b.uuid = e.budgetUuid
        WHERE e.cycleUuid = :cycleUuid
            AND (:budgetUuid IS NULL OR e.budgetUuid = :budgetUuid)
            AND (:unbudgetedOnly = 0 OR e.budgetUuid IS NULL)
        ORDER BY e.dateExpense DESC
        """,
    )
    fun expenses(cycleUuid: String, budgetUuid: String?, unbudgetedOnly: Boolean): Flow<List<ExpenseItem>>

    @Query("SELECT * FROM recurrent_expenses ORDER BY createdAt")
    fun recurrentExpenses(): Flow<List<RecurrentExpenseEntity>>
}

@Dao
interface BudgetDao {
    /** A cycle's budgets with what was spent, the most used first (like GET /budgets). */
    @Query(
        """
        SELECT b.uuid, b.description, b.valBudget, COALESCE(SUM(e.valExpense), 0) AS totalSpent
        FROM budgets b
        LEFT JOIN expenses e ON e.budgetUuid = b.uuid AND e.cycleUuid = b.cycleUuid
        WHERE b.cycleUuid = :cycleUuid
        GROUP BY b.uuid
        ORDER BY COUNT(e.uuid) DESC
        """,
    )
    fun budgetTotals(cycleUuid: String): Flow<List<BudgetTotal>>

    @Query("SELECT COALESCE(SUM(valExpense), 0) FROM expenses WHERE cycleUuid = :cycleUuid AND budgetUuid IS NULL")
    fun unbudgetedTotal(cycleUuid: String): Flow<Double>

    @Query("SELECT * FROM budgets WHERE cycleUuid = :cycleUuid AND recurrentBudgetUuid = :recurrentBudgetUuid")
    suspend fun copyOf(recurrentBudgetUuid: String, cycleUuid: String): BudgetEntity?

    /** Copies made before the server linked them to their recurrent budget. */
    @Query(
        "SELECT * FROM budgets WHERE cycleUuid = :cycleUuid AND recurrentBudgetUuid IS NULL AND description = :description",
    )
    suspend fun unlinkedByDescription(description: String, cycleUuid: String): BudgetEntity?

    @Query("SELECT * FROM recurrent_budgets ORDER BY createdAt")
    fun recurrentBudgets(): Flow<List<RecurrentBudgetEntity>>
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes WHERE cycleUuid = :cycleUuid ORDER BY createdAt DESC")
    fun incomes(cycleUuid: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM recurrent_incomes ORDER BY createdAt")
    fun recurrentIncomes(): Flow<List<RecurrentIncomeEntity>>
}

@Dao
interface SavingDao {
    /** A cycle's deposits (withdrawals aren't listed), newest first. */
    @Query(
        """
        SELECT s.*, t.description AS savingTypeDescription FROM savings s
        JOIN saving_types t ON t.uuid = s.savingTypeUuid
        WHERE s.cycleUuid = :cycleUuid AND s.movementType = 'Income'
        ORDER BY s.createdAt DESC
        """,
    )
    fun savings(cycleUuid: String): Flow<List<SavingItem>>

    /** Like the backend's grouped-savings: every movement ever, and the active cycle's. */
    @Query(
        """
        SELECT t.uuid AS uuid, t.description AS description,
            MAX(s.isRecurrentSaving) AS isRecurrentSaving,
            SUM(CASE s.movementType WHEN 'Outcome' THEN -s.valSaving ELSE s.valSaving END) AS totalGlobal,
            SUM(CASE WHEN c.isActive = 1 THEN
                CASE s.movementType WHEN 'Outcome' THEN -s.valSaving ELSE s.valSaving END
                ELSE 0 END) AS totalLastMonth,
            MAX(CASE s.movementType WHEN 'Income' THEN s.dateSaving END) AS lastSaving
        FROM savings s
        JOIN saving_types t ON t.uuid = s.savingTypeUuid
        JOIN cycles c ON c.uuid = s.cycleUuid
        GROUP BY t.description
        ORDER BY lastSaving DESC
        """,
    )
    fun groupedSavings(): Flow<List<GroupedSaving>>

    @Query(
        """
        SELECT r.*, t.description AS savingTypeDescription FROM recurrent_savings r
        JOIN saving_types t ON t.uuid = r.savingTypeUuid
        ORDER BY r.createdAt DESC
        """,
    )
    fun recurrentSavings(): Flow<List<RecurrentSavingItem>>

    @Query("SELECT * FROM saving_types WHERE uuid = :uuid")
    suspend fun savingType(uuid: String): SavingTypeEntity?

    @Query("SELECT * FROM saving_types WHERE description = :description LIMIT 1")
    suspend fun savingTypeNamed(description: String): SavingTypeEntity?

    @Query("SELECT * FROM recurrent_savings WHERE savingTypeUuid = :savingTypeUuid LIMIT 1")
    suspend fun recurrentSavingOfType(savingTypeUuid: String): RecurrentSavingEntity?
}
