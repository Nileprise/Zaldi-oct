package com.example.features.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwitchAccount
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.network.DispatchEndpointTrace
import com.example.core.utils.GeoUtils
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.LedgerEntryEntity
import com.example.services.localstorage.ReviewEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.VehicleEntity
import com.example.services.location.GpsTelemetryState

enum class AdminAppTab(val route: String, val label: String) {
    DASHBOARD("admin_dashboard", "1. Dashboard"),
    BOOKINGS("admin_bookings", "2. Bookings"),
    DRIVERS_FLEET("admin_drivers", "3. Drivers & Fleet"),
    PRICING_MATCH("admin_pricing", "4. Pricing & Match"),
    PAYMENTS_REPORTS("admin_reports", "5. Payments & Reports")
}

/**
 * Standalone Zaldi Admin Application (`apps/admin/src/`).
 * Implements all Admin App components and pages separately from the Customer & Driver apps:
 * - Components: `Sidebar`, `Header`, `DashboardCard`, `MapPanel`, `DriverTable`, `CustomerTable`,
 *   `BookingTable`, `VehicleTable`, `PaymentTable`, `NotificationPanel`
 * - Pages: `Dashboard`, `Customers`, `Drivers`, `Vehicles`, `Bookings`, `LiveTracking`,
 *   `Payments`, `Pricing`, `ServiceAreas`, `Notifications`, `Reports`, `Complaints`, `Settings`
 */
