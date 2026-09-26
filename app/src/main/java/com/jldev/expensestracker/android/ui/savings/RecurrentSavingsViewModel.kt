package com.jldev.expensestracker.android.ui.savings

import com.jldev.expensestracker.android.data.remote.api.SavingsApi
import com.jldev.expensestracker.android.data.remote.model.RecurrentSaving
import com.jldev.expensestracker.android.data.remote.model.RecurrentSavingCreate
import com.jldev.expensestracker.android.data.remote.model.RecurrentSavingUpdate
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecurrentSavingsViewModel @Inject constructor(
    private val savingsApi: SavingsApi,
    userMessages: UserMessages,
) : EntryListViewModel<RecurrentSaving>(userMessages, "Error al obtener los ahorros recurrentes") {

    init {
        refresh()
    }

    override suspend fun fetchPage(skip: Int, limit: Int) = savingsApi.listRecurrent(skip, limit)

    fun save(values: EntryValues, enabled: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Ahorro recurrente registrado" else "Ahorro recurrente actualizado",
            failureMessage = if (isNew) {
                "No se pudo registrar el ahorro recurrente"
            } else {
                "No se pudo actualizar el ahorro recurrente"
            },
            createAnother = createAnother,
        ) { item ->
            if (item == null) {
                savingsApi.createRecurrent(RecurrentSavingCreate(values.description, values.amount))
            } else {
                savingsApi.updateRecurrent(
                    item.id,
                    RecurrentSavingUpdate(
                        // Renames the saving type in the backend, so only send a real change.
                        description = values.description.takeIf { it != item.savingType.description },
                        valSaving = values.amount,
                        enabled = enabled,
                    ),
                )
            }
        }
    }

    fun delete() = submit("Ahorro recurrente eliminado", "No se pudo eliminar el ahorro recurrente") { item ->
        savingsApi.deleteRecurrent(checkNotNull(item).id)
    }
}
