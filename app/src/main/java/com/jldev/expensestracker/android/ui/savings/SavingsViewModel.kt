package com.jldev.expensestracker.android.ui.savings

import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.remote.api.SavingsApi
import com.jldev.expensestracker.android.data.remote.model.Saving
import com.jldev.expensestracker.android.data.remote.model.SavingUpdate
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.CycleEntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The "Ahorros" tab: deposits of the selected cycle. */
@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsApi: SavingsApi,
    cycleRepository: CycleRepository,
    userMessages: UserMessages,
) : CycleEntryListViewModel<Saving>(cycleRepository, userMessages, "Error al obtener los ahorros") {

    init {
        start()
    }

    override suspend fun fetchPage(skip: Int, limit: Int) =
        savingsApi.list(cycleRepository.selectedCycleId.value, skip, limit)

    fun save(values: EntryValues, createRecurrent: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Ahorro registrado" else "Ahorro actualizado",
            failureMessage = if (isNew) "No se pudo registrar el ahorro" else "No se pudo actualizar el ahorro",
            createAnother = createAnother,
        ) { saving ->
            if (saving == null) {
                savingsApi.create(newSaving(values, cycleRepository.selectedCycleId.value, createRecurrent))
            } else {
                savingsApi.update(
                    saving.id,
                    SavingUpdate(
                        // The backend renames the saving type (for all its savings), so only send a real change.
                        description = values.description.takeIf { it != saving.savingType.description },
                        valSaving = values.amount,
                    ),
                )
            }
        }
    }

    fun delete() = submit("Ahorro eliminado", "No se pudo eliminar el ahorro") { saving ->
        savingsApi.delete(checkNotNull(saving).id)
    }
}
