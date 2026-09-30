package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.errors.DiagnosticSeverity
import com.example.core.errors.GlobalErrorHandler
import com.example.core.network.ZaldiDispatchNetworkClient
import com.example.features.authentication.AuthenticationScreen
import com.example.features.authentication.ZaldiFirebaseAuthManager
import com.example.features.booking.AdminAppTab
import com.example.features.booking.CustomerAppPage
import com.example.features.booking.ZaldiStandaloneAdminAppScreen
import com.example.features.booking.ZaldiStandaloneCustomerAppScreen
import com.example.features.dashboard.DashboardScreen
import com.example.features.earnings.EarningsScreen
import com.example.features.kyconboarding.KycOnboardingScreen
import com.example.features.maptracking.MapTrackingScreen
import com.example.features.orderengine.IncomingOrderOverlay
import com.example.features.profile.ProfileScreen
import com.example.services.localstorage.ZaldiDatabase
import com.example.services.localstorage.ZaldiRepository
import com.example.services.location.LocationTelemetryBus
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

/**
 * Standalone Android Launcher Activity #1: Zaldi Customer Application (`apps/customer`).
 * Also supports launching into Driver App or Admin App when started with `ZALDI_TARGET_APP`.
 */
class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val database = ZaldiDatabase.getInstance(applicationContext)
        val networkClient = ZaldiDispatchNetworkClient()
        val repository = ZaldiRepository(database.zaldiDao(), networkClient)
        val firebaseAuthManager = ZaldiFirebaseAuthManager(applicationContext)
        val requestedAppCode = intent?.getStringExtra("ZALDI_TARGET_APP")

        setContent {
            val viewModel: ZaldiDriverViewModel = viewModel(
                factory = ZaldiDriverViewModel.Factory(repository, firebaseAuthManager)
            )
            LaunchedEffect(requestedAppCode) {
                val targetApp = ZaldiApplicationId.entries.firstOrNull { it.code == requestedAppCode }
                    ?: ZaldiApplicationId.CUSTOMER_APP
                viewModel.setInitialApplication(targetApp)
            }
            val profile by viewModel.driverProfile.collectAsStateWithLifecycle()
            val isDark = profile?.isDarkMode ?: true

            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                ZaldiDriverRootApp(
                    viewModel = viewModel,
                    fusedLocationClient = fusedLocationClient
                )
            }
        }
    }
}

/**
 * Helper to inspect current Foreground & Background GPS Location permissions.
 */
fun checkLocationPermissions(context: Context): Pair<Boolean, Boolean> {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasForeground = fineGranted || coarseGranted

    val hasBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        hasForeground
    }
    return hasForeground to hasBackground
}

