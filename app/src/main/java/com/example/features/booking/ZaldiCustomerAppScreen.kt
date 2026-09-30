package com.example.features.booking

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.network.DispatchEndpointTrace
import com.example.core.utils.GeoUtils
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.VehicleEntity
import java.util.Locale
import kotlin.math.roundToInt

enum class CustomerAppPage(val code: String, val label: String) {
    HOME_SEARCH("HOME_SEARCH", "1. Search & Map"),
    VEHICLE_FARE("VEHICLE_FARE", "2. Vehicle & Fare"),
    LIVE_TRACKING("LIVE_TRACKING", "3. Live Tracking"),
    PAYMENT("PAYMENT", "4. Payment & Rate"),
    RIDE_HISTORY("RIDE_HISTORY", "5. Ride History"),
    PROFILE_SUPPORT("PROFILE_SUPPORT", "6. Profile & OTP")
}

data class CustomerVehicleOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val capacityKg: Int,
    val baseFare: Double,
    val perKmRate: Double,
    val etaMin: Int,
    val tierCode: String
)

private val CUSTOMER_VEHICLE_OPTIONS = listOf(
    CustomerVehicleOption(
        id = "BIKE",
        name = "Zaldi Bike Express",
        subtitle = "Instant 2-Wheeler Courier & Documents",
        capacityKg = 20,
        baseFare = 12.0,
        perKmRate = 0.65,
        etaMin = 2,
        tierCode = "ZALDI_EV"
    ),
    CustomerVehicleOption(
        id = "AUTO",
        name = "Zaldi 3W Auto Cargo",
        subtitle = "Urban Retail Boxes & Medium Loads",
        capacityKg = 250,
        baseFare = 24.0,
        perKmRate = 0.95,
        etaMin = 4,
        tierCode = "ZALDI_PRIME"
    ),
    CustomerVehicleOption(
        id = "EV_VAN",
        name = "Zaldi EV Cargo Van",
        subtitle = "Cold-Chain & Fragile Pallets",
        capacityKg = 750,
        baseFare = 42.0,
        perKmRate = 1.25,
        etaMin = 5,
        tierCode = "ZALDI_EV"
    ),
    CustomerVehicleOption(
        id = "TRUCK",
        name = "Zaldi Mini Truck XL",
        subtitle = "Intercity Hub & Heavy Freight",
        capacityKg = 1500,
        baseFare = 65.0,
        perKmRate = 1.65,
        etaMin = 7,
        tierCode = "ZALDI_VAN"
    )
)

data class LocationPresetItem(
    val label: String,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffAddress: String,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val distanceKm: Double,
    val defaultCargo: String
)

private val LOCATION_SUGGESTIONS = listOf(
    LocationPresetItem(
        label = "Nalgonda → Hyderabad (105 km)",
        pickupAddress = "Clock Tower Center, Nalgonda",
        pickupLat = 17.0500,
        pickupLng = 79.2667,
        dropoffAddress = "SLN Terminus, Gachibowli, Hyderabad",
        dropoffLat = 17.4401,
        dropoffLng = 78.3489,
        distanceKm = 105.0,
        defaultCargo = "Fragile Load"
    ),
    LocationPresetItem(
        label = "Hitech City → RGIA Airport (32 km)",
        pickupAddress = "Cyber Towers, Hitech City, Hyderabad",
        pickupLat = 17.4504,
        pickupLng = 78.3808,
        dropoffAddress = "RGIA Cargo Terminal, Shamshabad",
        dropoffLat = 17.2403,
        dropoffLng = 78.4294,
        distanceKm = 32.4,
        defaultCargo = "Express Air Parcel"
    ),
    LocationPresetItem(
        label = "SF Mission → Biotech Hub (6.8 km)",
        pickupAddress = "450 Mission St, SF Transit Tower",
        pickupLat = 37.7897,
        pickupLng = -122.4014,
        dropoffAddress = "1600 Owens St, Mission Bay Hub",
        dropoffLat = 37.7675,
        dropoffLng = -122.3921,
        distanceKm = 6.8,
        defaultCargo = "Cold Chain"
    ),
    LocationPresetItem(
        label = "Banjara Hills → Secunderabad (11 km)",
        pickupAddress = "Road No. 12, Banjara Hills, Hyderabad",
        pickupLat = 17.4156,
        pickupLng = 78.4347,
        dropoffAddress = "Secunderabad Railway Parcel Hub",
        dropoffLat = 17.4399,
        dropoffLng = 78.4983,
        distanceKm = 11.2,
        defaultCargo = "Commercial Goods"
    )
)

