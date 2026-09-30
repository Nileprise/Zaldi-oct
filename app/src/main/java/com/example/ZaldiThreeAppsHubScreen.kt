package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.network.ZaldiDispatchNetworkClient
import com.example.features.authentication.ZaldiFirebaseAuthManager
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.ZaldiDatabase
import com.example.services.localstorage.ZaldiRepository
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.location.LocationServices

/**
 * Standalone Android Launcher Activity #2: Zaldi Driver Application (`apps/driver`).
 */
class DriverAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val database = ZaldiDatabase.getInstance(applicationContext)
        val networkClient = ZaldiDispatchNetworkClient()
        val repository = ZaldiRepository(database.zaldiDao(), networkClient)
        val firebaseAuthManager = ZaldiFirebaseAuthManager(applicationContext)

        setContent {
            val viewModel: ZaldiDriverViewModel = viewModel(
                factory = ZaldiDriverViewModel.Factory(repository, firebaseAuthManager)
            )
            LaunchedEffect(Unit) {
                viewModel.setInitialApplication(ZaldiApplicationId.DRIVER_APP)
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
 * Standalone Android Launcher Activity #3: Zaldi Admin Application (`apps/admin`).
 */
class AdminAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val database = ZaldiDatabase.getInstance(applicationContext)
        val networkClient = ZaldiDispatchNetworkClient()
        val repository = ZaldiRepository(database.zaldiDao(), networkClient)
        val firebaseAuthManager = ZaldiFirebaseAuthManager(applicationContext)

        setContent {
            val viewModel: ZaldiDriverViewModel = viewModel(
                factory = ZaldiDriverViewModel.Factory(repository, firebaseAuthManager)
            )
            LaunchedEffect(Unit) {
                viewModel.setInitialApplication(ZaldiApplicationId.ADMIN_APP)
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
 * 3-Application Launcher Hub (`apps/customer`, `apps/driver`, `apps/admin`).
 * Lets the user inspect and launch each of the 3 separate applications either in-place (0ms)
 * or as a separate Android Activity (`MainActivity`, `DriverAppActivity`, `AdminAppActivity`).
 */
@Composable
fun ZaldiThreeAppsHubScreen(
    activeDriver: DriverProfileEntity?,
    allDriversCount: Int,
    onlineDriversCount: Int,
    activeOrder: RideOrderEntity?,
    totalOrdersCount: Int,
    onOpenAppInPlace: (ZaldiApplicationId) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("zaldi_three_apps_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ZALDI 3-APPLICATION SUITE • SEPARATE APPS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "All 3 applications (Customer App, Driver App, Admin App) have separate navigation bars, separate Android Launcher Activities, and share real-time SQLite + Dispatch synchronization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Application 1: Zaldi Customer App
        StandaloneAppLauncherCard(
            appNumber = "APPLICATION 1",
            title = "Zaldi Customer App",
            repoPath = "apps/customer",
            accentColor = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.DirectionsCar,
            statusSummary = activeOrder?.let {
                "Active Booking #${it.orderId.take(6)} (${it.status}) • OTP PIN: ${it.riderPin}"
            } ?: "Ready to Book • $onlineDriversCount Drivers Online in 5km Radius",
            featuresList = "Pages: Search & Map • VehicleSelector & FareCard • LiveTracking & ETA • Payment & 5★ Rating • RideHistory • Profile & OTP",
            openTestTag = "hub_open_customer_app",
            onOpenInPlace = { onOpenAppInPlace(ZaldiApplicationId.CUSTOMER_APP) },
            onLaunchSeparateActivity = {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("ZALDI_TARGET_APP", ZaldiApplicationId.CUSTOMER_APP.code)
                }
                context.startActivity(intent)
            }
        )

        // Application 2: Zaldi Driver App
        StandaloneAppLauncherCard(
            appNumber = "APPLICATION 2",
            title = "Zaldi Driver App",
            repoPath = "apps/driver",
            accentColor = MaterialTheme.colorScheme.secondary,
            icon = Icons.Default.LocalShipping,
            statusSummary = "Active Driver: ${activeDriver?.fullName ?: "Mateo Vance"} • ${if (activeDriver?.isOnline == true) "ONLINE" else "OFFLINE"} • ★ ${activeDriver?.rating ?: 4.94f}",
            featuresList = "Pages: DriverHome & Surge Heatmap • ActiveTrip & Navigation • Earnings & Offline SQLite Sync • KYC & Docs • Vehicle & Profile",
            openTestTag = "hub_open_driver_app",
            onOpenInPlace = { onOpenAppInPlace(ZaldiApplicationId.DRIVER_APP) },
            onLaunchSeparateActivity = {
                val intent = Intent(context, DriverAppActivity::class.java)
                context.startActivity(intent)
            }
        )

        // Application 3: Zaldi Admin App
        StandaloneAppLauncherCard(
            appNumber = "APPLICATION 3",
            title = "Zaldi Admin App",
            repoPath = "apps/admin",
            accentColor = MaterialTheme.colorScheme.tertiary,
            icon = Icons.Default.Security,
            statusSummary = "Fleet: $onlineDriversCount/$allDriversCount Online • $totalOrdersCount SQLite Trips • 8-Step Redis Matcher Ready",
            featuresList = "Pages: Admin Dashboard & MapPanel • Bookings & Live Control • DriverTable & KYC Approval • Pricing & 8-Step Match • Payments & Reports",
            openTestTag = "hub_open_admin_app",
            onOpenInPlace = { onOpenAppInPlace(ZaldiApplicationId.ADMIN_APP) },
            onLaunchSeparateActivity = {
                val intent = Intent(context, AdminAppActivity::class.java)
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun StandaloneAppLauncherCard(
    appNumber: String,
    title: String,
    repoPath: String,
    accentColor: Color,
    icon: ImageVector,
    statusSummary: String,
    featuresList: String,
    openTestTag: String,
    onOpenInPlace: () -> Unit,
    onLaunchSeparateActivity: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, accentColor, MaterialTheme.shapes.large),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$appNumber • $title",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Surface(
                                color = accentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = repoPath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = statusSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = featuresList,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenInPlace,
                    modifier = Modifier
                        .weight(0.58f)
                        .height(48.dp)
                        .testTag(openTestTag),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF0B0F17)
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open $title", fontWeight = FontWeight.Black)
                }

                OutlinedButton(
                    onClick = onLaunchSeparateActivity,
                    modifier = Modifier
                        .weight(0.42f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Activity", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
