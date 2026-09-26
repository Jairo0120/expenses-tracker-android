package com.jldev.expensestracker.android.ui.shell

import androidx.annotation.DrawableRes
import com.jldev.expensestracker.android.R

/** Drawer entries, matching the Expo app's drawer. */
enum class Section(val title: String, @param:DrawableRes val icon: Int) {
    Expenses("Gastos", R.drawable.ic_payments),
    Incomes("Ingresos", R.drawable.ic_trending_up),
    Savings("Ahorros", R.drawable.ic_savings),
    ;

    val tabs: List<Tab> get() = Tab.entries.filter { it.section == this }
}

/** Bottom tabs of each section, in display order. The first one is the section's landing tab. */
enum class Tab(val section: Section, val label: String, @param:DrawableRes val icon: Int) {
    Expenses(Section.Expenses, "Gastos", R.drawable.ic_payments),
    RecurrentExpenses(Section.Expenses, "Recurrentes", R.drawable.ic_repeat),
    Budgets(Section.Expenses, "Presupuestos", R.drawable.ic_checklist),

    Incomes(Section.Incomes, "Ingresos", R.drawable.ic_trending_up),
    RecurrentIncomes(Section.Incomes, "Recurrentes", R.drawable.ic_repeat),

    SavingsSummary(Section.Savings, "Resumen", R.drawable.ic_home),
    Savings(Section.Savings, "Ahorros", R.drawable.ic_savings),
    RecurrentSavings(Section.Savings, "Recurrentes", R.drawable.ic_repeat),
}