/**
 * Complete Zaldi Customer Application (`apps/customer/src/`).
 * Implements all Customer App components and pages:
 * - Components: `AppHeader`, `SearchLocation`, `PickupInput`, `DropInput`, `LocationSuggestions`,
 *   `MapView`, `VehicleSelector`, `FareCard`, `DriverCard`, `BookingCard`, `TrackingMap`,
 *   `DriverMarker`, `RouteLine`, `ETA`, `PaymentCard`
 * - Pages: `Home`, `LocationSearch`, `Booking`, `DriverSearching`, `DriverAssigned`,
 *   `LiveTracking`, `Payment`, `RideHistory`, `Profile`, `Notifications`, `Support`, `Login/OTP`
 */
@Composable
fun ZaldiStandaloneCustomerAppScreen(
    currentPage: CustomerAppPage,
    onSelectPage: (CustomerAppPage) -> Unit,
    activeDriver: DriverProfileEntity?,
    allDrivers: List<DriverProfileEntity>,
    vehicles: List<VehicleEntity>,
    activeOrder: RideOrderEntity?,
    allOrders: List<RideOrderEntity>,
    routeProgress: Float,
    lastDispatchTrace: DispatchEndpointTrace?,
    onBookCustomerRide: (
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
    onConfirmGoOrAdvanceTrip: (RideOrderEntity) -> Unit,
    onPreviousTripStep: (RideOrderEntity?) -> Unit,
    onSubmitCustomerRating: (passengerName: String, rating: Int, comment: String, badgeTag: String) -> Unit,
    onSwitchToDriverApp: () -> Unit,
    onSwitchToAdminApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("standalone_customer_app_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Standalone Customer App Cross-App Live Sync Bar (when an order is active)
        if (activeOrder != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIVE BOOKING #${activeOrder.orderId.take(6)} • ${activeOrder.status.replace("_", " ")}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "OTP PIN: ${activeOrder.riderPin} • Synced with Driver App & Admin App via SQLite",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onSwitchToDriverApp,
                            modifier = Modifier.testTag("customer_jump_to_driver_app_button")
                        ) {
                            Text("Open Driver App →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        ZaldiCustomerAppSection(
            activeDriver = activeDriver,
            allDrivers = allDrivers,
            vehicles = vehicles,
            activeOrder = activeOrder,
            allOrders = allOrders,
            routeProgress = routeProgress,
            lastDispatchTrace = lastDispatchTrace,
            onBookCustomerRide = onBookCustomerRide,
            onConfirmGoOrAdvanceTrip = onConfirmGoOrAdvanceTrip,
            onPreviousTripStep = onPreviousTripStep,
            onSubmitCustomerRating = onSubmitCustomerRating,
            externalPage = currentPage,
            onExternalPageChange = onSelectPage
        )
    }
}

@Composable
fun ZaldiCustomerAppSection(
    activeDriver: DriverProfileEntity?,
    allDrivers: List<DriverProfileEntity>,
    vehicles: List<VehicleEntity>,
    activeOrder: RideOrderEntity?,
    allOrders: List<RideOrderEntity>,
    routeProgress: Float,
    lastDispatchTrace: DispatchEndpointTrace?,
    onBookCustomerRide: (
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
    onConfirmGoOrAdvanceTrip: (RideOrderEntity) -> Unit,
    onPreviousTripStep: (RideOrderEntity?) -> Unit,
    onSubmitCustomerRating: (passengerName: String, rating: Int, comment: String, badgeTag: String) -> Unit,
    externalPage: CustomerAppPage? = null,
    onExternalPageChange: ((CustomerAppPage) -> Unit)? = null
) {
    var internalPage by rememberSaveable { mutableStateOf(CustomerAppPage.HOME_SEARCH) }
    var pageHistory by remember { mutableStateOf(listOf<CustomerAppPage>()) }
    val currentPage = externalPage ?: internalPage

    val navigateToCustomerPage: (CustomerAppPage) -> Unit = { target ->
        if (target != currentPage) {
            pageHistory = (pageHistory + currentPage).takeLast(10)
            internalPage = target
            onExternalPageChange?.invoke(target)
        }
    }

    val navigatePreviousCustomerPage: () -> Unit = {
        if (pageHistory.isNotEmpty()) {
            val prev = pageHistory.last()
            pageHistory = pageHistory.dropLast(1)
            internalPage = prev
            onExternalPageChange?.invoke(prev)
        } else if (currentPage.ordinal > 0) {
            val prev = CustomerAppPage.entries[currentPage.ordinal - 1]
            internalPage = prev
            onExternalPageChange?.invoke(prev)
        }
    }

    // Customer State (`store/authStore`, `store/locationStore`, `store/bookingStore`)
    var customerName by rememberSaveable { mutableStateOf("Vikram Aditya") }
    var customerPhone by rememberSaveable { mutableStateOf("+91 98480-11223") }
    var customerOtpVerified by rememberSaveable { mutableStateOf(true) }
    var otpInput by rememberSaveable { mutableStateOf("482910") }

    var pickupAddress by rememberSaveable { mutableStateOf("Clock Tower Center, Nalgonda") }
    var pickupLat by rememberSaveable { mutableStateOf(17.0500) }
    var pickupLng by rememberSaveable { mutableStateOf(79.2667) }

    var dropoffAddress by rememberSaveable { mutableStateOf("SLN Terminus, Gachibowli, Hyderabad") }
    var dropoffLat by rememberSaveable { mutableStateOf(17.4401) }
    var dropoffLng by rememberSaveable { mutableStateOf(78.3489) }
    var distanceKm by rememberSaveable { mutableStateOf(105.0) }

    var selectedVehicleId by rememberSaveable { mutableStateOf("EV_VAN") }
    var cargoType by rememberSaveable { mutableStateOf("Fragile Load") }
    var cargoWeightKg by rememberSaveable { mutableIntStateOf(320) }

    var selectedPaymentMethod by rememberSaveable { mutableStateOf("UPI / Zaldi Pay") }
    var selectedRatingStars by rememberSaveable { mutableIntStateOf(5) }
    var ratingFeedback by rememberSaveable { mutableStateOf("Fast pickup, accurate live GPS tracking & safe delivery!") }
    var paymentCompletedBanner by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedVehicle = remember(selectedVehicleId) {
        CUSTOMER_VEHICLE_OPTIONS.firstOrNull { it.id == selectedVehicleId } ?: CUSTOMER_VEHICLE_OPTIONS[2]
    }

    val calculatedFare = remember(distanceKm, selectedVehicle) {
        val raw = selectedVehicle.baseFare + (distanceKm * selectedVehicle.perKmRate)
        (raw * 100.0).roundToInt() / 100.0
    }

    val onlineDrivers = remember(allDrivers) { allDrivers.filter { it.isOnline } }
    val assignedDriver = activeDriver ?: allDrivers.firstOrNull()
    val assignedVehicle = remember(vehicles, assignedDriver?.driverId) {
        vehicles.firstOrNull { it.driverId == assignedDriver?.driverId } ?: vehicles.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("zaldi_customer_app_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. AppHeader.jsx — Customer App Top Bar with Quick Page Navigation & Previous Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (pageHistory.isNotEmpty() || currentPage != CustomerAppPage.HOME_SEARCH) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { navigatePreviousCustomerPage() }
                                    .testTag("customer_app_prev_page_button")
                            ) {
                                Text(
                                    text = "← Prev",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Zaldi Customer App",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ZALDI CUSTOMER APP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "apps/customer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$customerName • ★ 4.97 • ${onlineDrivers.size} Drivers Nearby",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Active Trip Live Status Pill
                    if (activeOrder != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clickable { navigateToCustomerPage(CustomerAppPage.LIVE_TRACKING) }
                                .testTag("customer_active_trip_pill")
                        ) {
                            Text(
                                text = "LIVE: ${activeOrder.status.replace("_", " ")} →",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Customer App 6-Page Stepper Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomerAppPage.entries.forEach { page ->
                        FilterChip(
                            selected = currentPage == page,
                            onClick = { navigateToCustomerPage(page) },
                            label = {
                                Text(
                                    text = page.label,
                                    fontWeight = if (currentPage == page) FontWeight.Black else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("customer_page_tab_${page.code}")
                        )
                    }
                }
            }
        }

        // Render Active Customer App Page (`pages/`)
        when (currentPage) {
            CustomerAppPage.HOME_SEARCH -> {
                CustomerHomeSearchPage(
                    pickupAddress = pickupAddress,
                    onPickupChange = { pickupAddress = it },
                    dropoffAddress = dropoffAddress,
                    onDropoffChange = { dropoffAddress = it },
                    distanceKm = distanceKm,
                    calculatedFare = calculatedFare,
                    selectedVehicle = selectedVehicle,
                    onlineDriversCount = onlineDrivers.size,
                    onSelectPreset = { preset ->
                        pickupAddress = preset.pickupAddress
                        pickupLat = preset.pickupLat
                        pickupLng = preset.pickupLng
                        dropoffAddress = preset.dropoffAddress
                        dropoffLat = preset.dropoffLat
                        dropoffLng = preset.dropoffLng
                        distanceKm = preset.distanceKm
                        cargoType = preset.defaultCargo
                    },
                    onSwapLocations = {
                        val tmpAddr = pickupAddress
                        val tmpLat = pickupLat
                        val tmpLng = pickupLng
                        pickupAddress = dropoffAddress
                        pickupLat = dropoffLat
                        pickupLng = dropoffLng
                        dropoffAddress = tmpAddr
                        dropoffLat = tmpLat
                        dropoffLng = tmpLng
                    },
                    onProceedToVehicleSelect = {
                        navigateToCustomerPage(CustomerAppPage.VEHICLE_FARE)
                    },
                    onInstantOneTapBook = {
                        onBookCustomerRide(
                            customerName,
                            pickupAddress,
                            pickupLat,
                            pickupLng,
                            dropoffAddress,
                            dropoffLat,
                            dropoffLng,
                            cargoType,
                            cargoWeightKg,
                            selectedVehicle.tierCode,
                            calculatedFare,
                            distanceKm
                        )
                        navigateToCustomerPage(CustomerAppPage.LIVE_TRACKING)
                    }
                )
            }

            CustomerAppPage.VEHICLE_FARE -> {
                CustomerVehicleFareBookingPage(
                    pickupAddress = pickupAddress,
                    dropoffAddress = dropoffAddress,
                    distanceKm = distanceKm,
                    selectedVehicle = selectedVehicle,
                    onSelectVehicle = { selectedVehicleId = it.id },
                    cargoType = cargoType,
                    onSelectCargoType = { cargoType = it },
                    cargoWeightKg = cargoWeightKg,
                    onWeightChange = { cargoWeightKg = it },
                    calculatedFare = calculatedFare,
                    onBackToSearch = { navigateToCustomerPage(CustomerAppPage.HOME_SEARCH) },
                    onConfirmBooking = {
                        onBookCustomerRide(
                            customerName,
                            pickupAddress,
                            pickupLat,
                            pickupLng,
                            dropoffAddress,
                            dropoffLat,
                            dropoffLng,
                            cargoType,
                            cargoWeightKg,
                            selectedVehicle.tierCode,
                            calculatedFare,
                            distanceKm
                        )
                        navigateToCustomerPage(CustomerAppPage.LIVE_TRACKING)
                    }
                )
            }

            CustomerAppPage.LIVE_TRACKING -> {
                CustomerLiveTrackingPage(
                    activeOrder = activeOrder,
                    latestOrder = allOrders.firstOrNull(),
                    assignedDriver = assignedDriver,
                    assignedVehicle = assignedVehicle,
                    routeProgress = routeProgress,
                    lastDispatchTrace = lastDispatchTrace,
                    onAdvanceTripStep = { order ->
                        onConfirmGoOrAdvanceTrip(order)
                        if (order.status == "IN_PROGRESS") {
                            navigateToCustomerPage(CustomerAppPage.PAYMENT)
                        }
                    },
                    onPreviousTripStep = onPreviousTripStep,
                    onGoToPayment = { navigateToCustomerPage(CustomerAppPage.PAYMENT) },
                    onBookAnotherRide = { navigateToCustomerPage(CustomerAppPage.HOME_SEARCH) }
                )
            }

            CustomerAppPage.PAYMENT -> {
                val targetOrder = activeOrder ?: allOrders.firstOrNull()
                CustomerPaymentAndRatingPage(
                    order = targetOrder,
                    calculatedFare = targetOrder?.totalPayout ?: calculatedFare,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onSelectPaymentMethod = { selectedPaymentMethod = it },
                    selectedRatingStars = selectedRatingStars,
                    onSelectRatingStars = { selectedRatingStars = it },
                    ratingFeedback = ratingFeedback,
                    onRatingFeedbackChange = { ratingFeedback = it },
                    paymentCompletedBanner = paymentCompletedBanner,
                    onPayAndComplete = {
                        if (targetOrder != null && targetOrder.status != "COMPLETED") {
                            onConfirmGoOrAdvanceTrip(targetOrder)
                        }
                        onSubmitCustomerRating(
                            customerName,
                            selectedRatingStars,
                            ratingFeedback,
                            "Paid via $selectedPaymentMethod"
                        )
                        paymentCompletedBanner = "Payment of ${GeoUtils.formatCurrency(targetOrder?.totalPayout ?: calculatedFare)} via $selectedPaymentMethod settled & ${selectedRatingStars}★ rating saved!"
                    },
                    onViewRideHistory = { navigateToCustomerPage(CustomerAppPage.RIDE_HISTORY) }
                )
            }

            CustomerAppPage.RIDE_HISTORY -> {
                CustomerRideHistoryPage(
                    allOrders = allOrders,
                    onSelectOrderForTracking = {
                        navigateToCustomerPage(CustomerAppPage.LIVE_TRACKING)
                    },
                    onRebookOrder = { order ->
                        pickupAddress = order.pickupAddress
                        pickupLat = order.pickupLat
                        pickupLng = order.pickupLng
                        dropoffAddress = order.dropoffAddress
                        dropoffLat = order.dropoffLat
                        dropoffLng = order.dropoffLng
                        distanceKm = order.distanceKm
                        cargoType = order.cargoDetails
                        cargoWeightKg = order.cargoWeightKg
                        navigateToCustomerPage(CustomerAppPage.VEHICLE_FARE)
                    }
                )
            }

            CustomerAppPage.PROFILE_SUPPORT -> {
                CustomerProfileNotificationsSupportPage(
                    customerName = customerName,
                    onCustomerNameChange = { customerName = it },
                    customerPhone = customerPhone,
                    onCustomerPhoneChange = { customerPhone = it },
                    otpInput = otpInput,
                    onOtpInputChange = { otpInput = it },
                    customerOtpVerified = customerOtpVerified,
                    onVerifyOtp = { customerOtpVerified = true },
                    activeOrder = activeOrder,
                    allOrdersCount = allOrders.size
                )
            }
        }
    }
}

@Composable
private fun CustomerHomeSearchPage(
    pickupAddress: String,
    onPickupChange: (String) -> Unit,
    dropoffAddress: String,
    onDropoffChange: (String) -> Unit,
    distanceKm: Double,
    calculatedFare: Double,
    selectedVehicle: CustomerVehicleOption,
    onlineDriversCount: Int,
    onSelectPreset: (LocationPresetItem) -> Unit,
    onSwapLocations: () -> Unit,
    onProceedToVehicleSelect: () -> Unit,
    onInstantOneTapBook: () -> Unit
) {
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
            // MapView.jsx — Customer Interactive Route Preview & Nearby Drivers Map
            Surface(
                color = Color(0xFF0B0F17),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .testTag("customer_map_view")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val emerald = MaterialTheme.colorScheme.secondary
                    val amber = MaterialTheme.colorScheme.primary
                    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        for (i in 1..5) {
                            val x = w * (i / 6f)
                            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                        }
                        for (j in 1..2) {
                            val y = h * (j / 3f)
                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        }

                        val pickupPos = Offset(w * 0.18f, h * 0.68f)
                        val dropPos = Offset(w * 0.82f, h * 0.28f)

                        // 5km Search Radius Circle
                        drawCircle(
                            color = emerald.copy(alpha = 0.12f),
                            radius = 44.dp.toPx(),
                            center = pickupPos
                        )
                        drawCircle(
                            color = emerald.copy(alpha = 0.35f),
                            radius = 44.dp.toPx(),
                            center = pickupPos,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                        )

                        // RouteLine.jsx
                        drawLine(
                            color = amber,
                            start = pickupPos,
                            end = dropPos,
                            strokeWidth = 4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))
                        )

                        // DriverMarker.jsx — Nearby Drivers
                        listOf(
                            Offset(w * 0.14f, h * 0.42f),
                            Offset(w * 0.24f, h * 0.80f),
                            Offset(w * 0.29f, h * 0.50f)
                        ).forEach { pos ->
                            drawCircle(color = emerald.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = pos)
                            drawCircle(color = emerald, radius = 4.dp.toPx(), center = pos)
                        }

                        // Pickup & Dropoff Markers
                        drawCircle(color = emerald.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = pickupPos)
                        drawCircle(color = emerald, radius = 6.dp.toPx(), center = pickupPos)

                        drawCircle(color = amber.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = dropPos)
                        drawCircle(color = amber, radius = 6.dp.toPx(), center = dropPos)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MAPVIEW.JSX • $onlineDriversCount DRIVERS NEAR PICKUP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ETA ~${selectedVehicle.etaMin} MIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = pickupAddress.take(24),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${distanceKm} km • ${GeoUtils.formatCurrency(calculatedFare)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = dropoffAddress.take(24),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // LocationSuggestions.jsx — Instant 1-Tap Popular Corridors
            Text(
                text = "LOCATIONSUGGESTIONS.JSX • 1-TAP POPULAR ROUTES:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LOCATION_SUGGESTIONS.forEachIndexed { idx, preset ->
                    FilterChip(
                        selected = pickupAddress == preset.pickupAddress,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.label) },
                        modifier = Modifier.testTag("customer_location_preset_$idx")
                    )
                }
            }

            // PickupInput.jsx
            OutlinedTextField(
                value = pickupAddress,
                onValueChange = onPickupChange,
                label = { Text("Pickup Location (PickupInput.jsx)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Pickup",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_pickup_input")
            )

            // DropInput.jsx + Swap Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dropoffAddress,
                    onValueChange = onDropoffChange,
                    label = { Text("Drop-off Destination (DropInput.jsx)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Dropoff",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("customer_dropoff_input")
                )

                OutlinedButton(
                    onClick = onSwapLocations,
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("customer_swap_locations_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap Locations"
                    )
                }
            }

            // Action CTAs: Select Vehicle & Fare OR 1-Tap Instant Book
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onProceedToVehicleSelect,
                    modifier = Modifier
                        .weight(0.45f)
                        .height(52.dp)
                        .testTag("customer_select_vehicle_cta"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Vehicles")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vehicles & Fare", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onInstantOneTapBook,
                    modifier = Modifier
                        .weight(0.55f)
                        .height(52.dp)
                        .testTag("customer_instant_book_cta"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF0B0F17)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = "Instant Book")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ BOOK NOW (${GeoUtils.formatCurrency(calculatedFare)})",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerVehicleFareBookingPage(
    pickupAddress: String,
    dropoffAddress: String,
    distanceKm: Double,
    selectedVehicle: CustomerVehicleOption,
    onSelectVehicle: (CustomerVehicleOption) -> Unit,
    cargoType: String,
    onSelectCargoType: (String) -> Unit,
    cargoWeightKg: Int,
    onWeightChange: (Int) -> Unit,
    calculatedFare: Double,
    onBackToSearch: () -> Unit,
    onConfirmBooking: () -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VEHICLESELECTOR.JSX & FARECARD.JSX",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$pickupAddress → $dropoffAddress ($distanceKm km)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onBackToSearch) {
                    Text("← Edit Route")
                }
            }

            // VehicleSelector.jsx — 4 Vehicle Classes
            CUSTOMER_VEHICLE_OPTIONS.forEach { option ->
                val isSelected = option.id == selectedVehicle.id
                val optionFare = ((option.baseFare + distanceKm * option.perKmRate) * 100.0).roundToInt() / 100.0

                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectVehicle(option) }
                        .testTag("customer_vehicle_option_${option.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = option.name,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = "${option.name} • Up to ${option.capacityKg}kg",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${option.subtitle} • ETA ${option.etaMin} mins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = GeoUtils.formatCurrency(optionFare),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$${option.perKmRate}/km",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Cargo Classification Chips
            Text(
                text = "BOOKINGCARD.JSX • CARGO TYPE & WEIGHT (${cargoWeightKg} KG):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Fragile Load" to 180,
                    "Express Air Parcel" to 45,
                    "Cold Chain" to 320,
                    "Heavy Pallet" to 850,
                    "Commercial Goods" to 400
                ).forEach { (label, defaultWeight) ->
                    FilterChip(
                        selected = cargoType == label,
                        onClick = {
                            onSelectCargoType(label)
                            onWeightChange(defaultWeight.coerceAtMost(selectedVehicle.capacityKg))
                        },
                        label = { Text(label) }
                    )
                }
            }

            // FareCard.jsx Breakdown
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Base Fare (${selectedVehicle.name})", style = MaterialTheme.typography.bodySmall)
                        Text(GeoUtils.formatCurrency(selectedVehicle.baseFare), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Distance Charge ($distanceKm km × $${selectedVehicle.perKmRate}/km)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            GeoUtils.formatCurrency(distanceKm * selectedVehicle.perKmRate),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL GUARANTEED FARE (ETA ~${selectedVehicle.etaMin}m)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = GeoUtils.formatCurrency(calculatedFare),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Button(
                onClick = onConfirmBooking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("customer_confirm_booking_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color(0xFF06281E)
                )
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Confirm Booking")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONFIRM BOOKING & ASSIGN DRIVER (${GeoUtils.formatCurrency(calculatedFare)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun CustomerLiveTrackingPage(
    activeOrder: RideOrderEntity?,
    latestOrder: RideOrderEntity?,
    assignedDriver: DriverProfileEntity?,
    assignedVehicle: VehicleEntity?,
    routeProgress: Float,
    lastDispatchTrace: DispatchEndpointTrace?,
    onAdvanceTripStep: (RideOrderEntity) -> Unit,
    onPreviousTripStep: (RideOrderEntity?) -> Unit,
    onGoToPayment: () -> Unit,
    onBookAnotherRide: () -> Unit
) {
    val displayedOrder = activeOrder ?: latestOrder

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
            if (displayedOrder == null) {
                Text(
                    text = "No active customer booking yet.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onBookAnotherRide) {
                    Text("Book Your First Ride")
                }
            } else {
                val statusText = when (displayedOrder.status) {
                    "INCOMING" -> "DRIVER SEARCHING • BROADCASTING TO NEARBY DRIVERS"
                    "ACCEPTED" -> "DRIVER ASSIGNED • EN ROUTE TO PICKUP"
                    "ARRIVED_PICKUP" -> "DRIVER AT PICKUP • SHARE OTP PIN ${displayedOrder.riderPin}"
                    "IN_PROGRESS" -> "LIVE TRACKING • IN TRANSIT TO DESTINATION"
                    "COMPLETED" -> "RIDE COMPLETED • READY FOR PAYMENT & RATING"
                    else -> displayedOrder.status
                }

                // ETA.jsx & Status Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${displayedOrder.pickupAddress} → ${displayedOrder.dropoffAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "OTP PIN: ${displayedOrder.riderPin}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // TrackingMap.jsx + DriverMarker.jsx + RouteLine.jsx + ETA.jsx
                val effectiveProgress = when (displayedOrder.status) {
                    "INCOMING" -> 0.08f
                    "ACCEPTED" -> 0.35f
                    "ARRIVED_PICKUP" -> 0.55f
                    "IN_PROGRESS" -> routeProgress.coerceIn(0.60f, 0.92f)
                    "COMPLETED" -> 1.0f
                    else -> 0.25f
                }

                Surface(
                    color = Color(0xFF080C14),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .testTag("customer_tracking_map_canvas")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val emerald = MaterialTheme.colorScheme.secondary
                        val amber = MaterialTheme.colorScheme.primary
                        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            for (i in 1..6) {
                                val x = w * (i / 7f)
                                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                            }
                            val start = Offset(w * 0.14f, h * 0.72f)
                            val end = Offset(w * 0.86f, h * 0.26f)

                            // Background full route
                            drawLine(
                                color = Color(0xFF334155),
                                start = start,
                                end = end,
                                strokeWidth = 5.dp.toPx()
                            )

                            // Active completed route line
                            val driverPos = Offset(
                                x = start.x + (end.x - start.x) * effectiveProgress,
                                y = start.y + (end.y - start.y) * effectiveProgress
                            )
                            drawLine(
                                color = emerald,
                                start = start,
                                end = driverPos,
                                strokeWidth = 5.dp.toPx()
                            )

                            // Pickup & Dropoff pins
                            drawCircle(color = emerald, radius = 6.dp.toPx(), center = start)
                            drawCircle(color = amber, radius = 6.dp.toPx(), center = end)

                            // Live DriverMarker.jsx
                            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = driverPos)
                            drawCircle(color = emerald, radius = 6.dp.toPx(), center = driverPos)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TRACKINGMAP.JSX • ${(effectiveProgress * 100).roundToInt()}% PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ETA: ${(displayedOrder.estimatedMinutes * (1.05f - effectiveProgress)).roundToInt().coerceAtLeast(1)} MIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { effectiveProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary
                )

                // DriverCard.jsx — Assigned Driver & Vehicle Details
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_driver_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Driver",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Column {
                                Text(
                                    text = "${assignedDriver?.fullName ?: "Mateo Vance"} • ★ ${assignedDriver?.rating ?: 4.94f}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${assignedVehicle?.makeAndModel ?: "2025 Tata Ace EV / Tesla Model Y"} • ${assignedVehicle?.plateNumber ?: "ZLD-904E"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Driver Phone: ${assignedDriver?.phoneNumber ?: "+1 (415) 890-4210"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = GeoUtils.formatCurrency(displayedOrder.totalPayout),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${displayedOrder.distanceKm} km",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Instant Step Controls (Previous Step + Next Step + Go to Payment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPreviousTripStep(activeOrder) },
                        modifier = Modifier
                            .weight(0.28f)
                            .height(50.dp)
                            .testTag("customer_tracking_prev_step_button")
                    ) {
                        Text("← Prev", fontWeight = FontWeight.Bold)
                    }

                    if (activeOrder != null) {
                        Button(
                            onClick = { onAdvanceTripStep(activeOrder) },
                            modifier = Modifier
                                .weight(0.48f)
                                .height(50.dp)
                                .testTag("customer_tracking_next_step_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color(0xFF0B0F17)
                            )
                        ) {
                            Text(
                                text = when (activeOrder.status) {
                                    "INCOMING" -> "⚡ Driver Accept"
                                    "ACCEPTED" -> "⚡ Arrive Pickup"
                                    "ARRIVED_PICKUP" -> "⚡ Verify OTP & Go"
                                    else -> "⚡ Arrive Dropoff"
                                },
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onGoToPayment,
                        modifier = Modifier
                            .weight(if (activeOrder != null) 0.24f else 0.72f)
                            .height(50.dp)
                            .testTag("customer_tracking_pay_button")
                    ) {
                        Text("Pay →", fontWeight = FontWeight.Bold)
                    }
                }

                if (lastDispatchTrace != null) {
                    Text(
                        text = "Dispatch Trace: ${lastDispatchTrace.statusLabel} (${lastDispatchTrace.endpointUrl})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerPaymentAndRatingPage(
    order: RideOrderEntity?,
    calculatedFare: Double,
    selectedPaymentMethod: String,
    onSelectPaymentMethod: (String) -> Unit,
    selectedRatingStars: Int,
    onSelectRatingStars: (Int) -> Unit,
    ratingFeedback: String,
    onRatingFeedbackChange: (String) -> Unit,
    paymentCompletedBanner: String?,
    onPayAndComplete: () -> Unit,
    onViewRideHistory: () -> Unit
) {
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
                    imageVector = Icons.Default.Payment,
                    contentDescription = "PaymentCard.jsx",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text(
                        text = "PAYMENTCARD.JSX • SETTLEMENT & DRIVER RATING",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = order?.let { "Trip #${it.orderId.take(8)} • ${it.pickupAddress} → ${it.dropoffAddress}" }
                            ?: "Select payment method and rate your Zaldi trip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (paymentCompletedBanner != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = paymentCompletedBanner,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Payment Method Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("UPI / Zaldi Pay", "Zaldi Wallet", "Corporate Card ••4821", "Cash at Dropoff").forEach { method ->
                    FilterChip(
                        selected = selectedPaymentMethod == method,
                        onClick = { onSelectPaymentMethod(method) },
                        label = { Text(method) }
                    )
                }
            }

            // 5-Star Rating Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RATE YOUR DRIVER:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "$star Stars",
                            tint = if (star <= selectedRatingStars) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onSelectRatingStars(star) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = ratingFeedback,
                onValueChange = onRatingFeedbackChange,
                label = { Text("Customer Review / Delivery Note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPayAndComplete,
                    modifier = Modifier
                        .weight(0.65f)
                        .height(52.dp)
                        .testTag("customer_pay_now_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color(0xFF06281E)
                    )
                ) {
                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Pay")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PAY ${GeoUtils.formatCurrency(calculatedFare)} & SUBMIT ${selectedRatingStars}★",
                        fontWeight = FontWeight.Black
                    )
                }

                OutlinedButton(
                    onClick = onViewRideHistory,
                    modifier = Modifier
                        .weight(0.35f)
                        .height(52.dp)
                ) {
                    Text("History →", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerRideHistoryPage(
    allOrders: List<RideOrderEntity>,
    onSelectOrderForTracking: (RideOrderEntity) -> Unit,
    onRebookOrder: (RideOrderEntity) -> Unit
) {
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
                    imageVector = Icons.Default.History,
                    contentDescription = "RideHistory",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "PAGES/RIDEHISTORY • CUSTOMER TRIPS (${allOrders.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
            }

            allOrders.take(8).forEachIndexed { idx, order ->
                if (idx > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${order.pickupAddress} → ${order.dropoffAddress}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${order.cargoDetails} (${order.cargoWeightKg}kg) • ${order.distanceKm} km • Status: ${order.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = GeoUtils.formatCurrency(order.totalPayout),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black
                        )
                        OutlinedButton(
                            onClick = {
                                if (order.status in listOf("INCOMING", "ACCEPTED", "ARRIVED_PICKUP", "IN_PROGRESS")) {
                                    onSelectOrderForTracking(order)
                                } else {
                                    onRebookOrder(order)
                                }
                            }
                        ) {
                            Text(
                                if (order.status in listOf("INCOMING", "ACCEPTED", "ARRIVED_PICKUP", "IN_PROGRESS")) {
                                    "Track"
                                } else {
                                    "Rebook"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerProfileNotificationsSupportPage(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerPhone: String,
    onCustomerPhoneChange: (String) -> Unit,
    otpInput: String,
    onOtpInputChange: (String) -> Unit,
    customerOtpVerified: Boolean,
    onVerifyOtp: () -> Unit,
    activeOrder: RideOrderEntity?,
    allOrdersCount: Int
) {
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
                text = "PAGES/PROFILE • LOGIN/OTP • NOTIFICATIONS • SUPPORT",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = onCustomerPhoneChange,
                    label = { Text("Mobile (OTP Login)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = otpInput,
                    onValueChange = onOtpInputChange,
                    label = { Text("6-Digit Customer OTP") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onVerifyOtp) {
                    Text(if (customerOtpVerified) "✓ OTP Verified" else "Verify OTP")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Notifications Feed (`pages/Notifications`)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "LIVE CUSTOMER NOTIFICATIONS ($allOrdersCount trips tracked)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = activeOrder?.let {
                    "• Active Booking #${it.orderId.take(6)} (${it.status}): ${it.pickupAddress} → ${it.dropoffAddress} | Rider PIN: ${it.riderPin}"
                } ?: "• Ready for instant booking. 3 verified Zaldi drivers online within 5km radius.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Support (`pages/Support`)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = "Customer Support",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "24/7 ZALDI CUSTOMER DISPATCH SUPPORT • SLA < 60s",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
