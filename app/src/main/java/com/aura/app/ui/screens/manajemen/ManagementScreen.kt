package com.aura.app.ui.screens.manajemen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.aura.app.R
import com.aura.app.data.*
import com.aura.app.ui.components.*
import com.aura.app.ui.theme.*
import com.aura.app.core.format.RupiahFormatter
import com.aura.app.core.format.TanggalFormatter
import java.util.Locale

@Composable internal fun ManagementScreen(go:(String)->Unit, modifier: Modifier = Modifier) {
    Page(modifier = modifier) { Heading(stringResource(R.string.screen_copy_135),"Kelola seluruh operasional toko")
        listOf("Buka menu kasir" to "k1_beranda_kasir","Daftar barang, stok" to "manajemen_barang","Catat restock" to "catat_restock","Riwayat keluar-masuk" to "pergerakan_stok","Daftar pemasok" to "supplier","Pengeluaran" to "pengeluaran","Pemasukan" to "pemasukan","Log audit seluruh aktivitas" to "riwayat_aktivitas","Pengaturan toko" to "pengaturan").forEach{ActionRow(it.first,"Kelola ${it.first.lowercase()}"){go(it.second)}}
    }
}
