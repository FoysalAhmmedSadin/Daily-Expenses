package com.example.di

import com.example.core.data.AccountRepositoryImpl
import com.example.core.data.BackupAndExportRepositoryImpl
import com.example.core.data.BudgetRepositoryImpl
import com.example.core.data.CategoryRepositoryImpl
import com.example.core.data.PreferencesRepositoryImpl
import com.example.core.data.TransactionRepositoryImpl
import com.example.core.database.ExpenseDatabase
import com.example.core.datastore.PreferencesDataSource
import com.example.core.domain.GetAnalyticsReportUseCase
import com.example.core.domain.GetBudgetsWithProgressUseCase
import com.example.core.domain.GetDashboardSummaryUseCase
import com.example.core.repository.AccountRepository
import com.example.core.repository.BackupAndExportRepository
import com.example.core.repository.BudgetRepository
import com.example.core.repository.CategoryRepository
import com.example.core.repository.PreferencesRepository
import com.example.core.repository.TransactionRepository
import com.example.core.worker.BudgetAlertNotifier
import com.example.ui.screens.AccountsViewModel
import com.example.ui.screens.AnalyticsViewModel
import com.example.ui.screens.BudgetsViewModel
import com.example.ui.screens.HomeViewModel
import com.example.ui.screens.MainViewModel
import com.example.ui.screens.SettingsViewModel
import com.example.ui.screens.TransactionEditorViewModel
import com.example.ui.screens.TransactionsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single { ExpenseDatabase.build(androidContext()) }
    single { get<ExpenseDatabase>().accountDao() }
    single { get<ExpenseDatabase>().categoryDao() }
    single { get<ExpenseDatabase>().transactionDao() }
    single { get<ExpenseDatabase>().budgetDao() }
    single { PreferencesDataSource(androidContext()) }
}

val repositoryModule = module {
    single<AccountRepository> { AccountRepositoryImpl(get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get(), get(), get(), get()) }
    single<BudgetRepository> { BudgetRepositoryImpl(get(), get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<BackupAndExportRepository> {
        BackupAndExportRepositoryImpl(androidContext(), get(), get(), get(), get(), get(), get(), get())
    }
    single { BudgetAlertNotifier(androidContext(), get(), get(), get()) }
}

val useCaseModule = module {
    factory { GetBudgetsWithProgressUseCase(get(), get(), get()) }
    factory { GetDashboardSummaryUseCase(get(), get(), get(), get(), get()) }
    factory { GetAnalyticsReportUseCase(get(), get()) }
}

val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), androidContext()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { TransactionsViewModel(get(), get(), get(), get(), get(), androidContext()) }
    viewModel { TransactionEditorViewModel(get(), get(), get(), get(), get(), get(), androidContext()) }
    viewModel { AccountsViewModel(get(), get(), get(), androidContext()) }
    viewModel { AnalyticsViewModel(get(), get()) }
    viewModel { BudgetsViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), androidContext()) }
}

val allAppModules = listOf(
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
