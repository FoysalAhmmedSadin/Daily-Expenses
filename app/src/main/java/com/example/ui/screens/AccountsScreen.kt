package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.CategoryIcons
import com.example.core.common.CurrencyFormatter
import com.example.core.common.LocalAppStrings
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PremiumCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onOpenAccountDetail: (Long) -> Unit,
    onStartTransfer: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val prefs = state.preferences

    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.accounts) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("accounts_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingAccount = null
                            showAccountDialog = true
                        },
                        modifier = Modifier.testTag("add_account_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = strings.addAccount)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PremiumCard {
                        Text(
                            text = strings.totalBalance,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(
                                state.totalBalance,
                                prefs.effectiveCurrencyCode,
                                prefs.numberFormat,
                                hideAmount = prefs.hideBalances
                            ),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                items(state.accounts, key = { it.id }) { account ->
                    val tint = Color(account.colorHex)
                    PremiumCard(
                        onClick = { onOpenAccountDetail(account.id) },
                        modifier = Modifier.testTag("account_card_${account.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                icon = CategoryIcons.forAccountType(account.type),
                                tintColor = tint,
                                size = 48.dp,
                                contentDescription = account.name
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = strings.accountTypeLabel(account.type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CurrencyFormatter.format(
                                    account.balance,
                                    prefs.effectiveCurrencyCode,
                                    prefs.numberFormat,
                                    hideAmount = prefs.hideBalances
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (account.balance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                            )
                        }
                        if (account.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = account.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onStartTransfer(account.id) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.quickTransfer)
                            }
                            OutlinedButton(
                                onClick = {
                                    editingAccount = account
                                    showAccountDialog = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.editAccount, modifier = Modifier.size(16.dp))
                            }
                            if (state.accounts.size > 1) {
                                OutlinedButton(
                                    onClick = { viewModel.deleteAccount(account.id) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = strings.delete,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAccountDialog) {
        AccountEditorDialog(
            initial = editingAccount,
            onDismiss = { showAccountDialog = false },
            onSave = { account ->
                viewModel.saveAccount(account)
                showAccountDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onAddTransactionForAccount: (Long, TransactionType) -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val prefs = state.preferences
    val account = state.accounts.firstOrNull { it.id == accountId }

    val accountTxs = remember(state.transactions, accountId) {
        state.transactions.filter {
            it.transaction.accountId == accountId || it.transaction.toAccountId == accountId
        }
    }

    val totalInflow = remember(accountTxs, accountId) {
        accountTxs.sumOf { item ->
            val tx = item.transaction
            when {
                tx.type == TransactionType.INCOME && tx.accountId == accountId -> tx.amount
                tx.type == TransactionType.TRANSFER && tx.toAccountId == accountId -> tx.amount
                else -> 0.0
            }
        }
    }
    val totalOutflow = remember(accountTxs, accountId) {
        accountTxs.sumOf { item ->
            val tx = item.transaction
            when {
                tx.type == TransactionType.EXPENSE && tx.accountId == accountId -> tx.amount
                tx.type == TransactionType.TRANSFER && tx.accountId == accountId -> tx.amount
                else -> 0.0
            }
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: strings.account) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (account != null) {
                    item {
                        PremiumCard {
                            Text(
                                text = strings.accountTypeLabel(account.type),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.format(
                                    account.balance,
                                    prefs.effectiveCurrencyCode,
                                    prefs.numberFormat,
                                    hideAmount = prefs.hideBalances
                                ),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.income,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(
                                            totalInflow,
                                            prefs.effectiveCurrencyCode,
                                            prefs.numberFormat,
                                            hideAmount = prefs.hideBalances
                                        ),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = IncomeGreen
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.expense,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(
                                            totalOutflow,
                                            prefs.effectiveCurrencyCode,
                                            prefs.numberFormat,
                                            hideAmount = prefs.hideBalances
                                        ),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ExpenseRed
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onAddTransactionForAccount(accountId, TransactionType.EXPENSE) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(strings.expense)
                                }
                                OutlinedButton(
                                    onClick = { onAddTransactionForAccount(accountId, TransactionType.TRANSFER) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(strings.transfer)
                                }
                            }
                        }
                    }
                }

                item {
                    PremiumCard {
                        Text(
                            text = strings.recentTransactions,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (accountTxs.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                title = strings.noTransactionsTitle,
                                subtitle = strings.noTransactionsSubtitle
                            )
                        } else {
                            Column {
                                accountTxs.forEachIndexed { idx, item ->
                                    TransactionRowItem(
                                        item = item,
                                        preferences = prefs,
                                        onClick = { onEditTransaction(item.transaction.id) }
                                    )
                                    if (idx < accountTxs.lastIndex) {
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
}

@Composable
private fun AccountEditorDialog(
    initial: Account?,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: AccountType.CASH) }
    var balanceStr by remember {
        mutableStateOf(
            initial?.initialBalance?.let {
                if (it == 0.0) "" else if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var colorHex by remember { mutableLongStateOf(initial?.colorHex ?: CategoryIcons.paletteColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addAccount else strings.editAccount) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.accountName) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input")
                )
                Text(strings.accountType, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(strings.accountTypeLabel(t)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { v ->
                        val cleaned = v.filter { it.isDigit() || it == '.' || it == '-' }
                        balanceStr = cleaned
                    },
                    label = { Text(strings.openingBalance) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_balance_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteOptional) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIcons.paletteColors.forEach { hex ->
                        val selected = colorHex == hex
                        Surface(
                            shape = CircleShape,
                            color = Color(hex),
                            border = if (selected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else null,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { colorHex = hex }
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opening = balanceStr.toDoubleOrNull() ?: 0.0
                    onSave(
                        Account(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            type = type,
                            balance = initial?.balance ?: opening,
                            initialBalance = opening,
                            colorHex = colorHex,
                            iconKey = CategoryIcons.defaultKeyForAccountType(type),
                            note = note.trim(),
                            createdAt = initial?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_account_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
