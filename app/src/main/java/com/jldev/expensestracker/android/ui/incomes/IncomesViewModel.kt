package com.jldev.expensestracker.android.ui.incomes

import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.IncomesRepository
import com.jldev.expensestracker.android.data.local.IncomeEntity
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.CycleEntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class IncomesViewModel @Inject constructor(
    private val incomesRepository: IncomesRepository,
    cycleRepository: CycleRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : CycleEntryListViewModel<IncomeEntity>(cycleRepository, syncRepository, userMessages) {

    override fun items(cycleUuid: String) = incomesRepository.incomes(cycleUuid)

    fun save(values: EntryValues, createRecurrent: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Ingreso registrado" else "Ingreso actualizado",
            failureMessage = if (isNew) "No se pudo registrar el ingreso" else "No se pudo actualizar el ingreso",
            createAnother = createAnother,
        ) { income ->
            if (income == null) {
                incomesRepository.create(
                    values.description, values.amount, Instant.now(), cycleForNewItems().uuid, createRecurrent,
                )
            } else {
                incomesRepository.update(income, values.description, values.amount)
            }
        }
    }

    fun delete() = submit("Ingreso eliminado", "No se pudo eliminar el ingreso") { income ->
        incomesRepository.delete(checkNotNull(income))
    }
}
