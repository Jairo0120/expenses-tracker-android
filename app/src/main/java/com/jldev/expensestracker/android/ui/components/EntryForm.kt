package com.jldev.expensestracker.android.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jldev.expensestracker.android.R
import com.jldev.expensestracker.android.ui.theme.DodgerBlue
import com.jldev.expensestracker.android.ui.theme.PersianRed
import java.util.Locale

/** A validated amount (whole pesos) and trimmed description. */
data class EntryValues(val amount: Double, val description: String)

/** Amount + description form state; recreate it (e.g. with remember(key)) to start a fresh form. */
@Stable
class EntryForm(initialAmount: Double? = null, initialDescription: String = "") {
    /** Digits only; formatted as money for display. */
    val amount = TextFieldState(initialAmount?.let { String.format(Locale.US, "%.0f", it) } ?: "")
    val description = TextFieldState(initialDescription)

    private var showErrors by mutableStateOf(false)
    val amountError: Boolean get() = showErrors && amount.text.isEmpty()
    val descriptionError: Boolean get() = showErrors && description.text.isBlank()

    /** Returns the values, or null after turning on the error messages. */
    fun validate(): EntryValues? {
        showErrors = true
        if (amount.text.isEmpty() || description.text.isBlank()) return null
        return EntryValues(amount.text.toString().toDouble(), description.text.trim().toString())
    }

    /** Fills an empty description, e.g. with the name of the budget just picked (as in the Expo form). */
    fun suggestDescription(text: String) {
        if (description.text.isBlank()) description.setTextAndPlaceCursorAtEnd(text)
    }
}

private object DigitsOnly : InputTransformation {
    override val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)

    override fun TextFieldBuffer.transformInput() {
        if (!asCharSequence().all(Char::isDigit)) revertAllChanges()
    }
}

/** Shows digits as `$ 1.234.567` (es-CO) without changing the stored text. */
private object MoneyFormat : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        if (length == 0) return
        var index = length - 3
        while (index > 0) {
            insert(index, ".")
            index -= 3
        }
        insert(0, "$ ")
    }
}

@Composable
private fun formFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DodgerBlue.c400,
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = DodgerBlue.c300,
    cursorColor = DodgerBlue.c300,
)

@Composable
fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedThumbColor = DodgerBlue.c500, checkedTrackColor = DodgerBlue.c200),
        )
    }
}

/**
 * The Expo app's create/edit modal. New entries get Guardar and Crear otro; existing ones get
 * Actualizar, plus a delete icon by the title (with a confirmation the Expo app didn't have).
 * [extraContent] goes below the two fields. The card sits near the top of the screen and stays above the
 * keyboard: it moves up when the keyboard opens, and the fields scroll while the buttons stay visible.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryFormDialog(
    form: EntryForm,
    title: String,
    amountLabel: String,
    descriptionLabel: String,
    isEditing: Boolean,
    isSaving: Boolean,
    deleteQuestion: String,
    onDismiss: () -> Unit,
    onSave: (values: EntryValues, createAnother: Boolean) -> Unit,
    onDelete: () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    var confirmingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(form) { focusRequester.requestFocus() }

    // The text keyboard is much taller than the number pad: move the card up to leave room for it.
    val topGap by animateDpAsState(if (WindowInsets.isImeVisible) 8.dp else 56.dp, label = "topGap")

    Dialog(
        onDismissRequest = onDismiss,
        // The dialog handles the insets itself, so it knows where the keyboard is.
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // A full-screen dialog window: the scrim area dismisses, the card is pinned to the top.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
                // Everything below lays out in the area above the keyboard and clear of the system bars.
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DodgerBlue.c950),
                border = BorderStroke(1.dp, DodgerBlue.c800),
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = topGap, bottom = 8.dp)
                    .fillMaxWidth()
                    // Taps on the card itself must not reach the dismissing background.
                    .clickable(interactionSource = null, indication = null, onClick = {}),
            ) {
                // Fields scroll when they don't fit above the keyboard; the buttons below always show.
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        if (isEditing) {
                            IconButton(onClick = { confirmingDelete = true }, enabled = !isSaving) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = "Eliminar",
                                    tint = PersianRed.c400,
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        state = form.amount,
                        label = { Text(amountLabel) },
                        placeholder = { Text("$ 0") },
                        inputTransformation = DigitsOnly.then(InputTransformation.maxLength(12)),
                        outputTransformation = MoneyFormat,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        enabled = !isSaving,
                        isError = form.amountError,
                        supportingText = if (form.amountError) ({ Text("Campo numérico requerido.") }) else null,
                        colors = formFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                    OutlinedTextField(
                        state = form.description,
                        label = { Text(descriptionLabel) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        enabled = !isSaving,
                        isError = form.descriptionError,
                        supportingText = if (form.descriptionError) ({ Text("Campo requerido.") }) else null,
                        colors = formFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    extraContent()
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
                    if (isEditing) {
                        Button(onClick = { form.validate()?.let { onSave(it, false) } }, enabled = !isSaving) {
                            Text("Actualizar")
                        }
                    } else {
                        TextButton(onClick = { form.validate()?.let { onSave(it, true) } }, enabled = !isSaving) {
                            Text("Crear otro")
                        }
                        Button(onClick = { form.validate()?.let { onSave(it, false) } }, enabled = !isSaving) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            text = { Text(deleteQuestion) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) { Text("Eliminar", color = PersianRed.c400) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") } },
        )
    }
}
