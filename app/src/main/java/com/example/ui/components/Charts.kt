package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.CurrencyFormatter
import com.example.core.common.LocalAppStrings
import com.example.core.model.CategoryBreakdown
import com.example.core.model.ChartPoint
import com.example.core.model.UserPreferences
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun IncomeVsExpenseBarChart(
    points: List<ChartPoint>,
    preferences: UserPreferences,
    incomeLabel: String,
    expenseLabel: String,
    currencyCode: String = preferences.effectiveCurrencyCode,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    var selectedIndex by remember(points) { mutableIntStateOf(-1) }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 220),
        label = "bar_anim"
    )
    val maxValue = remember(points) {
        points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend + Selected Tooltip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(color = IncomeGreen, label = incomeLabel)
                LegendDot(color = ExpenseRed, label = expenseLabel)
            }
            if (selectedIndex in points.indices) {
                val p = points[selectedIndex]
                val inc = CurrencyFormatter.format(p.income, currencyCode, preferences.numberFormat, hideAmount = preferences.hideBalances, includeDecimals = false)
                val exp = CurrencyFormatter.format(p.expense, currencyCode, preferences.numberFormat, hideAmount = preferences.hideBalances, includeDecimals = false)
                Text(
                    text = "${p.label}: +$inc / -$exp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val step = size.width.toFloat() / points.size.coerceAtLeast(1)
                        val idx = (offset.x / step).toInt().coerceIn(0, points.lastIndex)
                        selectedIndex = if (selectedIndex == idx) -1 else idx
                    }
                }
        ) {
            val h = size.height
            val w = size.width
            // Horizontal guide lines
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val groupWidth = w / points.size
            val barWidth = (groupWidth * 0.28f).coerceIn(6.dp.toPx(), 18.dp.toPx())
            val gap = (barWidth * 0.28f).coerceAtLeast(2.dp.toPx())

            points.forEachIndexed { index, point ->
                val centerX = index * groupWidth + groupWidth / 2f
                val isSelected = selectedIndex == index
                val incomeH = ((point.income / maxValue).toFloat() * h * animProgress).coerceAtLeast(if (point.income > 0) 4.dp.toPx() else 0f)
                val expenseH = ((point.expense / maxValue).toFloat() * h * animProgress).coerceAtLeast(if (point.expense > 0) 4.dp.toPx() else 0f)

                if (isSelected) {
                    drawRoundRect(
                        color = gridColor.copy(alpha = 0.25f),
                        topLeft = Offset(index * groupWidth + 2.dp.toPx(), 0f),
                        size = Size(groupWidth - 4.dp.toPx(), h),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }

                if (incomeH > 0f) {
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(centerX - barWidth - gap / 2f, h - incomeH),
                        size = Size(barWidth, incomeH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                if (expenseH > 0f) {
                    drawRoundRect(
                        color = ExpenseRed,
                        topLeft = Offset(centerX + gap / 2f, h - expenseH),
                        size = Size(barWidth, expenseH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val step = if (points.size > 8) 2 else 1
            points.forEachIndexed { idx, point ->
                val label = if (idx % step == 0 || idx == points.lastIndex) point.label else ""
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedIndex == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SpendingTrendLineChart(
    points: List<ChartPoint>,
    preferences: UserPreferences,
    currencyCode: String = preferences.effectiveCurrencyCode,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val maxExpense = remember(points) {
        points.maxOfOrNull { it.expense }?.coerceAtLeast(1.0) ?: 1.0
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIdx in points.indices) {
            val p = points[selectedIdx]
            Text(
                text = "${p.label}: ${
                    CurrencyFormatter.format(p.expense, currencyCode, preferences.numberFormat, hideAmount = preferences.hideBalances)
                }",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size == 1) {
                            selectedIdx = 0
                        } else {
                            val stepX = size.width.toFloat() / (points.size - 1)
                            val idx = ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, points.lastIndex)
                            selectedIdx = if (selectedIdx == idx) -1 else idx
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val padY = 12.dp.toPx()
            val usableH = (h - padY * 2f).coerceAtLeast(1f)

            for (i in 0..2) {
                val y = padY + usableH * (i / 2f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            val coords = points.mapIndexed { idx, pt ->
                val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * w
                val ratio = (pt.expense / maxExpense).toFloat().coerceIn(0f, 1f)
                val y = padY + usableH * (1f - ratio)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 0 until coords.lastIndex) {
                        val p0 = coords[i]
                        val p1 = coords[i + 1]
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, h)
                    lineTo(coords.first().x, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.0f)),
                        startY = 0f,
                        endY = h
                    )
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            coords.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIdx
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = lineColor,
                    radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = pt
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = if (points.size > 8) 2 else 1
            points.forEachIndexed { idx, pt ->
                if (idx % step == 0 || idx == points.lastIndex) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedIdx == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    breakdowns: List<CategoryBreakdown>,
    preferences: UserPreferences,
    totalLabel: String,
    currencyCode: String = preferences.effectiveCurrencyCode,
    modifier: Modifier = Modifier
) {
    if (breakdowns.isEmpty()) return
    val strings = LocalAppStrings.current
    val langTag = preferences.effectiveLanguageTag
    val topItems = remember(breakdowns) { breakdowns.take(6) }
    val totalAmount = remember(breakdowns) { breakdowns.sumOf { it.totalAmount } }
    var selectedIdx by remember(topItems) { mutableIntStateOf(-1) }
    val animSweep by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 220),
        label = "donut_anim"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(topItems) {
                        detectTapGestures { tap ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = tap.x - cx
                            val dy = tap.y - cy
                            val dist = hypot(dx, dy)
                            val minDim = minOf(size.width, size.height).toFloat()
                            if (dist > minDim * 0.22f) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f
                                var acc = 0f
                                val hit = topItems.indexOfFirst { item ->
                                    val sweep = (item.totalAmount / totalAmount.coerceAtLeast(1.0)).toFloat() * 360f
                                    val match = angle in acc..(acc + sweep)
                                    acc += sweep
                                    match
                                }
                                selectedIdx = if (selectedIdx == hit) -1 else hit
                            }
                        }
                    }
            ) {
                val stroke = 20.dp.toPx()
                val inset = stroke / 2f + 4.dp.toPx()
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                var startAngle = -90f
                val gap = if (topItems.size > 1) 2.5f else 0f

                topItems.forEachIndexed { idx, item ->
                    val fraction = (item.totalAmount / totalAmount.coerceAtLeast(1.0)).toFloat()
                    val sweep = (fraction * 360f - gap).coerceAtLeast(2f) * animSweep
                    val color = Color(item.category?.colorHex ?: 0xFF8A9099)
                    val isSelected = idx == selectedIdx
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) stroke + 4.dp.toPx() else stroke,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += fraction * 360f
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val displayAmt = if (selectedIdx in topItems.indices) {
                    topItems[selectedIdx].totalAmount
                } else {
                    totalAmount
                }
                val displayLabel = if (selectedIdx in topItems.indices) {
                    topItems[selectedIdx].category?.localizedName(langTag, strings) ?: totalLabel
                } else {
                    totalLabel
                }
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = CurrencyFormatter.format(
                        displayAmt,
                        currencyCode,
                        preferences.numberFormat,
                        hideAmount = preferences.hideBalances,
                        includeDecimals = false
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topItems.forEachIndexed { idx, item ->
                val color = Color(item.category?.colorHex ?: 0xFF8A9099)
                val name = item.category?.localizedName(langTag, strings) ?: strings.otherCategory
                val pct = CurrencyFormatter.formatPercent((item.percentage * 100f).toDouble(), preferences.numberFormat)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedIdx == idx) color.copy(alpha = 0.12f) else Color.Transparent
                        )
                        .clickable { selectedIdx = if (selectedIdx == idx) -1 else idx }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pct,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class CategoryBarItem(
    val key: String,
    val name: String,
    val amount: Double,
    val percentage: Float,
    val transactionCount: Int,
    val color: Color
)

@Composable
fun CategoryExpenseBarChart(
    breakdowns: List<CategoryBreakdown>,
    preferences: UserPreferences,
    otherLabel: String,
    currencyCode: String = preferences.effectiveCurrencyCode,
    modifier: Modifier = Modifier
) {
    if (breakdowns.isEmpty()) return
    val strings = LocalAppStrings.current
    val langTag = preferences.effectiveLanguageTag
    val bars = remember(breakdowns, langTag, otherLabel, strings) {
        val sorted = breakdowns.sortedByDescending { it.totalAmount }
        if (sorted.size <= 6) {
            sorted.mapIndexed { idx, item ->
                CategoryBarItem(
                    key = item.category?.id?.toString() ?: "cat_$idx",
                    name = item.category?.localizedName(langTag, strings) ?: otherLabel,
                    amount = item.totalAmount,
                    percentage = item.percentage,
                    transactionCount = item.transactionCount,
                    color = Color(item.category?.colorHex ?: 0xFF8A9099)
                )
            }
        } else {
            val head = sorted.take(5).mapIndexed { idx, item ->
                CategoryBarItem(
                    key = item.category?.id?.toString() ?: "cat_$idx",
                    name = item.category?.localizedName(langTag, strings) ?: otherLabel,
                    amount = item.totalAmount,
                    percentage = item.percentage,
                    transactionCount = item.transactionCount,
                    color = Color(item.category?.colorHex ?: 0xFF8A9099)
                )
            }
            val tail = sorted.drop(5)
            head + CategoryBarItem(
                key = "other",
                name = otherLabel,
                amount = tail.sumOf { it.totalAmount },
                percentage = tail.sumOf { it.percentage.toDouble() }.toFloat(),
                transactionCount = tail.sumOf { it.transactionCount },
                color = Color(0xFF8A9099)
            )
        }
    }

    var selectedIdx by remember(bars) { mutableIntStateOf(-1) }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 220),
        label = "category_bar_anim"
    )
    val maxAmount = remember(bars) {
        bars.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIdx in bars.indices) {
            val selected = bars[selectedIdx]
            val formattedAmount = CurrencyFormatter.format(
                selected.amount,
                currencyCode,
                preferences.numberFormat,
                hideAmount = preferences.hideBalances,
                includeDecimals = false
            )
            val pctText = CurrencyFormatter.formatPercent(
                (selected.percentage * 100f).toDouble(),
                preferences.numberFormat
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = selected.color.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("category_bar_selected_callout")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(selected.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selected.name,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${strings.transactionsCount(selected.transactionCount, preferences.numberFormat)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$formattedAmount ($pctText)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = selected.color
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .semantics { contentDescription = strings.spendingByCategory }
                .pointerInput(bars) {
                    detectTapGestures { offset ->
                        val step = size.width.toFloat() / bars.size.coerceAtLeast(1)
                        val idx = (offset.x / step).toInt().coerceIn(0, bars.lastIndex)
                        selectedIdx = if (selectedIdx == idx) -1 else idx
                    }
                }
        ) {
            val h = size.height
            val w = size.width
            val topPad = 8.dp.toPx()
            val usableH = (h - topPad).coerceAtLeast(1f)

            for (i in 0..3) {
                val y = topPad + usableH * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val colWidth = w / bars.size.coerceAtLeast(1)
            val barWidth = (colWidth * 0.48f).coerceIn(14.dp.toPx(), 36.dp.toPx())
            val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

            bars.forEachIndexed { index, bar ->
                val isSelected = selectedIdx == index
                val dimmed = selectedIdx != -1 && !isSelected
                val centerX = index * colWidth + colWidth / 2f
                val ratio = (bar.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                val barHeight = (ratio * usableH * animProgress).coerceAtLeast(
                    if (bar.amount > 0.0) 6.dp.toPx() else 0f
                )

                if (isSelected) {
                    drawRoundRect(
                        color = bar.color.copy(alpha = 0.12f),
                        topLeft = Offset(index * colWidth + 3.dp.toPx(), 0f),
                        size = Size((colWidth - 6.dp.toPx()).coerceAtLeast(4f), h),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }

                if (barHeight > 0f) {
                    val alpha = if (dimmed) 0.45f else 1f
                    val topColor = bar.color.copy(alpha = alpha)
                    val bottomColor = bar.color.copy(alpha = alpha * 0.72f)
                    val topY = h - barHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(topColor, bottomColor),
                            startY = topY,
                            endY = h
                        ),
                        topLeft = Offset(centerX - barWidth / 2f, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = corner
                    )
                    if (isSelected) {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(centerX - barWidth / 2f, topY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            bars.forEachIndexed { index, bar ->
                val isSelected = selectedIdx == index
                val pctText = CurrencyFormatter.toLocalizedDigits(
                    "${kotlin.math.round(bar.percentage * 100f).toInt()}%",
                    preferences.numberFormat
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIdx = if (selectedIdx == index) -1 else index }
                        .padding(horizontal = 2.dp, vertical = 4.dp)
                        .testTag("category_bar_label_${bar.key}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(bar.color)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bar.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = pctText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