@Composable
fun ZaldiStandaloneAdminAppScreen(
    currentTab: AdminAppTab,
    onSelectTab: (AdminAppTab) -> Unit,
    activeDriver: DriverProfileEntity?,
    allDrivers: List<DriverProfileEntity>,
    vehicles: List<VehicleEntity>,
    activeOrder: RideOrderEntity?,
    allOrders: List<RideOrderEntity>,
    ledgerEntries: List<LedgerEntryEntity>,
    reviews: List<ReviewEntity>,
    lastDispatchTrace: DispatchEndpointTrace?,
    simulateRaceLockConflict: Boolean,
    onToggleRaceLockConflict: (Boolean) -> Unit,
    gpsTelemetry: GpsTelemetryState,
    onBookCustomRide: (
        customerName: String,
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropoffAddress: String,
        dropoffLat: Double,
        dropoffLng: Double,
        cargoDetails: String,
        cargoWeightKg: Int,
        vehicleTier: String,
        guaranteedFare: Double,
        distanceKm: Double
    ) -> Unit,
    onCreateCustomDriver: (
        firstName: String,
        lastName: String,
        phoneNumber: String,
        vehicleMakeModel: String,
        plateNumber: String,
        capacityKg: Int,
        vehicleTier: String
    ) -> Unit,
    onSelectDriverSession: (String) -> Unit,
    onToggleDriverAvailability: (String) -> Unit,
    onUpdateDriverApproval: (driverId: String, status: String) -> Unit,
    onSubmitCustomerRating: (passengerName: String, rating: Int, comment: String, badgeTag: String) -> Unit,
    onConfirmGoOrAdvanceTrip: (RideOrderEntity) -> Unit,
    onPreviousTripStep: (RideOrderEntity?) -> Unit,
    onAdvanceTripById: (String) -> Unit,
    onQuickDispatchOrder: () -> Unit
) {
    var showCreateDriverDialog by remember { mutableStateOf(false) }
    var dFirstName by remember { mutableStateOf("Karthik") }
    var dLastName by remember { mutableStateOf("Rao") }
    var dPhone by remember { mutableStateOf("+91 98480-55120") }
    var dVehicleModel by remember { mutableStateOf("2025 Tata Ace EV Cargo") }
    var dPlate by remember { mutableStateOf("TS05-Z891") }
    var dCapacityKg by remember { mutableStateOf("1200") }
    var dTier by remember { mutableStateOf("ZALDI_VAN") }

    val onlineCount = remember(allDrivers) { allDrivers.count { it.isOnline } }
    val totalGmv = remember(allOrders) { allOrders.sumOf { it.totalPayout } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("standalone_admin_app_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Admin Header Card (`apps/admin/src/components/Header.jsx`)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Zaldi Admin App",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ZALDI ADMIN APPLICATION",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "apps/admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$onlineCount/${allDrivers.size} Drivers Online • ${allOrders.size} Trips • GMV ${GeoUtils.formatCurrency(totalGmv)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showCreateDriverDialog = true },
                            modifier = Modifier.testTag("admin_add_driver_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add Driver",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Driver", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onQuickDispatchOrder,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color(0xFF06281E)
                            ),
                            modifier = Modifier.testTag("admin_quick_dispatch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Dispatch Ping",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dispatch", fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Admin App 5-Section Sidebar/Tab Strip (`Sidebar.jsx`)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdminAppTab.entries.forEach { tab ->
                        FilterChip(
                            selected = currentTab == tab,
                            onClick = { onSelectTab(tab) },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (currentTab == tab) FontWeight.Black else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("admin_subtab_${tab.route}")
                        )
                    }
                }
            }
        }

        when (currentTab) {
            AdminAppTab.DASHBOARD -> {
                ZaldiWebAdminPanelSection(
                    allDrivers = allDrivers,
                    vehicles = vehicles,
                    allOrders = allOrders,
                    activeDriverId = activeDriver?.driverId,
                    onToggleDriverAvailability = onToggleDriverAvailability,
                    onSelectDriverSession = onSelectDriverSession,
                    onAdvanceTrip = onConfirmGoOrAdvanceTrip,
                    onAdvanceTripById = onAdvanceTripById,
                    onQuickDispatchOrder = onQuickDispatchOrder
                )
            }

            AdminAppTab.BOOKINGS -> {
                // Admin Bookings & Live Trip Control + Dispatch Endpoint Console
                if (activeOrder != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ACTIVE TRIP CONTROLLER • #${activeOrder.orderId.take(8)} (${activeOrder.status})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${activeOrder.passengerName}: ${activeOrder.pickupAddress} → ${activeOrder.dropoffAddress}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onPreviousTripStep(activeOrder) },
                                    modifier = Modifier.weight(0.35f)
                                ) {
                                    Text("← Prev Step", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onConfirmGoOrAdvanceTrip(activeOrder) },
                                    modifier = Modifier.weight(0.65f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = Color(0xFF06281E)
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Advance Trip State →", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                CustomerBookRideSection(
                    activeDriver = activeDriver,
                    allDrivers = allDrivers,
                    lastDispatchTrace = lastDispatchTrace,
                    simulateRaceLockConflict = simulateRaceLockConflict,
                    onToggleRaceLockConflict = onToggleRaceLockConflict,
                    onBookRideViaDispatchEndpoint = onBookCustomRide
                )
            }

            AdminAppTab.DRIVERS_FLEET -> {
                // Admin DriverTable.jsx & VehicleTable.jsx Management
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwitchAccount,
                                        contentDescription = "Driver Table",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Column {
                                    Text(
                                        text = "DRIVERTABLE.JSX & VEHICLETABLE.JSX (${allDrivers.size} DRIVERS)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Active Receiving Driver: ${activeDriver?.fullName ?: "Mateo Vance"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(onClick = { showCreateDriverDialog = true }) {
                                Text("+ Register Driver", fontWeight = FontWeight.Bold)
                            }
                        }

                        allDrivers.forEachIndexed { index, drv ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                            val drvVehicles = vehicles.filter { it.driverId == drv.driverId }
                            val isSelected = drv.driverId == activeDriver?.driverId

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${drv.fullName} • ${drv.phoneNumber} • ★ ${drv.rating}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "KYC: ${drv.kycStatus} • ${if (drv.isOnline) "ONLINE" else "OFFLINE"} • Vehicle: ${drvVehicles.firstOrNull()?.makeAndModel ?: "Assigned Fleet"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { onToggleDriverAvailability(drv.driverId) }) {
                                            Text(if (drv.isOnline) "Set Offline" else "Set Online")
                                        }
                                        Button(
                                            onClick = { onSelectDriverSession(drv.driverId) },
                                            enabled = !isSelected
                                        ) {
                                            Text(if (isSelected) "Active" else "Select")
                                        }
                                    }
                                }

                                // Admin Approval Status Pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "VERIFIED" to "Approve",
                                        "PENDING_DOCS" to "Pending",
                                        "REJECTED" to "Reject",
                                        "SUSPENDED" to "Suspend"
                                    ).forEach { (statusCode, statusLabel) ->
                                        FilterChip(
                                            selected = drv.kycStatus == statusCode,
                                            onClick = { onUpdateDriverApproval(drv.driverId, statusCode) },
                                            label = { Text(statusLabel) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminAppTab.PRICING_MATCH -> {
                ZaldiPlatformArchitectureSection(
                    allDrivers = allDrivers,
                    vehicles = vehicles,
                    allOrders = allOrders,
                    activeOrder = activeOrder,
                    gpsTelemetry = gpsTelemetry,
                    onUpdateDriverApproval = onUpdateDriverApproval,
                    onToggleDriverOnline = onToggleDriverAvailability,
                    onTriggerDispatchOffer = onQuickDispatchOrder,
                    onAdvanceTripLifecycle = onConfirmGoOrAdvanceTrip,
                    onSubmitCustomerRating = onSubmitCustomerRating
                )
            }

            AdminAppTab.PAYMENTS_REPORTS -> {
                // Admin PaymentTable.jsx, CustomerTable.jsx, Reports, Complaints & NotificationPanel.jsx
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "PaymentTable.jsx",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "PAYMENTTABLE.JSX & FINANCIAL REPORTS (${ledgerEntries.size} LEDGER ROWS)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                        }

                        ledgerEntries.take(6).forEachIndexed { idx, entry ->
                            if (idx > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${entry.title} • ${entry.paymentMethod}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${entry.routeSummary} • Sync: ${entry.syncStatus}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = GeoUtils.formatCurrency(entry.amount),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (entry.amount >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = "Complaints & Reviews",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "CUSTOMERTABLE.JSX • REVIEWS & COMPLAINTS (${reviews.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                        }

                        reviews.take(5).forEach { rev ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${rev.passengerName} • ${rev.rating}★ • ${rev.badgeTag}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = rev.comment,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = rev.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDriverDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDriverDialog = false },
            title = { Text("Admin: Register New Driver & Vehicle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dFirstName,
                            onValueChange = { dFirstName = it },
                            label = { Text("First Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dLastName,
                            onValueChange = { dLastName = it },
                            label = { Text("Last Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = dPhone,
                        onValueChange = { dPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dVehicleModel,
                        onValueChange = { dVehicleModel = it },
                        label = { Text("Vehicle Make & Model") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dPlate,
                            onValueChange = { dPlate = it },
                            label = { Text("Plate Number") },
                            singleLine = true,
                            modifier = Modifier.weight(0.55f)
                        )
                        OutlinedTextField(
                            value = dCapacityKg,
                            onValueChange = { dCapacityKg = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Capacity (kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.45f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateCustomDriver(
                            dFirstName,
                            dLastName,
                            dPhone,
                            dVehicleModel,
                            dPlate,
                            dCapacityKg.toIntOrNull() ?: 1200,
                            dTier
                        )
                        showCreateDriverDialog = false
                    }
                ) {
                    Text("Register Driver")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDriverDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
