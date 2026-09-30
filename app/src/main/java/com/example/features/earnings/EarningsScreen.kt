package com.example.features.earnings

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.utils.GeoUtils
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.LedgerEntryEntity

/**
 * Driver Earnings Ledger, Chart Visualizations & Instant Payout Engine (`features/earnings`).
 */
@Composable
fun EarningsScreen(
    profile: DriverProfileEntity?,
    ledgerEntries: List<LedgerEntryEntity>,
    pendingOfflineLogs: List<LedgerEntryEntity>,
    onTriggerInstantPayout: (String) -> Unit,
    onLogOfflineEarning: (title: String, routeSummary: String, amount: Double, paymentMethod: String) -> Unit,
    onSyncOfflineLogs: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedBank by remember { mutableStateOf("Chase Checking ••4821") }
    var showOfflineLogForm by remember { mutableStateOf(false) }
    var offlineTitleInput by remember { mutableStateOf("Offline Dead-Zone Cargo Delivery") }
    var offlineRouteInput by remember { mutableStateOf("Bayview Industrial Hub -> Dogpatch Depot") }
    var offlineAmountInput by remember { mutableStateOf("38.50") }
    var offlinePaymentMethod by remember { mutableStateOf("CASH_COLLECTED") }

    val filteredEntries = remember(ledgerEntries, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> ledgerEntries
            "OFFLINE_QUEUE" -> ledgerEntries.filter { it.isOfflineLog || it.syncStatus == "PENDING_OFFLINE_SYNC" }
            else -> ledgerEntries.filter { it.entryType == selectedFilter }
        }
    }

    val totalPendingOfflineAmount = remember(pendingOfflineLogs) {
        pendingOfflineLogs.sumOf { it.amount }
    }

    val totalGrossCredits = remember(ledgerEntries) {
        ledgerEntries.filter { it.amount > 0 }.sumOf { it.amount }
    }

    val weeklyBars = listOf(
        "Mon" to 164.0f,
        "Tue" to 212.5f,
        "Wed" to 188.0f,
        "Thu" to 245.0f,
        "Fri" to 290.0f,
        "Sat" to 338.0f,
        "Sun" to 274.0f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Available Wallet Balance & Instant Payout Trigger Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.large),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE PAYOUT BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = GeoUtils.formatCurrency(profile?.walletBalance ?: 0.0),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("wallet_balance_text")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet Balance",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Payout Destination Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Chase Checking ••4821",
                        "Zaldi FastCard ••9910"
                    ).forEach { bank ->
                        FilterChip(
                            selected = selectedBank == bank,
                            onClick = { selectedBank = bank },
                            label = { Text(bank) }
                        )
                    }
                }

                Button(
                    onClick = { onTriggerInstantPayout(selectedBank) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("instant_payout_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color(0xFF06281E)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = "Instant Payout")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INSTANT PAYOUT TO ${selectedBank.uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 1B. Room SQLite Offline Earnings Logs & Sync Queue Card
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Room Offline Earnings DB",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "ROOM SQLITE OFFLINE EARNINGS LOGS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${pendingOfflineLogs.size} Pending Sync (${GeoUtils.formatCurrency(totalPendingOfflineAmount)}) • Table: ledger_entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showOfflineLogForm = !showOfflineLogForm },
                        modifier = Modifier.testTag("toggle_offline_earning_form_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log Offline Earning",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showOfflineLogForm) "Close" else "Log Offline")
                    }
                }

                if (showOfflineLogForm) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "INSERT OFFLINE EARNINGS LOG INTO ROOM SQLITE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = offlineTitleInput,
                                onValueChange = { offlineTitleInput = it },
                                label = { Text("Offline Trip / Delivery Title") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("offline_earning_title_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = offlineRouteInput,
                                    onValueChange = { offlineRouteInput = it },
                                    label = { Text("Route Corridor Summary") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.65f)
                                )
                                OutlinedTextField(
                                    value = offlineAmountInput,
                                    onValueChange = { offlineAmountInput = it },
                                    label = { Text("Amount ($)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(0.35f)
                                        .testTag("offline_earning_amount_input")
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("CASH_COLLECTED", "OFFLINE_VOUCHER", "ZALDI_WALLET").forEach { method ->
                                    FilterChip(
                                        selected = offlinePaymentMethod == method,
                                        onClick = { offlinePaymentMethod = method },
                                        label = { Text(method.replace("_", " ")) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val amt = offlineAmountInput.toDoubleOrNull() ?: 38.50
                                    onLogOfflineEarning(
                                        offlineTitleInput,
                                        offlineRouteInput,
                                        amt,
                                        offlinePaymentMethod
                                    )
                                    showOfflineLogForm = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_offline_earning_button")
                            ) {
                                Text("Save Offline Earning to Room Database")
                            }
                        }
                    }
                }

                Button(
                    onClick = onSyncOfflineLogs,
                    enabled = pendingOfflineLogs.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_offline_earnings_button")
                ) {
                    Icon(
                        imageVector = if (pendingOfflineLogs.isEmpty()) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = "Sync Offline Earnings"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pendingOfflineLogs.isEmpty()) {
                            "ALL OFFLINE EARNINGS LOGS SYNCED"
                        } else {
                            "SYNC ${pendingOfflineLogs.size} OFFLINE EARNINGS LOGS TO CLOUD"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Weekly Earnings Visualization Chart (`features/earnings`)
        val barPrimary = MaterialTheme.colorScheme.primary
        val barSecondary = MaterialTheme.colorScheme.secondary

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Weekly Chart",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "WEEKLY DISPATCH REVENUE BREAKDOWN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Base Fare (Emerald) + Surge & Tips (Amber)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = GeoUtils.formatCurrency(totalGrossCredits),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                }

                // Custom Compose Canvas Bar Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .testTag("earnings_bar_chart")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = 360f
                        val count = weeklyBars.size
                        val slotWidth = size.width / count
                        val barWidth = (slotWidth * 0.48f).coerceAtLeast(16f)

                        weeklyBars.forEachIndexed { idx, (_, amount) ->
                            val totalBarHeight = (amount / maxVal).coerceIn(0.15f, 1f) * size.height
                            val surgeHeight = totalBarHeight * 0.32f
                            val baseHeight = totalBarHeight - surgeHeight
                            val x = idx * slotWidth + (slotWidth - barWidth) / 2f

                            // Base Fare segment
                            drawRoundRect(
                                color = barSecondary,
                                topLeft = Offset(x, size.height - baseHeight),
                                size = Size(barWidth, baseHeight),
                                cornerRadius = CornerRadius(8f, 8f)
                            )

                            // Surge + Tip top segment
                            drawRoundRect(
                                color = barPrimary,
                                topLeft = Offset(x, size.height - totalBarHeight),
                                size = Size(barWidth, surgeHeight + 4f),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }
                    }
                }

                // Day labels row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weeklyBars.forEach { (day, amt) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${amt.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. SQLite Financial Ledger Filter & Entries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All (${ledgerEntries.size})",
                "OFFLINE_QUEUE" to "Offline Logs",
                "TRIP_EARNING" to "Trips",
                "SURGE_BONUS" to "Surge",
                "INSTANT_PAYOUT" to "Payouts"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label) },
                    modifier = Modifier.testTag("ledger_filter_$key")
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SQLITE TRANSACTION LEDGER (${filteredEntries.size} RECORDS)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                filteredEntries.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    val isCredit = item.amount >= 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCredit) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.entryType) {
                                        "INSTANT_PAYOUT" -> Icons.Default.AccountBalance
                                        "SURGE_BONUS" -> Icons.Default.Bolt
                                        else -> if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                                    },
                                    contentDescription = item.entryType,
                                    tint = if (isCredit) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Ref: ${item.referenceCode} • ${item.dayLabel} • ${item.settlementStatus} (${item.syncStatus})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.isOfflineLog) {
                                    Text(
                                        text = "Route: ${item.routeSummary} • Method: ${item.paymentMethod}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = (if (isCredit) "+" else "-") + GeoUtils.formatCurrency(kotlin.math.abs(item.amount)),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCredit) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bal: ${GeoUtils.formatCurrency(item.balanceAfter)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
