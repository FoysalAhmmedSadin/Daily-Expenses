# Daily Expenses — Personal Finance Manager

**Daily Expenses** (`com.track.dailyexpenses.app`) is an offline-first Android personal finance application built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**.

## Features

- **Interactive Financial Dashboard**
  - Real-time Total Balance, Total Income, Total Expenses, and Monthly Savings calculated from local database transactions.
  - Date filters for **Today**, **This Week**, **This Month**, **This Year**, and **Custom Date Range**.
  - **Monthly Expense Summary** with an interactive vertical category bar chart, month-over-month % trend badge, and top spending category breakdown.
  - **80% Budget Limit Progress Alerts** surfaced prominently on the dashboard whenever any category or overall monthly budget reaches 80% or more of its spending cap.
  - Interactive Income vs. Expense bar chart, 6-month Spending Trend chart, Expense Breakdown donut chart, Account Balance carousel, and Recent Transactions list.
  - One-tap balance privacy masking toggle.

- **Per-Category Monthly Budgets**
  - Set monthly spending caps per expense category or as an overall monthly budget.
  - Track spent, remaining, overspent amounts, and percentage progress bars with configurable alert thresholds (default **80%**).
  - Real-time budget alert notifications when expenses cross threshold limits.

- **Transactions & Automated Recurring Entries**
  - Fast creation and editing for **Expenses**, **Income**, and **Account Transfers**.
  - Search by notes, categories, or accounts with type, account, category, date range, and recurring filters.
  - Schedule repeating transactions (**Daily**, **Weekly**, **Monthly**, **Yearly**) in Room with automatic execution on app startup and every 12 hours via WorkManager.
  - Attach receipt images safely using the Android Photo Picker.

- **Multi-Account & Category Management**
  - Manage Cash, Bank, Savings, Credit Card, Mobile Banking, and Custom accounts with atomic balance updates.
  - Built-in and customizable expense/income categories with localized names and vector icons.

- **Analytics, Export & Encrypted Backups**
  - Daily, Weekly, Monthly, and Yearly analytics reports with savings rate, daily average spend, and month-over-month comparisons.
  - Export transactions by **Selected Month** or **All Months** to **CSV** and generate **PDF** statements.
  - Versioned **AES-256-GCM** encrypted local backups and restores with automatic pre-restore safety snapshots.
  - **Google Drive Cloud Backup & Sync**: Sign in with Google (`Credential Manager` + `AuthorizationClient`) to store backups privately in your Google Drive App Data folder (`drive.appdata`), with optional passphrase encryption, cloud backup history/restore, account switching, and **Manual / Daily / Weekly** automatic background backups via WorkManager.

- **Customization, Security & Widget**
  - **English** and **Bangla** UI localization, South Asian (`Lakh` / `Crore`) and standard number formatting, and multiple currencies (`BDT`, `USD`, `EUR`, `GBP`, `INR`).
  - **Light**, **Dark**, **AMOLED Black** (`#000000`), and **System** Material 3 themes with **8 selectable accent colors** (Emerald `#34C759` default, Ocean Blue, Indigo, Violet, Coral Red, Amber Gold, Teal Cyan, Rose Pink).
  - Optional Biometric / device credential lock and Jetpack Glance home-screen summary widget.
  - Dedicated **About Developer** screen in Settings with direct support email integration.

## Tech Stack & Architecture

- **Language:** Kotlin (JVM 11)
- **UI:** Jetpack Compose, Material 3, Type-safe Navigation Compose, Coil
- **Architecture:** Clean Architecture + MVVM (`ViewModel`, `StateFlow`, Use Cases, Repositories)
- **Persistence:** Room Database (with KSP) & Jetpack DataStore Preferences
- **Cloud Backup & Networking:** Credential Manager, Google Identity Services (`AuthorizationClient`), OkHttp, Google Drive REST API v3 (`appDataFolder`)
- **DI & Background Work:** Koin, Kotlin Coroutines & Flow, WorkManager, Glance AppWidget
- **Testing:** JUnit4, Kotlin Coroutines Test, Robolectric, MockWebServer, Jetpack Compose UI Test

## Build & Run

- **Minimum SDK:** 24 (`Android 7.0`)
- **Target / Compile SDK:** 36
- **Package ID:** `com.track.dailyexpenses.app`

Run unit and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```

Build the debug APK:
```bash
gradle :app:assembleDebug
```

The compiled debug APK is placed in:
- `app/build/outputs/apk/debug/app-debug.apk`
- `.build-outputs/app-debug.apk`
- `APK_DOWNLOAD/app-debug.apk`

## Developer & License

- **Developer:** Foysal Ahmmed Sadin (`foysal24x7@gmail.com`)
- **License:** Apache License, Version 2.0 — see `LICENSE` and `NOTICE` for details.
