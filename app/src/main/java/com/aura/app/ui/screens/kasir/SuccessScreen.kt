package com.aura.app.ui.screens.kasir

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

@Composable internal fun SuccessScreen(invoice:String,total:Long,isOwner:Boolean,home:()->Unit,history:()->Unit,exitCashier:()->Unit, modifier: Modifier = Modifier) {
    Page(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(25.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.size(78.dp).clip(CircleShape).background(Color(0xFFE3F5E9)),contentAlignment=Alignment.Center) {
                Text(stringResource(R.string.screen_copy_110),fontSize=44.sp,color=Green,fontWeight=FontWeight.Bold)
            }
            Text(stringResource(R.string.screen_copy_111),fontSize=22.sp,fontWeight=FontWeight.ExtraBold)
            Text(stringResource(R.string.kasir_success_server_saved),fontSize=11.sp,color=Muted)
        }
        Panel {
            Text(stringResource(R.string.screen_copy_113),fontWeight=FontWeight.Bold)
            Text(invoice,fontSize=11.sp,color=Muted)
            HorizontalDivider(Modifier.padding(vertical=10.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(stringResource(R.string.screen_copy_114)); Text(money(total),fontWeight=FontWeight.Bold) }
        }
        Primary(stringResource(R.string.screen_copy_115),home)
        Secondary(stringResource(R.string.screen_copy_116),history)
        if(isOwner) Secondary(stringResource(R.string.screen_copy_117),exitCashier)
    }
}
