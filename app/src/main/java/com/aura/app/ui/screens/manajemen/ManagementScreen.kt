package com.aura.app.ui.screens.manajemen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.ui.components.ActionRow
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red

@Composable
internal fun ManagementScreen(go: (String) -> Unit, modifier: Modifier = Modifier) {
    Page(modifier) {
        Text("AURA  •  Manajemen", color = Red, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Heading("Pusat Kendali Operasional", "Kelola aktivitas dan inventaris toko dari satu tempat")
        Panel {
            Text("Operasional & Transaksi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Akses fitur utama untuk melayani pelanggan dan memantau aktivitas toko.", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            ActionRow("Kasir", "Mode penjualan dan pencatatan transaksi") { go("k1_beranda_kasir") }
            ActionRow("Riwayat Hari Ini", "Lihat transaksi yang sudah dicatat") { go("riwayat_hari_ini") }
        }
        Spacer(Modifier.height(8.dp))
        Panel {
            Text("Logistik & Persediaan Barang", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Pantau katalog, stok, dan pasokan barang.", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            ActionRow("Manajemen Barang", "Katalog, kategori, harga, dan status stok") { go("manajemen_barang") }
            ActionRow("Pergerakan Stok", "Riwayat stok masuk, keluar, dan penyesuaian") { go("pergerakan_stok") }
            ActionRow("Catat Restock", "Catat pasokan masuk dan harga beli") { go("catat_restock") }
            ActionRow("Supplier", "Kelola daftar pemasok toko") { go("supplier") }
        }
        Spacer(Modifier.height(8.dp))
        Panel {
            Text("Keuangan & Laporan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Catat arus kas dan evaluasi performa usaha.", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            ActionRow("Pemasukan", "Lihat dan catat arus kas masuk") { go("pemasukan") }
            ActionRow("Pengeluaran", "Lihat dan catat biaya operasional") { go("pengeluaran") }
            ActionRow("Analisis Penjualan", "Ringkasan penjualan dan kondisi stok") { go("analisis") }
        }
        Spacer(Modifier.height(8.dp))
        Panel {
            Text("Pengelolaan Toko", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ActionRow("Riwayat Aktivitas", "Audit aktivitas owner dan kasir") { go("riwayat_aktivitas") }
            ActionRow("Pengaturan Toko", "Akun kasir dan preferensi toko") { go("pengaturan") }
        }
    }
}
