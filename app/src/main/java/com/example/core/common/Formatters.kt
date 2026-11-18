package com.example.core.common

import android.content.Context
import android.text.format.DateFormat
import com.example.core.model.AppCurrency
import com.example.core.model.AppLanguage
import com.example.core.model.CurrencyInfo
import com.example.core.model.DateFilterPeriod
import com.example.core.model.IsoCurrencyCatalog
import com.example.core.model.NumberFormatStyle
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.SupportedLanguageCatalog
import com.example.core.model.ViewMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

object CurrencyFormatter {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toLocalizedDigits(input: String, style: NumberFormatStyle): String {
        if (style == NumberFormatStyle.ENGLISH) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun format(
        amount: Double,
        currency: AppCurrency,
        numberFormat: NumberFormatStyle,
        hideAmount: Boolean = false,
        showSign: Boolean = false,
        includeDecimals: Boolean = true
    ): String = format(
        amount = amount,
        currencyInfo = IsoCurrencyCatalog.fromAppCurrency(currency),
        numberFormat = numberFormat,
        hideAmount = hideAmount,
        showSign = showSign,
        includeDecimals = includeDecimals
    )

    fun format(
        amount: Double,
        currencyCode: String,
        numberFormat: NumberFormatStyle,
        hideAmount: Boolean = false,
        showSign: Boolean = false,
        includeDecimals: Boolean = true
    ): String = format(
        amount = amount,
        currencyInfo = IsoCurrencyCatalog.resolve(currencyCode),
        numberFormat = numberFormat,
        hideAmount = hideAmount,
        showSign = showSign,
        includeDecimals = includeDecimals
    )

    fun format(
        amount: Double,
        currencyInfo: CurrencyInfo,
        numberFormat: NumberFormatStyle,
        hideAmount: Boolean = false,
        showSign: Boolean = false,
        includeDecimals: Boolean = true
    ): String {
        if (hideAmount) return "${currencyInfo.symbol} ••••••"

        val negative = amount < 0
        val absValue = abs(amount)
        val fractionDigits = if (includeDecimals) currencyInfo.fractionDigits.coerceIn(0, 3) else 0

        val formattedNumber = if (fractionDigits > 0) {
            val scale = 10.0.pow(fractionDigits.toDouble()).roundToLong()
            val roundedMinor = (absValue * scale).roundToLong()
            val whole = roundedMinor / scale
            val frac = (roundedMinor % scale).toInt()
            val groupedWhole = formatGroupedInteger(whole, currencyInfo.usesSouthAsianGrouping)
            "$groupedWhole.${frac.toString().padStart(fractionDigits, '0')}"
        } else {
            val whole = absValue.roundToLong()
            formatGroupedInteger(whole, currencyInfo.usesSouthAsianGrouping)
        }

        val signPrefix = when {
            negative -> "-"
            showSign && amount > 0.0 -> "+"
            else -> ""
        }
        val raw = "$signPrefix${currencyInfo.symbol}$formattedNumber"
        return toLocalizedDigits(raw, numberFormat)
    }

    fun formatPercent(
        value: Double,
        numberFormat: NumberFormatStyle,
        showSign: Boolean = false
    ): String {
        val sign = if (showSign && value > 0) "+" else ""
        val text = String.format(Locale.US, "%s%.1f%%", sign, value)
        return toLocalizedDigits(text, numberFormat)
    }

    private fun formatGroupedInteger(value: Long, southAsian: Boolean): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        return if (southAsian) {
            val lastThree = digits.takeLast(3)
            val rest = digits.dropLast(3)
            val pairs = rest.reversed().chunked(2).joinToString(",").reversed()
            "$pairs,$lastThree"
        } else {
            digits.reversed().chunked(3).joinToString(",").reversed()
        }
    }
}

object DateTimeUtils {

    private val banglaMonthsShort = arrayOf(
        "জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
    )

