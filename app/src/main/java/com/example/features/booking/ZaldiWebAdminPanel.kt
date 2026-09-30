package com.example.features.booking

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.utils.GeoUtils
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.VehicleEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private class ZaldiAdminWebViewBridge(
    private val onDispatchOrder: () -> Unit,
    private val onToggleDriverOnline: (String) -> Unit,
    private val onAdvanceTripStatus: (String) -> Unit
) {
    @JavascriptInterface
    fun dispatchNewOrder() {
        onDispatchOrder()
    }

    @JavascriptInterface
    fun toggleDriverOnline(driverId: String) {
        onToggleDriverOnline(driverId)
    }

    @JavascriptInterface
    fun advanceTripStatus(tripId: String) {
        onAdvanceTripStatus(tripId)
    }
}

@Composable
fun ZaldiWebAdminPanelSection(
    allDrivers: List<DriverProfileEntity>,
    vehicles: List<VehicleEntity>,
    allOrders: List<RideOrderEntity>,
    activeDriverId: String?,
    onToggleDriverAvailability: (String) -> Unit,
    onSelectDriverSession: (String) -> Unit,
    onAdvanceTrip: (RideOrderEntity) -> Unit,
    onAdvanceTripById: (String) -> Unit,
    onQuickDispatchOrder: () -> Unit
) {
    var tripFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, ACTIVE, INCOMING, COMPLETED
    var driverFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, ONLINE, OFFLINE
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var viewRenderMode by rememberSaveable { mutableStateOf("DASHBOARD") } // DASHBOARD, WEBVIEW, JSX_SOURCE

    val onlineDriversCount = remember(allDrivers) { allDrivers.count { it.isOnline } }
    val activeTripsCount = remember(allOrders) {
        allOrders.count { it.status in listOf("INCOMING", "ACCEPTED", "ARRIVED_PICKUP", "IN_PROGRESS") }
    }
    val totalGmv = remember(allOrders) { allOrders.sumOf { it.totalPayout } }

    val filteredOrders = remember(allOrders, tripFilter, searchQuery) {
        allOrders.filter { order ->
            val isActive = order.status in listOf("INCOMING", "ACCEPTED", "ARRIVED_PICKUP", "IN_PROGRESS")
            val matchesFilter = when (tripFilter) {
                "ACTIVE" -> isActive
                "INCOMING" -> order.status == "INCOMING"
                "COMPLETED" -> order.status == "COMPLETED"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase(Locale.US)
                order.orderId.lowercase(Locale.US).contains(q) ||
                    order.passengerName.lowercase(Locale.US).contains(q) ||
                    order.pickupAddress.lowercase(Locale.US).contains(q) ||
                    order.dropoffAddress.lowercase(Locale.US).contains(q) ||
                    order.cargoDetails.lowercase(Locale.US).contains(q)
            }
            matchesFilter && matchesSearch
        }
    }

    val filteredDrivers = remember(allDrivers, driverFilter) {
        allDrivers.filter { drv ->
            when (driverFilter) {
                "ONLINE" -> drv.isOnline
                "OFFLINE" -> !drv.isOnline
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("zaldi_web_admin_panel"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Web Admin Panel Top Header Bar + View Mode Switcher
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), MaterialTheme.shapes.large),
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
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ZALDI WEB ADMIN PANEL",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "REACT + TAILWIND",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Monitor all active trips & real-time Redis GEORADIUS driver availability",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onQuickDispatchOrder,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF0B0F17)
                        ),
                        modifier = Modifier.testTag("admin_quick_dispatch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Dispatch Trip",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ping Trip", fontWeight = FontWeight.Black)
                    }
                }

                // 3-way sub-mode selector: Interactive Dashboard / Embedded React WebView / React + Tailwind Source
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewRenderMode == "DASHBOARD",
                        onClick = { viewRenderMode = "DASHBOARD" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Live Admin UI",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Live Admin UI") },
                        modifier = Modifier.testTag("admin_mode_dashboard")
                    )
                    FilterChip(
                        selected = viewRenderMode == "WEBVIEW",
                        onClick = { viewRenderMode = "WEBVIEW" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "React + Tailwind WebView",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("React + Tailwind WebView") },
                        modifier = Modifier.testTag("admin_mode_webview")
                    )
                    FilterChip(
                        selected = viewRenderMode == "JSX_SOURCE",
                        onClick = { viewRenderMode = "JSX_SOURCE" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "AdminPanel.jsx Code",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("AdminPanel.jsx Source") },
                        modifier = Modifier.testTag("admin_mode_jsx_source")
                    )
                }

                // Real-time KPI Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiMetricTile(
                        title = "ACTIVE TRIPS",
                        primaryValue = "$activeTripsCount",
                        subValue = "${allOrders.size} Total in DB",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_kpi_active_trips")
                    )
                    AdminKpiMetricTile(
                        title = "DRIVERS ONLINE",
                        primaryValue = "$onlineDriversCount/${allDrivers.size}",
                        subValue = "5km Redis GEO",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_kpi_drivers_online")
                    )
                    AdminKpiMetricTile(
                        title = "DISPATCH GMV",
                        primaryValue = GeoUtils.formatCurrency(totalGmv),
                        subValue = "1.5x Avg Surge",
                        accentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_kpi_total_gmv")
                    )
                }
            }
        }

        when (viewRenderMode) {
            "WEBVIEW" -> {
                ZaldiAdminWebViewCard(
                    allDrivers = allDrivers,
                    vehicles = vehicles,
                    allOrders = allOrders,
                    onDispatchOrder = onQuickDispatchOrder,
                    onToggleDriverOnline = onToggleDriverAvailability,
                    onAdvanceTripStatus = onAdvanceTripById
                )
            }

            "JSX_SOURCE" -> {
                ZaldiAdminReactCodePreviewCard()
            }

            else -> {
                // 2. Real-Time Driver Availability Monitor Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_driver_availability_card"),
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Driver Availability",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text(
                                        text = "REAL-TIME DRIVER AVAILABILITY (${filteredDrivers.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Live Redis GEO index (driver_locations) • Tap status pill to toggle Online/Offline",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL", "ONLINE", "OFFLINE").forEach { filter ->
                                FilterChip(
                                    selected = driverFilter == filter,
                                    onClick = { driverFilter = filter },
                                    label = { Text(filter) },
                                    modifier = Modifier.testTag("driver_filter_$filter")
                                )
                            }
                        }

                        filteredDrivers.forEachIndexed { index, drv ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                            val driverVehicle = vehicles.firstOrNull { it.driverId == drv.driverId }
                                ?: vehicles.firstOrNull { it.id == drv.activeVehicleId }
                            val geohash = GeoUtils.encodeGeohash(drv.currentLat.toDouble(), drv.currentLng.toDouble(), 7)
                            val isCurrentSession = drv.driverId == activeDriverId

                            Surface(
                                color = if (drv.isOnline) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (drv.isOnline) {
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (drv.isOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                                    )
                                            )
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = drv.fullName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "★ ${String.format(Locale.US, "%.2f", drv.rating)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (isCurrentSession) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "ACTIVE SESSION",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${drv.phoneNumber} • KYC: ${drv.kycStatus} • Wallet: ${GeoUtils.formatCurrency(drv.walletBalance)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Real-time Online/Offline availability toggle button
                                        Surface(
                                            color = if (drv.isOnline) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .clickable { onToggleDriverAvailability(drv.driverId) }
                                                .testTag("toggle_driver_availability_${drv.driverId.take(6)}")
                                        ) {
                                            Text(
                                                text = if (drv.isOnline) "● ONLINE" else "○ OFFLINE",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (drv.isOnline) {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (driverVehicle != null) {
                                                "${driverVehicle.makeAndModel} (${driverVehicle.plateNumber} • ${driverVehicle.vehicleTier})"
                                            } else {
                                                "Zaldi Fleet EV • Ready"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.GpsFixed,
                                                    contentDescription = "Geohash",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "[${geohash.uppercase()}] ${String.format(Locale.US, "%.3f, %.3f", drv.currentLat, drv.currentLng)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            if (!isCurrentSession) {
                                                Text(
                                                    text = "Use Driver",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable { onSelectDriverSession(drv.driverId) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. All Active Trips & Dispatch Queue Monitor Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_active_trips_card"),
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
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Active Trips Monitor",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "ACTIVE TRIPS & DISPATCH MONITOR (${filteredOrders.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Live PostgreSQL/SQLite trips table with spatial POINT(lng lat) & surge fares",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Filter Chips for Trip Status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL", "ACTIVE", "INCOMING", "COMPLETED").forEach { filter ->
                                FilterChip(
                                    selected = tripFilter == filter,
                                    onClick = { tripFilter = filter },
                                    label = { Text(filter) },
                                    modifier = Modifier.testTag("trip_filter_$filter")
                                )
                            }
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search trips by UUID, customer, Nalgonda/Hyderabad, or cargo...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Trips"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_trip_search_input")
                        )

                        if (filteredOrders.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "No trips match filter \"$tripFilter\"",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedButton(onClick = onQuickDispatchOrder) {
                                        Text("+ Dispatch New Trip (zaldi_ping)")
                                    }
                                }
                            }
                        } else {
                            filteredOrders.forEach { order ->
                                val assignedDriver = allDrivers.firstOrNull { it.driverId == order.driverId }
                                val isTripActive = order.status in listOf("INCOMING", "ACCEPTED", "ARRIVED_PICKUP", "IN_PROGRESS")

                                Surface(
                                    color = if (isTripActive) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (isTripActive) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    color = if (isTripActive) {
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    },
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = order.status,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isTripActive) {
                                                            MaterialTheme.colorScheme.onSecondaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "#${order.orderId.take(8)}…",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${order.cargoDetails} (${order.cargoWeightKg}kg)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = GeoUtils.formatCurrency(order.totalPayout),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        Text(
                                            text = order.passengerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "PICKUP: ${order.pickupAddress} [${order.pickupGeohash.uppercase()}]",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "DROPOFF: ${order.dropoffAddress} [${order.dropoffGeohash.uppercase()}]",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Driver: ${assignedDriver?.fullName ?: "Mateo Vance"} • ${order.distanceKm} km (${order.tierCategory})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            if (order.status != "COMPLETED" && order.status != "DECLINED") {
                                                Button(
                                                    onClick = { onAdvanceTrip(order) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary,
                                                        contentColor = Color(0xFF06281E)
                                                    ),
                                                    modifier = Modifier
                                                        .height(34.dp)
                                                        .testTag("admin_advance_trip_${order.orderId.take(6)}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Advance Trip",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (order.status == "INCOMING") "Accept & Go" else "Advance Step",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiMetricTile(
    title: String,
    primaryValue: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ZaldiAdminWebViewCard(
    allDrivers: List<DriverProfileEntity>,
    vehicles: List<VehicleEntity>,
    allOrders: List<RideOrderEntity>,
    onDispatchOrder: () -> Unit,
    onToggleDriverOnline: (String) -> Unit,
    onAdvanceTripStatus: (String) -> Unit
) {
    val payloadJson = remember(allDrivers, vehicles, allOrders) {
        val root = JSONObject()
        val driversArray = JSONArray()
        allDrivers.forEach { d ->
            val v = vehicles.firstOrNull { it.driverId == d.driverId }
                ?: vehicles.firstOrNull { it.id == d.activeVehicleId }
            val obj = JSONObject().apply {
                put("id", d.driverId)
                put("name", d.fullName)
                put("phone", d.phoneNumber)
                put("kycStatus", d.kycStatus)
                put("isOnline", d.isOnline)
                put("lat", d.currentLat.toDouble())
                put("lng", d.currentLng.toDouble())
                put("geohash", GeoUtils.encodeGeohash(d.currentLat.toDouble(), d.currentLng.toDouble(), 7).uppercase())
                put("tier", v?.vehicleTier ?: "ZALDI_EV")
                put("plate", v?.plateNumber ?: "ZLD-904E")
                put("vehicle", v?.makeAndModel ?: "2025 Tesla Model Y")
                put("wallet", d.walletBalance)
                put("rating", d.rating.toDouble())
            }
            driversArray.put(obj)
        }
        val tripsArray = JSONArray()
        allOrders.forEach { o ->
            val obj = JSONObject().apply {
                put("id", o.orderId)
                put("customerName", o.passengerName)
                put("status", o.status)
                put("pickupAddress", o.pickupAddress)
                put("dropoffAddress", o.dropoffAddress)
                put("cargoDetails", o.cargoDetails)
                put("cargoWeightKg", o.cargoWeightKg)
                put("tierCategory", o.tierCategory)
                put("distanceKm", o.distanceKm)
                put("finalFare", o.totalPayout)
            }
            tripsArray.put(obj)
        }
        root.put("drivers", driversArray)
        root.put("trips", tripsArray)
        root.toString()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp)
            .testTag("admin_react_tailwind_webview"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    addJavascriptInterface(
                        ZaldiAdminWebViewBridge(
                            onDispatchOrder = onDispatchOrder,
                            onToggleDriverOnline = onToggleDriverOnline,
                            onAdvanceTripStatus = onAdvanceTripStatus
                        ),
                        "ZaldiAdminBridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript(
                                "if (window.updateZaldiAdminState) { window.updateZaldiAdminState($payloadJson); }",
                                null
                            )
                        }
                    }
                    loadUrl("file:///android_asset/zaldi_admin_panel.html")
                }
            },
            update = { webView ->
                webView.evaluateJavascript(
                    "if (window.updateZaldiAdminState) { window.updateZaldiAdminState($payloadJson); }",
                    null
                )
            }
        )
    }
}

@Composable
private fun ZaldiAdminReactCodePreviewCard() {
    val reactTailwindSnippet = remember {
        """
        // admin_panel/src/AdminPanel.jsx (React 18 + Tailwind CSS + Socket.IO)
        import React, { useState, useEffect, useMemo } from 'react';
        import { io } from 'socket.io-client';

        export default function ZaldiAdminPanel() {
          const [drivers, setDrivers] = useState([]);
          const [trips, setTrips] = useState([]);
          const [tripFilter, setTripFilter] = useState('ALL');
          const [driverFilter, setDriverFilter] = useState('ALL');

          useEffect(() => {
            const socket = io('wss://api.zaldi.com', { transports: ['websocket'] });
            socket.on('driver_location_broadcast', ({ driverId, lat, lng }) => {
              setDrivers(prev => prev.map(d =>
                d.id === driverId ? { ...d, currentLat: lat, currentLng: lng, isOnline: true } : d
              ));
            });
            socket.on('trip_status_updated', (trip) => {
              setTrips(prev => [trip, ...prev.filter(t => t.id !== trip.id)]);
            });
            return () => socket.disconnect();
          }, []);

          return (
            <div className="min-h-screen bg-[#0B0F17] text-slate-100 p-6">
              <header className="flex justify-between items-center border-b border-slate-800 pb-4">
                <h1 className="text-2xl font-black text-white">
                  ZALDI <span className="text-amber-400">ADMIN DISPATCH PANEL</span>
                </h1>
              </header>
              {/* Active Trips & Real-Time Driver Availability Grid */}
            </div>
          );
        }
        """.trimIndent()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_jsx_source_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "admin_panel/src/AdminPanel.jsx • React 18 & Tailwind CSS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = Color(0xFF0B0F17),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = reactTailwindSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
