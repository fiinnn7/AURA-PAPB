package com.aura.app.ui.screens.analisis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.core.format.RupiahFormatter
import com.aura.app.data.remote.dto.TitikHarianDto
import com.aura.app.ui.analisis.AnalisisPeriodeUi
import com.aura.app.ui.theme.Green
import com.aura.app.ui.theme.Ink
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Purple
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft
import java.util.Locale

internal val ChartPalette = listOf(
    Purple,
    Red,
    Color(0xFF2563EB),
    Color(0xFF0F766E),
    Color(0xFFD97706),
    Color(0xFF9CA3AF),
)

internal val AmberColor = Color(0xFFD97706)
internal val DangerColor = Color(0xFFB91C1C)
internal val BorderCardColor = Color(0xFFEFE7E5)

internal object AnalisisFmt {
    fun rupiah(amount: Long): String = RupiahFormatter.format(amount)

    fun pct(value: Double?): String {
        if (value == null) return "-"
        return String.format(Locale("id", "ID"), "%.1f%%", value)
    }

    fun waktuSingkat(iso: String): String {
        if (iso.length >= 16 && iso.contains("T")) {
            val hhmm = iso.substring(11, 16).replace(":", ".")
            return "Diperbarui $hhmm WIB"
        }
        return ""
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Muted,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
    )
}

@Composable
internal fun PeriodSegmentedBar(
    selected: AnalisisPeriodeUi,
    onSelect: (AnalisisPeriodeUi) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, BorderCardColor, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnalisisPeriodeUi.entries.forEach { p ->
            val isSelected = p == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Purple else Color.Transparent)
                    .clickable { onSelect(p) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = p.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else Ink,
                )
            }
        }
    }
}

@Composable
internal fun ChangeBadge(changePct: Double?) {
    if (changePct == null) {
        Text(
            text = "Baru",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Muted,
        )
        return
    }
    val isUp = changePct >= 0.0
    val prefix = if (isUp) "▲" else "▼"
    val color = if (isUp) Green else DangerColor
    val absVal = kotlin.math.abs(changePct)
    Text(
        text = "$prefix ${AnalisisFmt.pct(absVal)}",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
internal fun ClickableAnalysisCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderCardColor),
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
internal fun FinancialLineChart(
    points: List<TitikHarianDto>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val maxVal = points.maxOf { maxOf(it.omzet, it.labaKotor) }.coerceAtLeast(1L).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(color = Purple, label = "Omzet")
            LegendDot(color = Red, label = "Laba Kotor")
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
        ) {
            val w = size.width
            val h = size.height
            val padY = 8.dp.toPx()
            val usableH = (h - padY * 2).coerceAtLeast(1f)

            // Grid lines
            for (i in 0..3) {
                val y = padY + usableH * (i / 3f)
                drawLine(
                    color = Color(0xFFEFE7E5),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            fun buildPath(selector: (TitikHarianDto) -> Long): Path {
                val path = Path()
                val n = points.size
                points.forEachIndexed { idx, pt ->
                    val x = if (n == 1) w / 2f else w * idx / (n - 1).toFloat()
                    val ratio = (selector(pt).coerceAtLeast(0L) / maxVal).coerceIn(0f, 1f)
                    val y = padY + usableH * (1f - ratio)
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                return path
            }

            drawPath(
                path = buildPath { it.omzet },
                color = Purple,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = buildPath { it.labaKotor },
                color = Red,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(points.first().tanggal.takeLast(5), fontSize = 10.sp, color = Muted)
            Text(points.last().tanggal.takeLast(5), fontSize = 10.sp, color = Muted)
        }
    }
}

@Composable
internal fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = Muted)
    }
}

data class DonutSlice(val label: String, val pct: Double, val color: Color)

@Composable
internal fun ProportionDonutChart(
    slices: List<DonutSlice>,
    centerTitle: String,
    centerSubtitle: String,
    diameter: Dp = 150.dp,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(diameter)) {
                val strokeW = 22.dp.toPx()
                val inset = strokeW / 2f
                val arcSize = Size(size.width - strokeW, size.height - strokeW)
                if (slices.isEmpty() || slices.sumOf { it.pct } <= 0.0) {
                    drawArc(
                        color = Color(0xFFE5E7EB),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeW),
                    )
                } else {
                    var startAngle = -90f
                    slices.forEach { s ->
                        val sweep = ((s.pct / 100.0) * 360.0).toFloat().coerceAtLeast(1f)
                        drawArc(
                            color = s.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                        )
                        startAngle += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = centerSubtitle,
                    fontSize = 10.sp,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
internal fun StrProgressBar(strPct: Double?) {
    if (strPct == null) {
        Text("STR: Tidak dapat dihitung", fontSize = 11.sp, color = Muted)
        return
    }
    val color = when {
        strPct >= 70.0 -> Green
        strPct >= 30.0 -> AmberColor
        else -> DangerColor
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("STR", fontSize = 11.sp, color = Muted, modifier = Modifier.width(26.dp))
        LinearProgressIndicator(
            progress = { (strPct / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Soft,
        )
        Text(
            text = AnalisisFmt.pct(strPct),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
internal fun TrendSparkline(
    values: List<Int>,
    isUp: Boolean,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    val lineColor = if (isUp) Green else DangerColor
    val maxV = values.maxOrNull()?.coerceAtLeast(1)?.toFloat() ?: 1f

    Canvas(modifier = modifier.height(30.dp).fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val pad = 3.dp.toPx()
        val usable = (h - pad * 2).coerceAtLeast(1f)
        val path = Path()
        val n = values.size
        values.forEachIndexed { idx, v ->
            val x = if (n == 1) w / 2f else w * idx / (n - 1).toFloat()
            val y = pad + usable * (1f - (v / maxV).coerceIn(0f, 1f))
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
internal fun StatusChip(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}
