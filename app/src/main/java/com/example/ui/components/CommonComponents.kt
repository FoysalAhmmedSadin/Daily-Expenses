package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.CategoryIcons
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.LocalAppStrings
import com.example.core.model.BudgetWithProgress
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.TransactionType
import com.example.core.model.TransactionWithDetails
import com.example.core.model.UserPreferences
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = LocalIsDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = primary,
            checkedBorderColor = primary,
            uncheckedThumbColor = if (isDark) Color(0xFFD0D5DD) else Color(0xFF475467),
            uncheckedTrackColor = if (isDark) Color(0xFF232732) else Color(0xFFD0D5DD),
            uncheckedBorderColor = if (isDark) Color(0xFF6E7787) else Color(0xFF667085),
            disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            disabledCheckedTrackColor = primary.copy(alpha = 0.45f),
            disabledUncheckedThumbColor = if (isDark) Color(0xFF6B7280) else Color(0xFF98A2B3),
            disabledUncheckedTrackColor = if (isDark) Color(0xFF191C24) else Color(0xFFE4E7EC),
            disabledUncheckedBorderColor = if (isDark) Color(0xFF3E4452) else Color(0xFFB0B8C4)
        ),
        modifier = modifier
    )
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
            border = border
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
            border = border
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun CategoryIconBadge(
    icon: ImageVector,
    tintColor: Color,
    size: Dp = 44.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(tintColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun TransactionRowItem(
    item: TransactionWithDetails,
    preferences: UserPreferences,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val tx = item.transaction
    val isTransfer = tx.type == TransactionType.TRANSFER
    val langTag = preferences.effectiveLanguageTag
    val txCurrencyCode = tx.currencyCode.ifBlank { preferences.effectiveCurrencyCode }

    val title = when {
        isTransfer -> "${item.account?.name ?: strings.account} \u2192 ${item.toAccount?.name ?: strings.account}"
        else -> item.category?.localizedName(langTag, strings) ?: strings.transactionTypeLabel(tx.type)
    }

    val subtitleParts = buildList {
        if (!isTransfer) item.account?.name?.let { add(it) }
        if (tx.note.isNotBlank()) add(tx.note)
        add(DateTimeUtils.formatTime(tx.timestamp, preferences.numberFormat, languageTag = langTag))
    }

    val accentColor = when (tx.type) {
        TransactionType.EXPENSE -> Color(item.category?.colorHex ?: 0xFFFF3B30)
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }

    val amountColor = when (tx.type) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }

    val formattedAmount = CurrencyFormatter.format(
        amount = tx.amount,
        currencyCode = txCurrencyCode,
        numberFormat = preferences.numberFormat,
        hideAmount = preferences.hideBalances
    ).let { raw ->
        if (preferences.hideBalances) raw
        else when (tx.type) {
            TransactionType.EXPENSE -> "-$raw"
            TransactionType.INCOME -> "+$raw"
            TransactionType.TRANSFER -> raw
        }
    }

    val icon = if (isTransfer) Icons.Rounded.SwapHoriz
    else CategoryIcons.resolve(item.category?.iconKey)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(
            icon = icon,
            tintColor = accentColor,
            contentDescription = title
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (tx.recurrence != RecurrenceFrequency.NONE) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = strings.recurring,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (!tx.receiptUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = strings.receiptAttachment,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitleParts.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = formattedAmount,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = amountColor
        )
    }
}

@Composable
fun BudgetProgressItem(
    budgetWithProgress: BudgetWithProgress,
    preferences: UserPreferences,
    currencyCode: String = preferences.effectiveCurrencyCode,
    onClick: (() -> Unit)? = null,
    tagPrefix: String = "",
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val langTag = preferences.effectiveLanguageTag
    val title = budgetWithProgress.category?.localizedName(langTag, strings)
        ?: strings.overallMonthlyBudget

    val animatedProgress by animateFloatAsState(
        targetValue = budgetWithProgress.progressRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 200),
        label = "budget_progress"
    )

    val isWarningOrOver = budgetWithProgress.isOverspent ||
        budgetWithProgress.isWarning ||
        budgetWithProgress.isReachedEightyPercent

    val statusColor = when {
        budgetWithProgress.isOverspent -> ExpenseRed
        budgetWithProgress.isWarning || budgetWithProgress.isReachedEightyPercent -> WarningAmber
        else -> IncomeGreen
    }

    val spentText = CurrencyFormatter.format(
        budgetWithProgress.spentAmount,
        currencyCode,
        preferences.numberFormat,
        hideAmount = preferences.hideBalances
    )
    val limitText = CurrencyFormatter.format(
        budgetWithProgress.budget.amountLimit,
        currencyCode,
        preferences.numberFormat,
        hideAmount = preferences.hideBalances
    )
    val percentText = CurrencyFormatter.toLocalizedDigits(
        "${budgetWithProgress.progressPercent}%",
        preferences.numberFormat
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(6.dp)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val icon = budgetWithProgress.category?.let { CategoryIcons.resolve(it.iconKey) }
                    ?: ImageVector.vectorResource(R.drawable.ic_chart_no_axes_combined)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isWarningOrOver) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = strings.budgetAlerts,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = statusColor.copy(alpha = 0.14f),
                modifier = Modifier.testTag("${tagPrefix}budget_percent_${budgetWithProgress.budget.id}")
            ) {
                Text(
                    text = percentText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$spentText / $limitText",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag("${tagPrefix}budget_progress_bar_${budgetWithProgress.budget.id}"),
            color = statusColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rem = budgetWithProgress.remainingAmount
            val remLabel = if (rem >= 0) {
                "${strings.remaining}: ${
                    CurrencyFormatter.format(rem, currencyCode, preferences.numberFormat, hideAmount = preferences.hideBalances)
                }"
            } else {
                "${strings.overspentBy}: ${
                    CurrencyFormatter.format(-rem, currencyCode, preferences.numberFormat, hideAmount = preferences.hideBalances)
                }"
            }
            Text(
                text = remLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (rem < 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (budgetWithProgress.isOverspent || budgetWithProgress.isReachedEightyPercent) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (budgetWithProgress.isOverspent) strings.limitExceeded else strings.approachingLimit,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor,
                    modifier = Modifier.testTag("${tagPrefix}budget_status_${budgetWithProgress.budget.id}")
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun PassphraseDialog(
    title: String,
    hint: String,
    confirmText: String,
    cancelText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(hint) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("passphrase_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(passphrase) },
                enabled = passphrase.length >= 4,
                modifier = Modifier.testTag("confirm_passphrase_button")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        }
    )
}

@Composable
fun MonthlyCsvExportDialog(
    allTransactions: List<TransactionWithDetails>,
    preferences: UserPreferences,
    initialMonthYear: String = DateTimeUtils.currentMonthYear(),
    allowAllMonthsOption: Boolean = true,
    onDismiss: () -> Unit,
    onExport: (monthYear: String?) -> Unit
) {
    val strings = LocalAppStrings.current
    val langTag = preferences.effectiveLanguageTag
    val currCode = preferences.effectiveCurrencyCode
    var selectedMonthYear by remember { mutableStateOf(initialMonthYear) }
    var exportAllMonths by remember { mutableStateOf(false) }

    val availableMonths = remember(allTransactions) {
        allTransactions
            .groupBy { DateTimeUtils.currentMonthYear(it.transaction.timestamp) }
            .entries
            .sortedByDescending { it.key }
            .take(6)
            .map { it.key to it.value.size }
    }

    val matchingTransactions = remember(allTransactions, selectedMonthYear, exportAllMonths) {
        if (exportAllMonths) {
            allTransactions
        } else {
            val (start, end) = DateTimeUtils.monthYearRange(selectedMonthYear)
            allTransactions.filter { it.transaction.timestamp in start..end }
        }
    }

    val monthIncome = remember(matchingTransactions) {
        matchingTransactions.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
    }
    val monthExpense = remember(matchingTransactions) {
        matchingTransactions.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = strings.exportMonthlyCsv,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(strings.exportMonthlyCsv, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (allowAllMonthsOption) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !exportAllMonths,
                            onClick = { exportAllMonths = false },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("csv_scope_month")
                        ) {
                            Text(strings.selectedMonth, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SegmentedButton(
                            selected = exportAllMonths,
                            onClick = { exportAllMonths = true },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("csv_scope_all")
                        ) {
                            Text(strings.allMonths, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (!exportAllMonths) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedMonthYear = DateTimeUtils.shiftMonthYear(selectedMonthYear, -1) },
                                modifier = Modifier.testTag("csv_month_prev")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = strings.previousMonth)
                            }
                            Text(
                                text = DateTimeUtils.formatMonthLabel(
                                    selectedMonthYear,
                                    langTag,
                                    preferences.numberFormat
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("csv_selected_month_label")
                            )
                            IconButton(
                                onClick = { selectedMonthYear = DateTimeUtils.shiftMonthYear(selectedMonthYear, 1) },
                                modifier = Modifier.testTag("csv_month_next")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = strings.nextMonth)
                            }
                        }
                    }

                    if (availableMonths.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableMonths.forEach { (monthKey, count) ->
                                val label = "${
                                    DateTimeUtils.formatMonthLabel(monthKey, langTag, preferences.numberFormat)
                                } (${CurrencyFormatter.toLocalizedDigits(count.toString(), preferences.numberFormat)})"
                                FilterChip(
                                    selected = selectedMonthYear == monthKey,
                                    onClick = { selectedMonthYear = monthKey },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (matchingTransactions.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (matchingTransactions.isEmpty()) {
                            Text(
                                text = strings.noTransactionsInMonth,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = strings.transactionsCount(matchingTransactions.size, preferences.numberFormat),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${strings.income}: ${
                                        CurrencyFormatter.format(
                                            monthIncome,
                                            currCode,
                                            preferences.numberFormat,
                                            hideAmount = preferences.hideBalances
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${strings.expense}: ${
                                        CurrencyFormatter.format(
                                            monthExpense,
                                            currCode,
                                            preferences.numberFormat,
                                            hideAmount = preferences.hideBalances
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(if (exportAllMonths) null else selectedMonthYear) },
                enabled = matchingTransactions.isNotEmpty(),
                modifier = Modifier.testTag("confirm_export_monthly_csv")
            ) {
                Text(strings.exportCsv)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

