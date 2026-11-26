package com.aura.app.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aura.app.R
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Input
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.components.Primary
import com.aura.app.ui.keuangan.KeuanganUiState

@Composable
internal fun PengaturanTokoScreen(
    uiState: KeuanganUiState,
    onNameChange: (String) -> Unit,
    onBusinessTypeChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onOpeningBalanceChange: (String) -> Unit,
    onSaveStore: () -> Unit,
    onSaveOpeningBalance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Page(modifier = modifier) {
        Heading(stringResource(R.string.shop_settings_title), stringResource(R.string.shop_settings_subtitle))
        uiState.errorMessage?.let { Panel { androidx.compose.material3.Text(it) } }
        uiState.toko?.let { toko ->
            Panel {
                androidx.compose.material3.Text(
                    stringResource(R.string.shop_id_label, toko.idToko),
                )
            }
        }
        Input(
            stringResource(R.string.shop_name_label),
            uiState.namaTokoDraft,
            onNameChange,
        )
        Input(
            stringResource(R.string.shop_business_type_label),
            uiState.jenisUsahaDraft,
            onBusinessTypeChange,
        )
        Input(
            stringResource(R.string.shop_address_label),
            uiState.alamatDraft,
            onAddressChange,
        )
        Primary(
            stringResource(R.string.shop_save_profile),
            onSaveStore,
            enabled = !uiState.isSaving && uiState.namaTokoDraft.isNotBlank(),
        )
        Panel {
            androidx.compose.material3.Text(stringResource(R.string.shop_opening_balance_note))
            Input(
                stringResource(R.string.shop_opening_balance_label),
                uiState.saldoAwalDraft,
                onOpeningBalanceChange,
                number = true,
            )
            Primary(
                stringResource(R.string.shop_save_opening_balance),
                onSaveOpeningBalance,
                enabled = !uiState.isSaving && uiState.saldoAwalDraft.toLongOrNull() != null,
            )
        }
    }
}
