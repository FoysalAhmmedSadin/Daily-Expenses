/*
 * Copyright 2026 Expense Manager Contributors
 * Inspired by https://github.com/nkuppan/expensemanager (Apache License 2.0)
 */
package com.example

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.ThemeMode
import com.example.ui.navigation.ExpenseManagerShell
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.ExpenseManagerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModel()
    private var openAddTxTrigger by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openAddTxTrigger = intent?.getBooleanExtra(EXTRA_OPEN_ADD_TX, false) == true

        setContent {
            val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()
            val isLocked by mainViewModel.isAppLocked.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED -> true
            }

            LaunchedEffect(isDark) {
                val style = if (isDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            ExpenseManagerTheme(
                themeMode = prefs.themeMode,
                accentColor = prefs.accentColor,
                language = prefs.language,
                languageTag = prefs.effectiveLanguageTag
            ) {
                if (isLocked) {
                    AppLockScreen(onUnlock = mainViewModel::onUnlocked)
                } else {
                    ExpenseManagerShell(
                        openAddTxTrigger = openAddTxTrigger,
                        onConsumeAddTxTrigger = { openAddTxTrigger = false },
                        onToggleHideBalances = mainViewModel::toggleHideBalances
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_ADD_TX, false)) {
            openAddTxTrigger = true
        }
    }

    companion object {
        const val EXTRA_OPEN_ADD_TX = "extra_open_add_tx"
    }
}
