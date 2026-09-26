package com.jldev.expensestracker.android.ui.common

import androidx.lifecycle.viewModelScope
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.local.CycleEntity
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** An [EntryListViewModel] whose items belong to the cycle the user picked (the active one by default). */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class CycleEntryListViewModel<T : Any>(
    protected val cycleRepository: CycleRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : EntryListViewModel<T>(syncRepository, userMessages) {

    val cycles: StateFlow<List<CycleEntity>> =
        cycleRepository.cycles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentCycle: StateFlow<CycleEntity?> =
        cycleRepository.currentCycle.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The items of the cycle [cycleUuid]. */
    protected abstract fun items(cycleUuid: String): Flow<List<T>>

    final override fun items(): Flow<List<T>> = cycleRepository.currentCycle
        .map { it?.uuid }
        .distinctUntilChanged()
        .flatMapLatest { uuid -> if (uuid == null) flowOf(emptyList()) else items(uuid) }

    fun selectCycle(cycleUuid: String) = cycleRepository.select(cycleUuid)

    /** The cycle new items go to. */
    protected fun cycleForNewItems(): CycleEntity =
        checkNotNull(currentCycle.value) { "No hay un ciclo para registrar" }
}
