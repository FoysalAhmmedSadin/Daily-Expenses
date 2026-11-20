package com.example.core.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.core.common.AppStrings
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.EnglishStrings
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
        preferences: UserPreferences,
        strings: AppStrings = EnglishStrings
    ): Int {
        val writer = outputStream.bufferedWriter(Charsets.UTF_8)
        // UTF-8 BOM for Excel Bangla and CJK compatibility
        writer.write("\uFEFF")
        writer.write(strings.csvHeaderRow)
        writer.write("\n")
        val langTag = preferences.effectiveLanguageTag
        transactions.forEach { item ->
            val tx = item.transaction
            val txCurrency = IsoCurrencyCatalog.resolve(tx.currencyCode)
            val date = DateTimeUtils.formatDate(tx.timestamp, langTag, preferences.numberFormat)
            val time = DateTimeUtils.formatTime(tx.timestamp, preferences.numberFormat, languageTag = langTag)
            val typeLabel = strings.transactionTypeLabel(tx.type)
            val categoryName = item.category?.localizedName(langTag, strings) ?: "-"
            val accountName = item.account?.name ?: "-"
            val toAccountName = item.toAccount?.name ?: "-"
            val decimals = txCurrency.fractionDigits.coerceIn(0, 3)
            val amountStr = String.format(Locale.US, "%.${decimals}f", tx.amount)
            val recurrenceLabel = strings.recurrenceLabel(tx.recurrence)
            val row = listOf(
                date,
                time,
                typeLabel,
                categoryName,
                accountName,
                toAccountName,
                amountStr,
                txCurrency.code,
                tx.note,
                recurrenceLabel
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
        preferences: UserPreferences,
        strings: AppStrings = EnglishStrings
    ): Int {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#17191C")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6E747E")
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
            color = Color.parseColor("#248A3D")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D70015")
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
        canvas.drawText(ellipsizeForWidth(strings.pdfStatementTitle, titlePaint, pageWidth - 2 * margin), margin, y, titlePaint)
        y += 16f
        val generatedDate = DateTimeUtils.formatDateTime(
            System.currentTimeMillis(),
            langTag,
            preferences.numberFormat
        )
        canvas.drawText(
            ellipsizeForWidth(strings.pdfGeneratedMeta(generatedDate, activeCurrencyCode), subtitlePaint, pageWidth - 2 * margin),
            margin,
            y,
            subtitlePaint
        )
        y += 20f

        // Totals calculation
        val totalBalance = accounts.sumOf { it.balance }
        var totalIncome = 0.0
        var totalExpense = 0.0
        transactions.forEach { item ->
            when (item.transaction.type) {
                TransactionType.INCOME -> totalIncome += item.transaction.amount
                TransactionType.EXPENSE -> totalExpense += item.transaction.amount
                TransactionType.TRANSFER -> Unit
            }
        }
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
            strings.totalBalance to CurrencyFormatter.format(totalBalance, activeCurrencyCode, preferences.numberFormat, languageTag = langTag),
            strings.totalIncome to CurrencyFormatter.format(totalIncome, activeCurrencyCode, preferences.numberFormat, languageTag = langTag),
            strings.totalExpenses to CurrencyFormatter.format(totalExpense, activeCurrencyCode, preferences.numberFormat, languageTag = langTag),
            strings.netCashFlow to CurrencyFormatter.format(netSavings, activeCurrencyCode, preferences.numberFormat, languageTag = langTag)
        )
        summaryItems.forEachIndexed { idx, (label, value) ->
            val x = margin + 10f + idx * colWidth
            val maxColTextWidth = colWidth - 14f
            canvas.drawText(ellipsizeForWidth(label, subtitlePaint, maxColTextWidth), x, y + 22f, subtitlePaint)
            val valuePaint = if (idx == 2) redPaint else greenPaint
            canvas.drawText(ellipsizeForWidth(value, valuePaint, maxColTextWidth), x, y + 44f, valuePaint)
        }
        y += 86f

        // Accounts section
        canvas.drawText(strings.pdfAccountsOverview, margin, y, sectionPaint)
        y += 16f
        accounts.forEach { acc ->
            if (y > pageHeight - 60f) newPage()
            val bal = CurrencyFormatter.format(acc.balance, activeCurrencyCode, preferences.numberFormat, languageTag = langTag)
            val balWidth = bodyPaint.measureText(bal)
            val accMaxWidth = (pageWidth - 2 * margin - balWidth - 16f).coerceAtLeast(80f)
            val accLabel = "${acc.name} (${strings.accountTypeLabel(acc.type)})"
            canvas.drawText(ellipsizeForWidth(accLabel, bodyPaint, accMaxWidth), margin, y, bodyPaint)
            canvas.drawText(bal, pageWidth - margin - balWidth, y, bodyPaint)
            y += 16f
        }
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 20f

        // Transactions table
        canvas.drawText(strings.pdfTransactionsCount(transactions.size, preferences.numberFormat), margin, y, sectionPaint)
        y += 18f

        fun drawTableHeader() {
            canvas.drawText(ellipsizeForWidth(strings.pdfColDate, subtitlePaint, 82f), margin, y, subtitlePaint)
            canvas.drawText(ellipsizeForWidth(strings.pdfColDetails, subtitlePaint, 180f), margin + 88f, y, subtitlePaint)
            canvas.drawText(ellipsizeForWidth(strings.pdfColAccount, subtitlePaint, 110f), margin + 275f, y, subtitlePaint)
            val amtHeader = strings.pdfColAmount
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
                TransactionType.TRANSFER -> "${strings.transfer} -> ${item.toAccount?.name ?: "-"}"
                else -> item.category?.localizedName(langTag, strings) ?: strings.transactionTypeLabel(tx.type)
            }
            val detail = if (tx.note.isNotBlank()) "$label (${tx.note})" else label
            val accName = item.account?.name ?: "-"
            val amtStr = CurrencyFormatter.format(
                amount = tx.amount,
                currencyCode = tx.currencyCode,
                numberFormat = preferences.numberFormat,
                showSign = tx.type != TransactionType.TRANSFER,
                languageTag = langTag
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

            val amtWidth = amtPaint.measureText(amtStr)
            val maxAccWidth = (pageWidth - margin - amtWidth - 10f - (margin + 275f)).coerceAtLeast(60f)

            canvas.drawText(ellipsizeForWidth(date, bodyPaint, 82f), margin, y, bodyPaint)
            canvas.drawText(ellipsizeForWidth(detail, bodyPaint, 180f), margin + 88f, y, bodyPaint)
            canvas.drawText(ellipsizeForWidth(accName, bodyPaint, maxAccWidth), margin + 275f, y, bodyPaint)
            canvas.drawText(amtStr, pageWidth - margin - amtWidth, y, amtPaint)
            y += 18f
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
        return transactions.size
    }

    private fun ellipsizeForWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (text.isEmpty() || paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellipsisWidth = paint.measureText(ellipsis)
        val avail = (maxWidth - ellipsisWidth).coerceAtLeast(0f)
        val count = paint.breakText(text, true, avail, null)
        return if (count <= 0) ellipsis else text.substring(0, count) + ellipsis
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
