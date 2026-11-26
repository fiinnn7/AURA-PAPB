package com.aura.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.model.Peran
import com.aura.app.data.preferences.SesiAktif
import com.aura.app.ui.components.ActionRow
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.theme.AuraTheme
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Purple
import com.aura.app.ui.theme.Red

@Composable
internal fun ProfileScreen(
    sesi: SesiAktif?,
    onEditProfil: () -> Unit,
    onPengaturan: () -> Unit,
    onGantiAkun: () -> Unit,
    onKeluar: () -> Unit,
    onSalinIdToko: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val name = sesi?.nama.orEmpty().ifBlank { "-" }
    val storeId = sesi?.idToko.orEmpty().ifBlank { "-" }
    val peranLabel = stringResource(
        if (sesi?.peran == Peran.OWNER) R.string.role_owner else R.string.role_kasir,
    )

    Page(modifier = modifier) {
        Text("AURA  •  Profil", color = Red, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("Informasi akun dan pengaturan toko", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Purple),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(peranLabel, fontSize = 10.sp, color = Purple, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp))
                    Text(stringResource(R.string.profil_id_toko_format, storeId), fontSize = 11.sp, color = Muted)
                    if (!sesi?.email.isNullOrBlank()) {
                        Text(sesi!!.email!!, fontSize = 11.sp, color = Muted)
                    }
                    if (!sesi?.nomorHp.isNullOrBlank()) {
                        Text(sesi!!.nomorHp!!, fontSize = 11.sp, color = Muted)
                    }
                }
                if (storeId != "-") {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(storeId))
                        onSalinIdToko()
                    }) {
                        Text(stringResource(R.string.profil_salin), color = Red, fontSize = 12.sp)
                    }
                }
            }
        }

        Text("Pengaturan & Usaha", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 3.dp, top = 8.dp, bottom = 5.dp))
        ActionRow(
            title = stringResource(R.string.profil_edit_title),
            detail = stringResource(R.string.sheet_avatar_edit_profil_detail),
            onClick = onEditProfil,
        )
        ActionRow(
            title = stringResource(R.string.screen_copy_152),
            detail = stringResource(R.string.screen_copy_155),
            onClick = onPengaturan,
        )
        ActionRow(
            title = stringResource(R.string.screen_copy_153),
            detail = stringResource(R.string.sheet_avatar_ganti_akun_detail),
            onClick = onGantiAkun,
        )
        ActionRow(
            title = stringResource(R.string.screen_copy_154),
            detail = stringResource(R.string.sheet_avatar_keluar_detail),
            onClick = onKeluar,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    AuraTheme {
        ProfileScreen(
            sesi = SesiAktif(
                token = "t",
                expiresAt = 0L,
                akunId = "1",
                idToko = "AB12CD34",
                peran = Peran.OWNER,
                nama = "Budi Santoso",
                email = "owner@aura.id",
                nomorHp = "08123456789",
            ),
            onEditProfil = {},
            onPengaturan = {},
            onGantiAkun = {},
            onKeluar = {},
            onSalinIdToko = {},
        )
    }
}
