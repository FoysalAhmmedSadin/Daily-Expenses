/*
 * Copyright 2026 Expense Manager Contributors
 * Inspired by https://github.com/nkuppan/expensemanager (Apache License 2.0)
 */
package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.core.worker.NotificationChannels
import com.example.core.worker.WorkScheduler
import com.example.di.allAppModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class ExpenseManagerApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@ExpenseManagerApp)
                modules(allAppModules)
            }
        }
        NotificationChannels.ensureCreated(this)
        runCatching {
            WorkScheduler.scheduleRecurringWorker(this)
        }
    }
}
