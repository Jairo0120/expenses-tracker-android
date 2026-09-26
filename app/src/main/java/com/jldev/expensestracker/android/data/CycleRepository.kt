package com.jldev.expensestracker.android.data

import com.jldev.expensestracker.android.data.local.AppDatabase
import com.jldev.expensestracker.android.data.local.CycleEntity
import com.jldev.expensestracker.android.data.model.CycleStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The active cycle: the one the server flags, else the one containing [today] (cycles are months), else
 * the newest. [cycles] is newest first.
 */
fun activeCycle(cycles: List<CycleEntity>, today: LocalDate = LocalDate.now()): CycleEntity? =
    cycles.firstOrNull { it.isActive }
        ?: cycles.firstOrNull { !today.isBefore(it.startDate) && !today.isAfter(it.endDate) }
        ?: cycles.firstOrNull()

/** App-wide cycle state: the cycles, which one screens show, and its totals. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CycleRepository @Inject constructor(database: AppDatabase) {
    private val cycleDao = database.cycleDao()

    /** Newest first. */
    val cycles: Flow<List<CycleEntity>> = cycleDao.cycles()

    private val _selectedCycleUuid = MutableStateFlow<String?>(null)

    /** Null means the active cycle. */
    val selectedCycleUuid: StateFlow<String?> = _selectedCycleUuid.asStateFlow()

    /** The cycle screens show: the selected one, else the active one. */
    val currentCycle: Flow<CycleEntity?> = combine(cycles, _selectedCycleUuid) { cycles, selected ->
        cycles.firstOrNull { it.uuid == selected } ?: activeCycle(cycles)
    }.distinctUntilChanged()

    val status: Flow<CycleStatus?> = currentCycle.flatMapLatest { cycle ->
        if (cycle == null) flowOf(null) else cycleDao.status(cycle.uuid)
    }

    fun select(cycleUuid: String) {
        _selectedCycleUuid.value = cycleUuid
    }

    /** The server's active cycle, for changes that apply to "the current month". */
    suspend fun active(): CycleEntity? = activeCycle(cycles.first())

    /** Back to the active cycle, e.g. on logout. */
    fun reset() {
        _selectedCycleUuid.value = null
    }
}
