@file:UseSerializers(InstantSerializer::class)

package com.jldev.expensestracker.android.data.remote.model

import com.jldev.expensestracker.android.data.remote.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class Income(
    val id: Int,
    val description: String,
    val valIncome: Double,
    val dateIncome: Instant,
    val isRecurrentIncome: Boolean = false,
    val cycleId: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class IncomeCreate(
    val description: String,
    val valIncome: Double,
    val dateIncome: Instant,
    /** Null targets the active cycle. */
    val cycleId: Int? = null,
    val createRecurrentIncome: Boolean = false,
)

@Serializable
data class IncomeUpdate(
    val description: String? = null,
    val valIncome: Double? = null,
    val dateIncome: Instant? = null,
    val cycleId: Int? = null,
)

@Serializable
data class RecurrentIncome(
    val id: Int,
    val description: String,
    val valIncome: Double,
    val enabled: Boolean = true,
    val userId: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class RecurrentIncomeCreate(
    val description: String,
    val valIncome: Double,
)

@Serializable
data class RecurrentIncomeUpdate(
    val description: String? = null,
    val valIncome: Double? = null,
    val enabled: Boolean? = null,
)
