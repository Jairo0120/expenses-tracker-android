package com.jldev.expensestracker.android.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.data.remote.NotAuthenticatedException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.io.IOException

/** Pushes queued changes and pulls, in the background; WorkManager retries it with backoff. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!authRepository.isSignedIn.value) return Result.success()
        return try {
            syncRepository.sync()
            Result.success()
        } catch (e: NotAuthenticatedException) {
            // No session: nothing to do until the user logs in again (logout also cancels this work).
            Result.failure()
        } catch (e: HttpException) {
            // Server errors are usually temporary; anything else won't fix itself by retrying.
            if (e.code() >= 500) Result.retry() else Result.failure()
        } catch (e: IOException) {
            Result.retry()
        }
    }
}
