package com.example.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.common.DateTimeUtils
import com.example.core.common.LocalAppStrings
import com.example.core.model.DriveBackupEntry
import com.example.core.model.DriveBackupSchedule
import com.example.core.model.UserPreferences
import com.example.ui.components.AppSwitch
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.PassphraseDialog
import com.example.ui.components.PremiumCard
import com.example.ui.theme.ExpenseRed

@Composable
fun GoogleDriveBackupCard(
    prefs: UserPreferences,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onSwitchAccount: () -> Unit,
    onDisconnect: () -> Unit,
    onBackUpNowClick: () -> Unit,
    onOpenHistoryClick: () -> Unit,
    onScheduleChange: (DriveBackupSchedule) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isConnected = !prefs.driveAccountEmail.isNullOrBlank()

    PremiumCard(modifier = modifier.testTag("google_drive_backup_card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                icon = if (isConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload,
                tintColor = MaterialTheme.colorScheme.primary,
                size = 40.dp,
                contentDescription = strings.googleDriveBackup
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.googleDriveBackup,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = strings.googleDriveSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isBusy) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isConnected) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.notConnected,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConnect,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("drive_connect_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(strings.connectGoogleDrive)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drive_connected_account_card")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = strings.connectedAs,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!prefs.driveAccountName.isNullOrBlank()) {
                        Text(
                            text = prefs.driveAccountName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = prefs.driveAccountEmail.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onSwitchAccount,
                            enabled = !isBusy,
                            modifier = Modifier.testTag("drive_switch_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.switchAccount)
                        }
                        TextButton(
                            onClick = onDisconnect,
                            enabled = !isBusy,
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed),
                            modifier = Modifier.testTag("drive_disconnect_button")
                        ) {
                            Text(strings.disconnect)
                        }
                    }
                }
            }

            prefs.lastDriveBackupAt?.let { lastAt ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drive_last_backup_card")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = DateTimeUtils.formatDateTime(lastAt, prefs.effectiveLanguageTag, prefs.numberFormat),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (prefs.lastDriveBackupStatus.isNotBlank()) {
                            Text(
                                text = prefs.lastDriveBackupStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBackUpNowClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("drive_backup_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.backUpNow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenHistoryClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("drive_restore_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.restoreFromDrive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = strings.autoBackupSchedule,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DriveBackupSchedule.entries.forEachIndexed { idx, schedule ->
                    val label = when (schedule) {
                        DriveBackupSchedule.MANUAL_ONLY -> strings.scheduleManualOnly
                        DriveBackupSchedule.DAILY -> strings.scheduleDaily
                        DriveBackupSchedule.WEEKLY -> strings.scheduleWeekly
                    }
                    SegmentedButton(
                        selected = prefs.driveBackupSchedule == schedule,
                        onClick = { onScheduleChange(schedule) },
                        shape = SegmentedButtonDefaults.itemShape(idx, DriveBackupSchedule.entries.size),
                        modifier = Modifier.testTag("drive_schedule_${schedule.name.lowercase()}")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun DriveUploadOptionsDialog(
    onDismiss: () -> Unit,
    onConfirmBackup: (passphrase: String?) -> Unit
) {
    val strings = LocalAppStrings.current
    var encryptWithPassphrase by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    val isValid = !encryptWithPassphrase || passphrase.length >= 4

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.googleDriveBackup) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.cloudBackupDialogSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.encryptWithPassphrase,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    AppSwitch(
                        checked = encryptWithPassphrase,
                        onCheckedChange = { encryptWithPassphrase = it },
                        modifier = Modifier.testTag("drive_encrypt_toggle")
                    )
                }
                if (encryptWithPassphrase) {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(strings.passphraseHint) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drive_encrypt_passphrase_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmBackup(if (encryptWithPassphrase) passphrase else null) },
                enabled = isValid,
                modifier = Modifier.testTag("drive_confirm_backup_button")
            ) {
                Text(strings.backUpNow)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun DriveBackupHistoryDialog(
    backups: List<DriveBackupEntry>,
    isBusy: Boolean,
    prefs: UserPreferences,
    onDismiss: () -> Unit,
    onRestore: (DriveBackupEntry) -> Unit,
    onDelete: (DriveBackupEntry) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.driveHistoryTitle) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isBusy) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!isBusy && backups.isEmpty()) {
                    Text(
                        text = strings.noDriveBackups,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(backups, key = { it.fileId }) { entry ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("drive_backup_entry_${entry.fileId}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = DateTimeUtils.formatDateTime(
                                                entry.modifiedTimeMillis,
                                                prefs.effectiveLanguageTag,
                                                prefs.numberFormat
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (entry.isEncrypted) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lock,
                                                contentDescription = strings.encryptedLabel,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    val metaParts = buildList {
                                        if (entry.sizeBytes > 0L) {
                                            add(Formatter.formatShortFileSize(context, entry.sizeBytes))
                                        }
                                        entry.transactionCount?.let {
                                            add(strings.transactionsCount(it, prefs.numberFormat))
                                        }
                                    }
                                    if (metaParts.isNotEmpty()) {
                                        Text(
                                            text = metaParts.joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { onRestore(entry) },
                                            enabled = !isBusy,
                                            modifier = Modifier.testTag("drive_restore_entry_${entry.fileId}")
                                        ) {
                                            Text(strings.restoreFromDrive)
                                        }
                                        IconButton(
                                            onClick = { onDelete(entry) },
                                            enabled = !isBusy,
                                            modifier = Modifier.testTag("drive_delete_entry_${entry.fileId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = strings.delete,
                                                tint = ExpenseRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun DriveDecryptPassphraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    PassphraseDialog(
        title = strings.passphraseTitle,
        hint = strings.passphraseHint,
        confirmText = strings.confirmAction,
        cancelText = strings.cancel,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
