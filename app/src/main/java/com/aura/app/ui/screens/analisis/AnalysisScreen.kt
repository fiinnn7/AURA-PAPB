package com.aura.app.ui.screens.analisis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.core.model.StatusBarang
import com.aura.app.core.model.StatusCatatanKeuangan
import com.aura.app.core.model.StatusTransaksi
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.ItemTransaksiEntity
import com.aura.app.ui.keuangan.PemasukanTampilan
import com.aura.app.data.local.entity.PengeluaranEntity
import com.aura.app.data.local.entity.PergerakanStokEntity
import com.aura.app.data.local.entity.TransaksiPenjualanEntity
import com.aura.app.ui.components.*
import com.aura.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun AnalysisScreen(
    tab: String,
    setTab: (String) -> Unit,
    products: List<BarangEntity>,
    tx: List<TransaksiPenjualanEntity>,
    items: List<ItemTransaksiEntity>,
    expenses: List<PengeluaranEntity>,
    incomes: List<PemasukanTampilan>,
    movements: List<PergerakanStokEntity>,
    loading: Boolean = false,
    error: String? = null,
    retry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val successful = tx.filter { it.status == StatusTransaksi.SELESAI }
    val successfulIds = successful.map { it.id }.toSet()
    val revenue = successful.sumOf { it.total }
    val costOfGoods = items.filter { it.transaksiId in successfulIds }.sumOf { it.hppSnapshot * it.jumlah }
    val validExpenses = expenses.filter { it.status == StatusCatatanKeuangan.TERCATAT }
    val cashOutflow = validExpenses.sumOf { it.nominal }
    // Restock/pembelian is inventory acquisition, not an operating expense for profit.
    val operatingExpense = validExpenses.filter { it.jenis.name == "KUSTOM" }.sumOf { it.nominal }
    val capital = incomes.filter { it.status == "TERCATAT" && it.jenis == "KUSTOM" }.sumOf { it.nominal }
    val activeProducts = products.filter { it.status == StatusBarang.AKTIF }
    val stockValue = activeProducts.sumOf { it.stokSaatIni.toLong() * it.hppRataRata }
    val soldMost = items.filter { it.transaksiId in successfulIds }
        .groupBy { it.namaBarangSnapshot }.mapValues { (_, rows) -> rows.sumOf { it.jumlah } }
        .maxByOrNull { it.value }?.key ?: "-"

    val midnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val daySeries = (6 downTo 0).map { offset ->
        val day = (midnight.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
        val start = day.timeInMillis
        val end = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val amount = successful.filter { it.dibuatPada >= start && it.dibuatPada < end }.sumOf { it.total }
        SimpleDateFormat("EE", Locale("id", "ID")).format(Date(start)) to amount
    }
    val maxDay = daySeries.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L

    Page(modifier) {
        Text("AURA  •  Analisis", color = Red, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Heading("Analisis Bisnis", "Perhitungan berdasarkan transaksi, stok, dan keuangan dari server")
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Ringkasan", "Penjualan", "Stok", "Keuangan").forEach {
                FilterChip(selected = tab == it, onClick = { setTab(it) }, label = { Text(it, fontSize = 10.sp) })
            }
        }
        if (error != null) Panel {
            Text("Data analisis belum sepenuhnya tersinkron", color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(error, color = Muted, fontSize = 10.sp)
            Text("Coba lagi", color = Red, modifier = Modifier.padding(top = 6.dp).clickable { retry() }, fontWeight = FontWeight.Bold)
        }
        if (loading && products.isEmpty() && tx.isEmpty() && expenses.isEmpty()) Panel { Text("Memuat data analisis dari server…", color = Muted, fontSize = 12.sp) }
        when (tab) {
            "Ringkasan" -> {
                Panel {
                    Text("Total Penjualan", color = Muted, fontSize = 11.sp)
                    Text(money(revenue), color = Red, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${successful.size} transaksi berhasil tercatat", color = Muted, fontSize = 10.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { Metric("HPP Terjual", money(costOfGoods)) }
                    Box(Modifier.weight(1f)) { Metric("Pengeluaran", money(cashOutflow)) }
                }
                Metric("Estimasi Laba Operasional", money(revenue - costOfGoods - operatingExpense), "Pengeluaran operasional tidak memasukkan pembelian/restock persediaan")
                Text("Penjualan 7 Hari Terakhir", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                Panel {
                    Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        daySeries.forEach { (day, amount) ->
                            Column(Modifier.weight(1f).height(112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().height(15.dp), contentAlignment = Alignment.Center) {
                                    Text(if (amount == 0L) "–" else compactMoney(amount), fontSize = 8.sp, color = Muted, maxLines = 1)
                                }
                                Box(Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.BottomCenter) {
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                                    if (amount > 0L) Box(Modifier.width(18.dp).height((70f * amount.toFloat() / maxDay.toFloat()).coerceAtLeast(3f).dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(Red))
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(day, fontSize = 9.sp, color = Muted, maxLines = 1)
                            }
                        }
                    }
                }
                Text("Pemasukan tambahan tercatat: ${money(capital)}", color = Muted, fontSize = 10.sp)
            }
            "Penjualan" -> {
                Metric("Total Penjualan", money(revenue))
                Metric("HPP Barang Terjual", money(costOfGoods))
                Metric("Transaksi Berhasil", successful.size.toString())
                Metric("Rata-rata per Transaksi", money(if (successful.isEmpty()) 0 else revenue / successful.size))
                Metric("Produk Terlaris", soldMost)
            }
            "Stok" -> {
                Metric("Jumlah Produk Aktif", activeProducts.size.toString())
                Metric("Nilai Persediaan", money(stockValue))
                Metric("Produk Terlaris", soldMost)
                Metric("Pergerakan Stok", movements.size.toString())
                Metric("Restock Tercatat", movements.count { it.sumber.name == "RESTOCK" }.toString())
                Metric("Stok Menipis", activeProducts.count { it.stokSaatIni in 1..it.stokMinimum }.toString())
                Metric("Stok Habis", activeProducts.count { it.stokSaatIni <= 0 }.toString())
            }
            else -> {
                Metric("Pengeluaran Tercatat", money(cashOutflow))
                Metric("Pengeluaran Operasional", money(operatingExpense))
                Metric("Pembelian / Restock", money(validExpenses.filter { it.jenis.name == "PEMBELIAN" }.sumOf { it.nominal }))
                Metric("Pemasukan Tambahan", money(capital))
                Panel {
                    Text("Catatan perhitungan", fontWeight = FontWeight.Bold)
                    Text("Laba operasional dihitung dari penjualan dikurangi HPP barang terjual dan pengeluaran kustom. Pembelian/restock diperlakukan sebagai penambahan persediaan, bukan biaya operasional.", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Heading("Sumber Data", "Data yang disinkronkan dari server")
        val counters = listOf("Barang aktif" to activeProducts.size, "Transaksi" to tx.size, "Item transaksi" to items.size, "Pengeluaran" to expenses.size, "Pergerakan stok" to movements.size)
        counters.forEach { (label, count) ->
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 11.sp, color = Ink)
                    Text(count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = (count / 5000f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().padding(top = 7.dp), color = Red)
            }
        }
        Heading("Stok Perlu Dipantau")
        activeProducts.filter { it.stokSaatIni <= it.stokMinimum }.sortedBy { it.stokSaatIni }.take(5).forEach { product ->
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(product.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Minimum ${product.stokMinimum} ${product.satuan}", color = Muted, fontSize = 10.sp)
                    }
                    Text("${product.stokSaatIni} ${product.satuan}", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun compactMoney(value: Long): String = when {
    value >= 1_000_000 -> "Rp${"%.1f".format(Locale("id", "ID"), value / 1_000_000.0)}jt"
    value >= 1_000 -> "Rp${value / 1_000}k"
    else -> "Rp$value"
}
