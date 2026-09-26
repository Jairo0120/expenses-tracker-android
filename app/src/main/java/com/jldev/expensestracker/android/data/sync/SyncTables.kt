package com.jldev.expensestracker.android.data.sync

import com.jldev.expensestracker.android.data.local.BudgetEntity
import com.jldev.expensestracker.android.data.local.CycleEntity
import com.jldev.expensestracker.android.data.local.ExpenseEntity
import com.jldev.expensestracker.android.data.local.IncomeEntity
import com.jldev.expensestracker.android.data.local.RecurrentBudgetEntity
import com.jldev.expensestracker.android.data.local.RecurrentExpenseEntity
import com.jldev.expensestracker.android.data.local.RecurrentIncomeEntity
import com.jldev.expensestracker.android.data.local.RecurrentSavingEntity
import com.jldev.expensestracker.android.data.local.SavingEntity
import com.jldev.expensestracker.android.data.local.SavingTypeEntity
import com.jldev.expensestracker.android.data.local.SyncDao
import com.jldev.expensestracker.android.data.local.SyncRecord
import com.jldev.expensestracker.android.data.remote.ApiJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant

/** A synced table: its name on the wire and how to store its records locally. */
class SyncTable<T : SyncRecord>(
    val name: String,
    private val serializer: KSerializer<T>,
    private val upsert: suspend SyncDao.(List<T>) -> Unit,
    private val delete: suspend SyncDao.(List<String>) -> Unit,
    /** Fields the server sends but doesn't accept in a push. */
    private val readOnly: Set<String> = emptySet(),
) {
    /** Stores pulled or pushed-back records: upserts live ones, removes deleted ones. */
    suspend fun store(dao: SyncDao, records: List<JsonElement>) {
        val decoded = records.map { ApiJson.decodeFromJsonElement(serializer, it) }
        val (deleted, live) = decoded.partition { it.deletedAt != null }
        if (live.isNotEmpty()) dao.upsert(live)
        if (deleted.isNotEmpty()) dao.delete(deleted.map { it.uuid })
    }

    suspend fun remove(dao: SyncDao, uuids: List<String>) = dao.delete(uuids)

    /** A push of [record] as it is now (the whole record, so nulls clear fields on the server). */
    fun change(record: T, deleted: Boolean = false): SyncChange = SyncChange(
        entity = name,
        uuid = record.uuid,
        updatedAt = record.updatedAt,
        deleted = deleted,
        data = if (deleted) JsonObject(emptyMap()) else dataOf(record),
    )

    private fun dataOf(record: T): JsonObject {
        val json = PushJson.encodeToJsonElement(serializer, record).jsonObject
        return JsonObject(json.filterKeys { it !in META_FIELDS && it !in readOnly })
    }

    private companion object {
        /** Managed by the server or sent outside `data`. */
        val META_FIELDS = setOf("uuid", "sync_version", "created_at", "updated_at", "deleted_at")

        /** Like ApiJson, but writes nulls: in a push, a null reference means "remove it". */
        val PushJson = Json(ApiJson) { explicitNulls = true }
    }
}

/** Every synced table, in dependency order (referenced tables first). */
object SyncTables {
    val cycles = SyncTable(
        "cycles", CycleEntity.serializer(), SyncDao::upsertCycles, SyncDao::deleteCycles,
        // The server decides which cycle is active.
        readOnly = setOf("is_active"),
    )
    val savingTypes = SyncTable(
        "saving_types", SavingTypeEntity.serializer(), SyncDao::upsertSavingTypes, SyncDao::deleteSavingTypes,
    )
    val recurrentBudgets = SyncTable(
        "recurrent_budgets", RecurrentBudgetEntity.serializer(),
        SyncDao::upsertRecurrentBudgets, SyncDao::deleteRecurrentBudgets,
    )
    val recurrentExpenses = SyncTable(
        "recurrent_expenses", RecurrentExpenseEntity.serializer(),
        SyncDao::upsertRecurrentExpenses, SyncDao::deleteRecurrentExpenses,
    )
    val recurrentIncomes = SyncTable(
        "recurrent_incomes", RecurrentIncomeEntity.serializer(),
        SyncDao::upsertRecurrentIncomes, SyncDao::deleteRecurrentIncomes,
    )
    val recurrentSavings = SyncTable(
        "recurrent_savings", RecurrentSavingEntity.serializer(),
        SyncDao::upsertRecurrentSavings, SyncDao::deleteRecurrentSavings,
    )
    val budgets = SyncTable("budgets", BudgetEntity.serializer(), SyncDao::upsertBudgets, SyncDao::deleteBudgets)
    val expenses = SyncTable("expenses", ExpenseEntity.serializer(), SyncDao::upsertExpenses, SyncDao::deleteExpenses)
    val incomes = SyncTable("incomes", IncomeEntity.serializer(), SyncDao::upsertIncomes, SyncDao::deleteIncomes)
    val savings = SyncTable("savings", SavingEntity.serializer(), SyncDao::upsertSavings, SyncDao::deleteSavings)

    val all: List<SyncTable<*>> = listOf(
        cycles, savingTypes, recurrentBudgets, recurrentExpenses, recurrentIncomes, recurrentSavings,
        budgets, expenses, incomes, savings,
    )
    val byName: Map<String, SyncTable<*>> = all.associateBy { it.name }
}

/** "Now" for new or edited records (their `updated_at`). */
fun now(): Instant = Instant.now()
