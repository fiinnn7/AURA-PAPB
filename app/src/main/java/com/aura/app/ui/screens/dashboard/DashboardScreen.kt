package com.aura.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.core.model.StatusBarang
import com.aura.app.core.model.StatusCatatanKeuangan
import com.aura.app.core.model.StatusTransaksi
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.PengeluaranEntity
import com.aura.app.data.local.entity.TokoEntity
import com.aura.app.data.local.entity.TransaksiPenjualanEntity
import com.aura.app.ui.components.*
import com.aura.app.ui.theme.*
import java.util.Calendar

@Composable
internal fun DashboardScreen(
    store: TokoEntity?,
    products: List<BarangEntity>,
    tx: List<TransaksiPenjualanEntity>,
    expenses: List<PengeluaranEntity>,
    hide: Boolean,
    toggle: () -> Unit,
    go: (String) -> Unit,
    loading: Boolean = false,
    error: String? = null,
    retry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val start = today.timeInMillis
    val end = today.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
    val completed = tx.filter { it.status == StatusTransaksi.SELESAI }
    val todayTransactions = completed.filter { it.dibuatPada >= start && it.dibuatPada < end }
    val todayRevenue = todayTransactions.sumOf { it.total }
    val todayExpenses = expenses.filter {
        it.status == StatusCatatanKeuangan.TERCATAT && it.tanggal >= start && it.tanggal < end
    }.sumOf { it.nominal }
    val activeProducts = products.filter { it.status == StatusBarang.AKTIF }
    val lowStock = activeProducts.filter { it.stokSaatIni in 1..it.stokMinimum }
    val outStock = activeProducts.filter { it.stokSaatIni <= 0 }
    val recent = completed.sortedByDescending { it.dibuatPada }.take(4)

    Page(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AURA", color = Red, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text(store?.namaToko ?: "Dashboard Toko", color = Muted, fontSize = 12.sp)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { go("notifikasi") }, contentAlignment = Alignment.Center) {
                Text("♧", color = Red, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Red).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL SALDO TOKO", color = Color.White.copy(alpha = .8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(if (hide) "◉" else "◎", color = Color.White, modifier = Modifier.clickable { toggle() })
            }
            Text(if (hide) "Rp ••••••••" else money(store?.saldoKumulatif ?: 0), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("Kasir", "＋", Modifier.weight(1f)) { go("k1_beranda_kasir") }
                QuickAction("Restock", "▣", Modifier.weight(1f)) { go("catat_restock") }
                QuickAction("Pemasukan", "↗", Modifier.weight(1f)) { go("catat_pemasukan") }
                QuickAction("Pengeluaran", "↘", Modifier.weight(1f)) { go("catat_pengeluaran") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Heading("Ringkasan Hari Ini", "Data transaksi dan stok dari server")
        if (error != null) {
            Panel {
                Text("Sebagian data belum berhasil disinkronkan", color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(error, color = Muted, fontSize = 10.sp)
                Text("Coba lagi", color = Red, modifier = Modifier.clickable { retry() }.padding(top = 6.dp), fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { Metric("Penjualan", money(todayRevenue), "${todayTransactions.size} transaksi") }
            Box(Modifier.weight(1f)) { Metric("Pengeluaran Hari Ini", money(todayExpenses)) }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { Metric("Stok Menipis", lowStock.size.toString(), "Perlu restock") }
            Box(Modifier.weight(1f)) { Metric("Stok Habis", outStock.size.toString(), "Segera isi") }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Heading("Perlu Perhatian", "Pantau ketersediaan barang")
            Text("Lihat semua", color = Red, fontSize = 11.sp, modifier = Modifier.clickable { go("manajemen_barang") })
        }
        if (loading && products.isEmpty()) Panel { Text("Memuat barang dari server…", color = Muted, fontSize = 12.sp) }
        else if (lowStock.isEmpty() && outStock.isEmpty()) Panel { Text("Semua stok dalam kondisi aman.", color = Green, fontSize = 12.sp) }
        else (outStock + lowStock).take(3).forEach { product ->
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Soft), contentAlignment = Alignment.Center) { Text("▣", color = Red, fontSize = 18.sp) }
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(product.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink)
                        Text("Stok ${product.stokSaatIni} ${product.satuan} • Minimum ${product.stokMinimum}", color = Muted, fontSize = 10.sp)
                    }
                    Text(if (product.stokSaatIni <= 0) "Habis" else "Menipis", color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Heading("Transaksi Terbaru")
            Text("Riwayat", color = Red, fontSize = 11.sp, modifier = Modifier.clickable { go("riwayat_hari_ini_owner") })
        }
        if (recent.isEmpty()) Panel { Text(if (loading) "Memuat transaksi dari server…" else "Belum ada transaksi selesai.", color = Muted, fontSize = 12.sp) }
        else recent.forEach { item ->
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.nomorTransaksi, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${item.kasirNama} • ${item.metodePembayaran.name}", color = Muted, fontSize = 10.sp)
                    }
                    Text(money(item.total), color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Heading("Menu Cepat")
        listOf("Manajemen Barang" to "manajemen_barang", "Analisis Penjualan" to "analisis", "Supplier" to "supplier", "Riwayat Aktivitas" to "riwayat_aktivitas").forEach { (label, route) -> ActionRow(label, "Buka modul") { go(route) } }
    }
}

@Composable
private fun QuickAction(label: String, symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .14f)).clickable { onClick() }.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}
