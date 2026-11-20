package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.LocalAppStrings
import com.example.core.model.Budget
import com.example.core.model.BudgetWithProgress
import com.example.core.model.Category
import com.example.ui.components.BudgetProgressItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PremiumCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningAmber

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val prefs = state.preferences
    val langTag = prefs.effectiveLanguageTag
    val currencyCode = prefs.effectiveCurrencyCode

    var showEditorDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetWithProgress?>(null) }

    val overspentCount = state.budgets.count { it.isOverspent }
    val warningCount = state.budgets.count { (it.isWarning || it.isReachedEightyPercent) && !it.isOverspent }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.budgets,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Button(
                        onClick = {
                            editingBudget = null
                            showEditorDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("add_budget_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.addBudget)
                    }
                }
            }

            // Month Navigator
            item {
                PremiumCard(contentPadding = 10.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.shiftMonth(-1) }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = strings.previousMonth
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatMonthLabel(
                                state.monthYear,
                                langTag,
                                prefs.numberFormat
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { viewModel.shiftMonth(1) }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = strings.nextMonth
                            )
                        }
                    }
                }
            }

            // Overspending Alert Banner
            if (overspentCount > 0 || warningCount > 0) {
                item {
                    val alertColor = if (overspentCount > 0) ExpenseRed else WarningAmber
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = alertColor.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = strings.budgetAlerts,
                                tint = alertColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (overspentCount > 0) {
                                    strings.budgetsExceededBanner(overspentCount, prefs.numberFormat)
                                } else {
                                    strings.budgetsWarningBanner(warningCount, prefs.numberFormat)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = alertColor
                            )
                        }
                    }
                }
            }

            if (state.budgets.isEmpty()) {
                item {
                    PremiumCard {
                        EmptyStateView(
                            icon = ImageVector.vectorResource(R.drawable.ic_chart_no_axes_combined),
                            title = strings.noBudgetsTitle,
                            subtitle = strings.noBudgetsSubtitle,
                            actionLabel = strings.addBudget,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onAction = {
                                editingBudget = null
                                showEditorDialog = true
                            }
                        )
                    }
                }
            } else {
                items(state.budgets, key = { it.budget.id }) { item ->
                    PremiumCard(
                        onClick = {
                            editingBudget = item
                            showEditorDialog = true
                        },
                        modifier = Modifier.testTag("budget_card_${item.budget.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BudgetProgressItem(
                                    budgetWithProgress = item,
                                    preferences = prefs,
                                    currencyCode = currencyCode
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteBudget(item.budget.id) },
                                modifier = Modifier.testTag("delete_budget_${item.budget.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = strings.delete,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        BudgetEditorDialog(
            monthYear = state.monthYear,
            initial = editingBudget?.budget,
            existingBudgets = state.budgets.map { it.budget },
            categories = state.expenseCategories,
            languageTag = langTag,
            numberFormat = prefs.numberFormat,
            onDismiss = { showEditorDialog = false },
            onSave = { budget ->
                viewModel.saveBudget(budget)
                showEditorDialog = false
            }
        )
    }
}

@Composable
private fun BudgetEditorDialog(
    monthYear: String,
    initial: Budget?,
    existingBudgets: List<Budget>,
    categories: List<Category>,
    languageTag: String,
    numberFormat: com.example.core.model.NumberFormatStyle,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    val strings = LocalAppStrings.current
    val existingCatIds = remember(existingBudgets) {
        existingBudgets.mapNotNull { it.categoryId }.toSet()
    }
    var selectedCategoryId by remember {
        mutableStateOf(
            if (initial != null) {
                initial.categoryId
            } else {
                categories.firstOrNull { it.id !in existingCatIds }?.id
                    ?: categories.firstOrNull()?.id
            }
        )
    }
    var limitInput by remember {
        mutableStateOf(
            initial?.amountLimit?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var thresholdPercent by remember { mutableFloatStateOf((initial?.alertThresholdPercent ?: 80).toFloat()) }
    val matchingExisting = remember(selectedCategoryId, existingBudgets, initial) {
        if (initial != null) null
        else existingBudgets.firstOrNull { it.categoryId == selectedCategoryId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .widthIn(max = 520.dp),
        title = { Text(if (initial == null) strings.addBudget else strings.editBudget) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.categorySpendingCap, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text(strings.overallMonthlyBudget) },
                        modifier = Modifier.testTag("budget_category_chip_overall")
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.localizedName(languageTag, strings)) },
                            modifier = Modifier.testTag("budget_category_chip_${cat.id}")
                        )
                    }
                }

                if (matchingExisting != null) {
                    Text(
                        text = strings.updatesExistingCap,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber
                    )
                }

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { v ->
                        val cleaned = v.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1) limitInput = cleaned
                    },
                    label = { Text(strings.monthlyLimit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )

                Text(
                    text = "${strings.alertThreshold}: ${
                        CurrencyFormatter.toLocalizedDigits("${thresholdPercent.toInt()}%", numberFormat)
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = thresholdPercent,
                    onValueChange = { thresholdPercent = it },
                    valueRange = 50f..100f,
                    steps = 4,
                    modifier = Modifier.testTag("budget_threshold_slider")
                )
            }
        },
        confirmButton = {
            val limit = limitInput.toDoubleOrNull()
            Button(
                onClick = {
                    if (limit != null && limit > 0.0) {
                        onSave(
                            Budget(
                                id = initial?.id ?: 0L,
                                categoryId = selectedCategoryId,
                                amountLimit = limit,
                                monthYear = initial?.monthYear ?: monthYear,
                                alertThresholdPercent = thresholdPercent.toInt(),
                                createdAt = initial?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = limit != null && limit > 0.0,
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
