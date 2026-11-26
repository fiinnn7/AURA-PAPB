package com.aura.app.ui.screens.analisis

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
import com.aura.app.data.remote.dto.AnalisisRingkasanResponseDto
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

@Composable
internal fun AnalysisScreen(
    uiState: AnalisisUiState,
    onSelectPeriode: (AnalisisPeriodeUi) -> Unit,
    onRefresh: () -> Unit,
    onToggleProporsi: (Boolean) -> Unit,
    onOpenMovement: (String) -> Unit,
    onOpenRunway: () -> Unit,
    onOpenProportion: (String) -> Unit,
    onOpenTrend: (String) -> Unit,
    onOpenCorrelation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        if (uiState.overview is AnalisisDataState.Loading) {
            onRefresh()
        }
    }

    Page(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Heading("Analisis Bisnis", "Ringkasan performa toko Anda")
            }
            Text(
                text = if (uiState.isRefreshing) "Memuat..." else "Segarkan",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Red,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !uiState.isRefreshing) { onRefresh() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        PeriodSegmentedBar(selected = uiState.periode, onSelect = onSelectPeriode)
        Spacer(Modifier.height(6.dp))

        when (val state = uiState.overview) {
            is AnalisisDataState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Red)
                }
            }

            is AnalisisDataState.Error -> {
                Panel {
                    Text("Gagal memuat analisis", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(state.message, fontSize = 12.sp, color = Muted)
                    Spacer(Modifier.height(10.dp))
                    AuraRedButton(onClick = onRefresh) {
                        Text("Coba Lagi")
                    }
                }
            }

            is AnalisisDataState.Success -> {
                OverviewContent(
                    data = state.data,
                    isFromCache = state.isFromCache,
                    proporsiProfit = uiState.proporsiTabProfit,
                    onToggleProporsi = onToggleProporsi,
                    onOpenMovement = onOpenMovement,
                    onOpenRunway = onOpenRunway,
                    onOpenProportion = onOpenProportion,
                    onOpenTrend = onOpenTrend,
                    onOpenCorrelation = onOpenCorrelation,
                )
            }
        }
    }
}

