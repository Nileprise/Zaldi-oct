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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.utils.GeoUtils
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.VehicleEntity
import com.example.services.location.GpsTelemetryState
import java.util.Locale
import kotlin.math.roundToInt

data class DriverMatchingCandidateEval(
    val driver: DriverProfileEntity,
    val vehicle: VehicleEntity?,
    val isOnlinePass: Boolean,
    val isApprovedPass: Boolean,
    val isVehicleMatchPass: Boolean,
    val isAvailablePass: Boolean,
    val distanceKm: Double,
    val etaMinutes: Int,
    val tripStatusLabel: String,
    val isEligibleForOffer: Boolean
)

/**
 * Interactive 4-Pillar Platform Architecture, 8-Step Driver Matching Algorithm (`backend/src/modules/matching`),
 * Dynamic Pricing Matrix (`Bike`, `Auto`, `Truck`), Driver Approval Workflow (`Pending`, `Approved`, `Rejected`, `Suspended`),
 * and End-to-End Customer Lifecycle (`Pickup -> Drop -> Select Vehicle -> Matching -> Live Tracking -> Payment -> Rating`).
 */
@Composable
fun ZaldiPlatformArchitectureSection(
    allDrivers: List<DriverProfileEntity>,
    vehicles: List<VehicleEntity>,
    allOrders: List<RideOrderEntity>,
    activeOrder: RideOrderEntity?,
    gpsTelemetry: GpsTelemetryState,
    onUpdateDriverApproval: (driverId: String, status: String) -> Unit,
    onToggleDriverOnline: (driverId: String) -> Unit,
    onTriggerDispatchOffer: () -> Unit,
    onAdvanceTripLifecycle: (RideOrderEntity) -> Unit,
    onSubmitCustomerRating: (passengerName: String, rating: Int, comment: String, badgeTag: String) -> Unit
) {
    var selectedPillar by rememberSaveable { mutableStateOf("MATCHING_ENGINE") }
    var selectedVehicleClass by rememberSaveable { mutableStateOf("TRUCK") } // BIKE, AUTO, TRUCK
    var driverStatusFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, PENDING_DOCS, VERIFIED, REJECTED, SUSPENDED

    // Dynamic Pricing Matrix State (Admin -> Pricing -> Bike, Auto, Truck)
    var bikeBaseFare by rememberSaveable { mutableStateOf("25.0") }
    var bikePerKm by rememberSaveable { mutableStateOf("8.5") }
    var autoBaseFare by rememberSaveable { mutableStateOf("45.0") }
    var autoPerKm by rememberSaveable { mutableStateOf("14.0") }
    var truckBaseFare by rememberSaveable { mutableStateOf("120.0") }
    var truckPerKm by rememberSaveable { mutableStateOf("28.0") }
    var sampleDistanceKm by rememberSaveable { mutableStateOf("12.4") }

    // Customer Payment & Rating Step State
    var ratingCustomerName by rememberSaveable { mutableStateOf("Vikram Aditya") }
    var selectedStars by rememberSaveable { mutableIntStateOf(5) }
    var ratingComment by rememberSaveable {
        mutableStateOf("Fast driver matching, accurate live GPS marker & ETA on map!")
    }
    var selectedPaymentMethod by rememberSaveable { mutableStateOf("UPI / Zaldi Pay") }

    val pickupLat = activeOrder?.pickupLat ?: 17.0500
    val pickupLng = activeOrder?.pickupLng ?: 79.2667

    val activeBusyDriverIds = remember(allOrders) {
        allOrders.filter { it.status in listOf("ACCEPTED", "ARRIVED_PICKUP", "IN_PROGRESS") }
            .map { it.driverId }
            .toSet()
    }

    // 8-Step Driver Matching Algorithm Evaluation across all drivers
    val matchingEvaluations = remember(allDrivers, vehicles, activeBusyDriverIds, selectedVehicleClass, pickupLat, pickupLng) {
        allDrivers.map { drv ->
            val drvVehicle = vehicles.firstOrNull { it.driverId == drv.driverId }
                ?: vehicles.firstOrNull { it.id == drv.activeVehicleId }
            val step1Online = drv.isOnline
            val step2Approved = drv.kycStatus == "VERIFIED" || drv.kycStatus == "APPROVED"
            val tierText = drvVehicle?.vehicleTier ?: "ZALDI_VAN"
            val step3VehicleMatch = when (selectedVehicleClass) {
                "BIKE" -> tierText.contains("BIKE") || tierText.contains("EV")
                "AUTO" -> tierText.contains("AUTO") || tierText.contains("PRIME") || tierText.contains("EV")
                else -> true // Truck / Van / XL
            }
            val step4Available = !activeBusyDriverIds.contains(drv.driverId)
            val distKm = ((GeoUtils.calculateDistanceKm(
                pickupLat,
                pickupLng,
                drv.currentLat.toDouble(),
                drv.currentLng.toDouble()
            )).coerceIn(0.4, 18.5) * 10.0).roundToInt() / 10.0
            val etaMins = (distKm * 2.4).roundToInt().coerceAtLeast(2)
            val step7TripStatus = if (step4Available) "IDLE (READY)" else "ON_ACTIVE_TRIP"
            val eligible = step1Online && step2Approved && step3VehicleMatch && step4Available

            DriverMatchingCandidateEval(
                driver = drv,
                vehicle = drvVehicle,
                isOnlinePass = step1Online,
                isApprovedPass = step2Approved,
                isVehicleMatchPass = step3VehicleMatch,
                isAvailablePass = step4Available,
                distanceKm = distKm,
                etaMinutes = etaMins,
                tripStatusLabel = step7TripStatus,
                isEligibleForOffer = eligible
            )
        }.sortedWith(
            compareByDescending<DriverMatchingCandidateEval> { it.isEligibleForOffer }
                .thenBy { it.distanceKm }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("zaldi_platform_architecture_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & 4-Pillar Sub-Module Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), MaterialTheme.shapes.large),
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
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = "Unified Zaldi Architecture",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text(
                            text = "ZALDI 4-PILLAR SYSTEM ARCHITECTURE & MATCHING ENGINE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Customer App • Driver App • Admin Dashboard • Backend Matching & Realtime GPS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "MATCHING_ENGINE" to "8-Step Driver Matching",
                        "CUSTOMER_FLOW" to "Customer 14-Step Lifecycle",
                        "ADMIN_MODULES" to "Admin Approval & Pricing",
                        "MONOREPO_TREE" to "Monorepo & Realtime GPS"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedPillar == key,
                            onClick = { selectedPillar = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("arch_pillar_$key")
                        )
                    }
                }
            }
        }

        when (selectedPillar) {
            "MATCHING_ENGINE" -> {
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BACKEND: backend/src/modules/matching/matching.algorithm.js",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Customer Booking -> Matching Service -> Nearby Drivers -> Filter 8 Rules -> ETA -> Offer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onTriggerDispatchOffer,
                                modifier = Modifier.testTag("matching_create_offer_button")
                            ) {
                                Text("Create Offer", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Step 3 Filter: Vehicle Type selector (Bike, Auto, Truck)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Vehicle Filter:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            listOf("BIKE" to "Bike", "AUTO" to "Auto", "TRUCK" to "Truck").forEach { (tier, label) ->
                                FilterChip(
                                    selected = selectedVehicleClass == tier,
                                    onClick = { selectedVehicleClass = tier },
                                    label = { Text(label) }
                                )
                            }
                        }

                        // 8 Criteria Legend
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "1. Driver Online  •  2. Driver Approved  •  3. Correct Vehicle Type ($selectedVehicleClass)  •  4. Driver Available\n" +
                                    "5. Distance from Pickup  •  6. Estimated Arrival Time (ETA)  •  7. Current Trip Status  •  8. Driver Acceptance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        matchingEvaluations.forEach { eval ->
                            Surface(
                                color = if (eval.isEligibleForOffer) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (eval.isEligibleForOffer) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${eval.driver.fullName} (${eval.vehicle?.makeAndModel ?: "Tata Ace EV"})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "5. Distance: ${eval.distanceKm} km  •  6. ETA: ${eval.etaMinutes} mins  •  7. Status: ${eval.tripStatusLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = if (eval.isEligibleForOffer) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.errorContainer
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (eval.isEligibleForOffer) "MATCHED • OFFER READY" else "FILTERED OUT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (eval.isEligibleForOffer) Color(0xFF06281E) else MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RuleBadge("1. Online", eval.isOnlinePass)
                                        RuleBadge("2. Approved (${eval.driver.kycStatus})", eval.isApprovedPass)
                                        RuleBadge("3. Vehicle", eval.isVehicleMatchPass)
                                        RuleBadge("4. Available", eval.isAvailablePass)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "CUSTOMER_FLOW" -> {
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
                            text = "CUSTOMER APP END-TO-END BOOKING, TRACKING, PAYMENT & RATING",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )

                        val currentLifecycleStep = when (activeOrder?.status) {
                            "INCOMING" -> "8. Driver Searching & Offer Sent (15s SLA)"
                            "ACCEPTED" -> "10. Trip Created & Live Tracking (Driver En Route)"
                            "ARRIVED_PICKUP" -> "11. Driver at Pickup Location (Verify PIN ${activeOrder.riderPin})"
                            "IN_PROGRESS" -> "12. Trip Started -> Live Route Tracking to Drop-off"
                            "COMPLETED" -> "13. Trip Completed -> Payment & 5-Star Rating"
                            else -> "1–7. Pickup -> Drop -> Vehicle (Bike/Auto/Truck) -> Fare -> Matching"
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Active Stage: $currentLifecycleStep",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pipeline: Pickup Location → Drop Location → Select Vehicle (Bike / Auto / Truck) → Calculate Distance & Fare → Find Nearby Drivers → Driver Matching → Driver Accepts → Trip Created → Live Tracking → Pickup → Trip Started → Trip Completed → Payment → Rating",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (activeOrder != null) {
                                    Button(
                                        onClick = { onAdvanceTripLifecycle(activeOrder) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Advance Trip Stage (${activeOrder.status})")
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // PaymentCard & Rating submission (apps/customer/src/pages/Payment & Rating)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Payment and Rating",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "CUSTOMER PAYMENT CARD & DRIVER RATING (modules/payments & ratings)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("UPI / Zaldi Pay", "Zaldi Wallet", "Cash at Drop").forEach { pm ->
                                FilterChip(
                                    selected = selectedPaymentMethod == pm,
                                    onClick = { selectedPaymentMethod = pm },
                                    label = { Text(pm) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = ratingCustomerName,
                            onValueChange = { ratingCustomerName = it },
                            label = { Text("Customer Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Driver Rating:", style = MaterialTheme.typography.labelLarge)
                            (1..5).forEach { star ->
                                FilterChip(
                                    selected = selectedStars == star,
                                    onClick = { selectedStars = star },
                                    label = { Text("${star}★") }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = ratingComment,
                            onValueChange = { ratingComment = it },
                            label = { Text("Customer Trip Feedback & Rating Comment") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                onSubmitCustomerRating(
                                    ratingCustomerName,
                                    selectedStars,
                                    ratingComment,
                                    selectedPaymentMethod
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_customer_rating_button")
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Submit Rating")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Complete Payment ($selectedPaymentMethod) & Submit ${selectedStars}★ Rating")
                        }
                    }
                }
            }

            "ADMIN_MODULES" -> {
                // Admin Drivers Approval Workflow (Pending, Approved, Rejected, Suspended) + Pricing (Bike, Auto, Truck)
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
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Admin Driver Governance",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "ADMIN -> DRIVERS (PENDING / APPROVED / REJECTED / SUSPENDED)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Manage driver approval state to control eligibility in the 8-Step Matching Engine",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "ALL" to "All (${allDrivers.size})",
                                "PENDING_DOCS" to "Pending",
                                "VERIFIED" to "Approved",
                                "REJECTED" to "Rejected",
                                "SUSPENDED" to "Suspended"
                            ).forEach { (st, label) ->
                                FilterChip(
                                    selected = driverStatusFilter == st,
                                    onClick = { driverStatusFilter = st },
                                    label = { Text(label) }
                                )
                            }
                        }

                        val driversForAdmin = allDrivers.filter {
                            driverStatusFilter == "ALL" || it.kycStatus == driverStatusFilter
                        }

                        driversForAdmin.forEach { drv ->
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${drv.fullName} • ${drv.phoneNumber}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "KYC/Approval: ${drv.kycStatus} • Status: ${if (drv.isOnline) "ONLINE" else "OFFLINE"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onToggleDriverOnline(drv.driverId) }
                                        ) {
                                            Text(if (drv.isOnline) "Set Offline" else "Set Online")
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "PENDING_DOCS" to "Pending",
                                            "VERIFIED" to "Approve",
                                            "REJECTED" to "Reject",
                                            "SUSPENDED" to "Suspend"
                                        ).forEach { (statusKey, btnLabel) ->
                                            FilterChip(
                                                selected = drv.kycStatus == statusKey,
                                                onClick = { onUpdateDriverApproval(drv.driverId, statusKey) },
                                                label = { Text(btnLabel) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Admin -> Pricing (Bike, Auto, Truck)
                        Text(
                            text = "ADMIN -> PRICING MATRIX (BIKE / AUTO / TRUCK)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bikeBaseFare,
                                onValueChange = { bikeBaseFare = it },
                                label = { Text("Bike Base ($)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = autoBaseFare,
                                onValueChange = { autoBaseFare = it },
                                label = { Text("Auto Base ($)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = truckBaseFare,
                                onValueChange = { truckBaseFare = it },
                                label = { Text("Truck Base ($)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bikePerKm,
                                onValueChange = { bikePerKm = it },
                                label = { Text("Bike /km ($)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = autoPerKm,
                                onValueChange = { autoPerKm = it },
                                label = { Text("Auto /km ($)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = truckPerKm,
                                onValueChange = { truckPerKm = it },
                                label = { Text("Truck /km ($)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val distVal = sampleDistanceKm.toDoubleOrNull() ?: 12.4
                        val bikeEst = (bikeBaseFare.toDoubleOrNull() ?: 25.0) + distVal * (bikePerKm.toDoubleOrNull() ?: 8.5)
                        val autoEst = (autoBaseFare.toDoubleOrNull() ?: 45.0) + distVal * (autoPerKm.toDoubleOrNull() ?: 14.0)
                        val truckEst = (truckBaseFare.toDoubleOrNull() ?: 120.0) + distVal * (truckPerKm.toDoubleOrNull() ?: 28.0)

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Live Pricing Preview (%.1f km):  Bike = $%.2f  •  Auto = $%.2f  •  Truck = $%.2f",
                                    distVal,
                                    bikeEst,
                                    autoEst,
                                    truckEst
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            else -> {
                // Monorepo Structure & Realtime Location Telemetry Stream
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
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Realtime Location Pipeline",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "REALTIME GPS PIPELINE (Driver Phone -> Location Store -> Customer & Admin Map)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Surface(
                            color = Color(0xFF070A10),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "{\n" +
                                    "  \"source\": \"Driver Location Service (ZaldiLocationForegroundService)\",\n" +
                                    "  \"latitude\": ${String.format(Locale.US, "%.5f", gpsTelemetry.latitude)},\n" +
                                    "  \"longitude\": ${String.format(Locale.US, "%.5f", gpsTelemetry.longitude)},\n" +
                                    "  \"speed_kmh\": ${String.format(Locale.US, "%.1f", gpsTelemetry.speedKmh)},\n" +
                                    "  \"heading_deg\": ${String.format(Locale.US, "%.1f", gpsTelemetry.bearingDegrees)},\n" +
                                    "  \"geohash\": \"${gpsTelemetry.geohash}\",\n" +
                                    "  \"timestamp\": ${System.currentTimeMillis()},\n" +
                                    "  \"subscribers\": [\"apps/customer/TrackingMap.jsx\", \"apps/admin/MapPanel.jsx\"]\n" +
                                    "}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Monorepo Architecture",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "UNIFIED ZALDI MONOREPO MODULES",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• apps/customer/ -> PickupInput, DropInput, LocationSuggestions, VehicleSelector (Bike/Auto/Truck), FareCard, TrackingMap, DriverMarker, RouteLine, ETA, PaymentCard\n" +
                                    "• apps/driver/ -> DriverHeader, OnlineToggle, DriverMap, BookingRequest, TripCard, NavigationCard, EarningsCard, VehicleCard, DriverStatus\n" +
                                    "• apps/admin/ -> Dashboard, Customers, Drivers (Pending/Approved/Rejected/Suspended), Vehicles, Bookings, LiveTracking, Pricing (Bike/Auto/Truck), ServiceAreas, Payments, Notifications, Complaints, Reports, Settings\n" +
                                    "• backend/src/modules/ -> auth, customers, drivers, vehicles, bookings, trips, locations, matching (matching.algorithm.js), pricing, payments, notifications, ratings, complaints, reports\n" +
                                    "• shared/ -> constants (bookingStatus, vehicleTypes, userRoles, paymentStatus) & models (Customer, Driver, Vehicle, Booking, Trip, Location)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleBadge(label: String, passed: Boolean) {
    Surface(
        color = if (passed) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        },
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = label,
                tint = if (passed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (passed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
