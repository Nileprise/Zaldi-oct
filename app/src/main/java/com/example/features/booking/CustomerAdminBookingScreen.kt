package com.example.features.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.network.DispatchEndpointTrace
import com.example.core.utils.GeoUtils
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.VehicleEntity
import com.example.services.location.GpsTelemetryState

/**
 * Customer 'Book Ride' Screen (`/api/internal/dispatch`) + Web Admin Panel (`features/booking`).
 * Allows customers to input pickup and drop-off locations to trigger `zaldi_ping` driver pings,
 * switch between registered drivers, confirm the go, or inspect the React + Tailwind Web Admin Panel.
 */
@Composable
fun CustomerAdminBookingScreen(
    portalMode: String,
    onSelectPortalMode: (String) -> Unit,
    activeDriver: DriverProfileEntity?,
    allDrivers: List<DriverProfileEntity>,
    vehicles: List<VehicleEntity>,
    activeOrder: RideOrderEntity?,
    allOrders: List<RideOrderEntity>,
    lastDispatchTrace: DispatchEndpointTrace?,
    simulateRaceLockConflict: Boolean,
    onToggleRaceLockConflict: (Boolean) -> Unit,
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
    gpsTelemetry: GpsTelemetryState,
    routeProgress: Float = 0.25f,
    onConfirmGoOrAdvanceTrip: (RideOrderEntity) -> Unit,
    onPreviousTripStep: (RideOrderEntity?) -> Unit,
    onNavigatePrevious: () -> Unit,
    onAdvanceTripById: (String) -> Unit,
    onQuickDispatchOrder: () -> Unit,
    onOpenNavigationTab: () -> Unit
) {
    // Custom Driver Creation Dialog State
    var showCreateDriverDialog by remember { mutableStateOf(false) }
    var dFirstName by remember { mutableStateOf("Karthik") }
    var dLastName by remember { mutableStateOf("Rao") }
    var dPhone by remember { mutableStateOf("+91 98480-55120") }
    var dVehicleModel by remember { mutableStateOf("2025 Tata Ace EV Cargo") }
    var dPlate by remember { mutableStateOf("TS05-Z891") }
    var dCapacityKg by remember { mutableStateOf("1200") }
    var dTier by remember { mutableStateOf("ZALDI_VAN") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Segmented 4-Way Fast Portal Switcher: 1. Customer App | Dispatch API | Admin Panel | 8-Step Match
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isCustomerApp = portalMode == "CUSTOMER_APP"
                val isBookRide = portalMode == "BOOK_RIDE"
                val isAdminPanel = portalMode == "ADMIN_PANEL"
                val isArchitecture = portalMode == "ARCHITECTURE"

                Surface(
                    color = if (isCustomerApp) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPortalMode("CUSTOMER_APP") }
                        .testTag("switch_mode_customer_app")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Customer",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCustomerApp) Color(0xFF0B0F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    color = if (isBookRide) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPortalMode("BOOK_RIDE") }
                        .testTag("switch_mode_book_ride")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dispatch API",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isBookRide) Color(0xFF0B0F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    color = if (isAdminPanel) MaterialTheme.colorScheme.secondary else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPortalMode("ADMIN_PANEL") }
                        .testTag("switch_mode_admin_panel")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin Panel",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isAdminPanel) Color(0xFF06281E) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    color = if (isArchitecture) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPortalMode("ARCHITECTURE") }
                        .testTag("switch_mode_architecture")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "8-Step Match",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isArchitecture) Color(0xFF0B0F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Express 1-Tap Fast Action Bar for Instant User Experience (<15ms response)
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Express 1-Tap Dispatch",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text(
                            text = "EXPRESS 1-TAP DISPATCH (<12ms LATENCY)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (activeOrder != null) {
                                "Active Trip #${activeOrder.orderId.take(6)} (${activeOrder.status}) • Tap right button to advance"
                            } else {
                                "Instant Book -> Match -> Ping Driver in a single tap"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (activeOrder == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onPreviousTripStep(null) },
                            modifier = Modifier.testTag("express_restore_previous_trip_button")
                        ) {
                            Text("← Prev Trip", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                onBookCustomRide(
                                    "Vikram Aditya (Express)",
                                    "Clock Tower Center, Nalgonda",
                                    17.0500,
                                    79.2667,
                                    "SLN Terminus, Gachibowli, Hyderabad",
                                    17.4401,
                                    78.3489,
                                    "Express Priority Load",
                                    250,
                                    "ZALDI_VAN",
                                    145.00,
                                    105.0
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color(0xFF06281E)
                            ),
                            modifier = Modifier.testTag("express_one_tap_book_button")
                        ) {
                            Text("⚡ 1-Tap Book", fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onPreviousTripStep(activeOrder) },
                            modifier = Modifier.testTag("express_previous_trip_step_button")
                        ) {
                            Text("← Prev", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onConfirmGoOrAdvanceTrip(activeOrder) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color(0xFF0B0F17)
                            ),
                            modifier = Modifier.testTag("express_advance_trip_button")
                        ) {
                            Text(
                                text = when (activeOrder.status) {
                                    "INCOMING" -> "⚡ Accept"
                                    "ACCEPTED" -> "⚡ Arrive"
                                    "ARRIVED_PICKUP" -> "⚡ Start"
                                    else -> "⚡ Complete"
                                },
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Driver "Confirm the Go" Live Order Control Banner (when a ride is accepted or incoming)
        if (activeOrder != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                    .testTag("driver_confirm_go_card"),
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
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Active Dispatch",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "DRIVER DISPATCH STATUS: ${activeOrder.status.replace("_", " ")}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = activeOrder.cargoDetails,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "${activeOrder.passengerName}: ${activeOrder.pickupAddress} → ${activeOrder.dropoffAddress}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val actionLabel = when (activeOrder.status) {
                        "INCOMING" -> "DRIVER: ACCEPT REQUEST & CONFIRM THE GO"
                        "ACCEPTED" -> "DRIVER: ARRIVED AT PICKUP & CONFIRM GOODS LOADED"
                        "ARRIVED_PICKUP" -> "DRIVER: VERIFY PIN (${activeOrder.riderPin}) & START ROUTE TO DROP-OFF"
                        "IN_PROGRESS" -> "DRIVER: COMPLETE TRIP & SETTLE ${GeoUtils.formatCurrency(activeOrder.totalPayout)}"
                        else -> "OPEN ACTIVE NAVIGATION"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPreviousTripStep(activeOrder) },
                            modifier = Modifier
                                .weight(0.24f)
                                .height(50.dp)
                                .testTag("previous_trip_step_button")
                        ) {
                            Text("← Prev", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onConfirmGoOrAdvanceTrip(activeOrder) },
                            modifier = Modifier
                                .weight(0.54f)
                                .height(50.dp)
                                .testTag("confirm_the_go_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color(0xFF06281E)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Confirm Go")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = actionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenNavigationTab,
                            modifier = Modifier
                                .weight(0.22f)
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Live Map")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Map")
                        }
                    }
                }
            }
        }

        when (portalMode) {
            "ADMIN_PANEL" -> {
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
            "ARCHITECTURE" -> {
                // Unified 4-Pillar Architecture, 8-Step Driver Matching Engine, Admin Pricing & Customer Lifecycle
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
            "BOOK_RIDE" -> {
                // Customer App 'Book Ride' Dispatch API Screen (integrating with POST /api/internal/dispatch)
                CustomerBookRideSection(
                    activeDriver = activeDriver,
                    allDrivers = allDrivers,
                    lastDispatchTrace = lastDispatchTrace,
                    simulateRaceLockConflict = simulateRaceLockConflict,
                    onToggleRaceLockConflict = onToggleRaceLockConflict,
                    onBookRideViaDispatchEndpoint = onBookCustomRide
                )
            }
            else -> {
                // Default #1: Complete Customer App (`apps/customer`)
                ZaldiCustomerAppSection(
                    activeDriver = activeDriver,
                    allDrivers = allDrivers,
                    vehicles = vehicles,
                    activeOrder = activeOrder,
                    allOrders = allOrders,
                    routeProgress = routeProgress,
                    lastDispatchTrace = lastDispatchTrace,
                    onBookCustomerRide = onBookCustomRide,
                    onConfirmGoOrAdvanceTrip = onConfirmGoOrAdvanceTrip,
                    onPreviousTripStep = onPreviousTripStep,
                    onSubmitCustomerRating = onSubmitCustomerRating
                )
            }
        }

        // Active Driver Session & Custom Driver Switcher Card
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = "Custom Driver Switcher",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Column {
                            Text(
                                text = "RECEIVING DRIVER: ${activeDriver?.fullName?.uppercase() ?: "MATEO VANCE"}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Redis GEO Member: ${activeDriver?.driverId?.take(12) ?: "d4f8a920"}… • ${if (activeDriver?.isOnline == true) "ONLINE (5km Radius)" else "AUTO-ONLINE ON PING"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showCreateDriverDialog = true },
                        modifier = Modifier.testTag("create_custom_driver_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF0B0F17)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Custom Driver",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Driver", fontWeight = FontWeight.Bold)
                    }
                }

                if (allDrivers.size > 1) {
                    Text(
                        text = "SWITCH ACTIVE DRIVER RECEIVING PINGS (${allDrivers.size} DRIVERS IN SQLITE):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allDrivers.take(4).forEach { drv ->
                            val isCurrent = drv.driverId == activeDriver?.driverId
                            FilterChip(
                                selected = isCurrent,
                                onClick = { onSelectDriverSession(drv.driverId) },
                                label = {
                                    Text("${drv.firstName} (${drv.phoneNumber.takeLast(4)})")
                                },
                                modifier = Modifier.testTag("driver_chip_${drv.driverId.take(6)}")
                            )
                        }
                    }
                }
            }
        }

        // Live Dispatch Queue Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Dispatch Queue",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "LIVE DISPATCH QUEUE (${allOrders.size} TRIPS IN SQLITE)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                allOrders.take(5).forEachIndexed { idx, order ->
                    if (idx > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Cargo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${order.passengerName} • ${order.cargoDetails} (${order.cargoWeightKg}kg)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${order.pickupAddress} → ${order.dropoffAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Trip UUID: ${order.orderId.take(8)}… • Status: ${order.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = GeoUtils.formatCurrency(order.totalPayout),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${order.distanceKm} km",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Custom Driver Dialog
    if (showCreateDriverDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDriverDialog = false },
            title = { Text("Register Custom Driver & Vehicle") },
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
                        label = { Text("Phone Number (Unique)") },
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
                    },
                    modifier = Modifier.testTag("confirm_create_driver_button")
                ) {
                    Text("Create & Set Active")
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
