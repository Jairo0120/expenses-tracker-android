package com.jldev.expensestracker.android.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.R
import com.jldev.expensestracker.android.ui.budgets.RecurrentBudgetsScreen
import com.jldev.expensestracker.android.ui.components.AddButtonSlot
import com.jldev.expensestracker.android.ui.components.AddButtonState
import com.jldev.expensestracker.android.ui.components.LocalAddButtonState
import com.jldev.expensestracker.android.ui.expenses.ExpensesScreen
import com.jldev.expensestracker.android.ui.expenses.RecurrentExpensesScreen
import com.jldev.expensestracker.android.ui.incomes.IncomesScreen
import com.jldev.expensestracker.android.ui.incomes.RecurrentIncomesScreen
import com.jldev.expensestracker.android.ui.savings.RecurrentSavingsScreen
import com.jldev.expensestracker.android.ui.savings.SavingsScreen
import com.jldev.expensestracker.android.ui.savings.SavingsSummaryScreen
import com.jldev.expensestracker.android.ui.theme.DodgerBlue
import com.jldev.expensestracker.android.ui.theme.PersianRed
import com.jldev.expensestracker.android.ui.theme.Tailwind
import kotlinx.coroutines.launch

/** Drawer + top bar with the cycle summary + bottom tabs, like the Expo app's app/_layout.js. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(viewModel: ShellViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(Tab.Expenses) }
    val addButtonState = remember { AddButtonState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // Back returns to the landing tab before leaving the app; the drawer handler below takes precedence.
    BackHandler(enabled = tab != Tab.Expenses) { tab = Tab.Expenses }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                current = tab.section,
                onSelect = { section ->
                    tab = section.tabs.first()
                    scope.launch { drawerState.close() }
                },
                onLogout = { viewModel.logout(context) },
            )
        },
    ) {
        Scaffold(
            containerColor = Tailwind.neutral900,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = { AddButtonSlot(addButtonState) },
            topBar = {
                TopAppBar(
                    // The summary sits under the title so large amounts don't crowd it on small screens.
                    title = {
                        Column {
                            Text(tab.section.title)
                            uiState.status?.let { CycleSummary(it) }
                        }
                    },
                    navigationIcon = { MenuButton(drawerState) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Tailwind.neutral900,
                        titleContentColor = DodgerBlue.c400,
                        navigationIconContentColor = DodgerBlue.c400,
                    ),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.Black) {
                    tab.section.tabs.forEach { item ->
                        NavigationBarItem(
                            selected = item == tab,
                            onClick = { tab = item },
                            icon = { Icon(painterResource(item.icon), contentDescription = null) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DodgerBlue.c400,
                                selectedTextColor = DodgerBlue.c400,
                                indicatorColor = DodgerBlue.c950,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                CompositionLocalProvider(LocalAddButtonState provides addButtonState) {
                    TabContent(tab)
                }
            }
        }
    }
}

@Composable
private fun TabContent(tab: Tab) {
    when (tab) {
        Tab.Expenses -> ExpensesScreen()
        Tab.RecurrentExpenses -> RecurrentExpensesScreen()
        Tab.Budgets -> RecurrentBudgetsScreen()
        Tab.Incomes -> IncomesScreen()
        Tab.RecurrentIncomes -> RecurrentIncomesScreen()
        Tab.SavingsSummary -> SavingsSummaryScreen()
        Tab.Savings -> SavingsScreen()
        Tab.RecurrentSavings -> RecurrentSavingsScreen()
    }
}

@Composable
private fun MenuButton(drawerState: DrawerState) {
    val scope = rememberCoroutineScope()
    IconButton(onClick = { scope.launch { drawerState.open() } }) {
        Icon(painterResource(R.drawable.ic_menu), contentDescription = "Abrir menú")
    }
}

@Composable
private fun DrawerContent(
    current: Section,
    onSelect: (Section) -> Unit,
    onLogout: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = DodgerBlue.c950,
        modifier = Modifier.width(240.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = Color.White)
            Text(
                "Expenses Tracker",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Section.entries.forEach { section ->
            NavigationDrawerItem(
                label = { Text(section.title) },
                icon = { Icon(painterResource(section.icon), contentDescription = null) },
                selected = section == current,
                onClick = { onSelect(section) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = DodgerBlue.c900,
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = DodgerBlue.c200,
                    selectedIconColor = DodgerBlue.c200,
                    unselectedTextColor = Color.White,
                    unselectedIconColor = Color.White,
                ),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = PersianRed.c800, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Icon(painterResource(R.drawable.ic_logout), contentDescription = null)
            Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
        }
    }
}
