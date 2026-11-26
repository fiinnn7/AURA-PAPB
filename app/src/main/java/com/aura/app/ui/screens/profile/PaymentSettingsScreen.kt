package com.aura.app.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Input
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.components.Primary
import com.aura.app.ui.components.Secondary
import com.aura.app.ui.components.ServerFileImage
import com.aura.app.ui.keuangan.KeuanganUiState

@Composable
internal fun PaymentSettingsScreen(
    uiState: KeuanganUiState,
    onRecipientChange: (String) -> Unit,
    onAccountInfoChange: (String) -> Unit,
    onUploadQris: (android.content.ContentResolver, android.net.Uri) -> Unit,
    onRemoveQris: () -> Unit,
    onSave: () -> Unit,
    onLoadFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val qrisFileId = uiState.toko?.qrisFileId
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onUploadQris(context.contentResolver, uri)
    }

    Page(modifier = modifier) {
        Heading(stringResource(R.string.screen_copy_107), stringResource(R.string.payment_settings_subtitle))
        uiState.errorMessage?.let { Panel { Text(it) } }
        Panel {
            Text(stringResource(R.string.payment_qris_title))
            Spacer(Modifier.height(8.dp))
            if (qrisFileId == null) {
                Text(stringResource(R.string.payment_qris_missing))
            } else {
                ServerFileImage(
                    fileId = qrisFileId,
                    bytes = uiState.fileBytes[qrisFileId],
                    gagalDimuat = qrisFileId in uiState.fileIdsGagalDimuat,
                    onLoad = onLoadFile,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentDescription = stringResource(R.string.payment_qris_title),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
                enabled = !uiState.isUploadingFile,
            ) {
                Text(stringResource(if (qrisFileId == null) R.string.payment_qris_upload else R.string.payment_qris_replace))
            }
            if (qrisFileId != null) {
                OutlinedButton(onClick = onRemoveQris, enabled = !uiState.isSaving) {
                    Text(stringResource(R.string.payment_qris_remove))
                }
            }
            if (uiState.isUploadingFile) Text(stringResource(R.string.payment_qris_uploading))
        }
        Input(
            stringResource(R.string.payment_recipient),
            uiState.namaPenerimaDraft,
            onRecipientChange,
        )
        Input(
            stringResource(R.string.payment_account_info),
            uiState.infoRekeningDraft,
            onAccountInfoChange,
        )
        Primary(
            stringResource(R.string.action_save),
            onSave,
            enabled = !uiState.isSaving && !uiState.isUploadingFile,
        )
    }
}
