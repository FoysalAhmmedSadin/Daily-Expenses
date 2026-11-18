package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.model.AccountType
import com.example.core.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "expense_manager.db"

        fun build(context: Context): ExpenseDatabase {
            lateinit var instance: ExpenseDatabase
            instance = Room.databaseBuilder(
                context.applicationContext,
                ExpenseDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*DatabaseMigrations.ALL)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            SeedData.populateDefaults(instance)
                        }
                    }
                })
                .build()
            return instance
        }
    }
}

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'BDT'")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}

object SeedData {
    suspend fun populateDefaults(db: ExpenseDatabase) {
        if (db.categoryDao().getAll().isEmpty()) {
            db.categoryDao().insertAll(defaultCategories())
        }
        if (db.accountDao().getAll().isEmpty()) {
            db.accountDao().insertAll(defaultAccounts())
        }
    }

    fun defaultAccounts(): List<AccountEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            AccountEntity(
                name = "Cash Wallet",
                type = AccountType.CASH.name,
                balance = 0.0,
                initialBalance = 0.0,
                colorHex = 0xFF34C759,
                iconKey = "wallet",
                note = "Everyday cash on hand",
                createdAt = now
            ),
            AccountEntity(
                name = "Bank Account",
                type = AccountType.BANK.name,
                balance = 0.0,
                initialBalance = 0.0,
                colorHex = 0xFF0A84FF,
                iconKey = "bank",
                note = "Primary bank account",
                createdAt = now + 1
            ),
            AccountEntity(
                name = "Mobile Banking",
                type = AccountType.MOBILE_BANKING.name,
                balance = 0.0,
                initialBalance = 0.0,
                colorHex = 0xFFFF2D55,
                iconKey = "smartphone",
                note = "bKash / Nagad / Rocket",
                createdAt = now + 2
            )
        )
    }

    fun defaultCategories(): List<CategoryEntity> = listOf(
        // Expense Categories
        CategoryEntity(nameEn = "Food & Dining", nameBn = "খাবার ও রেস্টুরেন্ট", type = TransactionType.EXPENSE.name, iconKey = "restaurant", colorHex = 0xFFFF9F0A, isDefault = true),
        CategoryEntity(nameEn = "Groceries", nameBn = "বাজার ও মুদিখানা", type = TransactionType.EXPENSE.name, iconKey = "shopping_cart", colorHex = 0xFF34C759, isDefault = true),
        CategoryEntity(nameEn = "Transport", nameBn = "যাতায়াত", type = TransactionType.EXPENSE.name, iconKey = "directions_car", colorHex = 0xFF0A84FF, isDefault = true),
        CategoryEntity(nameEn = "House Rent", nameBn = "বাড়ি ভাড়া", type = TransactionType.EXPENSE.name, iconKey = "home", colorHex = 0xFF5856D6, isDefault = true),
        CategoryEntity(nameEn = "Utilities & Bills", nameBn = "বিদ্যুৎ ও বিল", type = TransactionType.EXPENSE.name, iconKey = "bolt", colorHex = 0xFF30B0C7, isDefault = true),
        CategoryEntity(nameEn = "Mobile & Internet", nameBn = "মোবাইল ও ইন্টারনেট", type = TransactionType.EXPENSE.name, iconKey = "smartphone", colorHex = 0xFFFF2D55, isDefault = true),
        CategoryEntity(nameEn = "Healthcare", nameBn = "চিকিৎসা ও ওষুধ", type = TransactionType.EXPENSE.name, iconKey = "local_hospital", colorHex = 0xFFFF3B30, isDefault = true),
        CategoryEntity(nameEn = "Shopping", nameBn = "কেনাকাটা", type = TransactionType.EXPENSE.name, iconKey = "shopping_bag", colorHex = 0xFFAF52DE, isDefault = true),
        CategoryEntity(nameEn = "Education", nameBn = "শিক্ষা", type = TransactionType.EXPENSE.name, iconKey = "school", colorHex = 0xFF5AC8FA, isDefault = true),
        CategoryEntity(nameEn = "Entertainment", nameBn = "বিনোদন", type = TransactionType.EXPENSE.name, iconKey = "movie", colorHex = 0xFF00C7BE, isDefault = true),
        CategoryEntity(nameEn = "Travel", nameBn = "ভ্রমণ", type = TransactionType.EXPENSE.name, iconKey = "flight", colorHex = 0xFF0A84FF, isDefault = true),
        CategoryEntity(nameEn = "Other Expense", nameBn = "অন্যান্য খরচ", type = TransactionType.EXPENSE.name, iconKey = "category", colorHex = 0xFF8A9099, isDefault = true),

        // Income Categories
        CategoryEntity(nameEn = "Salary", nameBn = "বেতন", type = TransactionType.INCOME.name, iconKey = "work", colorHex = 0xFF34C759, isDefault = true),
        CategoryEntity(nameEn = "Freelance", nameBn = "ফ্রিল্যান্সিং", type = TransactionType.INCOME.name, iconKey = "laptop", colorHex = 0xFF0A84FF, isDefault = true),
        CategoryEntity(nameEn = "Business", nameBn = "ব্যবসা", type = TransactionType.INCOME.name, iconKey = "store", colorHex = 0xFF5856D6, isDefault = true),
        CategoryEntity(nameEn = "Investment", nameBn = "বিনিয়োগ", type = TransactionType.INCOME.name, iconKey = "trending_up", colorHex = 0xFF00C7BE, isDefault = true),
        CategoryEntity(nameEn = "Gift & Bonus", nameBn = "উপহার ও বোনাস", type = TransactionType.INCOME.name, iconKey = "gift", colorHex = 0xFFFF9F0A, isDefault = true),
        CategoryEntity(nameEn = "Other Income", nameBn = "অন্যান্য আয়", type = TransactionType.INCOME.name, iconKey = "payments", colorHex = 0xFF34C759, isDefault = true)
    )
}
