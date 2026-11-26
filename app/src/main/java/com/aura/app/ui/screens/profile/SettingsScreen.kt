package com.aura.app.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aura.app.R
import com.aura.app.ui.components.ActionRow
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Page
import com.aura.app.ui.theme.AuraTheme

@Composable
internal fun SettingsScreen(
    onUbahSandi: () -> Unit,
    onAkunKasir: () -> Unit,
    onMetodePembayaran: () -> Unit,
    onPengaturanToko: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Page(modifier = modifier) {
        Heading(stringResource(R.string.screen_copy_155), stringResource(R.string.role_owner))
        ActionRow(
            title = stringResource(R.string.ubah_sandi_title),
            detail = stringResource(R.string.settings_ubah_sandi_detail),
            onClick = onUbahSandi,
        )
        ActionRow(
            title = stringResource(R.string.screen_copy_141),
            detail = stringResource(R.string.kasir_subtitle),
            onClick = onAkunKasir,
        )
        ActionRow(
            title = stringResource(R.string.screen_copy_107),
            detail = stringResource(R.string.screen_copy_147),
            onClick = onMetodePembayaran,
        )
        ActionRow(
            title = stringResource(R.string.shop_settings_title),
            detail = stringResource(R.string.shop_settings_subtitle),
            onClick = onPengaturanToko,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    AuraTheme {
        SettingsScreen(onUbahSandi = {}, onAkunKasir = {}, onMetodePembayaran = {}, onPengaturanToko = {})
    }
}
