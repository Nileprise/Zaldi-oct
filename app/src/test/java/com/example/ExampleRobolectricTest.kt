package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.ZaldiDispatchNetworkClient
import com.example.services.localstorage.ZaldiDatabase
import com.example.services.localstorage.ZaldiRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: ZaldiDatabase
    private lateinit var repository: ZaldiRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ZaldiDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository = ZaldiRepository(database.zaldiDao(), ZaldiDispatchNetworkClient())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zaldi Driver", appName)
    }

    @Test
    fun `room database persists driver profile and offline earnings logs without losing child relations`() = runBlocking {
        repository.ensureSeedDataInitialized()

        val initialProfile = repository.driverProfile.first()
        assertNotNull(initialProfile)
        assertEquals("Mateo", initialProfile?.firstName)

        // Verify initial offline earnings logs are stored in Room
        val initialPendingOffline = repository.pendingOfflineEarningsLogs.first()
        assertTrue(initialPendingOffline.isNotEmpty())

        // Update driver profile in Room and verify child vehicles and trips remain intact
        repository.saveDriverProfileToRoom(
            firstName = "Mateo",
            lastName = "Vance-Updated",
            phoneNumber = "+1 (415) 890-4210",
            email = "mateo.updated@zaldi.fleet"
        )
        val updatedProfile = repository.driverProfile.first()
        assertEquals("Vance-Updated", updatedProfile?.lastName)
        assertEquals("mateo.updated@zaldi.fleet", updatedProfile?.email)

        val vehicles = repository.vehicles.first()
        assertTrue("Vehicles must not be deleted on profile upsert", vehicles.isNotEmpty())

        // Record a new offline earning log in Room
        repository.recordOfflineEarningsLog(
            title = "Offline Cargo Trip Test",
            routeSummary = "Mission Bay -> Embarcadero",
            amount = 45.00,
            paymentMethod = "CASH_COLLECTED"
        )
        val afterInsertPending = repository.pendingOfflineEarningsLogs.first()
        assertEquals(initialPendingOffline.size + 1, afterInsertPending.size)

        // Sync offline logs and verify pending queue clears
        val syncedCount = repository.syncOfflineEarningsLogsToCloud()
        assertEquals(afterInsertPending.size, syncedCount)
        val afterSyncPending = repository.pendingOfflineEarningsLogs.first()
        assertTrue(afterSyncPending.isEmpty())
    }
}
