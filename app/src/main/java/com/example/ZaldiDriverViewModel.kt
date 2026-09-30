package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.errors.DiagnosticEvent
import com.example.core.errors.DiagnosticSeverity
import com.example.core.errors.GlobalErrorHandler
import com.example.core.network.DispatchEndpointTrace
import com.example.core.network.SocketConnectionState
import com.example.core.utils.GeoUtils
import com.example.core.utils.SurgeZoneCell
import com.example.features.authentication.ZaldiAuthSessionState
import com.example.features.authentication.ZaldiFirebaseAuthManager
import com.example.features.booking.AdminAppTab
import com.example.features.booking.CustomerAppPage
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.DriverWithVehiclesAndTrips
import com.example.services.localstorage.KycDocumentEntity
import com.example.services.localstorage.LedgerEntryEntity
import com.example.services.localstorage.ReviewEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.VehicleEntity
import com.example.services.localstorage.ZaldiRepository
import com.example.services.location.GpsTelemetryState
import com.example.services.location.LocationTelemetryBus
import com.example.services.location.ZaldiLocationForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ZaldiApplicationId(val code: String, val title: String, val pathLabel: String) {
    CUSTOMER_APP("CUSTOMER_APP", "1. Customer App", "apps/customer"),
    DRIVER_APP("DRIVER_APP", "2. Driver App", "apps/driver"),
    ADMIN_APP("ADMIN_APP", "3. Admin App", "apps/admin"),
    APPS_HUB("APPS_HUB", "3-Apps Hub", "apps/*")
}

enum class DriverAppTab(val route: String, val label: String) {
    DRIVER_HOME("driver_home", "Driver Home"),
    ACTIVE_TRIP("active_trip", "Active Trip"),
    EARNINGS("earnings", "Earnings"),
    KYC_DOCS("kyc_docs", "KYC & Docs"),
    VEHICLE_PROFILE("vehicle_profile", "Vehicle & Profile")
}

enum class ZaldiTab(val route: String, val label: String) {
    CUSTOMER_BOOKING("customer_booking", "Customer App"),
    DASHBOARD("dashboard", "Driver App"),
    MAP_TRACKING("map_tracking", "Navigation"),
    KYC_ONBOARDING("kyc_onboarding", "KYC & Docs"),
    EARNINGS("earnings", "Earnings"),
    PROFILE("profile", "Fleet & ERD")
}

data class ZaldiNavSnapshot(
    val activeApp: ZaldiApplicationId,
    val customerPage: CustomerAppPage,
    val driverTab: DriverAppTab,
    val adminTab: AdminAppTab,
    val legacyTab: ZaldiTab,
    val portalMode: String
)

