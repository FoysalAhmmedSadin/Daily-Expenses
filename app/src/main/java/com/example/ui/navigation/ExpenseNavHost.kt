package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.core.common.LocalAppStrings
import com.example.core.model.TransactionType
import com.example.ui.screens.AboutDeveloperScreen
import com.example.ui.screens.AccountDetailScreen
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AccountsViewModel
import com.example.ui.screens.AddEditTransactionScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AnalyticsViewModel
import com.example.ui.screens.AppearanceSettingsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.BudgetsViewModel
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CurrencySettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeViewModel
import com.example.ui.screens.LanguageSettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsViewModel
import com.example.ui.screens.TransactionEditorViewModel
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.TransactionsViewModel
import com.example.ui.screens.ViewModeSettingsScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable data object HomeRoute
@Serializable data object TransactionsRoute
@Serializable data object AnalyticsRoute
@Serializable data object BudgetsRoute
@Serializable data object SettingsRoute
@Serializable data object ViewModeSettingsRoute
@Serializable data object AppearanceSettingsRoute
@Serializable data object LanguageSettingsRoute
@Serializable data object CurrencySettingsRoute
@Serializable data object AccountsRoute
@Serializable data class AccountDetailRoute(val accountId: Long)
@Serializable data object CategoriesRoute
@Serializable data object AboutDeveloperRoute
@Serializable data class AddEditTransactionRoute(
    val transactionId: Long = 0L,
    val initialType: String? = null,
    val initialAccountId: Long? = null
)