    fun currentMonthYear(timeMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    fun previousMonthYear(monthYear: String): String = shiftMonthYear(monthYear, -1)

    fun shiftMonthYear(monthYear: String, delta: Int): String {
        val parts = monthYear.split("-")
        if (parts.size != 2) return currentMonthYear()
        var year = parts[0].toIntOrNull() ?: return currentMonthYear()
        var month = parts[1].toIntOrNull() ?: return currentMonthYear()
        month += delta
        while (month > 12) {
            month -= 12
            year += 1
        }
        while (month < 1) {
            month += 12
            year -= 1
        }
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    fun shiftAnchorForViewMode(anchorMillis: Long, viewMode: ViewMode, delta: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = anchorMillis }
        when (viewMode) {
            ViewMode.DAILY -> cal.add(Calendar.DAY_OF_YEAR, delta)
            ViewMode.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, delta)
            ViewMode.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, delta)
            }
            ViewMode.YEARLY -> cal.add(Calendar.YEAR, delta)
        }
        return cal.timeInMillis
    }

    fun rangeForViewMode(viewMode: ViewMode, anchorMillis: Long): Pair<Long, Long> {
        return when (viewMode) {
            ViewMode.DAILY -> startOfDay(anchorMillis) to endOfDay(anchorMillis)
            ViewMode.WEEKLY -> rangeForPeriod(DateFilterPeriod.THIS_WEEK, now = anchorMillis)
            ViewMode.MONTHLY -> monthYearRange(currentMonthYear(anchorMillis))
            ViewMode.YEARLY -> rangeForPeriod(DateFilterPeriod.THIS_YEAR, now = anchorMillis)
        }
    }

    fun monthYearRange(monthYear: String): Pair<Long, Long> {
        val parts = monthYear.split("-")
        val cal = Calendar.getInstance()
        val year = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    fun rangeForPeriod(
        period: DateFilterPeriod,
        customStart: Long? = null,
        customEnd: Long? = null,
        now: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return when (period) {
            DateFilterPeriod.TODAY -> startOfDay(now) to endOfDay(now)
            DateFilterPeriod.THIS_WEEK -> {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = now
                    firstDayOfWeek = Calendar.SATURDAY
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    if (timeInMillis > now) add(Calendar.WEEK_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = (startCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                startCal.timeInMillis to endCal.timeInMillis
            }
            DateFilterPeriod.THIS_MONTH -> {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = (startCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                startCal.timeInMillis to endCal.timeInMillis
            }
            DateFilterPeriod.THIS_YEAR -> {
                val year = cal.get(Calendar.YEAR)
                val startCal = Calendar.getInstance().apply {
                    set(year, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                startCal.timeInMillis to endCal.timeInMillis
            }
            DateFilterPeriod.CUSTOM -> {
                val s = customStart?.let { startOfDay(it) } ?: startOfDay(now)
                val e = customEnd?.let { endOfDay(it) } ?: endOfDay(now)
                minOf(s, e) to maxOf(s, e)
            }
        }
    }

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun formatViewModePeriodLabel(
        viewMode: ViewMode,
        anchorMillis: Long,
        languageTag: String,
        numberFormat: NumberFormatStyle
    ): String {
        return when (viewMode) {
            ViewMode.DAILY -> formatDate(anchorMillis, languageTag, numberFormat)
            ViewMode.WEEKLY -> {
                val (s, e) = rangeForViewMode(ViewMode.WEEKLY, anchorMillis)
                "${formatShortDate(s, languageTag, numberFormat)} – ${formatDate(e, languageTag, numberFormat)}"
            }
            ViewMode.MONTHLY -> {
                formatMonthLabel(currentMonthYear(anchorMillis), languageTag, numberFormat)
            }
            ViewMode.YEARLY -> {
                val cal = Calendar.getInstance().apply { timeInMillis = anchorMillis }
                CurrencyFormatter.toLocalizedDigits(cal.get(Calendar.YEAR).toString(), numberFormat)
            }
        }
    }

    fun formatDate(
        millis: Long,
        language: AppLanguage,
        numberFormat: NumberFormatStyle
    ): String = formatDate(millis, language.code, numberFormat)

    fun formatDate(
        millis: Long,
        languageTag: String,
        numberFormat: NumberFormatStyle
    ): String {
        val norm = SupportedLanguageCatalog.normalizeTag(languageTag)
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val text = if (norm == "bn") {
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = banglaMonthsShort[cal.get(Calendar.MONTH)]
            val year = cal.get(Calendar.YEAR)
            "$day $month $year"
        } else {
            val locale = SupportedLanguageCatalog.localeForTag(norm)
            SimpleDateFormat("dd MMM yyyy", locale).format(Date(millis))
        }
        return CurrencyFormatter.toLocalizedDigits(text, numberFormat)
    }

    fun formatShortDate(
        millis: Long,
        language: AppLanguage,
        numberFormat: NumberFormatStyle
    ): String = formatShortDate(millis, language.code, numberFormat)

    fun formatShortDate(
        millis: Long,
        languageTag: String,
        numberFormat: NumberFormatStyle
    ): String {
        val norm = SupportedLanguageCatalog.normalizeTag(languageTag)
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val text = if (norm == "bn") {
            "${cal.get(Calendar.DAY_OF_MONTH)} ${banglaMonthsShort[cal.get(Calendar.MONTH)]}"
        } else {
            val locale = SupportedLanguageCatalog.localeForTag(norm)
            SimpleDateFormat("dd MMM", locale).format(Date(millis))
        }
        return CurrencyFormatter.toLocalizedDigits(text, numberFormat)
    }

    fun formatTime(
        millis: Long,
        numberFormat: NumberFormatStyle,
        is24Hour: Boolean = false,
        languageTag: String = "en"
    ): String {
        val locale = SupportedLanguageCatalog.localeForTag(languageTag)
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        val text = SimpleDateFormat(pattern, locale).format(Date(millis))
        return CurrencyFormatter.toLocalizedDigits(text, numberFormat)
    }

    fun formatReminderTime(
        hour: Int,
        minute: Int,
        context: Context,
        languageTag: String,
        numberFormat: NumberFormatStyle
    ): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val is24Hour = DateFormat.is24HourFormat(context)
        return formatTime(cal.timeInMillis, numberFormat, is24Hour = is24Hour, languageTag = languageTag)
    }

    fun formatDateTime(
        millis: Long,
        language: AppLanguage,
        numberFormat: NumberFormatStyle
    ): String = formatDateTime(millis, language.code, numberFormat)

    fun formatDateTime(
        millis: Long,
        languageTag: String,
        numberFormat: NumberFormatStyle
    ): String {
        return "${formatDate(millis, languageTag, numberFormat)} • ${formatTime(millis, numberFormat, languageTag = languageTag)}"
    }

    fun formatSectionDateHeader(
        millis: Long,
        language: AppLanguage,
        numberFormat: NumberFormatStyle,
        todayLabel: String,
        yesterdayLabel: String
    ): String = formatSectionDateHeader(millis, language.code, numberFormat, todayLabel, yesterdayLabel)

    fun formatSectionDateHeader(
        millis: Long,
        languageTag: String,
        numberFormat: NumberFormatStyle,
        todayLabel: String,
        yesterdayLabel: String
    ): String {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)
        val yesterdayStart = startOfDay(now - 86_400_000L)
        val itemDayStart = startOfDay(millis)
        return when (itemDayStart) {
            todayStart -> todayLabel
            yesterdayStart -> yesterdayLabel
            else -> formatDate(millis, languageTag, numberFormat)
        }
    }

    fun nextRecurrenceTimestamp(current: Long, frequency: RecurrenceFrequency): Long? {
        if (frequency == RecurrenceFrequency.NONE) return null
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        when (frequency) {
            RecurrenceFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
            RecurrenceFrequency.NONE -> return null
        }
        return cal.timeInMillis
    }

    fun formatMonthLabel(
        monthYear: String,
        language: AppLanguage,
        numberFormat: NumberFormatStyle
    ): String = formatMonthLabel(monthYear, language.code, numberFormat)

    fun formatMonthLabel(
        monthYear: String,
        languageTag: String,
        numberFormat: NumberFormatStyle
    ): String {
        val parts = monthYear.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthYear
        val monthIdx = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val cal = Calendar.getInstance().apply { set(year, monthIdx, 1) }
        val norm = SupportedLanguageCatalog.normalizeTag(languageTag)
        val text = if (norm == "bn") {
            "${banglaMonthsShort[monthIdx.coerceIn(0, 11)]} $year"
        } else {
            val locale = SupportedLanguageCatalog.localeForTag(norm)
            SimpleDateFormat("MMMM yyyy", locale).format(cal.time)
        }
        return CurrencyFormatter.toLocalizedDigits(text, numberFormat)
    }

    fun formatShortMonthLabel(
        monthYear: String,
        languageTag: String,
        numberFormat: NumberFormatStyle
    ): String {
        val parts = monthYear.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthYear
        val monthIdx = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val cal = Calendar.getInstance().apply { set(year, monthIdx, 1) }
        val norm = SupportedLanguageCatalog.normalizeTag(languageTag)
        val text = if (norm == "bn") {
            banglaMonthsShort[monthIdx.coerceIn(0, 11)]
        } else {
            val locale = SupportedLanguageCatalog.localeForTag(norm)
            SimpleDateFormat("MMM", locale).format(cal.time)
        }
        return CurrencyFormatter.toLocalizedDigits(text, numberFormat)
    }
}
