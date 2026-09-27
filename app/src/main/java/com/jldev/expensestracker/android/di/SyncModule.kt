package com.jldev.expensestracker.android.di

import com.jldev.expensestracker.android.data.sync.SyncScheduler
import com.jldev.expensestracker.android.data.sync.WorkManagerSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindSyncScheduler(scheduler: WorkManagerSyncScheduler): SyncScheduler
}
