package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.LocalAppStrings
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.TransactionSortOrder
import com.example.core.model.TransactionType
import com.example.core.model.TransactionWithDetails
import com.example.ui.components.AppSwitch
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthlyCsvExportDialog
import com.example.ui.components.PremiumCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val prefs = state.preferences
    val filter = state.filter

    var showFilterSheet by remember { mutableStateOf(false) }
    var inspectingItem by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCsvExportDialog by remember { mutableStateOf(false) }
    var pendingCsvMonthYear by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            val month = pendingCsvMonthYear
            pendingCsvMonthYear = null
            if (uri != null) {
                viewModel.exportCsv(uri, month)
            }
        }
    )

    val activeFiltersCount = listOfNotNull(
        filter.accountId,
        filter.categoryId,
        filter.startTime,
        if (filter.recurringOnly) true else null
    ).size

    val langTag = prefs.effectiveLanguageTag
    val grouped = remember(state.items, langTag, prefs.numberFormat, strings) {
        state.items.groupBy { item ->
            DateTimeUtils.formatSectionDateHeader(
                millis = item.transaction.timestamp,
                languageTag = langTag,
                numberFormat = prefs.numberFormat,
                todayLabel = strings.today,
                yesterdayLabel = strings.yesterday
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.navTransactions,
                    style = MaterialTheme.typography.headlineMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showCsvExportDialog = true },
                        modifier = Modifier.testTag("export_monthly_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = strings.exportMonthlyCsv
                        )
                    }
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.testTag("open_filters_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeFiltersCount > 0) {
                                    Badge { Text(activeFiltersCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = strings.filters
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search bar
            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text(strings.searchTransactions) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = strings.searchTransactions)
                },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = strings.reset)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_transactions_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Type & Recurring chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter.type == null,
                    onClick = { viewModel.updateFilter { it.copy(type = null) } },
                    label = { Text(strings.all) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                TransactionType.entries.forEach { t ->
                    FilterChip(
                        selected = filter.type == t,
                        onClick = {
                            viewModel.updateFilter {
                                it.copy(type = if (it.type == t) null else t)
                            }
                        },
                        label = { Text(strings.transactionTypeLabel(t)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                FilterChip(
                    selected = filter.recurringOnly,
                    onClick = { viewModel.updateFilter { it.copy(recurringOnly = !it.recurringOnly) } },
                    label = { Text(strings.recurringOnly) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = strings.recurringOnly,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (state.items.isEmpty()) {
                PremiumCard(modifier = Modifier.padding(top = 12.dp)) {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = strings.noTransactionsTitle,
                        subtitle = strings.noTransactionsSubtitle,
                        actionLabel = strings.addTransaction,
                        onAction = onAddTransaction
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    grouped.forEach { (header, groupItems) ->
                        item(key = header) {
                            val byCurrency = groupItems.groupBy {
                                it.transaction.currencyCode.ifBlank { prefs.effectiveCurrencyCode }
                            }
                            val singleNetDay = groupItems.sumOf {
                                when (it.transaction.type) {
                                    TransactionType.INCOME -> it.transaction.amount
                                    TransactionType.EXPENSE -> -it.transaction.amount
                                    TransactionType.TRANSFER -> 0.0
                                }
                            }
                            val netText = if (byCurrency.size <= 1) {
                                val code = byCurrency.keys.firstOrNull() ?: prefs.effectiveCurrencyCode
                                CurrencyFormatter.format(
                                    singleNetDay,
                                    code,
                                    prefs.numberFormat,
                                    hideAmount = prefs.hideBalances,
                                    showSign = true
                                )
                            } else {
                                byCurrency.entries.joinToString(" • ") { (code, list) ->
                                    val netCur = list.sumOf {
                                        when (it.transaction.type) {
                                            TransactionType.INCOME -> it.transaction.amount
                                            TransactionType.EXPENSE -> -it.transaction.amount
                                            TransactionType.TRANSFER -> 0.0
                                        }
                                    }
                                    CurrencyFormatter.format(
                                        netCur,
                                        code,
                                        prefs.numberFormat,
                                        hideAmount = prefs.hideBalances,
                                        showSign = true
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = header,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = netText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (byCurrency.size > 1) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else if (singleNetDay >= 0) {
                                            IncomeGreen
                                        } else {
                                            ExpenseRed
                                        }
                                    )
                                }
                                PremiumCard(contentPadding = 8.dp) {
                                    groupItems.forEachIndexed { idx, item ->
                                        TransactionRowItem(
                                            item = item,
                                            preferences = prefs,
                                            onClick = { inspectingItem = item },
                                            modifier = Modifier.testTag("tx_row_${item.transaction.id}")
                                        )
                                        if (idx < groupItems.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
        )
    }

    // Transaction Detail Modal
    inspectingItem?.let { detail ->
        val tx = detail.transaction
        val txCurrency = tx.currencyCode.ifBlank { prefs.effectiveCurrencyCode }
        AlertDialog(
            onDismissRequest = { inspectingItem = null },
            title = { Text(strings.transactionDetails) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = CurrencyFormatter.format(
                            tx.amount,
                            txCurrency,
                            prefs.numberFormat,
                            hideAmount = prefs.hideBalances
                        ),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = when (tx.type) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary
                        }
                    )
                    DetailLine(strings.account, detail.account?.name ?: "-")
                    if (tx.type == TransactionType.TRANSFER) {
                        DetailLine(strings.toAccount, detail.toAccount?.name ?: "-")
                    } else {
                        DetailLine(
                            strings.category,
                            detail.category?.localizedName(langTag, strings) ?: "-"
                        )
                    }
                    DetailLine(
                        strings.dateTime,
                        DateTimeUtils.formatDateTime(tx.timestamp, langTag, prefs.numberFormat)
                    )
                    if (tx.recurrence != RecurrenceFrequency.NONE) {
                        DetailLine(
                            strings.recurring,
                            strings.recurrenceLabel(tx.recurrence)
                        )
                        tx.nextRecurrenceAt?.let { nextAt ->
                            DetailLine(
                                strings.nextScheduled,
                                DateTimeUtils.formatDateTime(nextAt, langTag, prefs.numberFormat)
                            )
                        }
                    }
                    if (tx.note.isNotBlank()) {
                        DetailLine(strings.noteOptional, tx.note)
                    }
                    if (!tx.receiptUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AsyncImage(
                            model = tx.receiptUri,
                            contentDescription = strings.receiptAttachment,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = tx.id
                        inspectingItem = null
                        onEditTransaction(id)
                    },
                    modifier = Modifier.testTag("edit_tx_detail_button")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.edit)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        inspectingItem = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed),
                    modifier = Modifier.testTag("delete_tx_detail_button")
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.delete)
                }
            }
        )
    }

    // Advanced Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.filters, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        viewModel.resetFilters()
                        showFilterSheet = false
                    }) {
                        Text(strings.reset)
                    }
                }

                // Account filter
                Text(strings.account, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.accounts.forEach { acc ->
                        FilterChip(
                            selected = filter.accountId == acc.id,
                            onClick = {
                                viewModel.updateFilter {
                                    it.copy(accountId = if (it.accountId == acc.id) null else acc.id)
                                }
                            },
                            label = { Text(acc.name) }
                        )
                    }
                }

                // Category filter
                Text(strings.category, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.categories.take(10).forEach { cat ->
                        FilterChip(
                            selected = filter.categoryId == cat.id,
                            onClick = {
                                viewModel.updateFilter {
                                    it.copy(categoryId = if (it.categoryId == cat.id) null else cat.id)
                                }
                            },
                            label = { Text(cat.localizedName(langTag, strings)) }
                        )
                    }
                }

                // Date Range
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = if (filter.startTime != null && filter.endTime != null) {
                        "${DateTimeUtils.formatShortDate(filter.startTime, langTag, prefs.numberFormat)} - ${DateTimeUtils.formatShortDate(filter.endTime, langTag, prefs.numberFormat)}"
                    } else {
                        strings.customRange
                    }
                    Text(label)
                }

                // Recurring toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.recurringOnly, style = MaterialTheme.typography.bodyMedium)
                    AppSwitch(
                        checked = filter.recurringOnly,
                        onCheckedChange = { checked ->
                            viewModel.updateFilter { it.copy(recurringOnly = checked) }
                        }
                    )
                }

                // Sort order
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionSortOrder.entries.forEach { sort ->
                        FilterChip(
                            selected = filter.sortOrder == sort,
                            onClick = { viewModel.updateFilter { it.copy(sortOrder = sort) } },
                            label = { Text(strings.sortOrderLabel(sort)) }
                        )
                    }
                }

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(strings.apply)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = filter.startTime,
            initialSelectedEndDateMillis = filter.endTime
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val s = pickerState.selectedStartDateMillis
                    val e = pickerState.selectedEndDateMillis ?: s
                    viewModel.updateFilter { it.copy(startTime = s, endTime = e) }
                    showDatePicker = false
                }) { Text(strings.apply) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            }
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(420.dp))
        }
    }

    if (showCsvExportDialog) {
        val defaultMonth = filter.startTime?.let { DateTimeUtils.currentMonthYear(it) }
            ?: DateTimeUtils.currentMonthYear()
        MonthlyCsvExportDialog(
            allTransactions = state.allTransactions,
            preferences = prefs,
            initialMonthYear = defaultMonth,
            allowAllMonthsOption = true,
            onDismiss = { showCsvExportDialog = false },
            onExport = { monthYear ->
                showCsvExportDialog = false
                pendingCsvMonthYear = monthYear
                val fileName = if (monthYear != null) {
                    "daily_expenses_$monthYear.csv"
                } else {
                    "daily_expenses_transactions.csv"
                }
                csvLauncher.launch(fileName)
            }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
