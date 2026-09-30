package com.example.services.localstorage

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Drivers Table (`drivers`)
 * Primary Key: id (UUID)
 * Unique Index: phone_number
 */
@Entity(
    tableName = "drivers",
    indices = [Index(value = ["phone_number"])]
)
data class DriverProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val driverId: String = "d4f8a920-7c1b-4e9a-8b12-9042zaldi001",

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "email")
    val email: String = "mateo.vance@zaldi.fleet",

    @ColumnInfo(name = "firebase_uid")
    val firebaseUid: String = "zld-auth-9042-mateo",

    @ColumnInfo(name = "kyc_status")
    val kycStatus: String, // Enum: "PENDING_DOCS", "UNDER_REVIEW", "VERIFIED"

    @ColumnInfo(name = "is_online")
    val isOnline: Boolean,

    @ColumnInfo(name = "current_lat")
    val currentLat: Float,

    @ColumnInfo(name = "current_lng")
    val currentLng: Float,

    @ColumnInfo(name = "wallet_balance")
    val walletBalance: Double,

    @ColumnInfo(name = "offline_pending_earnings")
    val offlinePendingEarnings: Double = 86.50,

    @ColumnInfo(name = "rating")
    val rating: Float,

    // App session & cockpit metadata
    @ColumnInfo(name = "is_authenticated")
    val isAuthenticated: Boolean = true,

    @ColumnInfo(name = "active_vehicle_id")
    val activeVehicleId: String = "v1a9c400-2b8e-4d11-9f01-vehicle00001",

    @ColumnInfo(name = "total_trips")
    val totalTrips: Int = 648,

    @ColumnInfo(name = "acceptance_rate")
    val acceptanceRate: Int = 96,

    @ColumnInfo(name = "cancellation_rate")
    val cancellationRate: Int = 1,

    @ColumnInfo(name = "backend_socket_url")
    val backendSocketUrl: String = "wss://api.zaldi.com",

    @ColumnInfo(name = "is_dark_mode")
    val isDarkMode: Boolean = true,

    @ColumnInfo(name = "is_selected_driver")
    val isSelectedDriver: Boolean = true,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAtMs: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}

/**
 * Vehicles Table (`vehicles`)
 * Primary Key: id (UUID)
 * Foreign Key: driver_id -> drivers.id (1:M relationship)
 * Unique Index: plate_number
 */
@Entity(
    tableName = "vehicles",
    foreignKeys = [
        ForeignKey(
            entity = DriverProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["driver_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["driver_id"]),
        Index(value = ["plate_number"])
    ]
)
data class VehicleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "driver_id")
    val driverId: String,

    @ColumnInfo(name = "vehicle_tier")
    val vehicleTier: String, // Enum: "ZALDI_EV", "ZALDI_PRIME", "ZALDI_VAN", "ZALDI_XL"

    @ColumnInfo(name = "plate_number")
    val plateNumber: String,

    @ColumnInfo(name = "capacity_kg")
    val capacityKg: Int,

    @ColumnInfo(name = "make_and_model")
    val makeAndModel: String,

    @ColumnInfo(name = "insurance_expiry")
    val insuranceExpiry: String, // ISO Date YYYY-MM-DD

    @ColumnInfo(name = "is_active")
    val isActive: Boolean
)

/**
 * Trips Table (`trips`)
 * Primary Key: id (UUID)
 * Foreign Keys:
 *   - driver_id -> drivers.id (1:M)
 *   - vehicle_id -> vehicles.id (1:M)
 *   - customer_id (UUID)
 */
