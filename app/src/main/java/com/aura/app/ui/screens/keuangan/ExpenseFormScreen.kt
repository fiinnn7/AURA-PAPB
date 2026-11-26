package com.aura.app.ui.screens.keuangan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import com.aura.app.ui.keuangan.KeuanganUiState

@Composable
internal fun ExpenseFormScreen(
    uiState: KeuanganUiState,
    onTitleChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onUploadEvidence: (android.content.ContentResolver, android.net.Uri) -> Unit,
    onRemoveEvidence: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = uiState.formPengeluaran
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onUploadEvidence(context.contentResolver, uri)
    }
    Page(modifier = modifier) {
        Heading(stringResource(R.string.finance_expense_add), stringResource(R.string.finance_form_expense_hint))
        uiState.errorMessage?.let { Panel { Text(it) } }
        Input(stringResource(R.string.finance_title_label), form.judul, onTitleChange)
        Text(stringResource(R.string.finance_expense_type_label))
        Row {
            listOf(
                "LISTRIK" to R.string.finance_expense_electricity,
                "SEWA" to R.string.finance_expense_rent,
                "GAJI" to R.string.finance_expense_salary,
                "LAINNYA" to R.string.finance_expense_other,
            ).forEach { (value, label) ->
                FilterChip(
                    selected = form.jenisBiaya == value,
                    onClick = { onTypeChange(value) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        Input(stringResource(R.string.finance_amount_label), form.nominal, onAmountChange, number = true)
        Input(stringResource(R.string.finance_note_label), form.catatan, onNoteChange)
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.OutlinedButton(onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }, enabled = !uiState.isUploadingFile) {
            Text(stringResource(R.string.finance_upload_evidence))
        }
        if (uiState.isUploadingFile) CircularProgressIndicator()
        if (uiState.buktiFileIdUnggahan != null) {
            Panel {
                Text(stringResource(R.string.finance_evidence_uploaded))
                Secondary(stringResource(R.string.finance_remove_evidence), onRemoveEvidence)
            }
        }
        Primary(
            stringResource(R.string.finance_save_expense),
            onSave,
            enabled = !uiState.isSaving && !uiState.isUploadingFile &&
                form.judul.isNotBlank() && (form.nominal.toLongOrNull() ?: 0L) > 0,
        )
        Secondary(stringResource(R.string.action_cancel), onBack)
    }
}