private data class TopLevelTab(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun ExpenseManagerShell(
    openAddTxTrigger: Boolean,
    onConsumeAddTxTrigger: () -> Unit,
    onToggleHideBalances: () -> Unit
) {
    val navController = rememberNavController()
    val strings = LocalAppStrings.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val budgetNavIcon = ImageVector.vectorResource(R.drawable.ic_chart_no_axes_combined)

    val tabs = listOf(
        TopLevelTab(HomeRoute, strings.navHome, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
        TopLevelTab(TransactionsRoute, strings.navTransactions, Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_transactions"),
        TopLevelTab(AnalyticsRoute, strings.navAnalytics, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_analytics"),
        TopLevelTab(BudgetsRoute, strings.navBudgets, budgetNavIcon, budgetNavIcon, "nav_budgets"),
        TopLevelTab(SettingsRoute, strings.navSettings, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    val isTopLevel = currentDestination?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<TransactionsRoute>() ||
            dest.hasRoute<AnalyticsRoute>() ||
            dest.hasRoute<BudgetsRoute>() ||
            dest.hasRoute<SettingsRoute>()
    } ?: true

    LaunchedEffect(openAddTxTrigger) {
        if (openAddTxTrigger) {
            onConsumeAddTxTrigger()
            navController.navigate(AddEditTransactionRoute())
        }
    }

    val navigateToTab: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        val tabAccent = MaterialTheme.colorScheme.primary
                        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = tabAccent,
                                    selectedTextColor = tabAccent,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor,
                                    indicatorColor = tabAccent.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isTopLevel,
                    enter = fadeIn(tween(80)),
                    exit = fadeOut(tween(60))
                ) {
                    FloatingActionButton(
                        onClick = { navController.navigate(AddEditTransactionRoute()) },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = strings.addTransaction
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        val tabAccent = MaterialTheme.colorScheme.primary
                        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = tabAccent,
                                    selectedTextColor = tabAccent,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor,
                                    indicatorColor = tabAccent.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    enterTransition = { fadeIn(animationSpec = tween(75)) },
                    exitTransition = { fadeOut(animationSpec = tween(60)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(75)) },
                    popExitTransition = { fadeOut(animationSpec = tween(60)) },
                    modifier = Modifier.weight(1f)
                ) {
                    composable<HomeRoute> {
                        val vm: HomeViewModel = koinViewModel()
                        HomeScreen(
                            homeViewModel = vm,
                            onToggleHideBalances = onToggleHideBalances,
                            onOpenAccounts = { navController.navigate(AccountsRoute) },
                            onOpenAccountDetail = { id -> navController.navigate(AccountDetailRoute(id)) },
                            onOpenTransactions = { navigateToTab(TransactionsRoute) },
                            onOpenTransaction = { id ->
                                navController.navigate(AddEditTransactionRoute(transactionId = id))
                            },
                            onOpenBudgets = { navigateToTab(BudgetsRoute) },
                            onAddTransaction = { navController.navigate(AddEditTransactionRoute()) }
                        )
                    }

                    composable<TransactionsRoute> {
                        val vm: TransactionsViewModel = koinViewModel()
                        TransactionsScreen(
                            viewModel = vm,
                            onAddTransaction = { navController.navigate(AddEditTransactionRoute()) },
                            onEditTransaction = { id ->
                                navController.navigate(AddEditTransactionRoute(transactionId = id))
                            }
                        )
                    }

                    composable<AnalyticsRoute> {
                        val vm: AnalyticsViewModel = koinViewModel()
                        AnalyticsScreen(
                            viewModel = vm,
                            onAddTransaction = { navController.navigate(AddEditTransactionRoute()) }
                        )
                    }

                    composable<BudgetsRoute> {
                        val vm: BudgetsViewModel = koinViewModel()
                        BudgetsScreen(viewModel = vm)
                    }

                    composable<SettingsRoute> {
                        val vm: SettingsViewModel = koinViewModel()
                        SettingsScreen(
                            viewModel = vm,
                            onOpenViewMode = { navController.navigate(ViewModeSettingsRoute) },
                            onOpenAppearance = { navController.navigate(AppearanceSettingsRoute) },
                            onOpenLanguage = { navController.navigate(LanguageSettingsRoute) },
                            onOpenCurrency = { navController.navigate(CurrencySettingsRoute) },
                            onOpenCategories = { navController.navigate(CategoriesRoute) },
                            onOpenAboutDeveloper = { navController.navigate(AboutDeveloperRoute) }
                        )
                    }

                    composable<ViewModeSettingsRoute> {
                        val vm: SettingsViewModel = koinViewModel()
                        ViewModeSettingsScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AppearanceSettingsRoute> {
                        val vm: SettingsViewModel = koinViewModel()
                        AppearanceSettingsScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<LanguageSettingsRoute> {
                        val vm: SettingsViewModel = koinViewModel()
                        LanguageSettingsScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<CurrencySettingsRoute> {
                        val vm: SettingsViewModel = koinViewModel()
                        CurrencySettingsScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AccountsRoute> {
                        val vm: AccountsViewModel = koinViewModel()
                        AccountsScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onOpenAccountDetail = { id -> navController.navigate(AccountDetailRoute(id)) },
                            onStartTransfer = { accountId ->
                                navController.navigate(
                                    AddEditTransactionRoute(
                                        initialType = TransactionType.TRANSFER.name,
                                        initialAccountId = accountId
                                    )
                                )
                            }
                        )
                    }

                    composable<AccountDetailRoute> { entry ->
                        val route = entry.toRoute<AccountDetailRoute>()
                        val vm: AccountsViewModel = koinViewModel()
                        AccountDetailScreen(
                            accountId = route.accountId,
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onAddTransactionForAccount = { accId, type ->
                                navController.navigate(
                                    AddEditTransactionRoute(
                                        initialType = type.name,
                                        initialAccountId = accId
                                    )
                                )
                            },
                            onEditTransaction = { txId ->
                                navController.navigate(AddEditTransactionRoute(transactionId = txId))
                            }
                        )
                    }

                    composable<CategoriesRoute> {
                        val vm: SettingsViewModel = koinViewModel()
                        CategoriesScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AboutDeveloperRoute> {
                        AboutDeveloperScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AddEditTransactionRoute> { entry ->
                        val route = entry.toRoute<AddEditTransactionRoute>()
                        val vm: TransactionEditorViewModel = koinViewModel()
                        val parsedType = route.initialType?.let {
                            runCatching { TransactionType.valueOf(it) }.getOrNull()
                        }
                        AddEditTransactionScreen(
                            viewModel = vm,
                            transactionId = route.transactionId.takeIf { it > 0L },
                            initialType = parsedType,
                            initialAccountId = route.initialAccountId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
