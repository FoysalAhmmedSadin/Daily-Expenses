package com.example.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.Budget
import com.example.core.model.Category
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.Transaction
import com.example.core.model.TransactionType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String,
    val balance: Double,
    val initialBalance: Double,
    val colorHex: Long,
    val iconKey: String,
    val note: String,
    val createdAt: Long
) {
    fun toDomain(): Account = Account(
        id = id,
        name = name,
        type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.CUSTOM),
        balance = balance,
        initialBalance = initialBalance,
        colorHex = colorHex,
        iconKey = iconKey,
        note = note,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            name = account.name,
            type = account.type.name,
            balance = account.balance,
            initialBalance = account.initialBalance,
            colorHex = account.colorHex,
            iconKey = account.iconKey,
            note = account.note,
            createdAt = account.createdAt
        )
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nameEn: String,
    val nameBn: String,
    val type: String,
    val iconKey: String,
    val colorHex: Long,
    val isDefault: Boolean
) {
    fun toDomain(): Category = Category(
        id = id,
        nameEn = nameEn,
        nameBn = nameBn,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        iconKey = iconKey,
        colorHex = colorHex,
        isDefault = isDefault
    )

    companion object {
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            nameEn = category.nameEn,
            nameBn = category.nameBn,
            type = category.type.name,
            iconKey = category.iconKey,
            colorHex = category.colorHex,
            isDefault = category.isDefault
        )
    }
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("accountId"),
        Index("toAccountId"),
        Index("timestamp"),
        Index("nextRecurrenceAt")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val amount: Double,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long?,
    val note: String,
    val timestamp: Long,
    val receiptUri: String?,
    val recurrence: String,
    val nextRecurrenceAt: Long?,
    val parentRecurringId: Long?,
    val currencyCode: String = "BDT"
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        amount = amount,
        categoryId = categoryId,
        accountId = accountId,
        toAccountId = toAccountId,
        note = note,
        timestamp = timestamp,
        receiptUri = receiptUri,
        recurrence = runCatching { RecurrenceFrequency.valueOf(recurrence) }
            .getOrDefault(RecurrenceFrequency.NONE),
        nextRecurrenceAt = nextRecurrenceAt,
        parentRecurringId = parentRecurringId,
        currencyCode = currencyCode.ifBlank { "BDT" }
    )

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity = TransactionEntity(
            id = transaction.id,
            type = transaction.type.name,
            amount = transaction.amount,
            categoryId = transaction.categoryId,
            accountId = transaction.accountId,
            toAccountId = transaction.toAccountId,
            note = transaction.note,
            timestamp = transaction.timestamp,
            receiptUri = transaction.receiptUri,
            recurrence = transaction.recurrence.name,
            nextRecurrenceAt = transaction.nextRecurrenceAt,
            parentRecurringId = transaction.parentRecurringId,
            currencyCode = transaction.currencyCode.ifBlank { "BDT" }
        )
    }
}

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId"),
        Index("monthYear")
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long?,
    val amountLimit: Double,
    val monthYear: String,
    val alertThresholdPercent: Int,
    val createdAt: Long
) {
    fun toDomain(): Budget = Budget(
        id = id,
        categoryId = categoryId,
        amountLimit = amountLimit,
        monthYear = monthYear,
        alertThresholdPercent = alertThresholdPercent,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(budget: Budget): BudgetEntity = BudgetEntity(
            id = budget.id,
            categoryId = budget.categoryId,
            amountLimit = budget.amountLimit,
            monthYear = budget.monthYear,
            alertThresholdPercent = budget.alertThresholdPercent,
            createdAt = budget.createdAt
        )
    }
}