class ZaldiDriverViewModel(
    private val repository: ZaldiRepository,
    private val firebaseAuthManager: ZaldiFirebaseAuthManager
) : ViewModel() {

    val authSessionState: StateFlow<ZaldiAuthSessionState> = firebaseAuthManager.sessionState

    private val defaultFastProfile = DriverProfileEntity(
        driverId = "d4f8a920-7c1b-4e9a-8b12-9042zaldi001",
        firstName = "Mateo",
        lastName = "Vance",
        phoneNumber = "+1 (415) 890-4210",
        kycStatus = "VERIFIED",
        isOnline = true,
        currentLat = 37.7897f,
        currentLng = -122.4014f,
        walletBalance = 428.60,
        rating = 4.94f,
        isAuthenticated = true,
        activeVehicleId = "v1a9c400-2b8e-4d11-9f01-vehicle00001",
        totalTrips = 648,
        acceptanceRate = 96,
        cancellationRate = 1,
        backendSocketUrl = "ws://10.0.2.2:3000",
        isDarkMode = true,
        isSelectedDriver = true
    )

    val driverProfile: StateFlow<DriverProfileEntity?> = repository.driverProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, defaultFastProfile)

    val allDrivers: StateFlow<List<DriverProfileEntity>> = repository.allDrivers
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(defaultFastProfile))

    val driverRelationalGraph: StateFlow<DriverWithVehiclesAndTrips?> = repository.driverRelationalGraph
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val kycDocuments: StateFlow<List<KycDocumentEntity>> = repository.kycDocuments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val vehicles: StateFlow<List<VehicleEntity>> = repository.vehicles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeOrder: StateFlow<RideOrderEntity?> = repository.activeOrder
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allOrders: StateFlow<List<RideOrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val ledgerEntries: StateFlow<List<LedgerEntryEntity>> = repository.ledgerEntries
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pendingOfflineEarningsLogs: StateFlow<List<LedgerEntryEntity>> = repository.pendingOfflineEarningsLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val reviews: StateFlow<List<ReviewEntity>> = repository.reviews
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val gpsTelemetry: StateFlow<GpsTelemetryState> = LocationTelemetryBus.telemetry
    val socketState: StateFlow<SocketConnectionState> = repository.networkClient.connectionState
    val socketPingMs: StateFlow<Int> = repository.networkClient.lastPingMs
    val lastDispatchTrace: StateFlow<DispatchEndpointTrace?> = repository.networkClient.lastDispatchTrace
    val diagnosticEvents: StateFlow<List<DiagnosticEvent>> = GlobalErrorHandler.events

    private val _activeApp = MutableStateFlow(ZaldiApplicationId.CUSTOMER_APP)
    val activeApp: StateFlow<ZaldiApplicationId> = _activeApp.asStateFlow()

    private val _customerPage = MutableStateFlow(CustomerAppPage.HOME_SEARCH)
    val customerPage: StateFlow<CustomerAppPage> = _customerPage.asStateFlow()

    private val _driverTab = MutableStateFlow(DriverAppTab.DRIVER_HOME)
    val driverTab: StateFlow<DriverAppTab> = _driverTab.asStateFlow()

    private val _adminTab = MutableStateFlow(AdminAppTab.DASHBOARD)
    val adminTab: StateFlow<AdminAppTab> = _adminTab.asStateFlow()

    private val _currentTab = MutableStateFlow(ZaldiTab.CUSTOMER_BOOKING)
    val currentTab: StateFlow<ZaldiTab> = _currentTab.asStateFlow()

    private val _bookingPortalMode = MutableStateFlow("CUSTOMER_APP")
    val bookingPortalMode: StateFlow<String> = _bookingPortalMode.asStateFlow()

    // Instant in-memory Navigation History Stack for 0ms "Previous" navigation across all 3 apps
    private val navHistory = ArrayDeque<ZaldiNavSnapshot>()
    private val _canGoPrevious = MutableStateFlow(false)
    val canGoPrevious: StateFlow<Boolean> = _canGoPrevious.asStateFlow()

    private val _surgeCells = MutableStateFlow(
        GeoUtils.generateSurgeHeatmapCells(37.7897, -122.4014)
    )
    val surgeCells: StateFlow<List<SurgeZoneCell>> = _surgeCells.asStateFlow()

    private val _selectedSurgeCell = MutableStateFlow<SurgeZoneCell?>(_surgeCells.value.firstOrNull())
    val selectedSurgeCell: StateFlow<SurgeZoneCell?> = _selectedSurgeCell.asStateFlow()

    private val _orderCountdownSeconds = MutableStateFlow(15)
    val orderCountdownSeconds: StateFlow<Int> = _orderCountdownSeconds.asStateFlow()

    private val _orderCountdownProgress = MutableStateFlow(1.0f)
    val orderCountdownProgress: StateFlow<Float> = _orderCountdownProgress.asStateFlow()

    private val _routeProgress = MutableStateFlow(0.15f)
    val routeProgress: StateFlow<Float> = _routeProgress.asStateFlow()

    private val _pendingOtpCode = MutableStateFlow<String?>(null)
    val pendingOtpCode: StateFlow<String?> = _pendingOtpCode.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _simulateRaceLockConflict = MutableStateFlow(false)
    val simulateRaceLockConflict: StateFlow<Boolean> = _simulateRaceLockConflict.asStateFlow()

    private var countdownJob: Job? = null
    private var navigationSimJob: Job? = null
    private var lastSyncedLat: Double = 0.0
    private var lastSyncedLng: Double = 0.0

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureSeedDataInitialized()
        }
        viewModelScope.launch(Dispatchers.IO) {
            gpsTelemetry.collect { gps ->
                if (gps.isTrackingActive) {
                    val dLat = kotlin.math.abs(gps.latitude - lastSyncedLat)
                    val dLng = kotlin.math.abs(gps.longitude - lastSyncedLng)
                    // Only write to SQLite when movement exceeds ~200m to prevent constant Room Flow recompositions
                    if (dLat > 0.002 || dLng > 0.002) {
                        lastSyncedLat = gps.latitude
                        lastSyncedLng = gps.longitude
                        repository.syncDriverGpsToTable(gps.latitude, gps.longitude)
                    }
                }
            }
        }
        viewModelScope.launch {
            activeOrder.collect { order ->
                if (order?.status == "INCOMING") {
                    startOrderCountdown(order)
                } else {
                    countdownJob?.cancel()
                }

                if (order != null && (order.status == "ACCEPTED" || order.status == "IN_PROGRESS")) {
                    startLiveRouteSimulation()
                } else if (order?.status == "ARRIVED_PICKUP") {
                    navigationSimJob?.cancel()
                    _routeProgress.value = 1.0f
                } else {
                    navigationSimJob?.cancel()
                    _routeProgress.value = 0.0f
                }
            }
        }
    }

    private fun currentSnapshot(): ZaldiNavSnapshot = ZaldiNavSnapshot(
        activeApp = _activeApp.value,
        customerPage = _customerPage.value,
        driverTab = _driverTab.value,
        adminTab = _adminTab.value,
        legacyTab = _currentTab.value,
        portalMode = _bookingPortalMode.value
    )

    private fun pushNavHistory() {
        val snap = currentSnapshot()
        if (navHistory.lastOrNull() != snap) {
            navHistory.addLast(snap)
            if (navHistory.size > 24) {
                navHistory.removeFirst()
            }
            _canGoPrevious.value = navHistory.isNotEmpty()
        }
    }

    fun setInitialApplication(appId: ZaldiApplicationId) {
        if (navHistory.isEmpty()) {
            _activeApp.value = appId
        }
    }

    fun selectApplication(appId: ZaldiApplicationId) {
        if (_activeApp.value == appId) return
        pushNavHistory()
        _activeApp.value = appId
    }

    fun selectCustomerPage(page: CustomerAppPage) {
        if (_customerPage.value == page && _activeApp.value == ZaldiApplicationId.CUSTOMER_APP) return
        pushNavHistory()
        _activeApp.value = ZaldiApplicationId.CUSTOMER_APP
        _customerPage.value = page
    }

    fun selectDriverTab(tab: DriverAppTab) {
        if (_driverTab.value == tab && _activeApp.value == ZaldiApplicationId.DRIVER_APP) return
        pushNavHistory()
        _activeApp.value = ZaldiApplicationId.DRIVER_APP
        _driverTab.value = tab
    }

    fun selectAdminTab(tab: AdminAppTab) {
        if (_adminTab.value == tab && _activeApp.value == ZaldiApplicationId.ADMIN_APP) return
        pushNavHistory()
        _activeApp.value = ZaldiApplicationId.ADMIN_APP
        _adminTab.value = tab
    }

    fun navigatePrevious() {
        if (navHistory.isNotEmpty()) {
            val prev = navHistory.removeLast()
            _activeApp.value = prev.activeApp
            _customerPage.value = prev.customerPage
            _driverTab.value = prev.driverTab
            _adminTab.value = prev.adminTab
            _currentTab.value = prev.legacyTab
            _bookingPortalMode.value = prev.portalMode
            _canGoPrevious.value = navHistory.isNotEmpty()
        } else {
            when (_activeApp.value) {
                ZaldiApplicationId.CUSTOMER_APP -> {
                    if (_customerPage.value != CustomerAppPage.HOME_SEARCH) {
                        _customerPage.value = CustomerAppPage.HOME_SEARCH
                    }
                }
                ZaldiApplicationId.DRIVER_APP -> {
                    if (_driverTab.value != DriverAppTab.DRIVER_HOME) {
                        _driverTab.value = DriverAppTab.DRIVER_HOME
                    } else {
                        _activeApp.value = ZaldiApplicationId.CUSTOMER_APP
                    }
                }
                ZaldiApplicationId.ADMIN_APP -> {
                    if (_adminTab.value != AdminAppTab.DASHBOARD) {
                        _adminTab.value = AdminAppTab.DASHBOARD
                    } else {
                        _activeApp.value = ZaldiApplicationId.CUSTOMER_APP
                    }
                }
                ZaldiApplicationId.APPS_HUB -> {
                    _activeApp.value = ZaldiApplicationId.CUSTOMER_APP
                }
            }
            _canGoPrevious.value = false
        }
    }

    fun selectTab(tab: ZaldiTab) {
        pushNavHistory()
        _currentTab.value = tab
        when (tab) {
            ZaldiTab.CUSTOMER_BOOKING -> _activeApp.value = ZaldiApplicationId.CUSTOMER_APP
            ZaldiTab.DASHBOARD -> {
                _activeApp.value = ZaldiApplicationId.DRIVER_APP
                _driverTab.value = DriverAppTab.DRIVER_HOME
            }
            ZaldiTab.MAP_TRACKING -> {
                _activeApp.value = ZaldiApplicationId.DRIVER_APP
                _driverTab.value = DriverAppTab.ACTIVE_TRIP
            }
            ZaldiTab.EARNINGS -> {
                _activeApp.value = ZaldiApplicationId.DRIVER_APP
                _driverTab.value = DriverAppTab.EARNINGS
            }
            ZaldiTab.KYC_ONBOARDING -> {
                _activeApp.value = ZaldiApplicationId.DRIVER_APP
                _driverTab.value = DriverAppTab.KYC_DOCS
            }
            ZaldiTab.PROFILE -> {
                _activeApp.value = ZaldiApplicationId.DRIVER_APP
                _driverTab.value = DriverAppTab.VEHICLE_PROFILE
            }
        }
    }

    fun openBookRideScreen() {
        selectApplication(ZaldiApplicationId.CUSTOMER_APP)
    }

    fun openWebAdminPanelScreen() {
        selectApplication(ZaldiApplicationId.ADMIN_APP)
    }

    fun setBookingPortalMode(mode: String) {
        if (_bookingPortalMode.value == mode) return
        pushNavHistory()
        _bookingPortalMode.value = mode
    }

    fun selectSurgeCell(cell: SurgeZoneCell) {
        _selectedSurgeCell.value = cell
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    fun toggleDriverOnline(context: Context, enable: Boolean) {
        val profile = driverProfile.value
        if (enable && profile?.kycStatus == "PENDING_DOCS") {
            _statusBannerMessage.value = "Complete KYC document verification before going Online."
            selectTab(ZaldiTab.KYC_ONBOARDING)
            return
        }
        _statusBannerMessage.value = if (enable) {
            "ONLINE • drivers.is_online=true & GPS Synced"
        } else {
            "OFFLINE • drivers.is_online=false"
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.setOnlineStatus(enable)
            if (enable) {
                ZaldiLocationForegroundService.startService(context)
            } else {
                ZaldiLocationForegroundService.stopService(context)
            }
        }
    }

    fun triggerIncomingOrderOffer(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = driverProfile.value
            if (profile != null && !profile.isOnline) {
                repository.setOnlineStatus(true)
            }
            val gps = gpsTelemetry.value
            repository.spawnIncomingDispatchOrder(gps.latitude, gps.longitude)
        }
    }

    private fun startOrderCountdown(order: RideOrderEntity) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val totalSeconds = 15
            for (sec in totalSeconds downTo 1) {
                if (!isActive) return@launch
                _orderCountdownSeconds.value = sec
                _orderCountdownProgress.value = sec.toFloat() / totalSeconds.toFloat()
                delay(1000L)
            }
            _orderCountdownSeconds.value = 0
            _orderCountdownProgress.value = 0f
            repository.updateOrderStatus(order, "DECLINED")
            _statusBannerMessage.value = "Trip ${order.orderId.take(8)}… timed out (15s SLA expired)"
            GlobalErrorHandler.logEvent(
                module = "features/order_engine",
                severity = DiagnosticSeverity.WARN,
                message = "Trip ${order.orderId.take(8)}… offer expired"
            )
        }
    }

    private fun startLiveRouteSimulation() {
        navigationSimJob?.cancel()
        _routeProgress.value = 0.18f
        navigationSimJob = viewModelScope.launch {
            while (isActive) {
                delay(3000L)
                val current = _routeProgress.value
                if (current < 0.94f) {
                    _routeProgress.value = (current + 0.08f).coerceAtMost(0.95f)
                }
            }
        }
    }

    fun toggleRaceLockSimulation(enabled: Boolean) {
        _simulateRaceLockConflict.value = enabled
        _statusBannerMessage.value = if (enabled) {
            "Race-Condition Lock Simulation ON (SELECT FOR UPDATE NOWAIT will conflict)"
        } else {
            "Race-Condition Lock Simulation OFF (Driver wins trip lock)"
        }
    }

    fun acceptIncomingOrder(order: RideOrderEntity) {
        countdownJob?.cancel()
        if (!_simulateRaceLockConflict.value) {
            _routeProgress.value = 0.15f
            if (_activeApp.value == ZaldiApplicationId.DRIVER_APP) {
                selectDriverTab(DriverAppTab.ACTIVE_TRIP)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val driverId = driverProfile.value?.driverId ?: order.driverId
            val (_, msg) = repository.acceptTripWithRaceLock(
                tripId = order.orderId,
                driverId = driverId,
                simulateCompetingDriverLock = _simulateRaceLockConflict.value
            )
            _statusBannerMessage.value = msg
        }
    }

    fun bookCustomCustomerRide(
        context: Context,
        customerName: String,
        pickupAddress: String,
        dropoffAddress: String,
        cargoDetails: String,
        cargoWeightKg: Int,
        vehicleTier: String,
        guaranteedFare: Double,
        distanceKm: Double,
        pickupLat: Double? = null,
        pickupLng: Double? = null,
        dropoffLat: Double? = null,
        dropoffLng: Double? = null
    ) {
        val profile = driverProfile.value
        _statusBannerMessage.value = "POST /api/internal/dispatch -> zaldi_ping sent to Driver ${profile?.firstName ?: "Mateo"}"
        viewModelScope.launch(Dispatchers.IO) {
            if (profile != null && !profile.isOnline) {
                repository.setOnlineStatus(true)
            }
            repository.createCustomCustomerRideBooking(
                customerName = customerName,
                pickupAddress = pickupAddress,
                dropoffAddress = dropoffAddress,
                cargoDetails = cargoDetails,
                cargoWeightKg = cargoWeightKg,
                vehicleTier = vehicleTier,
                guaranteedFare = guaranteedFare,
                distanceKm = distanceKm,
                pickupLatInput = pickupLat,
                pickupLngInput = pickupLng,
                dropoffLatInput = dropoffLat,
                dropoffLngInput = dropoffLng
            )
        }
    }

    fun registerCustomDriver(
        context: Context,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        vehicleMakeModel: String,
        plateNumber: String,
        capacityKg: Int,
        vehicleTier: String
    ) {
        _statusBannerMessage.value = "Registered & activated custom driver $firstName $lastName ($plateNumber)"
        viewModelScope.launch(Dispatchers.IO) {
            repository.createCustomDriver(
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                vehicleMakeModel = vehicleMakeModel,
                plateNumber = plateNumber,
                capacityKg = capacityKg,
                vehicleTier = vehicleTier
            )
        }
    }

    fun switchDriverSession(driverId: String) {
        _statusBannerMessage.value = "Switched active driver receiving zaldi_ping orders"
        viewModelScope.launch(Dispatchers.IO) {
            repository.switchActiveDriverSession(driverId)
        }
    }

    fun toggleSpecificDriverAvailability(driverId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowOnline = repository.toggleDriverOnlineById(driverId)
            _statusBannerMessage.value = "Admin Panel: Driver ${driverId.take(8)}… is now ${if (nowOnline) "ONLINE (5km Redis GEO)" else "OFFLINE"}"
        }
    }

    fun updateSpecificDriverApprovalStatus(driverId: String, status: String) {
        _statusBannerMessage.value = "Admin Panel: Driver ${driverId.take(8)}… set to $status"
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDriverApprovalById(driverId, status)
        }
    }

    fun submitCustomerTripReview(
        passengerName: String,
        rating: Int,
        comment: String,
        badgeTag: String
    ) {
        _statusBannerMessage.value = "Submitted ${rating}★ Customer Rating & Review to Room SQLite"
        viewModelScope.launch(Dispatchers.IO) {
            repository.submitCustomerRatingReview(passengerName, rating, comment, badgeTag)
        }
    }

    fun advanceOrderById(tripId: String) {
        val target = allOrders.value.firstOrNull { it.orderId == tripId } ?: return
        if (target.status == "INCOMING") {
            acceptIncomingOrder(target)
        } else {
            advanceOrderLifecycle(target)
        }
    }

    fun declineIncomingOrder(order: RideOrderEntity) {
        countdownJob?.cancel()
        _statusBannerMessage.value = "Trip ${order.orderId.take(8)}… Declined"
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOrderStatus(order, "DECLINED")
        }
    }

    fun attachProofOfDelivery(order: RideOrderEntity, podUrl: String) {
        _statusBannerMessage.value = "Saved trips.proof_of_delivery_url: $podUrl"
        viewModelScope.launch(Dispatchers.IO) {
            repository.attachProofOfDelivery(order, podUrl)
        }
    }

    fun advanceOrderLifecycle(order: RideOrderEntity) {
        when (order.status) {
            "ACCEPTED" -> {
                _routeProgress.value = 1.0f
                _statusBannerMessage.value = "Arrived at Pickup • Verify Rider PIN (${order.riderPin})"
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateOrderStatus(order, "ARRIVED_PICKUP")
                }
            }
            "ARRIVED_PICKUP" -> {
                _routeProgress.value = 0.18f
                _statusBannerMessage.value = "Trip In Progress • Capture Proof of Delivery at Dropoff"
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateOrderStatus(order, "IN_PROGRESS")
                }
            }
            "IN_PROGRESS" -> {
                _routeProgress.value = 1.0f
                _statusBannerMessage.value = "Trip ${order.orderId.take(8)}… Completed! +${GeoUtils.formatCurrency(order.totalPayout)} settled"
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateOrderStatus(order, "COMPLETED")
                }
            }
        }
    }

    fun revertOrderToPreviousStep(order: RideOrderEntity? = activeOrder.value) {
        countdownJob?.cancel()
        _routeProgress.value = 0.25f
        viewModelScope.launch(Dispatchers.IO) {
            val (_, msg) = repository.revertOrderToPreviousStatus(order)
            _statusBannerMessage.value = msg
        }
    }

    fun stepRouteProgressForward() {
        _routeProgress.value = (_routeProgress.value + 0.22f).coerceAtMost(0.98f)
    }

    fun requestOtpForPhone(phone: String) {
        val generatedOtp = "482910"
        _pendingOtpCode.value = generatedOtp
        _statusBannerMessage.value = "Zaldi Auth SMS Code: $generatedOtp"
    }

    fun verifyOtpAndLogin(phone: String, fullName: String, enteredOtp: String): Boolean {
        if (!GeoUtils.isValidOtp(enteredOtp)) {
            _statusBannerMessage.value = "Enter a valid 6-digit OTP code"
            return false
        }
        viewModelScope.launch {
            repository.updateAuthState(phone, fullName, isAuthenticated = true)
            _pendingOtpCode.value = null
            _statusBannerMessage.value = "Authenticated as $fullName"
        }
        return true
    }

    fun signInWithGoogleCredentialManager(
        context: Context,
        driverName: String,
        driverEmail: String,
        phone: String
    ) {
        viewModelScope.launch {
            val result = firebaseAuthManager.signInWithGoogleCredentialManager(
                activityContext = context,
                fallbackDriverName = driverName,
                fallbackDriverEmail = driverEmail
            )
            result.onSuccess { session ->
                repository.updateAuthState(
                    phoneNumber = phone,
                    fullName = session.displayName.ifBlank { driverName },
                    isAuthenticated = true,
                    email = session.email,
                    firebaseUid = session.firebaseUid
                )
                _pendingOtpCode.value = null
                _statusBannerMessage.value = session.lastAuthMessage
            }.onFailure { err ->
                _statusBannerMessage.value = "Google Sign-In error: ${err.message}"
            }
        }
    }

    fun signInWithFirebaseEmail(
        email: String,
        password: String,
        driverName: String,
        phone: String,
        isRegister: Boolean
    ) {
        viewModelScope.launch {
            val result = firebaseAuthManager.signInWithFirebaseEmailAndPassword(
                email = email,
                password = password,
                driverName = driverName,
                createNewAccount = isRegister
            )
            result.onSuccess { session ->
                repository.updateAuthState(
                    phoneNumber = phone,
                    fullName = session.displayName.ifBlank { driverName },
                    isAuthenticated = true,
                    email = session.email,
                    firebaseUid = session.firebaseUid
                )
                _pendingOtpCode.value = null
                _statusBannerMessage.value = session.lastAuthMessage
            }.onFailure { err ->
                _statusBannerMessage.value = "Firebase Auth error: ${err.message}"
            }
        }
    }

    fun saveDriverProfileInfo(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String
    ) {
        viewModelScope.launch {
            repository.saveDriverProfileToRoom(firstName, lastName, phoneNumber, email)
            _statusBannerMessage.value = "Saved Driver Profile ($firstName $lastName) to Room SQLite database"
        }
    }

    fun logOfflineEarningToRoom(
        title: String,
        routeSummary: String,
        amount: Double,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            repository.recordOfflineEarningsLog(title, routeSummary, amount, paymentMethod)
            _statusBannerMessage.value = "Logged offline earning (+${GeoUtils.formatCurrency(amount)}) to Room SQLite"
        }
    }

    fun syncOfflineEarningsLogs() {
        viewModelScope.launch {
            val count = repository.syncOfflineEarningsLogsToCloud()
            if (count > 0) {
                _statusBannerMessage.value = "Synced $count offline earnings logs from Room SQLite to Zaldi Cloud"
            } else {
                _statusBannerMessage.value = "All Room SQLite offline earnings logs are already synced"
            }
        }
    }

    fun signOutDriver(context: Context) {
        viewModelScope.launch {
            ZaldiLocationForegroundService.stopService(context)
            firebaseAuthManager.signOut()
            val current = driverProfile.value
            if (current != null) {
                repository.updateAuthState(current.phoneNumber, current.fullName, isAuthenticated = false)
            }
            _statusBannerMessage.value = "Signed out of Firebase Auth & Zaldi Driver"
        }
    }

    fun updateKycDocument(
        doc: KycDocumentEntity,
        docNumber: String,
        expiryDate: String,
        targetStatus: String
    ) {
        viewModelScope.launch {
            repository.submitKycDocument(doc, docNumber, expiryDate, targetStatus)
            _statusBannerMessage.value = "${doc.title} updated to $targetStatus"
        }
    }

    fun setOverallKycState(kycStatus: String) {
        viewModelScope.launch {
            repository.setOverallKycState(kycStatus)
            _statusBannerMessage.value = "drivers.kyc_status set to $kycStatus"
        }
    }

    fun executeInstantPayout(bankAccountLabel: String) {
        viewModelScope.launch {
            val ok = repository.triggerInstantPayout(bankAccountLabel)
            if (ok) {
                _statusBannerMessage.value = "Instant Payout settled to $bankAccountLabel"
            } else {
                _statusBannerMessage.value = "Minimum $5.00 balance required for Instant Payout"
            }
        }
    }

    fun addFleetVehicle(
        makeAndModel: String,
        plateNumber: String,
        capacityKg: Int,
        insuranceExpiry: String,
        vehicleTier: String
    ) {
        viewModelScope.launch {
            repository.addVehicle(makeAndModel, plateNumber, capacityKg, insuranceExpiry, vehicleTier)
            _statusBannerMessage.value = "Added $makeAndModel ($plateNumber • ${capacityKg}kg) to vehicles table"
        }
    }

    fun switchActiveVehicle(vehicleUuid: String) {
        viewModelScope.launch {
            repository.selectActiveVehicle(vehicleUuid)
            _statusBannerMessage.value = "Switched active vehicle (UUID ${vehicleUuid.take(8)}…)"
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            repository.toggleThemeMode()
        }
    }

    fun updateBackendUrl(url: String) {
        viewModelScope.launch {
            repository.updateBackendSocketUrl(url)
            _statusBannerMessage.value = "Updated Zaldi Backend Socket URL: $url"
        }
    }

    class Factory(
        private val repository: ZaldiRepository,
        private val firebaseAuthManager: ZaldiFirebaseAuthManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ZaldiDriverViewModel(repository, firebaseAuthManager) as T
        }
    }
}
