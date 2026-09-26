@file:UseSerializers(InstantSerializer::class)

package com.jldev.expensestracker.android.data.remote.model

import com.jldev.expensestracker.android.data.remote.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
enum class SavingMovement {
    @SerialName("Income") Income,
    @SerialName("Outcome") Outcome,
}

/** A named savings "pocket"; savings are grouped by it. */
@Serializable
data class SavingType(
    val id: Int,
    val description: String,
)

/** `SavingPublic`. */
@Serializable
data class Saving(
    val id: Int,
    val valSaving: Double,
    val dateSaving: Instant,
    val isRecurrentSaving: Boolean,
    val movementType: SavingMovement,
    val movementDescription: String,
    val savingType: SavingType,
    val cycle: Cycle,
)

/** From `GET /savings/grouped-savings`: totals per saving type. */
@Serializable
data class GroupedSaving(
    val id: Int,
    val description: String,
    val isRecurrentSaving: Boolean,
    val totalGlobal: Double,
    val totalLastMonth: Double,
    val lastSaving: Instant? = null,
)

/** [description] is the saving type's name; the backend creates the type if it doesn't exist. */
@Serializable
data class SavingCreate(
    val description: String,
    val valSaving: Double,
    val dateSaving: Instant,
    /** Null targets the active cycle. */
    val cycleId: Int? = null,
    val createRecurrentSaving: Boolean = false,
)

/** Withdrawal from the saving type named [saving]. */
@Serializable
data class SavingOutcomeCreate(
    val saving: String,
    val valOutcome: Double,
    val dateOutcome: Instant,
    val description: String,
    val cycleId: Int? = null,
)

@Serializable
data class SavingUpdate(
    val description: String? = null,
    val valSaving: Double? = null,
    val dateSaving: Instant? = null,
    val cycleId: Int? = null,
)

/** `RecurrentSavingPublic`. */
@Serializable
data class RecurrentSaving(
    val id: Int,
    val savingType: SavingType,
    val valSaving: Double,
    val enabled: Boolean,
    val userId: Int,
    val createdAt: Instant,
)

@Serializable
data class RecurrentSavingCreate(
    val description: String,
    val valSaving: Double,
)

@Serializable
data class RecurrentSavingUpdate(
    val description: String? = null,
    val valSaving: Double? = null,
    val enabled: Boolean? = null,
)
