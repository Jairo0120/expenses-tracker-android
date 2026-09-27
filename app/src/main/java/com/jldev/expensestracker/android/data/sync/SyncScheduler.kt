package com.jldev.expensestracker.android.data.sync

/** Runs syncs in the background (WorkManager in the app). */
interface SyncScheduler {
    /** A sync as soon as there's a connection, e.g. after a local change. */
    fun syncSoon()

    /** Periodic syncs while signed in. */
    fun startPeriodic()

    /** Stops everything, e.g. on logout. */
    fun stop()
}
