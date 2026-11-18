package com.example.core.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.model.Account
import com.example.core.model.IsoCurrencyCatalog
import com.example.core.model.TransactionType
import com.example.core.model.TransactionWithDetails
import com.example.core.model.UserPreferences
import java.io.OutputStream
import java.util.Locale

object ExportManager {

    fun writeCsv(
        outputStream: OutputStream,
        transactions: List<TransactionWithDetails>,
        preferences: UserPreferences
    ): Int {
        val writer = outputStream.bufferedWriter(Charsets.UTF_8)
        // UTF-8 BOM for Excel Bangla compatibility
        writer.write("\uFEFF")
        writer.write("Date,Time,Type,Category,Account,To Account,Amount,Currency,Note,Recurring\n")
        val langTag = preferences.effectiveLanguageTag
        transactions.forEach { item ->
            val tx = item.transaction
            val txCurrency = IsoCurrencyCatalog.resolve(tx.currencyCode)
            val date = DateTimeUtils.formatDate(tx.timestamp, langTag, preferences.numberFormat)
            val time = DateTimeUtils.formatTime(tx.timestamp, preferences.numberFormat, languageTag = langTag)
            val categoryName = item.category?.localizedName(langTag) ?: "-"
            val accountName = item.account?.name ?: "-"
            val toAccountName = item.toAccount?.name ?: "-"
            val decimals = txCurrency.fractionDigits.coerceIn(0, 3)
            val amountStr = String.format(Locale.US, "%.${decimals}f", tx.amount)
            val row = listOf(
                date,
                time,
                tx.type.name,
                categoryName,
                accountName,
                toAccountName,
                amountStr,
                txCurrency.code,
                tx.note,
                tx.recurrence.name
            ).joinToString(",") { csvEscape(it) }
            writer.write(row)
            writer.write("\n")
        }
        writer.flush()
        return transactions.size
    }

    fun writePdf(
        outputStream: OutputStream,
        accounts: List<Account>,
        transactions: List<TransactionWithDetails>,
        preferences: UserPreferences
    ): Int {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#17191C")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A9099")
            textSize = 10f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#17191C")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#17191C")
            textSize = 10f
        }
        val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#34C759")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF3B30")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F7F8")
            style = Paint.Style.FILL
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E3E7EB")
            strokeWidth = 1f
        }

        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas: Canvas = page.canvas
        var y = margin + 12f

        fun newPage() {
            document.finishPage(page)
            pageNum++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            y = margin + 12f
        }

        // Header
        val langTag = preferences.effectiveLanguageTag
        val activeCurrencyCode = preferences.effectiveCurrencyCode
        canvas.drawText("Daily Expenses — Financial Statement", margin, y, titlePaint)
        y += 16f
        val generatedDate = DateTimeUtils.formatDateTime(
            System.currentTimeMillis(),
            langTag,
            preferences.numberFormat
        )
        canvas.drawText("Generated: $generatedDate  |  Currency: $activeCurrencyCode", margin, y, subtitlePaint)
        y += 20f

        // Totals calculation
        val totalBalance = accounts.sumOf { it.balance }
        val totalIncome = transactions.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val totalExpense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
        val netSavings = totalIncome - totalExpense

        // Summary box
        canvas.drawRoundRect(
            RectF(margin, y, pageWidth - margin, y + 66f),
            10f,
            10f,
            cardPaint
        )
        val colWidth = (pageWidth - 2 * margin) / 4f
        val summaryItems = listOf(
            "Total Balance" to CurrencyFormatter.format(totalBalance, activeCurrencyCode, preferences.numberFormat),
            "Total Income" to CurrencyFormatter.format(totalIncome, activeCurrencyCode, preferences.numberFormat),
            "Total Expense" to CurrencyFormatter.format(totalExpense, activeCurrencyCode, preferences.numberFormat),
            "Net Savings" to CurrencyFormatter.format(netSavings, activeCurrencyCode, preferences.numberFormat)
        )
        summaryItems.forEachIndexed { idx, (label, value) ->
            val x = margin + 12f + idx * colWidth
            canvas.drawText(label, x, y + 22f, subtitlePaint)
            canvas.drawText(value, x, y + 44f, if (idx == 2) redPaint else greenPaint)
        }
        y += 86f

        // Accounts section
        canvas.drawText("Accounts Overview", margin, y, sectionPaint)
        y += 16f
        accounts.forEach { acc ->
            if (y > pageHeight - 60f) newPage()
            canvas.drawText("${acc.name} (${acc.type.name})", margin, y, bodyPaint)
            val bal = CurrencyFormatter.format(acc.balance, activeCurrencyCode, preferences.numberFormat)
            val w = bodyPaint.measureText(bal)
            canvas.drawText(bal, pageWidth - margin - w, y, bodyPaint)
            y += 16f
        }
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 20f

        // Transactions table
        canvas.drawText("Transactions (${transactions.size})", margin, y, sectionPaint)
        y += 18f

        fun drawTableHeader() {
            canvas.drawText("Date", margin, y, subtitlePaint)
            canvas.drawText("Category / Details", margin + 88f, y, subtitlePaint)
            canvas.drawText("Account", margin + 275f, y, subtitlePaint)
            val amtHeader = "Amount"
            canvas.drawText(amtHeader, pageWidth - margin - subtitlePaint.measureText(amtHeader), y, subtitlePaint)
            y += 6f
            canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 14f
        }
        drawTableHeader()

        transactions.forEach { item ->
            if (y > pageHeight - 44f) {
                newPage()
                drawTableHeader()
            }
            val tx = item.transaction
            val date = DateTimeUtils.formatShortDate(tx.timestamp, langTag, preferences.numberFormat)
            val label = when (tx.type) {
                TransactionType.TRANSFER -> "Transfer -> ${item.toAccount?.name ?: "-"}"
                else -> item.category?.localizedName(langTag) ?: tx.type.name
            }
            val detail = if (tx.note.isNotBlank()) "$label (${tx.note.take(20)})" else label
            val accName = (item.account?.name ?: "-").take(18)
            val amtStr = CurrencyFormatter.format(
                amount = tx.amount,
                currencyCode = tx.currencyCode,
                numberFormat = preferences.numberFormat,
                showSign = tx.type != TransactionType.TRANSFER
            ).let {
                when (tx.type) {
                    TransactionType.EXPENSE -> "-${it.removePrefix("+")}"
                    TransactionType.INCOME -> if (it.startsWith("+")) it else "+$it"
                    TransactionType.TRANSFER -> it
                }
            }
            val amtPaint = when (tx.type) {
                TransactionType.EXPENSE -> redPaint
                TransactionType.INCOME -> greenPaint
                TransactionType.TRANSFER -> bodyPaint
            }

            canvas.drawText(date, margin, y, bodyPaint)
            canvas.drawText(detail.take(30), margin + 88f, y, bodyPaint)
            canvas.drawText(accName, margin + 275f, y, bodyPaint)
            val amtWidth = amtPaint.measureText(amtStr)
            canvas.drawText(amtStr, pageWidth - margin - amtWidth, y, amtPaint)
            y += 18f
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
        return transactions.size
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
