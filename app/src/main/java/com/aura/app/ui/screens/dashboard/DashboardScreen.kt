package com.aura.app.ui.screens.dashboard

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
import com.aura.app.data.local.entity.TokoEntity
import java.util.Locale

@Composable internal fun DashboardScreen(store:TokoEntity?,products:List<ProductEntity>,tx:List<TransactionEntity>,records:List<CashRecordEntity>,hide:Boolean,toggle:()->Unit,go:(String)->Unit, modifier: Modifier = Modifier) {
    val today=tx.filter{it.status=="Selesai"}
    val expenses=records.filter{it.kind=="expense"&&!it.isPrive}.sumOf{it.amount}
    Page(modifier = modifier) {
        Text(stringResource(R.string.screen_copy_085),fontSize=12.sp,color=Muted)
        Heading(store?.namaToko ?: "Ringkasan Toko","${store?.jenisUsaha ?: "Retail"} • ${store?.idToko ?: ""}")
        Panel {
            Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(stringResource(R.string.screen_copy_086),fontSize=11.sp,color=Muted);Text(if(hide)"Rp ••••••••" else money(store?.saldoKumulatif?:0),fontSize=25.sp,fontWeight=FontWeight.ExtraBold,color=Red)};Text(if(hide)"◉" else "◎",Modifier.clickable{toggle()}.padding(8.dp),fontSize=22.sp,color=Red)}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Metric(stringResource(R.string.screen_copy_087),money(today.sumOf{it.total}),"${today.size} transaksi")};Box(Modifier.weight(1f)){Metric(stringResource(R.string.screen_copy_088),money(expenses))}}
        Heading(stringResource(R.string.screen_copy_089))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Metric(stringResource(R.string.screen_copy_090),products.count{it.stock in 1..it.minStock}.toString(),"Perlu restock")};Box(Modifier.weight(1f)){Metric(stringResource(R.string.screen_copy_091),products.count{it.stock==0}.toString(),"Segera isi")}}
        Heading(stringResource(R.string.screen_copy_092))
        listOf("Manajemen Barang" to "manajemen_barang","Catat Restock" to "catat_restock","Pengeluaran" to "pengeluaran","Pemasukan" to "pemasukan","Riwayat Hari Ini" to "riwayat_hari_ini","Notifikasi" to "notifikasi").forEach{ActionRow(it.first,"Buka modul") {go(it.second)}}
    }
}
