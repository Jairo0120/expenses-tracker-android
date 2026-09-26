package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.R
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

/** The repeat icon shown on entries created from a recurrent one. */
@Composable
fun RecurrentMarker(contentDescription: String, modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_repeat),
        contentDescription = contentDescription,
        tint = DodgerBlue.c300,
        modifier = modifier.size(20.dp),
    )
}
