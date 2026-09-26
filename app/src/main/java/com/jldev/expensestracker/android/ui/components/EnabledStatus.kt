package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.R
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

/** "Habilitado ✓" / "Deshabilitado ✕" on recurrent entries. */
@Composable
fun EnabledStatus(enabled: Boolean) {
    val color = if (enabled) DodgerBlue.c500 else Color.Gray
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (enabled) "Habilitado" else "Deshabilitado", style = MaterialTheme.typography.bodyMedium, color = color)
        Icon(
            painterResource(if (enabled) R.drawable.ic_check else R.drawable.ic_close),
            contentDescription = null,
            tint = if (enabled) DodgerBlue.c300 else Color.Gray,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(18.dp),
        )
    }
}
