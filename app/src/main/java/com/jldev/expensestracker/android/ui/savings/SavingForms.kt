package com.jldev.expensestracker.android.ui.savings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jldev.expensestracker.android.data.model.SavingItem
import com.jldev.expensestracker.android.ui.components.EntryForm
import com.jldev.expensestracker.android.ui.components.EntryFormDialog
import com.jldev.expensestracker.android.ui.components.EntryValues
import com.jldev.expensestracker.android.ui.components.LabeledSwitch

/** Create/edit dialog for a saving, used by the Ahorros and Resumen tabs. [editorKey] resets the form. */
@Composable
internal fun SavingFormDialog(
    editorKey: Any,
    saving: SavingItem?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (values: EntryValues, createRecurrent: Boolean, createAnother: Boolean) -> Unit,
    onDelete: () -> Unit = {},
) {
    val form = remember(editorKey) { EntryForm(saving?.saving?.valSaving, saving?.savingTypeDescription.orEmpty()) }
    var createRecurrent by remember(editorKey) { mutableStateOf(false) }
    EntryFormDialog(
        form = form,
        title = if (saving == null) "Nuevo ahorro" else "Editar ahorro",
        amountLabel = "Total del ahorro",
        descriptionLabel = "Descripción del ahorro",
        isEditing = saving != null,
        isSaving = isSaving,
        deleteQuestion = "¿Eliminar este ahorro?",
        onDismiss = onDismiss,
        onSave = { values, createAnother -> onSave(values, createRecurrent, createAnother) },
        onDelete = onDelete,
    ) {
        if (saving == null) {
            LabeledSwitch("¿Ahorro mensual?", createRecurrent, { createRecurrent = it }, enabled = !isSaving)
        }
    }
}
