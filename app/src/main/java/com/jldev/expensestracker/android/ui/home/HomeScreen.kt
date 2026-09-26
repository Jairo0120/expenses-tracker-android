package com.jldev.expensestracker.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.ui.format.formatMoney
import com.jldev.expensestracker.android.ui.format.formatShortDate
import com.jldev.expensestracker.android.ui.theme.PersianRed

@Composable
fun HomeScreen(
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) onSignedOut()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Hola, ${uiState.userName ?: "…"}",
                style = MaterialTheme.typography.headlineSmall,
            )

            uiState.status?.let { status ->
                Text("Disponible: ${formatMoney(status.moneyAvailable)}")
                Text("Gastado: ${formatMoney(status.spent)}")
            }
            if (uiState.isLoading) CircularProgressIndicator()
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(uiState.cycles, key = { it.id }) { cycle ->
                    ListItem(
                        headlineContent = { Text(formatShortDate(cycle.startDate)) },
                        supportingContent = { Text("${cycle.startDate} → ${cycle.endDate}") },
                    )
                }
            }

            OutlinedButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) {
                Text("Recargar")
            }
            Button(
                onClick = { viewModel.logout(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PersianRed.c800,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}
