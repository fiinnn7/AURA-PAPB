package com.aura.app.ui.screens.analisis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.data.remote.dto.KorelasiDetailResponseDto
import com.aura.app.data.remote.dto.PergerakanDetailResponseDto
import com.aura.app.data.remote.dto.ProporsiDetailResponseDto
import com.aura.app.data.remote.dto.TrenDetailResponseDto
import com.aura.app.data.remote.dto.UmurStokDetailResponseDto
import com.aura.app.ui.analisis.AnalisisDataState
import com.aura.app.ui.analisis.AnalisisPeriodeUi
import com.aura.app.ui.analisis.AnalisisUiState
import com.aura.app.ui.components.AuraRedButton
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.theme.Green
import com.aura.app.ui.theme.Ink
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Purple
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft

// =============================================================================
// 1. MOVEMENT LIST SCREEN (FAST / SLOW MOVING + STR)
// =============================================================================
@Composable
internal fun MovementListScreen(
    jenis: String,
    uiState: AnalisisUiState,
    onSelectPeriode: (AnalisisPeriodeUi) -> Unit,
    onSortChange: (String) -> Unit,
    onLoad: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    LaunchedEffect(jenis, uiState.periode) { onLoad() }

    Page {
        Heading(
            title = if (jenis == "fast") "Fast Moving" else "Slow Moving",
            subtitle = "STR = terjual ÷ (terjual + stok tersisa)",
        )
        PeriodSegmentedBar(selected = uiState.periode, onSelect = onSelectPeriode)
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("kecepatan" to "Kecepatan", "str" to "STR", "terjual" to "Terjual").forEach { (key, label) ->
                val active = uiState.movementSort == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Purple else Color.White)
                        .clickable { onSortChange(key) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else Ink)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        DetailStateContainer(uiState.movement, onRetry = onLoad) { data: PergerakanDetailResponseDto ->
            if (jenis == "slow" && data.totalModalSlow > 0L) {
                Panel {
                    Text("Total Modal Tertahan di Slow Moving", fontSize = 11.sp, color = Muted)
                    Text(AnalisisFmt.rupiah(data.totalModalSlow), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AmberColor)
                }
            }

            if (data.status == "insufficient_data") {
                Panel { Text("Butuh minimal 5 barang aktif berusia ≥ 14 hari.", fontSize = 12.sp, color = Muted) }
            } else if (data.items.isEmpty()) {
                Panel { Text("Belum ada produk pada kategori ini untuk periode ini.", fontSize = 12.sp, color = Muted) }
            } else {
                val sorted = when (uiState.movementSort) {
                    "str" -> data.items.sortedByDescending { it.strPct ?: -1.0 }
                    "terjual" -> data.items.sortedByDescending { it.qtyTerjual }
                    else -> data.items
                }
                sorted.forEach { item ->
                    ClickableAnalysisCard(onClick = { onOpenProduct(item.barangId) }, modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(item.nama, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            if (item.tidakLaku) {
                                StatusChip("Tidak laku", Soft, Muted)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Terjual ${item.qtyTerjual} ${item.satuan} · ${item.kecepatan}/hari · Stok: ${item.stok}",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                        Spacer(Modifier.height(6.dp))
                        StrProgressBar(item.strPct)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val runwayText = item.umurStokHari?.let { "Umur stok: ±$it hari" } ?: "Umur stok: Tidak dapat dihitung"
                            Text(runwayText, fontSize = 11.sp, color = Muted)
                            if (jenis == "slow") {
                                Text("Modal: ${AnalisisFmt.rupiah(item.modalTertahan)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 2. STOCK RUNWAY SCREEN
// =============================================================================
@Composable
internal fun StockRunwayScreen(
    uiState: AnalisisUiState,
    onSelectPeriode: (AnalisisPeriodeUi) -> Unit,
    onLoad: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    LaunchedEffect(uiState.periode) { onLoad() }

    Page {
        Heading("Estimasi Umur Stok", "Perkiraan sisa hari sebelum stok habis")
        PeriodSegmentedBar(selected = uiState.periode, onSelect = onSelectPeriode)
        Spacer(Modifier.height(10.dp))

        DetailStateContainer(uiState.runway, onRetry = onLoad) { data: UmurStokDetailResponseDto ->
            if (data.items.isEmpty()) {
                Panel { Text("Belum ada produk aktif.", fontSize = 12.sp, color = Muted) }
            } else {
                data.items.forEach { item ->
                    val (badgeLabel, badgeFg, badgeBg) = when (item.kategori) {
                        "kritis" -> Triple("Kritis", DangerColor, Color(0xFFFEE2E2))
                        "perhatian" -> Triple("Perlu diperhatikan", AmberColor, Color(0xFFFEF3C7))
                        "aman" -> Triple("Aman", Green, Color(0xFFDCFCE7))
                        else -> Triple("Tidak diketahui", Muted, Soft)
                    }
                    ClickableAnalysisCard(onClick = { onOpenProduct(item.barangId) }, modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(item.nama, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            StatusChip(badgeLabel, badgeBg, badgeFg)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Stok: ${item.stok} ${item.satuan} · Laju jual: ${item.rataHarian}/hari", fontSize = 12.sp, color = Muted)
                        Text(
                            text = item.umurStokHari?.let { "Perkiraan habis dalam ±$it hari" } ?: "Tidak dapat dihitung (belum ada penjualan)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.kategori == "kritis") DangerColor else Ink,
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. PROPORTION LIST SCREEN (PROFIT / EXPENSE)
// =============================================================================
@Composable
internal fun ProportionListScreen(
    jenis: String,
    uiState: AnalisisUiState,
    onSelectPeriode: (AnalisisPeriodeUi) -> Unit,
    onLoad: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    LaunchedEffect(jenis, uiState.periode) { onLoad() }

    Page {
        Heading(
            title = if (jenis == "profit") "Kontribusi ke Profit" else "Kontribusi ke Pengeluaran",
            subtitle = if (jenis == "profit") "Berdasarkan Laba Kotor & kelas ABC Pareto" else "Berdasarkan total biaya restock pada periode ini",
        )
        PeriodSegmentedBar(selected = uiState.periode, onSelect = onSelectPeriode)
        Spacer(Modifier.height(10.dp))

        DetailStateContainer(uiState.proportion, onRetry = onLoad) { data: ProporsiDetailResponseDto ->
            val positive = data.items.filter { !it.isRugi }
            val top5 = positive.take(5)
            val othersPct = (100.0 - top5.sumOf { it.persentase }).coerceAtLeast(0.0)
            val slices = buildList {
                top5.forEachIndexed { idx, item -> add(DonutSlice(item.nama, item.persentase, ChartPalette[idx % 5])) }
                if (othersPct > 0.0 && positive.size > 5) add(DonutSlice("Lainnya", othersPct, ChartPalette[5]))
            }
            Panel {
                ProportionDonutChart(
                    slices = slices,
                    centerTitle = AnalisisFmt.rupiah(data.totalNominal),
                    centerSubtitle = if (jenis == "profit") "Total Laba Kotor" else "Total Restock",
                    diameter = 180.dp,
                )
            }

            if (data.items.isEmpty()) {
                Panel { Text("Belum ada data pada periode ini.", fontSize = 12.sp, color = Muted) }
            } else {
                data.items.forEachIndexed { idx, item ->
                    val barColor = if (item.isRugi) DangerColor else ChartPalette[idx.coerceAtMost(4)]
                    ClickableAnalysisCard(onClick = { onOpenProduct(item.barangId) }, modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(barColor))
                            Text(item.nama, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            if (item.isRugi) {
                                StatusChip("Rugi", Color(0xFFFEE2E2), DangerColor)
                            } else if (item.kelasAbc != null) {
                                StatusChip("Kelas ${item.kelasAbc}", Soft, Purple)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(AnalisisFmt.rupiah(item.nominal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(AnalisisFmt.pct(item.persentase), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = barColor)
                        }
                        if (!item.isRugi) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (item.persentase / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = barColor,
                                trackColor = Soft,
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. TREND LIST SCREEN
// =============================================================================
@Composable
internal fun TrendListScreen(
    arah: String,
    uiState: AnalisisUiState,
    onSelectPeriode: (AnalisisPeriodeUi) -> Unit,
    onSwitchArah: (String) -> Unit,
    onLoad: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    LaunchedEffect(arah, uiState.periode) { onLoad() }

    Page {
        Heading("Tren Penjualan", if (arah == "naik") "Produk dengan laju penjualan meningkat" else "Produk dengan laju penjualan menurun")
        PeriodSegmentedBar(selected = uiState.periode, onSelect = onSelectPeriode)
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("naik" to "Tren Naik", "turun" to "Tren Turun").forEach { (key, label) ->
                val active = arah == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Purple else Color.White)
                        .clickable { onSwitchArah(key) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else Ink)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        DetailStateContainer(uiState.trend, onRetry = onLoad) { data: TrenDetailResponseDto ->
            Text(data.keteranganPembanding, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
            if (data.items.isEmpty()) {
                Panel { Text("Belum ada produk pada kategori tren ini.", fontSize = 12.sp, color = Muted) }
            } else {
                data.items.forEach { item ->
                    val isUp = item.arah == "naik"
                    ClickableAnalysisCard(onClick = { onOpenProduct(item.barangId) }, modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(item.nama, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            ChangeBadge(item.perubahanPct)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${item.lajuSekarang}/hari (sebelumnya ${item.lajuPembanding}/hari)",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                        Spacer(Modifier.height(8.dp))
                        TrendSparkline(values = item.sparkline, isUp = isUp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// 5. CORRELATION SCREEN (VIEW-ONLY, SINGLE-EXPAND ACCORDION)
// =============================================================================
@Composable
internal fun CorrelationScreen(
    uiState: AnalisisUiState,
    onSelectPeriode: (AnalisisPeriodeUi) -> Unit,
    onToggleProduct: (String) -> Unit,
    onLoad: () -> Unit,
) {
    LaunchedEffect(uiState.periode) { onLoad() }

    Page {
        Heading(
            title = "Korelasi Antar Produk",
            subtitle = "Angka × menunjukkan seberapa besar peluang dibeli bersamaan dibanding kebetulan",
        )
        PeriodSegmentedBar(selected = uiState.periode, onSelect = onSelectPeriode)
        Spacer(Modifier.height(10.dp))

        DetailStateContainer(uiState.correlation, onRetry = onLoad) { data: KorelasiDetailResponseDto ->
            Text("Dihitung dari ${data.keranjangDitemukan} keranjang belanja", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(bottom = 8.dp))

            if (data.status == "insufficient_data") {
                Panel {
                    Text(
                        "Butuh minimal ${data.keranjangDibutuhkan} transaksi berisi ≥ 2 produk. Saat ini baru ${data.keranjangDitemukan}.",
                        fontSize = 12.sp,
                        color = Muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (data.keranjangDitemukan.toFloat() / data.keranjangDibutuhkan.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color = Purple,
                        trackColor = Soft,
                    )
                }
            } else if (data.produk.isEmpty()) {
                Panel {
                    Text("Belum ditemukan pasangan produk dengan korelasi kuat (Lift > 1,2) pada periode ini.", fontSize = 12.sp, color = Muted)
                }
            } else {
                data.produk.forEach { prod ->
                    val expanded = uiState.expandedCorrelationId == prod.barangId
                    Surface(
                        onClick = { onToggleProduct(prod.barangId) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, if (expanded) Purple else BorderCardColor),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.nama, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    Text("${prod.terkait.size} produk berkorelasi · maks. ${prod.maxLift}×", fontSize = 11.sp, color = Muted)
                                }
                                Text(if (expanded) "▾" else "▸", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Purple)
                            }

                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    prod.terkait.forEach { rel ->
                                        val (strengthLabel, strengthColor, strengthBg) = when (rel.kekuatan) {
                                            "sangat_kuat" -> Triple("Sangat kuat", Purple, Color(0xFFF3E8FF))
                                            "kuat" -> Triple("Kuat", Color(0xFF1D4ED8), Color(0xFFDBEAFE))
                                            else -> Triple("Sedang", Muted, Soft)
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Soft)
                                                .padding(10.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    rel.nama,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Ink,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                StatusChip("$strengthLabel · ${rel.lift}×", strengthBg, strengthColor)
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(rel.pesan, fontSize = 11.sp, color = Muted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun <T> DetailStateContainer(
    state: AnalisisDataState<T>,
    onRetry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is AnalisisDataState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Red)
            }
        }
        is AnalisisDataState.Error -> {
            Panel {
                Text("Gagal memuat data", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(state.message, fontSize = 12.sp, color = Muted)
                Spacer(Modifier.height(8.dp))
                AuraRedButton(onClick = onRetry) {
                    Text("Coba Lagi")
                }
            }
        }
        is AnalisisDataState.Success -> content(state.data)
    }
}
