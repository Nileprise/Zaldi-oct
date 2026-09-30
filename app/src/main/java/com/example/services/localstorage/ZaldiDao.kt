package com.example.services.localstorage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ZaldiDao {
    // Drivers Table (`drivers`)
    @Query("SELECT * FROM drivers ORDER BY is_selected_driver DESC LIMIT 1")
    fun observeDriverProfile(): Flow<DriverProfileEntity?>

    @Query("SELECT * FROM drivers ORDER BY is_selected_driver DESC")
    fun observeAllDrivers(): Flow<List<DriverProfileEntity>>

    @Query("SELECT * FROM drivers ORDER BY is_selected_driver DESC LIMIT 1")
    suspend fun getDriverProfileOnce(): DriverProfileEntity?

    @Query("SELECT * FROM drivers WHERE id = :driverId LIMIT 1")
    suspend fun getDriverByIdOnce(driverId: String): DriverProfileEntity?

    @Query("UPDATE drivers SET is_online = :isOnline WHERE id = :driverId")
    suspend fun updateDriverOnlineStatus(driverId: String, isOnline: Boolean)

    @Query("UPDATE drivers SET kyc_status = :kycStatus WHERE id = :driverId")
    suspend fun updateDriverKycStatusById(driverId: String, kycStatus: String)

    @Query("UPDATE drivers SET is_selected_driver = CASE WHEN id = :driverId THEN 1 ELSE 0 END")
    suspend fun selectActiveDriverSession(driverId: String)

    @Upsert
    suspend fun upsertDriverProfile(profile: DriverProfileEntity)

    @Query("UPDATE drivers SET current_lat = :lat, current_lng = :lng WHERE id = :driverId")
    suspend fun updateDriverCoordinates(driverId: String, lat: Float, lng: Float)

    // 1:M Relational Graph Query (Driver -> Vehicles & Trips)
    @Transaction
    @Query("SELECT * FROM drivers LIMIT 1")
    fun observeDriverRelationalGraph(): Flow<DriverWithVehiclesAndTrips?>

    // KYC Documents
    @Query("SELECT * FROM kyc_documents ORDER BY id ASC")
    fun observeKycDocuments(): Flow<List<KycDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKycDocuments(docs: List<KycDocumentEntity>)

    @Update
    suspend fun updateKycDocument(doc: KycDocumentEntity)

    // Vehicles Table (`vehicles`)
    @Query("SELECT * FROM vehicles ORDER BY is_active DESC, make_and_model ASC")
    fun observeVehicles(): Flow<List<VehicleEntity>>

    @Upsert
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Upsert
    suspend fun insertVehicles(vehicles: List<VehicleEntity>)

    @Query("UPDATE vehicles SET is_active = CASE WHEN id = :vehicleUuid THEN 1 ELSE 0 END")
    suspend fun setActiveVehicle(vehicleUuid: String)

    // Trips Table (`trips`)
    @Query("SELECT * FROM trips WHERE status IN ('INCOMING', 'ACCEPTED', 'ARRIVED_PICKUP', 'IN_PROGRESS') ORDER BY created_at DESC LIMIT 1")
    fun observeActiveOrder(): Flow<RideOrderEntity?>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getOrderByIdOnce(tripId: String): RideOrderEntity?

    @Query("SELECT * FROM trips ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestOrderOnce(): RideOrderEntity?

    @Query("UPDATE trips SET status = :newStatus WHERE id = :tripUuid")
    suspend fun updateOrderStatusFast(tripUuid: String, newStatus: String)

    @Query("SELECT * FROM trips ORDER BY created_at DESC")
    fun observeAllOrders(): Flow<List<RideOrderEntity>>

    @Upsert
    suspend fun upsertOrder(order: RideOrderEntity)

    @Upsert
    suspend fun insertOrders(orders: List<RideOrderEntity>)

    @Query("UPDATE trips SET proof_of_delivery_url = :podUrl WHERE id = :tripUuid")
    suspend fun updateTripProofOfDelivery(tripUuid: String, podUrl: String)

    // Ledger & Offline Earnings Logs (`ledger_entries`)
    @Query("SELECT * FROM ledger_entries ORDER BY timestampMs DESC")
    fun observeLedgerEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE sync_status = 'PENDING_OFFLINE_SYNC' ORDER BY timestampMs DESC")
    fun observePendingOfflineEarningsLogs(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE sync_status = 'PENDING_OFFLINE_SYNC' ORDER BY timestampMs ASC")
    suspend fun getPendingOfflineEarningsLogsOnce(): List<LedgerEntryEntity>

    @Query("UPDATE ledger_entries SET sync_status = 'SYNCED', settlementStatus = 'SETTLED' WHERE sync_status = 'PENDING_OFFLINE_SYNC'")
    suspend fun markAllOfflineEarningsLogsSynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<LedgerEntryEntity>)

    // Reviews
    @Query("SELECT * FROM driver_reviews ORDER BY id DESC")
    fun observeReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)
}
