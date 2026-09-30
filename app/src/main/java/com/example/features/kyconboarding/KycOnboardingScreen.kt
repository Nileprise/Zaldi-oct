package com.example.features.kyconboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.KycDocumentEntity

/**
 * KYC Onboarding & Document Scanning Center (`features/kyc_onboarding`).
 * Integrates zero-permission Android Photo Picker (`PickVisualMedia`), interactive OCR barcode
 * document scanner modal, and Admin Review state transitions persisted in Room SQLite.
 */
@Composable
fun KycOnboardingScreen(
    profile: DriverProfileEntity?,
    documents: List<KycDocumentEntity>,
    onUpdateDocument: (KycDocumentEntity, String, String, String) -> Unit,
    onSetOverallKycState: (String) -> Unit
) {
    var scanningDoc by remember { mutableStateOf<KycDocumentEntity?>(null) }
    var docNumberInput by remember { mutableStateOf("") }
    var expiryInput by remember { mutableStateOf("") }
    var targetStatusInput by remember { mutableStateOf("APPROVED") }

    // Zero-permission Android Photo Picker (Google Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val doc = scanningDoc ?: documents.firstOrNull()
        if (doc != null && uri != null) {
            onUpdateDocument(
                doc,
                doc.documentNumber,
                doc.expiryDate,
                "APPROVED"
            )
            scanningDoc = null
        }
    }

    val overallStatus = profile?.kycStatus ?: "VERIFIED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Overall KYC Compliance Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    when (overallStatus) {
                        "VERIFIED" -> MaterialTheme.colorScheme.secondary
                        "UNDER_REVIEW" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    },
                    MaterialTheme.shapes.large
                ),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                when (overallStatus) {
                                    "VERIFIED" -> MaterialTheme.colorScheme.secondaryContainer
                                    "UNDER_REVIEW" -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.errorContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (overallStatus) {
                                "VERIFIED" -> Icons.Default.VerifiedUser
                                "UNDER_REVIEW" -> Icons.Default.HourglassTop
                                else -> Icons.Default.Warning
                            },
                            contentDescription = "KYC Status Icon",
                            tint = when (overallStatus) {
                                "VERIFIED" -> MaterialTheme.colorScheme.secondary
                                "UNDER_REVIEW" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PARTNER COMPLIANCE: ${overallStatus.replace("_", " ")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = when (overallStatus) {
                                "VERIFIED" -> "All 4 mandatory credentials approved. Priority Geohash dispatch unlocked."
                                "UNDER_REVIEW" -> "Zaldi Admin Compliance team is reviewing uploaded scans (SLA < 15 min)."
                                else -> "Action Required: Upload missing driver credentials to unlock Online dispatch."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                // Interactive Admin Review State Simulator Switcher
                Text(
                    text = "ADMIN REVIEW STATE SIMULATOR (TEST ONBOARDING GATES)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "VERIFIED" to "Approved",
                        "UNDER_REVIEW" to "Admin Queue",
                        "PENDING_DOCS" to "Action Needed"
                    ).forEach { (stateKey, label) ->
                        FilterChip(
                            selected = overallStatus == stateKey,
                            onClick = { onSetOverallKycState(stateKey) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("kyc_state_chip_$stateKey")
                        )
                    }
                }
            }
        }

        // 2. Document Cards List
        documents.forEach { doc ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kyc_doc_card_${doc.docType}"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (doc.docType == "DRIVER_LICENSE") {
                                    Icons.Default.Badge
                                } else {
                                    Icons.Default.Description
                                },
                                contentDescription = doc.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = doc.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        StatusPill(status = doc.status)
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "DOCUMENT ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = doc.documentNumber,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "EXPIRY DATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = doc.expiryDate,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Text(
                        text = doc.adminReviewNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scanningDoc = doc
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("photo_picker_${doc.docType}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Pick Document Image",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Photo Picker")
                        }

                        Button(
                            onClick = {
                                scanningDoc = doc
                                docNumberInput = doc.documentNumber
                                expiryInput = doc.expiryDate
                                targetStatusInput = "APPROVED"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("scan_doc_${doc.docType}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color(0xFF0B0F17)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "OCR Scanner",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OCR Scan",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Optical Document Scanner & Admin Verification Modal
    if (scanningDoc != null) {
        val currentDoc = scanningDoc!!
        AlertDialog(
            onDismissRequest = { scanningDoc = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scanner",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Optical KYC Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = currentDoc.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = docNumberInput,
                        onValueChange = { docNumberInput = it },
                        label = { Text("Extracted Document Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = expiryInput,
                        onValueChange = { expiryInput = it },
                        label = { Text("Expiry Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Verification Outcome:",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("APPROVED", "UNDER_REVIEW").forEach { statusOption ->
                            FilterChip(
                                selected = targetStatusInput == statusOption,
                                onClick = { targetStatusInput = statusOption },
                                label = { Text(statusOption.replace("_", " ")) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDocument(
                            currentDoc,
                            docNumberInput.ifBlank { currentDoc.documentNumber },
                            expiryInput.ifBlank { currentDoc.expiryDate },
                            targetStatusInput
                        )
                        scanningDoc = null
                    },
                    modifier = Modifier.testTag("confirm_kyc_scan_button")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Save Scan")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Commit to SQLite")
                }
            },
            dismissButton = {
                TextButton(onClick = { scanningDoc = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bg, fg) = when (status) {
        "APPROVED" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "UNDER_REVIEW" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = status.replace("_", " "),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
