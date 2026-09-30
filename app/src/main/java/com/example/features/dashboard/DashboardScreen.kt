package com.example.features.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.network.SocketConnectionState
import com.example.core.utils.GeoUtils
import com.example.core.utils.SurgeZoneCell
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.VehicleEntity
import com.example.services.location.GpsTelemetryState
import java.util.Locale
import kotlin.math.hypot

@Composable
fun DashboardScreen(
    profile: DriverProfileEntity?,
    activeVehicle: VehicleEntity?,
    gpsTelemetry: GpsTelemetryState,
    socketState: SocketConnectionState,
    socketPingMs: Int,
    surgeCells: List<SurgeZoneCell>,
    selectedSurgeCell: SurgeZoneCell?,
    activeOrder: RideOrderEntity?,
    onToggleOnline: (Boolean) -> Unit,
    onSelectSurgeCell: (SurgeZoneCell) -> Unit,
    onTriggerIncomingOrder: () -> Unit,
    onOpenActiveNavigation: () -> Unit
) {
    val context = LocalContext.current
    val isOnline = profile?.isOnline == true

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onToggleOnline(true)
    }

    val requestGoOnline = {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Online / Offline Master Dispatch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (isOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.large
                ),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOnline) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Online Status Icon",
                            tint = if (isOnline) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isOnline) "ONLINE • DISPATCH READY" else "OFFLINE • STANDBY",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isOnline) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = if (isOnline) {
                                "Foreground GPS Service & WebSocket Engine Active"
                            } else {
                                "Switch online to broadcast Geohash & receive orders"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isOnline,
                    onCheckedChange = { wantOnline ->
                        onToggleOnline(wantOnline)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("online_offline_toggle")
                )
            }
        }

        // 2. Active Trip Banner (if a ride is currently Accepted / In Progress)
        if (activeOrder != null && activeOrder.status != "INCOMING") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenActiveNavigation() }
                    .testTag("active_trip_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Active Navigation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "ACTIVE TRIP #${activeOrder.orderId} • ${activeOrder.status.replace("_", " ")}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${activeOrder.passengerName} → ${activeOrder.dropoffAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = "OPEN MAP →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 3. Live Geohash & Background Telemetry Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.GpsFixed,
                label = "GEOHASH CELL",
                value = gpsTelemetry.geohash.uppercase(),
                subValue = String.format(
                    Locale.US,
                    "%.4f, %.4f",
                    gpsTelemetry.latitude,
                    gpsTelemetry.longitude
                ),
                accentColor = MaterialTheme.colorScheme.primary
            )
            TelemetryMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                label = "GPS VELOCITY",
                value = "${String.format(Locale.US, "%.1f", gpsTelemetry.speedKmh)} km/h",
                subValue = "HDG ${gpsTelemetry.bearingDegrees.toInt()}° • #${gpsTelemetry.backgroundHeartbeats}",
                accentColor = MaterialTheme.colorScheme.secondary
            )
            TelemetryMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WifiTethering,
                label = "SOCKET ENGINE",
                value = "${socketPingMs}ms RTT",
                subValue = when (socketState) {
                    SocketConnectionState.CONNECTED_LIVE -> "LIVE WS:3000"
                    SocketConnectionState.LOCAL_DISPATCH_ENGINE -> "EDGE ENGINE"
                    SocketConnectionState.CONNECTING -> "HANDSHAKE"
                    SocketConnectionState.DISCONNECTED -> "IDLE"
                },
                accentColor = MaterialTheme.colorScheme.tertiary
            )
        }

        // 4. Interactive High-Demand Surge Heatmap Canvas (`features/dashboard`)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Surge Heatmap",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "HIGH-DEMAND GEOHASH SURGE HEATMAP",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap any hotspot zone to inspect live multiplier & queue depth",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PEAK 2.2x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Custom Vector Map & Radial Surge Heatmap Canvas
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val errorColor = MaterialTheme.colorScheme.error

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(265.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("surge_heatmap_canvas")
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(surgeCells) {
                                detectTapGestures { tapOffset ->
                                    val nearest = surgeCells.minByOrNull { cell ->
                                        val cx = cell.normalizedX * size.width
                                        val cy = cell.normalizedY * size.height
                                        hypot(tapOffset.x - cx, tapOffset.y - cy)
                                    }
                                    if (nearest != null) {
                                        onSelectSurgeCell(nearest)
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        // 1. Subtle City Grid Lines
                        val gridColor = Color(0xFF1E293B).copy(alpha = 0.65f)
                        for (i in 1..7) {
                            val x = w * (i / 8f)
                            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.2f)
                        }
                        for (j in 1..5) {
                            val y = h * (j / 6f)
                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
                        }

                        // 2. Arterial Highway Polylines
                        val highwayPath = Path().apply {
                            moveTo(0f, h * 0.78f)
                            quadraticTo(w * 0.42f, h * 0.52f, w, h * 0.14f)
                        }
                        drawPath(
                            path = highwayPath,
                            color = Color(0xFF334155),
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )

                        val crossArterial = Path().apply {
                            moveTo(w * 0.15f, 0f)
                            quadraticTo(w * 0.55f, h * 0.48f, w * 0.88f, h)
                        }
                        drawPath(
                            path = crossArterial,
                            color = Color(0xFF273549),
                            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                        )

                        // 3. Surge Heatmap Radial Glows & Hex Nodes
                        surgeCells.forEach { cell ->
                            val center = Offset(cell.normalizedX * w, cell.normalizedY * h)
                            val isSelected = selectedSurgeCell?.geohash == cell.geohash
                            val heatColor = when {
                                cell.multiplier >= 2.0 -> errorColor
                                cell.multiplier >= 1.7 -> primaryColor
                                else -> secondaryColor
                            }
                            val glowRadius = if (isSelected) 76f else 58f

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        heatColor.copy(alpha = if (isSelected) 0.55f else 0.36f),
                                        heatColor.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = glowRadius
                                ),
                                radius = glowRadius,
                                center = center
                            )

                            if (isSelected) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 18f,
                                    center = center,
                                    style = Stroke(width = 3f)
                                )
                            }

                            drawCircle(
                                color = heatColor,
                                radius = 11f,
                                center = center
                            )
                        }

                        // 4. Driver Live GPS Beacon at Center (Static zero-overhead rings)
                        val driverCenter = Offset(w * 0.50f, h * 0.52f)
                        if (isOnline) {
                            drawCircle(
                                color = secondaryColor.copy(alpha = 0.22f),
                                radius = w * 0.22f,
                                center = driverCenter,
                                style = Stroke(width = 2.0f)
                            )
                            drawCircle(
                                color = secondaryColor.copy(alpha = 0.12f),
                                radius = w * 0.36f,
                                center = driverCenter,
                                style = Stroke(width = 1.5f)
                            )
                        }
                        drawCircle(
                            color = Color.White,
                            radius = 13f,
                            center = driverCenter
                        )
                        drawCircle(
                            color = secondaryColor,
                            radius = 8f,
                            center = driverCenter
                        )
                    }

                    // Overlay Legend Top-Left
                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Live Radar",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isOnline) "SCANNING 6 GEOHASH SECTORS" else "HEATMAP PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                // Selected Surge Zone Inspector Row
                if (selectedSurgeCell != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${selectedSurgeCell.zoneName} • [${selectedSurgeCell.geohash}]",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${selectedSurgeCell.activeOrdersCount} active ride requests • ~${selectedSurgeCell.estimatedWaitMin} min avg wait",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${selectedSurgeCell.multiplier}x SURGE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF0B0F17),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Instant Order Dispatch Trigger Card (`features/order_engine` & Active Fleet summary)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Active Vehicle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = activeVehicle?.makeAndModel ?: "2025 Tesla Model Y Long Range",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Plate: ${activeVehicle?.plateNumber ?: "ZLD-904E"} • ${activeVehicle?.capacityKg ?: 550}kg Cap • ${activeVehicle?.vehicleTier?.replace("_", " ") ?: "ZALDI EV"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = GeoUtils.formatCurrency(profile?.walletBalance ?: 428.60),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = onTriggerIncomingOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("trigger_order_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF0B0F17)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Simulate Incoming Order"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECEIVE LIVE WEBSOCKET ORDER OFFER",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subValue: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
