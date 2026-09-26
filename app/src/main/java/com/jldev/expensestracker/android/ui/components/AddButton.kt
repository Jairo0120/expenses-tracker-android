package com.jldev.expensestracker.android.ui.components

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.jldev.expensestracker.android.R
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

@Composable
fun AddButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = DodgerBlue.c500,
        contentColor = Color.White,
        modifier = modifier,
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = contentDescription)
    }
}