@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = DriverProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["driver_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["driver_id"]),
        Index(value = ["vehicle_id"]),
        Index(value = ["customer_id"])
    ]
)
data class RideOrderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val orderId: String, // UUID

    @ColumnInfo(name = "driver_id")
    val driverId: String, // FK -> drivers.id

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String, // FK -> vehicles.id

    @ColumnInfo(name = "customer_id")
    val customerId: String, // FK UUID

    @ColumnInfo(name = "status")
    val status: String, // Enum: "INCOMING", "ACCEPTED", "ARRIVED_PICKUP", "IN_PROGRESS", "COMPLETED", "DECLINED"

    @ColumnInfo(name = "pickup_lat_lng")
    val pickupLatLngPoint: String, // Spatial Point representation: "POINT(lng lat)"

    @ColumnInfo(name = "dropoff_lat_lng")
    val dropoffLatLngPoint: String, // Spatial Point representation: "POINT(lng lat)"

    @ColumnInfo(name = "estimated_dist_km")
    val distanceKm: Double,

    @ColumnInfo(name = "final_fare")
    val totalPayout: Double,

    @ColumnInfo(name = "proof_of_delivery_url")
    val proofOfDeliveryUrl: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAtMs: Long = System.currentTimeMillis(),

    // Rich dispatch metadata for cockpit & navigation rendering
    @ColumnInfo(name = "passenger_name")
    val passengerName: String,

    @ColumnInfo(name = "passenger_rating")
    val passengerRating: Double,

    @ColumnInfo(name = "rider_pin")
    val riderPin: String,

    @ColumnInfo(name = "tier_category")
    val tierCategory: String,

    @ColumnInfo(name = "pickup_address")
    val pickupAddress: String,

    @ColumnInfo(name = "pickup_lat")
    val pickupLat: Double,

    @ColumnInfo(name = "pickup_lng")
    val pickupLng: Double,

    @ColumnInfo(name = "pickup_geohash")
    val pickupGeohash: String,

    @ColumnInfo(name = "dropoff_address")
    val dropoffAddress: String,

    @ColumnInfo(name = "dropoff_lat")
    val dropoffLat: Double,

    @ColumnInfo(name = "dropoff_lng")
    val dropoffLng: Double,

    @ColumnInfo(name = "dropoff_geohash")
    val dropoffGeohash: String,

    @ColumnInfo(name = "estimated_minutes")
    val estimatedMinutes: Int,

    @ColumnInfo(name = "base_fare")
    val baseFare: Double,

    @ColumnInfo(name = "surge_multiplier")
    val surgeMultiplier: Double,

    @ColumnInfo(name = "tip_amount")
    val tipAmount: Double,

    @ColumnInfo(name = "cargo_weight_kg")
    val cargoWeightKg: Int = 120,

    @ColumnInfo(name = "cargo_details")
    val cargoDetails: String = "Fragile Load",

    @ColumnInfo(name = "completed_at")
    val completedAtMs: Long? = null
)

/**
 * 1:M Relational Join Projections for Room
 */
data class DriverWithVehiclesAndTrips(
    @Embedded val driver: DriverProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "driver_id"
    )
    val vehicles: List<VehicleEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "driver_id"
    )
    val trips: List<RideOrderEntity>
)

@Entity(tableName = "kyc_documents")
data class KycDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val docType: String, // "DRIVER_LICENSE", "VEHICLE_REGISTRATION", "COMMERCIAL_INSURANCE", "NATIONAL_ID"
    val title: String,
    val subtitle: String,
    val documentNumber: String,
    val expiryDate: String,
    val status: String, // "REQUIRED", "UNDER_REVIEW", "APPROVED", "REJECTED"
    val adminReviewNote: String,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "driver_id")
    val driverId: String = "d4f8a920-7c1b-4e9a-8b12-9042zaldi001",
    val entryType: String, // "TRIP_EARNING", "SURGE_BONUS", "TIP", "INSTANT_PAYOUT", "OFFLINE_TRIP_EARNING"
    val title: String,
    val referenceCode: String,
    val amount: Double,
    val balanceAfter: Double,
    val dayLabel: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val settlementStatus: String = "SETTLED", // "SETTLED", "OFFLINE_CACHED"
    @ColumnInfo(name = "is_offline_log")
    val isOfflineLog: Boolean = false,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED", // "PENDING_OFFLINE_SYNC", "SYNCED"
    @ColumnInfo(name = "route_summary")
    val routeSummary: String = "SF Financial District -> SoMa Hub",
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "ZALDI_WALLET"
)

@Entity(tableName = "driver_reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val passengerName: String,
    val rating: Int,
    val comment: String,
    val badgeTag: String,
    val timestampLabel: String
)
