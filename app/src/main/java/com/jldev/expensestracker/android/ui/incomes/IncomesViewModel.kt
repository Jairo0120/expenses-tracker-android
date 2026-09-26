package com.jldev.expensestracker.android.ui.incomes

import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.remote.api.IncomesApi
import com.jldev.expensestracker.android.data.remote.model.Income
import com.jldev.expensestracker.android.data.remote.model.IncomeCreate
import com.jldev.expensestracker.android.data.remote.model.IncomeUpdate
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.CycleEntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class IncomesViewModel @Inject constructor(
    private val incomesApi: IncomesApi,
    cycleRepository: CycleRepository,
    userMessages: UserMessages,
) : CycleEntryListViewModel<Income>(cycleRepository, userMessages, "Error al obtener los ingresos") {

    init {
        start()
    }

    override suspend fun fetchPage(skip: Int, limit: Int) =
        incomesApi.list(cycleRepository.selectedCycleId.value, skip, limit)

    fun save(values: EntryValues, createRecurrent: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Ingreso registrado" else "Ingreso actualizado",
            failureMessage = if (isNew) "No se pudo registrar el ingreso" else "No se pudo actualizar el ingreso",
            createAnother = createAnother,
        ) { income ->
            if (income == null) {
                incomesApi.create(
                    IncomeCreate(
                        description = values.description,
                        valIncome = values.amount,
                        dateIncome = Instant.now(),
                        cycleId = cycleRepository.selectedCycleId.value,
                        createRecurrentIncome = createRecurrent,
                    ),
                )
            } else {
                incomesApi.update(income.id, IncomeUpdate(description = values.description, valIncome = values.amount))
            }
        }
    }

    fun delete() = submit("Ingreso eliminado", "No se pudo eliminar el ingreso") { income ->
        incomesApi.delete(checkNotNull(income).id)
    }
}