@Composable
fun ZaldiDriverRootApp(
    viewModel: ZaldiDriverViewModel,
    fusedLocationClient: FusedLocationProviderClient
) {
    val context = LocalContext.current
    val profile by viewModel.driverProfile.collectAsStateWithLifecycle()
    val authSessionState by viewModel.authSessionState.collectAsStateWithLifecycle()
    val allDrivers by viewModel.allDrivers.collectAsStateWithLifecycle()
    val simulateRaceLock by viewModel.simulateRaceLockConflict.collectAsStateWithLifecycle()
    val kycDocs by viewModel.kycDocuments.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val activeOrder by viewModel.activeOrder.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val ledgerEntries by viewModel.ledgerEntries.collectAsStateWithLifecycle()
    val pendingOfflineLogs by viewModel.pendingOfflineEarningsLogs.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsStateWithLifecycle()
    val socketState by viewModel.socketState.collectAsStateWithLifecycle()
    val socketPingMs by viewModel.socketPingMs.collectAsStateWithLifecycle()
    val lastDispatchTrace by viewModel.lastDispatchTrace.collectAsStateWithLifecycle()
    val surgeCells by viewModel.surgeCells.collectAsStateWithLifecycle()
    val selectedSurgeCell by viewModel.selectedSurgeCell.collectAsStateWithLifecycle()

    // Separate 3-Application States
    val activeApp by viewModel.activeApp.collectAsStateWithLifecycle()
    val customerPage by viewModel.customerPage.collectAsStateWithLifecycle()
    val driverTab by viewModel.driverTab.collectAsStateWithLifecycle()
    val adminTab by viewModel.adminTab.collectAsStateWithLifecycle()
    val canGoPrevious by viewModel.canGoPrevious.collectAsStateWithLifecycle()

    val countdownSeconds by viewModel.orderCountdownSeconds.collectAsStateWithLifecycle()
    val countdownProgress by viewModel.orderCountdownProgress.collectAsStateWithLifecycle()
    val routeProgress by viewModel.routeProgress.collectAsStateWithLifecycle()
    val pendingOtp by viewModel.pendingOtpCode.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val diagnosticEvents by viewModel.diagnosticEvents.collectAsStateWithLifecycle()

    // Location Permission Handler State
    val initialPermState = remember { checkLocationPermissions(context) }
    var hasForegroundLocationPerm by remember { mutableStateOf(initialPermState.first) }
    var hasBackgroundLocationPerm by remember { mutableStateOf(initialPermState.second) }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val (fg, bg) = checkLocationPermissions(context)
        hasForegroundLocationPerm = fg
        hasBackgroundLocationPerm = bg || isGranted
        LocationTelemetryBus.updatePermissionStatus(hasForegroundLocationPerm, hasBackgroundLocationPerm)
        GlobalErrorHandler.logEvent(
            module = "MainActivity/permissions",
            severity = DiagnosticSeverity.INFO,
            message = "Background GPS permission result: $hasBackgroundLocationPerm"
        )
    }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val (fg, bg) = checkLocationPermissions(context)
        hasForegroundLocationPerm = fine || coarse || fg
        hasBackgroundLocationPerm = bg
        LocationTelemetryBus.updatePermissionStatus(hasForegroundLocationPerm, hasBackgroundLocationPerm)

        if (hasForegroundLocationPerm) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        LocationTelemetryBus.updateFromHardwareLocation(
                            location,
                            "MAIN_ACTIVITY_FUSED_CLIENT"
                        )
                    }
                }
            } catch (_: SecurityException) {
            }
        }
        GlobalErrorHandler.logEvent(
            module = "MainActivity/permissions",
            severity = DiagnosticSeverity.INFO,
            message = "Foreground GPS permission granted=$hasForegroundLocationPerm"
        )
    }

    LaunchedEffect(Unit) {
        val (fg, bg) = checkLocationPermissions(context)
        hasForegroundLocationPerm = fg
        hasBackgroundLocationPerm = bg
        LocationTelemetryBus.updatePermissionStatus(fg, bg)
        if (fg) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        LocationTelemetryBus.updateFromHardwareLocation(
                            location,
                            "MAIN_ACTIVITY_FUSED_INIT"
                        )
                    }
                }
            } catch (_: SecurityException) {
            }
        }
    }

    val requestLocationPermissionsAction = {
        if (!hasForegroundLocationPerm) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            foregroundPermissionLauncher.launch(perms.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPerm) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    val activeVehicle = remember(vehicles, profile?.activeVehicleId) {
        vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull()
    }

    val showBackButton = canGoPrevious ||
        activeApp != ZaldiApplicationId.CUSTOMER_APP ||
        customerPage != CustomerAppPage.HOME_SEARCH

    if (showBackButton) {
        BackHandler {
            viewModel.navigatePrevious()
        }
    }

    if (profile != null && !profile!!.isAuthenticated && activeApp == ZaldiApplicationId.DRIVER_APP) {
        AuthenticationScreen(
            initialPhone = profile!!.phoneNumber,
            initialName = profile!!.fullName,
            pendingOtpCode = pendingOtp,
            authSessionState = authSessionState,
            onSignInWithGoogle = { ctx, name, email, phone ->
                viewModel.signInWithGoogleCredentialManager(ctx, name, email, phone)
            },
            onSignInWithFirebaseEmail = { email, password, name, phone, isRegister ->
                viewModel.signInWithFirebaseEmail(email, password, name, phone, isRegister)
            },
            onRequestOtp = { viewModel.requestOtpForPhone(it) },
            onVerifyOtp = { phone, name, otp -> viewModel.verifyOtpAndLogin(phone, name, otp) }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                ZaldiThreeAppHeaderBar(
                    activeApp = activeApp,
                    onSelectApp = { viewModel.selectApplication(it) },
                    isOnline = profile?.isOnline == true,
                    geohash = gpsTelemetry.geohash,
                    canGoPrevious = showBackButton,
                    hasForegroundPerm = hasForegroundLocationPerm,
                    hasBackgroundPerm = hasBackgroundLocationPerm,
                    bannerMessage = bannerMessage,
                    onDismissBanner = { viewModel.dismissBanner() },
                    onNavigatePrevious = { viewModel.navigatePrevious() },
                    onRequestLocationPermissions = requestLocationPermissionsAction,
                    onQuickDispatch = { viewModel.triggerIncomingOrderOffer(context) }
                )
            },
            bottomBar = {
                if (!isExpandedScreen && activeApp != ZaldiApplicationId.APPS_HUB) {
                    ZaldiAppSpecificBottomBar(
                        activeApp = activeApp,
                        customerPage = customerPage,
                        onSelectCustomerPage = { viewModel.selectCustomerPage(it) },
                        driverTab = driverTab,
                        onSelectDriverTab = { viewModel.selectDriverTab(it) },
                        adminTab = adminTab,
                        onSelectAdminTab = { viewModel.selectAdminTab(it) },
                        hasActiveOrder = activeOrder != null
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen && activeApp != ZaldiApplicationId.APPS_HUB) {
                    ZaldiAppSpecificNavigationRail(
                        activeApp = activeApp,
                        customerPage = customerPage,
                        onSelectCustomerPage = { viewModel.selectCustomerPage(it) },
                        driverTab = driverTab,
                        onSelectDriverTab = { viewModel.selectDriverTab(it) },
                        adminTab = adminTab,
                        onSelectAdminTab = { viewModel.selectAdminTab(it) }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (activeApp) {
                        // =========================================================================
                        // APPLICATION 1: STANDALONE ZALDI CUSTOMER APP (`apps/customer`)
                        // =========================================================================
                        ZaldiApplicationId.CUSTOMER_APP -> {
                            ZaldiStandaloneCustomerAppScreen(
                                currentPage = customerPage,
                                onSelectPage = { viewModel.selectCustomerPage(it) },
                                activeDriver = profile,
                                allDrivers = allDrivers,
                                vehicles = vehicles,
                                activeOrder = activeOrder,
                                allOrders = allOrders,
                                routeProgress = routeProgress,
                                lastDispatchTrace = lastDispatchTrace,
                                onBookCustomerRide = { customerName, pickup, pLat, pLng, dropoff, dLat, dLng, cargo, weightKg, tier, fare, distKm ->
                                    viewModel.bookCustomCustomerRide(
                                        context = context,
                                        customerName = customerName,
                                        pickupAddress = pickup,
                                        dropoffAddress = dropoff,
                                        cargoDetails = cargo,
                                        cargoWeightKg = weightKg,
                                        vehicleTier = tier,
                                        guaranteedFare = fare,
                                        distanceKm = distKm,
                                        pickupLat = pLat,
                                        pickupLng = pLng,
                                        dropoffLat = dLat,
                                        dropoffLng = dLng
                                    )
                                },
                                onConfirmGoOrAdvanceTrip = { order ->
                                    if (order.status == "INCOMING") {
                                        viewModel.acceptIncomingOrder(order)
                                    } else {
                                        viewModel.advanceOrderLifecycle(order)
                                    }
                                },
                                onPreviousTripStep = { order ->
                                    viewModel.revertOrderToPreviousStep(order)
                                },
                                onSubmitCustomerRating = { passengerName, rating, comment, badgeTag ->
                                    viewModel.submitCustomerTripReview(passengerName, rating, comment, badgeTag)
                                },
                                onSwitchToDriverApp = {
                                    viewModel.selectApplication(ZaldiApplicationId.DRIVER_APP)
                                },
                                onSwitchToAdminApp = {
                                    viewModel.selectApplication(ZaldiApplicationId.ADMIN_APP)
                                }
                            )
                        }

                        // =========================================================================
                        // APPLICATION 2: STANDALONE ZALDI DRIVER APP (`apps/driver`)
                        // =========================================================================
                        ZaldiApplicationId.DRIVER_APP -> {
                            when (driverTab) {
                                DriverAppTab.DRIVER_HOME -> DashboardScreen(
                                    profile = profile,
                                    activeVehicle = activeVehicle,
                                    gpsTelemetry = gpsTelemetry,
                                    socketState = socketState,
                                    socketPingMs = socketPingMs,
                                    surgeCells = surgeCells,
                                    selectedSurgeCell = selectedSurgeCell,
                                    activeOrder = activeOrder,
                                    onToggleOnline = { enable ->
                                        if (enable && !hasForegroundLocationPerm) {
                                            requestLocationPermissionsAction()
                                        }
                                        viewModel.toggleDriverOnline(context, enable)
                                    },
                                    onSelectSurgeCell = { cell -> viewModel.selectSurgeCell(cell) },
                                    onTriggerIncomingOrder = { viewModel.triggerIncomingOrderOffer(context) },
                                    onOpenActiveNavigation = { viewModel.selectDriverTab(DriverAppTab.ACTIVE_TRIP) }
                                )

                                DriverAppTab.ACTIVE_TRIP -> MapTrackingScreen(
                                    activeOrder = activeOrder,
                                    recentOrders = allOrders,
                                    gpsTelemetry = gpsTelemetry,
                                    routeProgress = routeProgress,
                                    onAdvanceOrderLifecycle = { order -> viewModel.advanceOrderLifecycle(order) },
                                    onRevertOrderLifecycle = { order -> viewModel.revertOrderToPreviousStep(order) },
                                    onAttachProofOfDelivery = { order, podUrl ->
                                        viewModel.attachProofOfDelivery(order, podUrl)
                                    },
                                    onStepRouteForward = { viewModel.stepRouteProgressForward() },
                                    onRequestDispatchOrder = { viewModel.triggerIncomingOrderOffer(context) }
                                )

                                DriverAppTab.EARNINGS -> EarningsScreen(
                                    profile = profile,
                                    ledgerEntries = ledgerEntries,
                                    pendingOfflineLogs = pendingOfflineLogs,
                                    onTriggerInstantPayout = { bank -> viewModel.executeInstantPayout(bank) },
                                    onLogOfflineEarning = { title, route, amount, method ->
                                        viewModel.logOfflineEarningToRoom(title, route, amount, method)
                                    },
                                    onSyncOfflineLogs = { viewModel.syncOfflineEarningsLogs() }
                                )

                                DriverAppTab.KYC_DOCS -> KycOnboardingScreen(
                                    profile = profile,
                                    documents = kycDocs,
                                    onUpdateDocument = { doc, num, exp, st ->
                                        viewModel.updateKycDocument(doc, num, exp, st)
                                    },
                                    onSetOverallKycState = { st -> viewModel.setOverallKycState(st) }
                                )

                                DriverAppTab.VEHICLE_PROFILE -> ProfileScreen(
                                    profile = profile,
                                    authSessionState = authSessionState,
                                    vehicles = vehicles,
                                    trips = allOrders,
                                    reviews = reviews,
                                    diagnosticEvents = diagnosticEvents,
                                    onSelectVehicle = { uuid -> viewModel.switchActiveVehicle(uuid) },
                                    onAddVehicle = { makeAndModel, plate, capacityKg, insuranceExpiry, tier ->
                                        viewModel.addFleetVehicle(makeAndModel, plate, capacityKg, insuranceExpiry, tier)
                                    },
                                    onUpdateBackendUrl = { url -> viewModel.updateBackendUrl(url) },
                                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                                    onSaveDriverProfile = { first, last, phone, email ->
                                        viewModel.saveDriverProfileInfo(first, last, phone, email)
                                    },
                                    onTriggerGoogleSignIn = {
                                        viewModel.signInWithGoogleCredentialManager(
                                            context = context,
                                            driverName = profile?.fullName ?: "Mateo Vance",
                                            driverEmail = authSessionState.email,
                                            phone = profile?.phoneNumber ?: "+1 (415) 890-4210"
                                        )
                                    },
                                    onSignOut = { viewModel.signOutDriver(context) }
                                )
                            }

                            // Driver App Incoming Order Offer Overlay (`BookingRequest.jsx`)
                            val incoming = activeOrder?.takeIf { it.status == "INCOMING" }
                            if (incoming != null) {
                                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                                    IncomingOrderOverlay(
                                        order = incoming,
                                        countdownSeconds = countdownSeconds,
                                        countdownProgress = countdownProgress,
                                        onAccept = { viewModel.acceptIncomingOrder(incoming) },
                                        onDecline = { viewModel.declineIncomingOrder(incoming) }
                                    )
                                }
                            }
                        }

                        // =========================================================================
                        // APPLICATION 3: STANDALONE ZALDI ADMIN APP (`apps/admin`)
                        // =========================================================================
                        ZaldiApplicationId.ADMIN_APP -> {
                            ZaldiStandaloneAdminAppScreen(
                                currentTab = adminTab,
                                onSelectTab = { viewModel.selectAdminTab(it) },
                                activeDriver = profile,
                                allDrivers = allDrivers,
                                vehicles = vehicles,
                                activeOrder = activeOrder,
                                allOrders = allOrders,
                                ledgerEntries = ledgerEntries,
                                reviews = reviews,
                                lastDispatchTrace = lastDispatchTrace,
                                simulateRaceLockConflict = simulateRaceLock,
                                onToggleRaceLockConflict = { viewModel.toggleRaceLockSimulation(it) },
                                gpsTelemetry = gpsTelemetry,
                                onBookCustomRide = { customerName, pickup, pLat, pLng, dropoff, dLat, dLng, cargo, weightKg, tier, fare, distKm ->
                                    viewModel.bookCustomCustomerRide(
                                        context = context,
                                        customerName = customerName,
                                        pickupAddress = pickup,
                                        dropoffAddress = dropoff,
                                        cargoDetails = cargo,
                                        cargoWeightKg = weightKg,
                                        vehicleTier = tier,
                                        guaranteedFare = fare,
                                        distanceKm = distKm,
                                        pickupLat = pLat,
                                        pickupLng = pLng,
                                        dropoffLat = dLat,
                                        dropoffLng = dLng
                                    )
                                },
                                onCreateCustomDriver = { first, last, phone, model, plate, capKg, tier ->
                                    viewModel.registerCustomDriver(
                                        context = context,
                                        firstName = first,
                                        lastName = last,
                                        phoneNumber = phone,
                                        vehicleMakeModel = model,
                                        plateNumber = plate,
                                        capacityKg = capKg,
                                        vehicleTier = tier
                                    )
                                },
                                onSelectDriverSession = { driverId -> viewModel.switchDriverSession(driverId) },
                                onToggleDriverAvailability = { driverId -> viewModel.toggleSpecificDriverAvailability(driverId) },
                                onUpdateDriverApproval = { driverId, status ->
                                    viewModel.updateSpecificDriverApprovalStatus(driverId, status)
                                },
                                onSubmitCustomerRating = { passengerName, rating, comment, badgeTag ->
                                    viewModel.submitCustomerTripReview(passengerName, rating, comment, badgeTag)
                                },
                                onConfirmGoOrAdvanceTrip = { order ->
                                    if (order.status == "INCOMING") {
                                        viewModel.acceptIncomingOrder(order)
                                    } else {
                                        viewModel.advanceOrderLifecycle(order)
                                    }
                                },
                                onPreviousTripStep = { order ->
                                    viewModel.revertOrderToPreviousStep(order)
                                },
                                onAdvanceTripById = { tripId -> viewModel.advanceOrderById(tripId) },
                                onQuickDispatchOrder = { viewModel.triggerIncomingOrderOffer(context) }
                            )
                        }

                        // =========================================================================
                        // 3-APPLICATION LAUNCHER HUB (`apps/*`)
                        // =========================================================================
                        ZaldiApplicationId.APPS_HUB -> {
                            ZaldiThreeAppsHubScreen(
                                activeDriver = profile,
                                allDriversCount = allDrivers.size,
                                onlineDriversCount = allDrivers.count { it.isOnline },
                                activeOrder = activeOrder,
                                totalOrdersCount = allOrders.size,
                                onOpenAppInPlace = { appId -> viewModel.selectApplication(appId) },
                                onInstantCustomerBook = {
                                    viewModel.bookCustomCustomerRide(
                                        context = context,
                                        customerName = "Vikram Aditya (Customer App)",
                                        pickupAddress = "Clock Tower Center, Nalgonda",
                                        dropoffAddress = "SLN Terminus, Gachibowli, Hyderabad",
                                        cargoDetails = "Fragile Load",
                                        cargoWeightKg = 320,
                                        vehicleTier = "ZALDI_VAN",
                                        guaranteedFare = 145.00,
                                        distanceKm = 105.0,
                                        pickupLat = 17.0500,
                                        pickupLng = 79.2667,
                                        dropoffLat = 17.4401,
                                        dropoffLng = 78.3489
                                    )
                                },
                                onAdvanceActiveOrder = { order ->
                                    if (order.status == "INCOMING") {
                                        viewModel.acceptIncomingOrder(order)
                                    } else {
                                        viewModel.advanceOrderLifecycle(order)
                                    }
                                },
                                onRevertActiveOrder = { order ->
                                    viewModel.revertOrderToPreviousStep(order)
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
private fun ZaldiThreeAppHeaderBar(
    activeApp: ZaldiApplicationId,
    onSelectApp: (ZaldiApplicationId) -> Unit,
    isOnline: Boolean,
    geohash: String,
    canGoPrevious: Boolean,
    hasForegroundPerm: Boolean,
    hasBackgroundPerm: Boolean,
    bannerMessage: String?,
    onDismissBanner: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onRequestLocationPermissions: () -> Unit,
    onQuickDispatch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Row 1: Separate 3-Application Switcher Strip (1. Customer App | 2. Driver App | 3. Admin App | Hub)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGoPrevious) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onNavigatePrevious() }
                        .testTag("topbar_previous_button")
                ) {
                    Text(
                        text = "← PREV",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            // App 1 Pill: Customer App
            val isCustomer = activeApp == ZaldiApplicationId.CUSTOMER_APP
            Surface(
                color = if (isCustomer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectApp(ZaldiApplicationId.CUSTOMER_APP) }
                    .testTag("topbar_book_ride_pill")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1. CUSTOMER APP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCustomer) Color(0xFF0B0F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }

            // App 2 Pill: Driver App
            val isDriver = activeApp == ZaldiApplicationId.DRIVER_APP
            Surface(
                color = if (isDriver) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectApp(ZaldiApplicationId.DRIVER_APP) }
                    .testTag("topbar_driver_app_pill")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "2. DRIVER APP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDriver) Color(0xFF06281E) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }

            // App 3 Pill: Admin App
            val isAdmin = activeApp == ZaldiApplicationId.ADMIN_APP
            Surface(
                color = if (isAdmin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectApp(ZaldiApplicationId.ADMIN_APP) }
                    .testTag("topbar_admin_panel_pill")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3. ADMIN APP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAdmin) Color(0xFF0B0F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }

            // 3-Apps Hub Button
            val isHub = activeApp == ZaldiApplicationId.APPS_HUB
            Surface(
                color = if (isHub) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable { onSelectApp(ZaldiApplicationId.APPS_HUB) }
                    .testTag("topbar_apps_hub_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "3-Apps Hub",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "HUB",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Row 2: Active Application Context Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (activeApp) {
                                ZaldiApplicationId.CUSTOMER_APP -> MaterialTheme.colorScheme.primary
                                ZaldiApplicationId.DRIVER_APP -> if (isOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                ZaldiApplicationId.ADMIN_APP -> MaterialTheme.colorScheme.tertiary
                                ZaldiApplicationId.APPS_HUB -> MaterialTheme.colorScheme.secondary
                            }
                        )
                )
                Text(
                    text = when (activeApp) {
                        ZaldiApplicationId.CUSTOMER_APP -> "ZALDI CUSTOMER (${activeApp.pathLabel})"
                        ZaldiApplicationId.DRIVER_APP -> "ZALDI DRIVER (${activeApp.pathLabel}) • [${geohash.uppercase()}]"
                        ZaldiApplicationId.ADMIN_APP -> "ZALDI ADMIN (${activeApp.pathLabel})"
                        ZaldiApplicationId.APPS_HUB -> "ZALDI 3-APP SUITE LAUNCHER"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = if (hasForegroundPerm && hasBackgroundPerm) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onRequestLocationPermissions() }
                        .testTag("grant_location_permission_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (hasForegroundPerm) Icons.Default.GpsFixed else Icons.Default.LocationOn,
                            contentDescription = "GPS Permission Status",
                            tint = if (hasForegroundPerm) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (hasForegroundPerm) "GPS OK" else "GPS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onQuickDispatch() }
                        .testTag("topbar_quick_dispatch_pill")
                ) {
                    Text(
                        text = "⚡ PING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (bannerMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bannerMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissBanner,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Notification",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZaldiAppSpecificBottomBar(
    activeApp: ZaldiApplicationId,
    customerPage: CustomerAppPage,
    onSelectCustomerPage: (CustomerAppPage) -> Unit,
    driverTab: DriverAppTab,
    onSelectDriverTab: (DriverAppTab) -> Unit,
    adminTab: AdminAppTab,
    onSelectAdminTab: (AdminAppTab) -> Unit,
    hasActiveOrder: Boolean
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        when (activeApp) {
            ZaldiApplicationId.CUSTOMER_APP -> {
                CustomerAppPage.entries.forEach { page ->
                    val showBadge = page == CustomerAppPage.LIVE_TRACKING && hasActiveOrder
                    NavigationBarItem(
                        selected = customerPage == page,
                        onClick = { onSelectCustomerPage(page) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (showBadge) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = page.icon(),
                                    contentDescription = page.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = page.shortNavLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("customer_nav_${page.code}")
                    )
                }
            }

            ZaldiApplicationId.DRIVER_APP -> {
                DriverAppTab.entries.forEach { tab ->
                    val showBadge = tab == DriverAppTab.ACTIVE_TRIP && hasActiveOrder
                    NavigationBarItem(
                        selected = driverTab == tab,
                        onClick = { onSelectDriverTab(tab) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (showBadge) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon(),
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("driver_nav_${tab.route}")
                    )
                }
            }

            ZaldiApplicationId.ADMIN_APP -> {
                AdminAppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = adminTab == tab,
                        onClick = { onSelectAdminTab(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon(),
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.shortNavLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("admin_nav_${tab.route}")
                    )
                }
            }

            ZaldiApplicationId.APPS_HUB -> {}
        }
    }
}

@Composable
private fun ZaldiAppSpecificNavigationRail(
    activeApp: ZaldiApplicationId,
    customerPage: CustomerAppPage,
    onSelectCustomerPage: (CustomerAppPage) -> Unit,
    driverTab: DriverAppTab,
    onSelectDriverTab: (DriverAppTab) -> Unit,
    adminTab: AdminAppTab,
    onSelectAdminTab: (AdminAppTab) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        when (activeApp) {
            ZaldiApplicationId.CUSTOMER_APP -> {
                CustomerAppPage.entries.forEach { page ->
                    NavigationRailItem(
                        selected = customerPage == page,
                        onClick = { onSelectCustomerPage(page) },
                        icon = { Icon(page.icon(), contentDescription = page.label) },
                        label = { Text(page.shortNavLabel()) }
                    )
                }
            }

            ZaldiApplicationId.DRIVER_APP -> {
                DriverAppTab.entries.forEach { tab ->
                    NavigationRailItem(
                        selected = driverTab == tab,
                        onClick = { onSelectDriverTab(tab) },
                        icon = { Icon(tab.icon(), contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }

            ZaldiApplicationId.ADMIN_APP -> {
                AdminAppTab.entries.forEach { tab ->
                    NavigationRailItem(
                        selected = adminTab == tab,
                        onClick = { onSelectAdminTab(tab) },
                        icon = { Icon(tab.icon(), contentDescription = tab.label) },
                        label = { Text(tab.shortNavLabel()) }
                    )
                }
            }

            ZaldiApplicationId.APPS_HUB -> {}
        }
    }
}

private fun CustomerAppPage.icon(): ImageVector = when (this) {
    CustomerAppPage.HOME_SEARCH -> Icons.Default.Search
    CustomerAppPage.VEHICLE_FARE -> Icons.Default.LocalShipping
    CustomerAppPage.LIVE_TRACKING -> Icons.Default.Navigation
    CustomerAppPage.PAYMENT -> Icons.Default.Payment
    CustomerAppPage.RIDE_HISTORY -> Icons.Default.History
    CustomerAppPage.PROFILE_SUPPORT -> Icons.Default.Person
}

private fun CustomerAppPage.shortNavLabel(): String = when (this) {
    CustomerAppPage.HOME_SEARCH -> "Book Ride"
    CustomerAppPage.VEHICLE_FARE -> "Vehicles"
    CustomerAppPage.LIVE_TRACKING -> "Track"
    CustomerAppPage.PAYMENT -> "Payment"
    CustomerAppPage.RIDE_HISTORY -> "History"
    CustomerAppPage.PROFILE_SUPPORT -> "Profile"
}

private fun DriverAppTab.icon(): ImageVector = when (this) {
    DriverAppTab.DRIVER_HOME -> Icons.Default.Dashboard
    DriverAppTab.ACTIVE_TRIP -> Icons.Default.Navigation
    DriverAppTab.EARNINGS -> Icons.Default.AccountBalanceWallet
    DriverAppTab.KYC_DOCS -> Icons.Default.VerifiedUser
    DriverAppTab.VEHICLE_PROFILE -> Icons.Default.DirectionsCar
}

private fun AdminAppTab.icon(): ImageVector = when (this) {
    AdminAppTab.DASHBOARD -> Icons.Default.Dashboard
    AdminAppTab.BOOKINGS -> Icons.Default.Radar
    AdminAppTab.DRIVERS_FLEET -> Icons.Default.LocalShipping
    AdminAppTab.PRICING_MATCH -> Icons.Default.Security
    AdminAppTab.PAYMENTS_REPORTS -> Icons.Default.AccountBalanceWallet
}

private fun AdminAppTab.shortNavLabel(): String = when (this) {
    AdminAppTab.DASHBOARD -> "Dashboard"
    AdminAppTab.BOOKINGS -> "Bookings"
    AdminAppTab.DRIVERS_FLEET -> "Drivers"
    AdminAppTab.PRICING_MATCH -> "Matching"
    AdminAppTab.PAYMENTS_REPORTS -> "Reports"
}
