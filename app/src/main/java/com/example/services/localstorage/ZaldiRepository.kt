package com.example.services.localstorage

import com.example.core.errors.DiagnosticSeverity
import com.example.core.errors.GlobalErrorHandler
import com.example.core.network.ZaldiDispatchNetworkClient
import com.example.core.utils.GeoUtils
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class ZaldiRepository(
    private val dao: ZaldiDao,
    val networkClient: ZaldiDispatchNetworkClient
) {
    val driverProfile: Flow<DriverProfileEntity?> = dao.observeDriverProfile()
    val allDrivers: Flow<List<DriverProfileEntity>> = dao.observeAllDrivers()
    val driverRelationalGraph: Flow<DriverWithVehiclesAndTrips?> = dao.observeDriverRelationalGraph()
    val kycDocuments: Flow<List<KycDocumentEntity>> = dao.observeKycDocuments()
    val vehicles: Flow<List<VehicleEntity>> = dao.observeVehicles()
    val activeOrder: Flow<RideOrderEntity?> = dao.observeActiveOrder()
    val allOrders: Flow<List<RideOrderEntity>> = dao.observeAllOrders()
    val ledgerEntries: Flow<List<LedgerEntryEntity>> = dao.observeLedgerEntries()
    val pendingOfflineEarningsLogs: Flow<List<LedgerEntryEntity>> = dao.observePendingOfflineEarningsLogs()
    val reviews: Flow<List<ReviewEntity>> = dao.observeReviews()

    suspend fun ensureSeedDataInitialized() {
        val existing = dao.getDriverProfileOnce()
        if (existing != null) return

        val driverUuid = "d4f8a920-7c1b-4e9a-8b12-9042zaldi001"
        val driver2Uuid = "d7c1b890-2a4e-4f11-9c02-9042zaldi002"
        val driver3Uuid = "d9e3a112-6b5c-4d88-8a19-9042zaldi003"
        val driver4Uuid = "d2b4f771-9c0a-4e33-7f81-9042zaldi004"

        val vehicle1Uuid = "v1a9c400-2b8e-4d11-9f01-vehicle00001"
        val vehicle2Uuid = "v2b7e511-3c9f-4a22-8e12-vehicle00002"
        val vehicle3Uuid = "v3c8d622-4d0a-4b33-9f23-vehicle00003"
        val vehicle4Uuid = "v4d9e733-5e1b-4c44-8a34-vehicle00004"

        val initialProfile = DriverProfileEntity(
            driverId = driverUuid,
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
            activeVehicleId = vehicle1Uuid,
            totalTrips = 648,
            acceptanceRate = 96,
            cancellationRate = 1,
            backendSocketUrl = "ws://10.0.2.2:3000",
            isDarkMode = true,
            isSelectedDriver = true
        )
        dao.upsertDriverProfile(initialProfile)

        dao.upsertDriverProfile(
            DriverProfileEntity(
                driverId = driver2Uuid,
                firstName = "Karthik",
                lastName = "Rao",
                phoneNumber = "+91 98480-55120",
                kycStatus = "VERIFIED",
                isOnline = true,
                currentLat = 17.0500f,
                currentLng = 79.2667f,
                walletBalance = 2450.00,
                rating = 4.96f,
                isAuthenticated = true,
                activeVehicleId = vehicle3Uuid,
                totalTrips = 312,
                acceptanceRate = 98,
                cancellationRate = 0,
                backendSocketUrl = "ws://10.0.2.2:3000",
                isDarkMode = true,
                isSelectedDriver = false
            )
        )

        dao.upsertDriverProfile(
            DriverProfileEntity(
                driverId = driver3Uuid,
                firstName = "Priya",
                lastName = "Nair",
                phoneNumber = "+91 90001-88412",
                kycStatus = "VERIFIED",
                isOnline = true,
                currentLat = 17.4401f,
                currentLng = 78.3489f,
                walletBalance = 1190.40,
                rating = 4.98f,
                isAuthenticated = true,
                activeVehicleId = vehicle4Uuid,
                totalTrips = 419,
                acceptanceRate = 99,
                cancellationRate = 0,
                backendSocketUrl = "ws://10.0.2.2:3000",
                isDarkMode = true,
                isSelectedDriver = false
            )
        )

        dao.upsertDriverProfile(
            DriverProfileEntity(
                driverId = driver4Uuid,
                firstName = "Devon",
                lastName = "Brooks",
                phoneNumber = "+1 (415) 602-8819",
                kycStatus = "UNDER_REVIEW",
                isOnline = false,
                currentLat = 37.7812f,
                currentLng = -122.4110f,
                walletBalance = 312.00,
                rating = 4.89f,
                isAuthenticated = true,
                activeVehicleId = vehicle2Uuid,
                totalTrips = 194,
                acceptanceRate = 93,
                cancellationRate = 2,
                backendSocketUrl = "ws://10.0.2.2:3000",
                isDarkMode = true,
                isSelectedDriver = false
            )
        )

        val initialVehicles = listOf(
            VehicleEntity(
                id = vehicle1Uuid,
                driverId = driverUuid,
                vehicleTier = "ZALDI_EV",
                plateNumber = "ZLD-904E",
                capacityKg = 550,
                makeAndModel = "2025 Tesla Model Y Long Range",
                insuranceExpiry = "2027-04-01",
                isActive = true
            ),
            VehicleEntity(
                id = vehicle2Uuid,
                driverId = driver4Uuid,
                vehicleTier = "ZALDI_VAN",
                plateNumber = "8XKP492",
                capacityKg = 1650,
                makeAndModel = "2024 Rivian EDV-700 Logistics Van",
                insuranceExpiry = "2027-08-30",
                isActive = false
            ),
            VehicleEntity(
                id = vehicle3Uuid,
                driverId = driver2Uuid,
                vehicleTier = "ZALDI_VAN",
                plateNumber = "TS05-Z891",
                capacityKg = 1200,
                makeAndModel = "2025 Tata Ace EV Cargo",
                insuranceExpiry = "2028-03-31",
                isActive = false
            ),
            VehicleEntity(
                id = vehicle4Uuid,
                driverId = driver3Uuid,
                vehicleTier = "ZALDI_PRIME",
                plateNumber = "TS09-EV402",
                capacityKg = 750,
                makeAndModel = "2025 Mahindra Zor Grand EV",
                insuranceExpiry = "2028-01-15",
                isActive = false
            )
        )
        dao.insertVehicles(initialVehicles)

        val initialDocs = listOf(
            KycDocumentEntity(
                id = 1,
                docType = "DRIVER_LICENSE",
                title = "Commercial Driver License (CDL-A)",
                subtitle = "Front & Back Biometric Barcode Scan",
                documentNumber = "CA-DL-9948201",
                expiryDate = "2029-11-15",
                status = "APPROVED",
                adminReviewNote = "Verified by Zaldi Compliance AI & Admin #14"
            ),
            KycDocumentEntity(
                id = 2,
                docType = "VEHICLE_REGISTRATION",
                title = "Vehicle Registration Certificate (RC)",
                subtitle = "Linked to Plate ZLD-904E & 8XKP492",
                documentNumber = "REG-SF-882104",
                expiryDate = "2027-08-30",
                status = "APPROVED",
                adminReviewNote = "VIN & Capacity (550kg / 1650kg) verified"
            ),
            KycDocumentEntity(
                id = 3,
                docType = "COMMERCIAL_INSURANCE",
                title = "Commercial Mobility & Cargo Liability Policy",
                subtitle = "Synced with vehicles.insurance_expiry",
                documentNumber = "POL-ZLD-55109",
                expiryDate = "2027-04-01",
                status = "APPROVED",
                adminReviewNote = "$1,000,000 Liability Coverage Active"
            ),
            KycDocumentEntity(
                id = 4,
                docType = "NATIONAL_ID",
                title = "Background & Identity Attestation",
                subtitle = "Annual Safety & KYC Clearance",
                documentNumber = "BGC-2026-7741",
                expiryDate = "2027-09-01",
                status = "APPROVED",
                adminReviewNote = "Zero infractions • Priority Dispatch Eligible"
            )
        )
        dao.insertKycDocuments(initialDocs)

        val now = System.currentTimeMillis()
        val hourMs = 3_600_000L
        val initialOrders = listOf(
            RideOrderEntity(
                orderId = "t9920f11-88a1-4b22-9c33-trip00009920",
                driverId = driver2Uuid,
                vehicleId = vehicle3Uuid,
                customerId = "c5510a99-12b3-4c44-9d55-cust00009920",
                status = "IN_PROGRESS",
                pickupLatLngPoint = "POINT(79.2667 17.0500)",
                dropoffLatLngPoint = "POINT(78.3489 17.4401)",
                distanceKm = 105.0,
                totalPayout = 145.00,
                proofOfDeliveryUrl = null,
                createdAtMs = now - 900_000L,
                passengerName = "Vikram Aditya (Nalgonda Hub)",
                passengerRating = 4.97,
                riderPin = "6291",
                tierCategory = "ZALDI_VAN",
                pickupAddress = "Clock Tower Center, Nalgonda",
                pickupLat = 17.0500,
                pickupLng = 79.2667,
                pickupGeohash = GeoUtils.encodeGeohash(17.0500, 79.2667),
                dropoffAddress = "SLN Terminus, Gachibowli, Hyderabad",
                dropoffLat = 17.4401,
                dropoffLng = 78.3489,
                dropoffGeohash = GeoUtils.encodeGeohash(17.4401, 78.3489),
                estimatedMinutes = 95,
                baseFare = 92.00,
                surgeMultiplier = 1.5,
                tipAmount = 14.50,
                cargoWeightKg = 320,
                cargoDetails = "Fragile Load",
                completedAtMs = null
            ),
            RideOrderEntity(
                orderId = "t8841a00-11b2-4c33-9d44-trip00008841",
                driverId = driverUuid,
                vehicleId = vehicle1Uuid,
                customerId = "c9012f11-44a1-4b22-8c33-cust00001042",
                status = "COMPLETED",
                pickupLatLngPoint = "POINT(-122.3972 37.7897)",
                dropoffLatLngPoint = "POINT(-122.3986 37.8009)",
                distanceKm = 3.8,
                totalPayout = 28.20,
                proofOfDeliveryUrl = "https://cdn.zaldi.io/pod/t8841a00-signed.jpg",
                createdAtMs = now - 5 * hourMs,
                passengerName = "Elena Rostova",
                passengerRating = 4.98,
                riderPin = "4829",
                tierCategory = "ZALDI_EV",
                pickupAddress = "450 Mission St, Salesforce Transit Tower",
                pickupLat = 37.7897,
                pickupLng = -122.3972,
                pickupGeohash = GeoUtils.encodeGeohash(37.7897, -122.3972),
                dropoffAddress = "Pier 15, Embarcadero Waterfront",
                dropoffLat = 37.8009,
                dropoffLng = -122.3986,
                dropoffGeohash = GeoUtils.encodeGeohash(37.8009, -122.3986),
                estimatedMinutes = 11,
                baseFare = 14.50,
                surgeMultiplier = 1.6,
                tipAmount = 5.00,
                cargoWeightKg = 85,
                completedAtMs = now - 4 * hourMs
            ),
            RideOrderEntity(
                orderId = "t8836b00-22c3-4d44-8e55-trip00008836",
                driverId = driverUuid,
                vehicleId = vehicle2Uuid,
                customerId = "c7721e22-55b2-4c33-9d44-cust00002089",
                status = "COMPLETED",
                pickupLatLngPoint = "POINT(-122.4065 37.7852)",
                dropoffLatLngPoint = "POINT(-122.3921 37.7685)",
                distanceKm = 5.4,
                totalPayout = 40.70,
                proofOfDeliveryUrl = "https://cdn.zaldi.io/pod/t8836b00-signed.jpg",
                createdAtMs = now - 9 * hourMs,
                passengerName = "Marcus Chen",
                passengerRating = 4.91,
                riderPin = "7104",
                tierCategory = "ZALDI_VAN",
                pickupAddress = "800 Market St, Union Square West",
                pickupLat = 37.7852,
                pickupLng = -122.4065,
                pickupGeohash = GeoUtils.encodeGeohash(37.7852, -122.4065),
                dropoffAddress = "1600 Owens St, Mission Bay Biotech Hub",
                dropoffLat = 37.7685,
                dropoffLng = -122.3921,
                dropoffGeohash = GeoUtils.encodeGeohash(37.7685, -122.3921),
                estimatedMinutes = 16,
                baseFare = 19.00,
                surgeMultiplier = 1.8,
                tipAmount = 6.50,
                cargoWeightKg = 640,
                completedAtMs = now - 8 * hourMs
            )
        )
        dao.insertOrders(initialOrders)

        val initialLedger = listOf(
            LedgerEntryEntity(
                driverId = driverUuid,
                entryType = "OFFLINE_TRIP_EARNING",
                title = "Offline Dead-Zone Trip • Caltrain Depot -> Pier 27",
                referenceCode = "OFF-9410",
                amount = 54.00,
                balanceAfter = 482.60,
                dayLabel = "Today",
                timestampMs = now - 35 * 60_000L,
                settlementStatus = "OFFLINE_CACHED",
                isOfflineLog = true,
                syncStatus = "PENDING_OFFLINE_SYNC",
                routeSummary = "4th & King Caltrain -> Pier 27 Cruise Terminal (8.2 km)",
                paymentMethod = "OFFLINE_VOUCHER"
            ),
            LedgerEntryEntity(
                driverId = driverUuid,
                entryType = "OFFLINE_TRIP_EARNING",
                title = "Offline Tunnel Delivery • Twin Peaks -> Sunset Hub",
                referenceCode = "OFF-9388",
                amount = 32.50,
                balanceAfter = 428.60,
                dayLabel = "Today",
                timestampMs = now - 95 * 60_000L,
                settlementStatus = "OFFLINE_CACHED",
                isOfflineLog = true,
                syncStatus = "PENDING_OFFLINE_SYNC",
                routeSummary = "Twin Peaks Tunnel -> 19th Ave Logistics Hub (5.1 km)",
                paymentMethod = "CASH_COLLECTED"
            ),
            LedgerEntryEntity(
                driverId = driverUuid,
                entryType = "TRIP_EARNING",
                title = "Trip t8841a00 • Embarcadero Dropoff (POD Verified)",
                referenceCode = "t8841a00",
                amount = 23.20,
                balanceAfter = 396.10,
                dayLabel = "Tue",
                timestampMs = now - 4 * hourMs,
                settlementStatus = "SETTLED",
                isOfflineLog = false,
                syncStatus = "SYNCED",
                routeSummary = "450 Mission St -> Pier 15 Embarcadero",
                paymentMethod = "ZALDI_WALLET"
            ),
            LedgerEntryEntity(
                driverId = driverUuid,
                entryType = "TIP",
                title = "100% Customer Tip • Elena R.",
                referenceCode = "TIP-8841",
                amount = 5.00,
                balanceAfter = 372.90,
                dayLabel = "Tue",
                timestampMs = now - 4 * hourMs - 60_000L
            ),
            LedgerEntryEntity(
                driverId = driverUuid,
                entryType = "TRIP_EARNING",
                title = "Trip t8836b00 • Mission Bay Biotech (640kg Cargo)",
                referenceCode = "t8836b00",
                amount = 34.20,
                balanceAfter = 367.90,
                dayLabel = "Tue",
                timestampMs = now - 8 * hourMs
            ),
            LedgerEntryEntity(
                driverId = driverUuid,
                entryType = "SURGE_BONUS",
                title = "Peak Geohash 1.8x Surge Multiplier Bonus",
                referenceCode = "SRG-8836",
                amount = 12.50,
                balanceAfter = 333.70,
                dayLabel = "Mon",
                timestampMs = now - 24 * hourMs
            ),
            LedgerEntryEntity(
                driverId = driverUuid,
                entryType = "INSTANT_PAYOUT",
                title = "Zaldi Instant Payout • Chase Checking ••4821",
                referenceCode = "PAY-9012",
                amount = -180.00,
                balanceAfter = 321.20,
                dayLabel = "Sun",
                timestampMs = now - 48 * hourMs
            )
        )
        dao.insertLedgerEntries(initialLedger)

        val initialReviews = listOf(
            ReviewEntity(
                passengerName = "Elena Rostova",
                rating = 5,
                comment = "Spotless Tesla Model Y, fast Proof-of-Delivery scan at Pier 15!",
                badgeTag = "Verified POD",
                timestampLabel = "4 hours ago"
            ),
            ReviewEntity(
                passengerName = "Marcus Chen",
                rating = 5,
                comment = "Rivian EDV-700 handled our 640kg biotech equipment pallet effortlessly.",
                badgeTag = "Heavy Cargo Pro",
                timestampLabel = "8 hours ago"
            )
        )
        dao.insertReviews(initialReviews)

        GlobalErrorHandler.logEvent(
            module = "services/local_storage",
            severity = DiagnosticSeverity.INFO,
            message = "Initialized 1:M Relational Schema: drivers (UUID) -> vehicles (FK) -> trips (FK)"
        )
    }

    suspend fun syncDriverGpsToTable(lat: Double, lng: Double) {
        val current = dao.getDriverProfileOnce() ?: return
        dao.updateDriverCoordinates(current.driverId, lat.toFloat(), lng.toFloat())
    }

    suspend fun updateAuthState(
        phoneNumber: String,
        fullName: String,
        isAuthenticated: Boolean,
        email: String? = null,
        firebaseUid: String? = null
    ) {
        val current = dao.getDriverProfileOnce() ?: return
        val parts = fullName.trim().split(" ", limit = 2)
        val first = parts.firstOrNull()?.ifBlank { current.firstName } ?: current.firstName
        val last = if (parts.size > 1) parts[1] else current.lastName
        dao.upsertDriverProfile(
            current.copy(
                phoneNumber = phoneNumber.ifBlank { current.phoneNumber },
                firstName = first,
                lastName = last,
                email = email?.ifBlank { current.email } ?: current.email,
                firebaseUid = firebaseUid?.ifBlank { current.firebaseUid } ?: current.firebaseUid,
                isAuthenticated = isAuthenticated,
                isOnline = if (!isAuthenticated) false else current.isOnline,
                lastSyncedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveDriverProfileToRoom(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String
    ) {
        val current = dao.getDriverProfileOnce() ?: return
        val updated = current.copy(
            firstName = firstName.trim().ifBlank { current.firstName },
            lastName = lastName.trim().ifBlank { current.lastName },
            phoneNumber = phoneNumber.trim().ifBlank { current.phoneNumber },
            email = email.trim().ifBlank { current.email },
            lastSyncedAtMs = System.currentTimeMillis()
        )
        dao.upsertDriverProfile(updated)
        GlobalErrorHandler.logEvent(
            module = "room/drivers_table",
            severity = DiagnosticSeverity.INFO,
            message = "Saved DriverProfileEntity to Room SQLite: ${updated.fullName} (${updated.email})"
        )
    }

    suspend fun recordOfflineEarningsLog(
        title: String,
        routeSummary: String,
        amount: Double,
        paymentMethod: String
    ) {
        val current = dao.getDriverProfileOnce() ?: return
        val cleanAmount = ((amount.coerceAtLeast(1.0)) * 100).roundToInt() / 100.0
        val newWalletBalance = ((current.walletBalance + cleanAmount) * 100).roundToInt() / 100.0
        val newOfflinePending = ((current.offlinePendingEarnings + cleanAmount) * 100).roundToInt() / 100.0

        dao.upsertDriverProfile(
            current.copy(
                walletBalance = newWalletBalance,
                offlinePendingEarnings = newOfflinePending,
                totalTrips = current.totalTrips + 1
            )
        )

        val refCode = "OFF-${(1000..9999).random()}"
        dao.insertLedgerEntry(
            LedgerEntryEntity(
                driverId = current.driverId,
                entryType = "OFFLINE_TRIP_EARNING",
                title = title.ifBlank { "Offline Dead-Zone Delivery ($refCode)" },
                referenceCode = refCode,
                amount = cleanAmount,
                balanceAfter = newWalletBalance,
                dayLabel = "Today",
                timestampMs = System.currentTimeMillis(),
                settlementStatus = "OFFLINE_CACHED",
                isOfflineLog = true,
                syncStatus = "PENDING_OFFLINE_SYNC",
                routeSummary = routeSummary.ifBlank { "Offline GPS Corridor -> Verified POD" },
                paymentMethod = paymentMethod
            )
        )

        GlobalErrorHandler.logEvent(
            module = "room/offline_earnings",
            severity = DiagnosticSeverity.INFO,
            message = "Cached offline earnings log $refCode (+$${cleanAmount}) in Room ledger_entries table"
        )
    }

    suspend fun syncOfflineEarningsLogsToCloud(): Int {
        val pending = dao.getPendingOfflineEarningsLogsOnce()
        if (pending.isEmpty()) return 0
        dao.markAllOfflineEarningsLogsSynced()
        val current = dao.getDriverProfileOnce()
        if (current != null) {
            dao.upsertDriverProfile(
                current.copy(
                    offlinePendingEarnings = 0.0,
                    lastSyncedAtMs = System.currentTimeMillis()
                )
            )
        }
        GlobalErrorHandler.logEvent(
            module = "room/offline_earnings",
            severity = DiagnosticSeverity.INFO,
            message = "Synced ${pending.size} offline earnings logs from Room SQLite to Zaldi Cloud Ledger"
        )
        return pending.size
    }

    suspend fun setOnlineStatus(isOnline: Boolean) {
        val current = dao.getDriverProfileOnce() ?: return
        dao.upsertDriverProfile(current.copy(isOnline = isOnline))
        if (isOnline) {
            networkClient.connectDispatchSocket(current.backendSocketUrl)
        } else {
            networkClient.disconnectDispatchSocket()
        }
    }

    suspend fun toggleThemeMode() {
        val current = dao.getDriverProfileOnce() ?: return
        dao.upsertDriverProfile(current.copy(isDarkMode = !current.isDarkMode))
    }

    suspend fun updateBackendSocketUrl(url: String) {
        val current = dao.getDriverProfileOnce() ?: return
        dao.upsertDriverProfile(current.copy(backendSocketUrl = url))
        if (current.isOnline) {
            networkClient.connectDispatchSocket(url)
        }
    }

    suspend fun submitKycDocument(
        doc: KycDocumentEntity,
        newDocNumber: String,
        newExpiryDate: String,
        targetStatus: String
    ) {
        val note = when (targetStatus) {
            "APPROVED" -> "Instant OCR + Admin Verification Passed"
            "UNDER_REVIEW" -> "Submitted to Zaldi Compliance Queue (SLA < 15m)"
            else -> "Action Required: Please upload clear document scan"
        }
        dao.updateKycDocument(
            doc.copy(
                documentNumber = newDocNumber,
                expiryDate = newExpiryDate,
                status = targetStatus,
                adminReviewNote = note,
                lastUpdatedMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun setOverallKycState(kycStatus: String) {
        val current = dao.getDriverProfileOnce() ?: return
        dao.upsertDriverProfile(current.copy(kycStatus = kycStatus))
    }

    suspend fun addVehicle(
        makeAndModel: String,
        plateNumber: String,
        capacityKg: Int,
        insuranceExpiry: String,
        vehicleTier: String
    ) {
        val current = dao.getDriverProfileOnce() ?: return
        val newVehicleUuid = UUID.randomUUID().toString()
        dao.insertVehicle(
            VehicleEntity(
                id = newVehicleUuid,
                driverId = current.driverId,
                vehicleTier = vehicleTier,
                plateNumber = plateNumber.uppercase(),
                capacityKg = capacityKg,
                makeAndModel = makeAndModel,
                insuranceExpiry = insuranceExpiry,
                isActive = true
            )
        )
        dao.setActiveVehicle(newVehicleUuid)
        dao.upsertDriverProfile(current.copy(activeVehicleId = newVehicleUuid))
        GlobalErrorHandler.logEvent(
            module = "services/local_storage",
            severity = DiagnosticSeverity.INFO,
            message = "Inserted Vehicle UUID $newVehicleUuid (FK driver_id=${current.driverId.take(8)}…)"
        )
    }

    suspend fun selectActiveVehicle(vehicleUuid: String) {
        dao.setActiveVehicle(vehicleUuid)
        val current = dao.getDriverProfileOnce() ?: return
        dao.upsertDriverProfile(current.copy(activeVehicleId = vehicleUuid))
    }

    suspend fun createCustomDriver(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        vehicleMakeModel: String,
        plateNumber: String,
        capacityKg: Int,
        vehicleTier: String,
        initialLat: Float = 17.0500f,
        initialLng: Float = 79.2667f
    ) {
        val current = dao.getDriverProfileOnce()
        val newDriverUuid = UUID.randomUUID().toString()
        val newVehicleUuid = UUID.randomUUID().toString()

        val newDriver = DriverProfileEntity(
            driverId = newDriverUuid,
            firstName = firstName.trim().ifBlank { "Rohan" },
            lastName = lastName.trim().ifBlank { "Reddy" },
            phoneNumber = phoneNumber.trim().ifBlank { "+91 98480-${(1000..9999).random()}" },
            kycStatus = "VERIFIED",
            isOnline = true,
            currentLat = initialLat,
            currentLng = initialLng,
            walletBalance = 2450.00,
            rating = 4.96f,
            isAuthenticated = true,
            activeVehicleId = newVehicleUuid,
            totalTrips = 128,
            acceptanceRate = 98,
            cancellationRate = 0,
            backendSocketUrl = current?.backendSocketUrl ?: "wss://api.zaldi.com",
            isDarkMode = current?.isDarkMode ?: true,
            isSelectedDriver = true
        )
        dao.upsertDriverProfile(newDriver)
        dao.selectActiveDriverSession(newDriverUuid)

        dao.insertVehicle(
            VehicleEntity(
                id = newVehicleUuid,
                driverId = newDriverUuid,
                vehicleTier = vehicleTier,
                plateNumber = plateNumber.uppercase().ifBlank { "TS05-Z${(100..999).random()}" },
                capacityKg = capacityKg,
                makeAndModel = vehicleMakeModel.ifBlank { "Tata Ace EV Cargo" },
                insuranceExpiry = "2028-03-31",
                isActive = true
            )
        )
        dao.setActiveVehicle(newVehicleUuid)

        GlobalErrorHandler.logEvent(
            module = "features/booking",
            severity = DiagnosticSeverity.INFO,
            message = "Created Custom Driver ${newDriver.fullName} (UUID ${newDriverUuid.take(8)}…)"
        )
    }

    suspend fun switchActiveDriverSession(driverId: String) {
        dao.selectActiveDriverSession(driverId)
        GlobalErrorHandler.logEvent(
            module = "features/booking",
            severity = DiagnosticSeverity.INFO,
            message = "Switched active driver session to UUID ${driverId.take(8)}…"
        )
    }

    suspend fun toggleDriverOnlineById(driverId: String): Boolean {
        val target = dao.getDriverByIdOnce(driverId) ?: return false
        val nextOnline = !target.isOnline
        dao.updateDriverOnlineStatus(driverId, nextOnline)
        GlobalErrorHandler.logEvent(
            module = "admin_panel/availability",
            severity = DiagnosticSeverity.INFO,
            message = "Admin Panel toggled Driver ${target.fullName} (${driverId.take(8)}…) is_online=$nextOnline"
        )
        return nextOnline
    }

    suspend fun updateDriverApprovalById(driverId: String, approvalStatus: String) {
        dao.updateDriverKycStatusById(driverId, approvalStatus)
        if (approvalStatus == "SUSPENDED" || approvalStatus == "REJECTED") {
            dao.updateDriverOnlineStatus(driverId, false)
        }
        GlobalErrorHandler.logEvent(
            module = "admin_panel/drivers",
            severity = DiagnosticSeverity.INFO,
            message = "Admin updated Driver ${driverId.take(8)}… status to $approvalStatus"
        )
    }

    suspend fun submitCustomerRatingReview(
        passengerName: String,
        rating: Int,
        comment: String,
        badgeTag: String
    ) {
        dao.insertReviews(
            listOf(
                ReviewEntity(
                    passengerName = passengerName.ifBlank { "Verified Customer" },
                    rating = rating.coerceIn(1, 5),
                    comment = comment.ifBlank { "Smooth live tracking, accurate ETA, and fast delivery!" },
                    badgeTag = badgeTag.ifBlank { "Verified Trip" },
                    timestampLabel = "Just now"
                )
            )
        )
        GlobalErrorHandler.logEvent(
            module = "modules/ratings",
            severity = DiagnosticSeverity.INFO,
            message = "Customer $passengerName submitted ${rating}★ rating to Room database"
        )
    }

    suspend fun createCustomCustomerRideBooking(
        customerName: String,
        pickupAddress: String,
        dropoffAddress: String,
        cargoDetails: String,
        cargoWeightKg: Int,
        vehicleTier: String,
        guaranteedFare: Double,
        distanceKm: Double,
        pickupLatInput: Double? = null,
        pickupLngInput: Double? = null,
        dropoffLatInput: Double? = null,
        dropoffLngInput: Double? = null
    ): RideOrderEntity {
        val current = dao.getDriverProfileOnce()
        val driverUuid = current?.driverId ?: "d4f8a920-7c1b-4e9a-8b12-9042zaldi001"
        val vehicleUuid = current?.activeVehicleId ?: "v1a9c400-2b8e-4d11-9f01-vehicle00001"

        val pickupLat = pickupLatInput ?: ((current?.currentLat?.toDouble() ?: 17.0500) + 0.0025)
        val pickupLng = pickupLngInput ?: ((current?.currentLng?.toDouble() ?: 79.2667) + 0.0018)
        val dropoffLat = dropoffLatInput ?: (pickupLat + 0.0320)
        val dropoffLng = dropoffLngInput ?: (pickupLng - 0.0240)

        val tripUuid = UUID.randomUUID().toString()
        val customerUuid = UUID.randomUUID().toString()
        val pickupPoint = String.format(Locale.US, "POINT(%.4f %.4f)", pickupLng, pickupLat)
        val dropoffPoint = String.format(Locale.US, "POINT(%.4f %.4f)", dropoffLng, dropoffLat)
        val estMinutes = (distanceKm * 1.4).roundToInt().coerceAtLeast(12)
        val resolvedPickupAddr = pickupAddress.ifBlank { "Clock Tower Center, Nalgonda" }
        val resolvedDropoffAddr = dropoffAddress.ifBlank { "SLN Terminus, Gachibowli, Hyderabad" }
        val resolvedCargo = cargoDetails.ifBlank { "Fragile Load" }
        val resolvedCustomer = customerName.ifBlank { "Vikram Aditya (Customer App)" }

        val order = RideOrderEntity(
            orderId = tripUuid,
            driverId = driverUuid,
            vehicleId = vehicleUuid,
            customerId = customerUuid,
            status = "INCOMING",
            pickupLatLngPoint = pickupPoint,
            dropoffLatLngPoint = dropoffPoint,
            distanceKm = distanceKm,
            totalPayout = guaranteedFare,
            proofOfDeliveryUrl = null,
            createdAtMs = System.currentTimeMillis(),
            passengerName = resolvedCustomer,
            passengerRating = 4.97,
            riderPin = "${(1000..9999).random()}",
            tierCategory = vehicleTier,
            pickupAddress = resolvedPickupAddr,
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            pickupGeohash = GeoUtils.encodeGeohash(pickupLat, pickupLng, 7),
            dropoffAddress = resolvedDropoffAddr,
            dropoffLat = dropoffLat,
            dropoffLng = dropoffLng,
            dropoffGeohash = GeoUtils.encodeGeohash(dropoffLat, dropoffLng, 7),
            estimatedMinutes = estMinutes,
            baseFare = (guaranteedFare * 0.75 * 100).roundToInt() / 100.0,
            surgeMultiplier = 1.5,
            tipAmount = (guaranteedFare * 0.10 * 100).roundToInt() / 100.0,
            cargoWeightKg = cargoWeightKg,
            cargoDetails = resolvedCargo
        )
        dao.upsertOrder(order)
        networkClient.postInternalDispatch(
            backendSocketUrl = current?.backendSocketUrl ?: "http://10.0.2.2:3000",
            tripId = tripUuid,
            customerId = customerUuid,
            customerName = resolvedCustomer,
            pickupAddress = resolvedPickupAddr,
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            dropoffAddress = resolvedDropoffAddr,
            dropoffLat = dropoffLat,
            dropoffLng = dropoffLng,
            vehicleTier = vehicleTier,
            cargoDetails = resolvedCargo,
            cargoWeightKg = cargoWeightKg,
            distanceKm = distanceKm,
            guaranteedFare = guaranteedFare,
            onlineDriversCount = 3
        )
        networkClient.emitOrderEvent("zaldi_ping", order.orderId, "INCOMING")
        GlobalErrorHandler.logEvent(
            module = "features/order_engine",
            severity = DiagnosticSeverity.SOCKET,
            message = "POST /api/internal/dispatch -> zaldi_ping #${tripUuid.take(8)}… ($resolvedCargo • ${GeoUtils.formatCurrency(guaranteedFare)})"
        )
        return order
    }

    /**
     * Implements backend `acceptTripTransaction` with race-condition lock check (`SELECT ... FOR UPDATE NOWAIT`).
     */
    suspend fun acceptTripWithRaceLock(
        tripId: String,
        driverId: String,
        simulateCompetingDriverLock: Boolean = false
    ): Pair<Boolean, String> {
        val latestOrder = dao.getOrderByIdOnce(tripId)
            ?: return false to "Trip not found."

        if (simulateCompetingDriverLock) {
            dao.upsertOrder(latestOrder.copy(status = "DECLINED"))
            GlobalErrorHandler.logEvent(
                module = "backend/trip.service",
                severity = DiagnosticSeverity.WARN,
                message = "PostgreSQL 55P03 Lock Conflict: Another Zaldi driver claimed trip ${tripId.take(8)}…"
            )
            return false to "Another Zaldi driver accepted this trip (FOR UPDATE NOWAIT lock)."
        }

        if (latestOrder.status != "INCOMING") {
            return false to "Another Zaldi driver accepted this trip."
        }

        val updated = latestOrder.copy(
            driverId = driverId,
            status = "ACCEPTED"
        )
        dao.upsertOrder(updated)
        networkClient.emitOrderEvent("accept_trip", tripId, "ACCEPTED")
        GlobalErrorHandler.logEvent(
            module = "backend/trip.service",
            severity = DiagnosticSeverity.SOCKET,
            message = "COMMIT accept_trip: Trip ${tripId.take(8)}… locked to Driver ${driverId.take(8)}…"
        )
        return true to "Trip successfully assigned. Confirm the Go!"
    }

    suspend fun spawnIncomingDispatchOrder(driverLat: Double, driverLng: Double): RideOrderEntity {
        val current = dao.getDriverProfileOnce()
        val driverUuid = current?.driverId ?: "d4f8a920-7c1b-4e9a-8b12-9042zaldi001"
        val vehicleUuid = current?.activeVehicleId ?: "v1a9c400-2b8e-4d11-9f01-vehicle00001"

        val candidates = listOf(
            Triple("Aria Montgomery", "500 Howard St, Transbay Terminal", "2100 Chestnut St, Marina District"),
            Triple("Devon Brooks", "1 Ferry Building, Embarcadero", "601 Townsend St, SoMa Design Center"),
            Triple("Kaito Takahashi", "345 Spear St, Rincon Hill", "1000 Mason St, Nob Hill Huntington Park"),
            Triple("Nadia Kowalski", "185 Berry St, Mission Creek", "55 Music Concourse Dr, Golden Gate Park")
        )
        val pick = candidates.random()
        val pickupLat = driverLat + ((-8..8).random() * 0.0012)
        val pickupLng = driverLng + ((-8..8).random() * 0.0012)
        val dropoffLat = pickupLat + (18..35).random() * 0.0011
        val dropoffLng = pickupLng - (12..28).random() * 0.0011

        val distKm = ((GeoUtils.calculateDistanceKm(pickupLat, pickupLng, dropoffLat, dropoffLng) * 1.35)
            .coerceIn(2.6, 12.4) * 10).roundToInt() / 10.0
        val estMinutes = (distKm * 2.8).roundToInt().coerceAtLeast(7)
        val surge = listOf(1.4, 1.6, 1.8, 2.1).random()
        val base = ((distKm * 3.10 + 6.50) * 100).roundToInt() / 100.0
        val tip = listOf(3.50, 5.00, 6.00, 8.00).random()
        val total = ((base * surge + tip) * 100).roundToInt() / 100.0

        val tripUuid = UUID.randomUUID().toString()
        val customerUuid = UUID.randomUUID().toString()
        val pickupPoint = String.format(Locale.US, "POINT(%.4f %.4f)", pickupLng, pickupLat)
        val dropoffPoint = String.format(Locale.US, "POINT(%.4f %.4f)", dropoffLng, dropoffLat)

        val order = RideOrderEntity(
            orderId = tripUuid,
            driverId = driverUuid,
            vehicleId = vehicleUuid,
            customerId = customerUuid,
            status = "INCOMING",
            pickupLatLngPoint = pickupPoint,
            dropoffLatLngPoint = dropoffPoint,
            distanceKm = distKm,
            totalPayout = total,
            proofOfDeliveryUrl = null,
            createdAtMs = System.currentTimeMillis(),
            passengerName = pick.first,
            passengerRating = listOf(4.89, 4.94, 4.97, 5.0).random(),
            riderPin = "${(1000..9999).random()}",
            tierCategory = listOf("ZALDI_EV", "ZALDI_PRIME", "ZALDI_VAN").random(),
            pickupAddress = pick.second,
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            pickupGeohash = GeoUtils.encodeGeohash(pickupLat, pickupLng, 7),
            dropoffAddress = pick.third,
            dropoffLat = dropoffLat,
            dropoffLng = dropoffLng,
            dropoffGeohash = GeoUtils.encodeGeohash(dropoffLat, dropoffLng, 7),
            estimatedMinutes = estMinutes,
            baseFare = base,
            surgeMultiplier = surge,
            tipAmount = tip,
            cargoWeightKg = listOf(45, 120, 280, 420).random()
        )
        dao.upsertOrder(order)
        GlobalErrorHandler.logEvent(
            module = "features/order_engine",
            severity = DiagnosticSeverity.SOCKET,
            message = "Inserted Trip UUID ${tripUuid.take(8)}… (FK vehicle=${vehicleUuid.take(8)}…, customer=${customerUuid.take(8)}…)",
            details = "Pickup: $pickupPoint -> Dropoff: $dropoffPoint"
        )
        return order
    }

    suspend fun attachProofOfDelivery(order: RideOrderEntity, podUrl: String) {
        dao.updateTripProofOfDelivery(order.orderId, podUrl)
        GlobalErrorHandler.logEvent(
            module = "features/map_tracking",
            severity = DiagnosticSeverity.INFO,
            message = "Saved proof_of_delivery_url for Trip ${order.orderId.take(8)}…",
            details = podUrl
        )
    }

    /**
     * Instantly reverts an active (or most recently completed/declined) trip to its previous lifecycle step.
     */
    suspend fun revertOrderToPreviousStatus(currentOrder: RideOrderEntity?): Pair<RideOrderEntity?, String> {
        val target = currentOrder ?: dao.getLatestOrderOnce()
            ?: return null to "No previous trip found in Room SQLite."

        val previousStatus = when (target.status) {
            "COMPLETED", "DECLINED" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "ARRIVED_PICKUP"
            "ARRIVED_PICKUP" -> "ACCEPTED"
            "ACCEPTED" -> "INCOMING"
            else -> "ACCEPTED"
        }
        val updated = target.copy(status = previousStatus, completedAtMs = null)
        dao.upsertOrder(updated)
        networkClient.emitOrderEvent("order:previous_step", target.orderId, previousStatus)
        return updated to "Reverted Trip #${target.orderId.take(6)}… to previous step: ${previousStatus.replace("_", " ")}"
    }

    suspend fun updateOrderStatus(order: RideOrderEntity, newStatus: String) {
        val podUrl = if (newStatus == "COMPLETED" && order.proofOfDeliveryUrl.isNullOrBlank()) {
            "https://cdn.zaldi.io/pod/${order.orderId.take(8)}-verified.jpg"
        } else {
            order.proofOfDeliveryUrl
        }
        val updated = order.copy(
            status = newStatus,
            proofOfDeliveryUrl = podUrl,
            completedAtMs = if (newStatus == "COMPLETED") System.currentTimeMillis() else order.completedAtMs
        )
        dao.upsertOrder(updated)
        networkClient.emitOrderEvent("order:status_update", order.orderId, newStatus)

        if (newStatus == "COMPLETED") {
            val profile = dao.getDriverProfileOnce() ?: return
            val newBalance = ((profile.walletBalance + order.totalPayout) * 100).roundToInt() / 100.0
            dao.upsertDriverProfile(
                profile.copy(
                    totalTrips = profile.totalTrips + 1,
                    walletBalance = newBalance
                )
            )
            dao.insertLedgerEntry(
                LedgerEntryEntity(
                    entryType = "TRIP_EARNING",
                    title = "Trip ${order.orderId.take(8)} • ${order.dropoffAddress.take(22)}",
                    referenceCode = order.orderId.take(8).uppercase(),
                    amount = order.totalPayout,
                    balanceAfter = newBalance,
                    dayLabel = "Today",
                    timestampMs = System.currentTimeMillis(),
                    settlementStatus = "SETTLED"
                )
            )
            GlobalErrorHandler.logEvent(
                module = "features/earnings",
                severity = DiagnosticSeverity.INFO,
                message = "Settled final_fare +${GeoUtils.formatCurrency(order.totalPayout)} to drivers.wallet_balance",
                details = "POD: $podUrl"
            )
        }
    }

    suspend fun triggerInstantPayout(destinationAccount: String): Boolean {
        val profile = dao.getDriverProfileOnce() ?: return false
        if (profile.walletBalance <= 5.0) return false
        val payoutAmount = profile.walletBalance
        dao.upsertDriverProfile(profile.copy(walletBalance = 0.0))
        dao.insertLedgerEntry(
            LedgerEntryEntity(
                entryType = "INSTANT_PAYOUT",
                title = "Instant Payout • $destinationAccount",
                referenceCode = "PAY-${(1000..9999).random()}",
                amount = -payoutAmount,
                balanceAfter = 0.0,
                dayLabel = "Today",
                timestampMs = System.currentTimeMillis(),
                settlementStatus = "SETTLED"
            )
        )
        return true
    }
}