@Composable
private fun OverviewContent(
    data: AnalisisRingkasanResponseDto,
    isFromCache: Boolean,
    proporsiProfit: Boolean,
    onToggleProporsi: (Boolean) -> Unit,
    onOpenMovement: (String) -> Unit,
    onOpenRunway: () -> Unit,
    onOpenProportion: (String) -> Unit,
    onOpenTrend: (String) -> Unit,
    onOpenCorrelation: () -> Unit,
) {
    val infoLine = buildString {
        append(AnalisisFmt.waktuSingkat(data.diperbaruiPada))
        if (isFromCache) append(" · Data cache")
    }
    if (infoLine.isNotBlank()) {
        Text(infoLine, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(bottom = 6.dp))
    }

    SectionLabel("1. Ringkasan Finansial")
    val fin = data.finansial
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Panel {
                Text("Omzet", fontSize = 11.sp, color = Muted)
                Text(AnalisisFmt.rupiah(fin.omzet), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(2.dp))
                ChangeBadge(fin.omzetPerubahanPct)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Panel {
                Text("Laba Bersih", fontSize = 11.sp, color = Muted)
                Text(AnalisisFmt.rupiah(fin.labaBersih), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(2.dp))
                ChangeBadge(fin.labaBersihPerubahanPct)
            }
        }
    }

    Panel {
        Text("Grafik Omzet & Laba Kotor Harian", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(8.dp))
        FinancialLineChart(points = fin.harian)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Panel {
                Text("Laba Kotor", fontSize = 11.sp, color = Muted)
                Text(AnalisisFmt.rupiah(fin.labaKotor), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Gross Margin: ${AnalisisFmt.pct(fin.grossMarginPct)}", fontSize = 10.sp, color = Muted)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Panel {
                Text("Biaya Operasional", fontSize = 11.sp, color = Muted)
                Text(AnalisisFmt.rupiah(fin.biayaOperasional), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Net Margin: ${AnalisisFmt.pct(fin.netMarginPct)}", fontSize = 10.sp, color = Muted)
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Panel {
                Text("Kerugian Penyesuaian", fontSize = 11.sp, color = Muted)
                Text(AnalisisFmt.rupiah(fin.kerugianPenyesuaian), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DangerColor)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Panel {
                Text("Modal Tertahan", fontSize = 11.sp, color = Muted)
                Text(AnalisisFmt.rupiah(fin.modalTertahan), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
        }
    }

    SectionLabel("2. Volume & Pergerakan Barang")
    val mov = data.pergerakan
    if (mov.status == "insufficient_data") {
        Panel {
            Text("Fast & Slow Moving belum tersedia", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Butuh minimal 5 barang aktif berusia ≥ 14 hari.", fontSize = 12.sp, color = Muted)
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ClickableAnalysisCard(onClick = { onOpenMovement("fast") }, modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fast Moving", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green)
                    Text("›", fontSize = 16.sp, color = Muted)
                }
                Spacer(Modifier.height(4.dp))
                Text("${mov.fastCount} produk", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                val topFast = mov.fastPreview.firstOrNull()
                Text(
                    text = if (topFast != null) "Top: ${topFast.nama}" else "Belum ada",
                    fontSize = 11.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ClickableAnalysisCard(onClick = { onOpenMovement("slow") }, modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Slow Moving", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberColor)
                    Text("›", fontSize = 16.sp, color = Muted)
                }
                Spacer(Modifier.height(4.dp))
                Text("${mov.slowCount} produk", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                val zeroCount = mov.slowPreview.count { it.tidakLaku }
                Text(
                    text = if (zeroCount > 0) "$zeroCount tidak laku" else "Tekan lihat STR",
                    fontSize = 11.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    ClickableAnalysisCard(onClick = onOpenRunway) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Estimasi Umur Stok", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    text = if (mov.runwayKritisCount > 0) "${mov.runwayKritisCount} produk diperkirakan habis ≤ 3 hari" else "Lihat perkiraan sisa hari semua stok",
                    fontSize = 11.sp,
                    color = if (mov.runwayKritisCount > 0) DangerColor else Muted,
                )
            }
            Text("›", fontSize = 18.sp, color = Muted)
        }
    }

    SectionLabel("3. Proporsi Penjualan")
    val prop = data.proporsi
    Panel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(true to "Profit", false to "Pengeluaran").forEach { (isProfitTab, label) ->
                val active = proporsiProfit == isProfitTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Purple else Soft)
                        .clickable { onToggleProporsi(isProfitTab) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else Ink)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        val list = if (proporsiProfit) prop.profitTop else prop.pengeluaranTop
        val othersPct = if (proporsiProfit) prop.profitLainnyaPct else prop.pengeluaranLainnyaPct
        val totalNom = if (proporsiProfit) prop.totalProfit else prop.totalPengeluaran

        val slices = buildList {
            list.forEachIndexed { idx, item ->
                add(DonutSlice(item.nama, item.persentase, ChartPalette[idx % 5]))
            }
            if (othersPct > 0.0) {
                add(DonutSlice("Lainnya", othersPct, ChartPalette[5]))
            }
        }

        ProportionDonutChart(
            slices = slices,
            centerTitle = AnalisisFmt.rupiah(totalNom),
            centerSubtitle = if (proporsiProfit) "Total Laba Kotor" else "Total Restock",
        )

        if (list.isEmpty()) {
            Text("Belum ada data proporsi pada periode ini.", fontSize = 12.sp, color = Muted)
        } else {
            list.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(ChartPalette[idx % 5]))
                    Text(item.nama, fontSize = 12.sp, color = Ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (item.kelasAbc != null) {
                        StatusChip(item.kelasAbc, Soft, Purple)
                    }
                    Text(AnalisisFmt.pct(item.persentase), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
            }
            if (othersPct > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(ChartPalette[5]))
                    Text("Lainnya", fontSize = 12.sp, color = Muted, modifier = Modifier.weight(1f))
                    Text(AnalisisFmt.pct(othersPct), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Lihat semua ›",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Red,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onOpenProportion(if (proporsiProfit) "profit" else "pengeluaran") }
                .padding(4.dp),
        )
    }

    SectionLabel("4. Tren Penjualan")
    val tr = data.tren
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ClickableAnalysisCard(onClick = { onOpenTrend("naik") }, modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tren Naik", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green)
                Text("›", fontSize = 16.sp, color = Muted)
            }
            Spacer(Modifier.height(4.dp))
            Text("${tr.naikCount} produk", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            val topUp = tr.naikPreview.firstOrNull()
            Text(
                text = if (topUp != null) "${topUp.nama} (+${AnalisisFmt.pct(topUp.perubahanPct)})" else "Belum ada",
                fontSize = 11.sp,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ClickableAnalysisCard(onClick = { onOpenTrend("turun") }, modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tren Turun", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerColor)
                Text("›", fontSize = 16.sp, color = Muted)
            }
            Spacer(Modifier.height(4.dp))
            Text("${tr.turunCount} produk", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            val topDown = tr.turunPreview.firstOrNull()
            Text(
                text = if (topDown != null) "${topDown.nama} (${AnalisisFmt.pct(topDown.perubahanPct)})" else "Belum ada",
                fontSize = 11.sp,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    SectionLabel("5. Korelasi Antar Produk")
    val kor = data.korelasi
    ClickableAnalysisCard(onClick = onOpenCorrelation) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Produk yang sering dibeli bersama", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(3.dp))
                if (kor.status == "insufficient_data") {
                    Text(
                        "Butuh ${kor.keranjangDibutuhkan} transaksi ≥ 2 produk (saat ini ${kor.keranjangDitemukan})",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (kor.keranjangDitemukan.toFloat() / kor.keranjangDibutuhkan.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Purple,
                        trackColor = Soft,
                    )
                } else {
                    Text(
                        "${kor.jumlahProduk} produk memiliki pasangan kuat · dari ${kor.keranjangDitemukan} keranjang",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                }
            }
            Text("›", fontSize = 18.sp, color = Muted)
        }
    }
    Spacer(Modifier.height(24.dp))
}
