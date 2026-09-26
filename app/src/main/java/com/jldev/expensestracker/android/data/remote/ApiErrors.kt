package com.jldev.expensestracker.android.data.remote

import com.jldev.expensestracker.android.data.sync.SyncRejectedException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
import java.io.IOException

/** A user-facing (Spanish) message for an error thrown by one of the API services. */
fun Throwable.toUserMessage(): String = when (this) {
    is NotAuthenticatedException -> "Tu sesión expiró. Inicia sesión de nuevo."
    is SyncRejectedException -> reason ?: "El servidor rechazó el cambio."
    is HttpException -> errorDetail() ?: "Error del servidor (${code()})."
    is IOException -> "No se pudo conectar con el servidor."
    else -> message ?: "Ocurrió un error inesperado."
}

/** FastAPI errors look like `{"detail": "Expense not found"}`; validation errors carry a list instead. */
private fun HttpException.errorDetail(): String? = try {
    val body = response()?.errorBody()?.string() ?: return null
    (ApiJson.parseToJsonElement(body).jsonObject["detail"] as? JsonPrimitive)?.content
} catch (e: Exception) {
    null
}
