package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.core.model.AppAccentColor
import com.example.core.model.AppCurrency
import com.example.core.model.AppLanguage
import com.example.core.model.IsoCurrencyCatalog
import com.example.core.model.NumberFormatStyle
import com.example.core.model.SupportedLanguageCatalog
import com.example.core.model.ThemeMode
import com.example.core.model.UserPreferences
import com.example.core.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private object DataStoreRegistry {
    private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

    fun getOrCreate(context: Context): DataStore<Preferences> {
        val appContext = context.applicationContext ?: context
        val file = appContext.preferencesDataStoreFile("expense_manager_prefs")
        val key = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
        return stores.computeIfAbsent(key) {
            PreferenceDataStoreFactory.create(
                produceFile = { file }
            )
        }
    }
}

class PreferencesDataSource(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = DataStoreRegistry.getOrCreate(context)

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val LANGUAGE = stringPreferencesKey("language")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val NUMBER_FORMAT = stringPreferencesKey("number_format")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val DAILY_REMINDER = booleanPreferencesKey("daily_reminder")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val legacyCurrency = prefs[Keys.CURRENCY]?.let { runCatching { AppCurrency.valueOf(it) }.getOrNull() }
            ?: AppCurrency.BDT
        val resolvedCurrencyCode = prefs[Keys.CURRENCY_CODE]?.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
            ?: legacyCurrency.code

        val legacyLanguage = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.ENGLISH
        val resolvedLangTag = prefs[Keys.LANGUAGE_TAG]?.takeIf { it.isNotBlank() }
            ?.let { SupportedLanguageCatalog.normalizeTag(it) }
            ?: legacyLanguage.code

        val resolvedViewMode = prefs[Keys.VIEW_MODE]?.let {
            runCatching { ViewMode.valueOf(it) }.getOrNull()
        } ?: ViewMode.MONTHLY

        UserPreferences(
            currency = IsoCurrencyCatalog.toAppCurrencyOrNull(resolvedCurrencyCode) ?: legacyCurrency,
            language = SupportedLanguageCatalog.toAppLanguage(resolvedLangTag),
            numberFormat = prefs[Keys.NUMBER_FORMAT]?.let {
                runCatching { NumberFormatStyle.valueOf(it) }.getOrNull()
            } ?: NumberFormatStyle.ENGLISH,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accentColor = prefs[Keys.ACCENT_COLOR]?.let { runCatching { AppAccentColor.valueOf(it) }.getOrNull() }
                ?: AppAccentColor.EMERALD,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            dailyReminderEnabled = prefs[Keys.DAILY_REMINDER] ?: false,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS] ?: true,
            hideBalances = prefs[Keys.HIDE_BALANCES] ?: false,
            lastLocalBackupAt = prefs[Keys.LAST_BACKUP_AT],
            lastLocalBackupStatus = prefs[Keys.LAST_BACKUP_STATUS] ?: "",
            viewMode = resolvedViewMode,
            languageTag = resolvedLangTag,
            currencyCode = resolvedCurrencyCode
        )
    }

    suspend fun setCurrency(currency: AppCurrency) {
        dataStore.edit {
            it[Keys.CURRENCY] = currency.name
            it[Keys.CURRENCY_CODE] = currency.code
        }
    }

    suspend fun setCurrencyCode(code: String) {
        val resolved = IsoCurrencyCatalog.resolve(code)
        dataStore.edit {
            it[Keys.CURRENCY_CODE] = resolved.code
            val legacy = IsoCurrencyCatalog.toAppCurrencyOrNull(resolved.code) ?: AppCurrency.BDT
            it[Keys.CURRENCY] = legacy.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit {
            it[Keys.LANGUAGE] = language.name
            it[Keys.LANGUAGE_TAG] = language.code
        }
    }

    suspend fun setLanguageTag(tag: String) {
        val normalized = SupportedLanguageCatalog.normalizeTag(tag)
        val legacy = SupportedLanguageCatalog.toAppLanguage(normalized)
        dataStore.edit {
            it[Keys.LANGUAGE_TAG] = normalized
            it[Keys.LANGUAGE] = legacy.name
        }
    }

    suspend fun setViewMode(viewMode: ViewMode) {
        dataStore.edit { it[Keys.VIEW_MODE] = viewMode.name }
    }

    suspend fun setNumberFormat(style: NumberFormatStyle) {
        dataStore.edit { it[Keys.NUMBER_FORMAT] = style.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(color: AppAccentColor) {
        dataStore.edit { it[Keys.ACCENT_COLOR] = color.name }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setDailyReminder(enabled: Boolean, hour: Int? = null, minute: Int? = null) {
        dataStore.edit {
            it[Keys.DAILY_REMINDER] = enabled
            if (hour != null) it[Keys.REMINDER_HOUR] = hour
            if (minute != null) it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setBudgetAlerts(enabled: Boolean) {
        dataStore.edit { it[Keys.BUDGET_ALERTS] = enabled }
    }

    suspend fun setHideBalances(hidden: Boolean) {
        dataStore.edit { it[Keys.HIDE_BALANCES] = hidden }
    }

    suspend fun setLastBackupInfo(timestamp: Long, status: String) {
        dataStore.edit {
            it[Keys.LAST_BACKUP_AT] = timestamp
            it[Keys.LAST_BACKUP_STATUS] = status
        }
    }
}
