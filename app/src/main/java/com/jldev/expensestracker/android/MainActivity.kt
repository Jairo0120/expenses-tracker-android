package com.jldev.expensestracker.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.ui.AppRoot
import com.jldev.expensestracker.android.ui.components.DevBadge
import com.jldev.expensestracker.android.ui.theme.ExpensesTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpensesTrackerTheme {
                Box(Modifier.fillMaxSize()) {
                    AppRoot()
                    if (BuildConfig.DEBUG) {
                        DevBadge(
                            Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 4.dp, end = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
