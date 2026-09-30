package com.example.features.maptracking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.utils.GeoUtils
import com.example.services.localstorage.RideOrderEntity
import com.example.services.location.GpsTelemetryState
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Live Navigation & Route Polyline Engine (`features/map_tracking`).
 * Renders real-time multi-segment vector polylines, driver puck interpolation,
 * turn-by-turn maneuver guidance, Rider PIN verification, and trip completion settlement.
 */
@Composable
fun MapTrackingScreen(
    activeOrder: RideOrderEntity?,
    recentOrders: List<RideOrderEntity>,
    gpsTelemetry: GpsTelemetryState,
    routeProgress: Float,
    onAdvanceOrderLifecycle: (RideOrderEntity) -> Unit,
    onRevertOrderLifecycle: (RideOrderEntity?) -> Unit,
    onAttachProofOfDelivery: (RideOrderEntity, String) -> Unit,
    onStepRouteForward: () -> Unit,
    onRequestDispatchOrder: () -> Unit
) {
    val displayedOrder = activeOrder ?: recentOrders.firstOrNull()
    val isLiveOrder = activeOrder != null && activeOrder.status != "INCOMING"

    val podPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (activeOrder != null && uri != null) {
            onAttachProofOfDelivery(
                activeOrder,
                "https://cdn.zaldi.io/pod/${activeOrder.orderId.take(8)}-photo.jpg"
            )
        }
    }

    val maneuver = GeoUtils.getCurrentManeuver(
        orderStatus = activeOrder?.status ?: "STANDBY",
        routeProgress = routeProgress,
        pickupAddress = displayedOrder?.pickupAddress ?: "450 Mission St",
        dropoffAddress = displayedOrder?.dropoffAddress ?: "Pier 15 Embarcadero"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Turn-by-Turn Maneuver HUD Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isLiveOrder) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
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
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        val turnIcon = when (maneuver.turnType) {
                            "LEFT" -> Icons.Default.TurnLeft
                            "RIGHT" -> Icons.Default.TurnRight
                            "ARRIVE" -> Icons.Default.Flag
                            else -> Icons.Default.Navigation
                        }
                        Icon(
                            imageVector = turnIcon,
                            contentDescription = "Maneuver Direction",
                            tint = Color(0xFF06281E),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (maneuver.distanceMeters > 0) {
                                "IN ${maneuver.distanceMeters} M • ${maneuver.turnType}"
                            } else {
                                "NAVIGATION GUIDANCE"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = maneuver.instruction,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = maneuver.streetName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Speed Limit & Live GPS Speed Badge
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${gpsTelemetry.speedKmh.roundToInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "KM/H",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Interactive Route Polyline Vector Map Canvas
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF080C14))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("route_polyline_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // City blocks & water channel
                        val waterPath = Path().apply {
                            moveTo(w * 0.78f, 0f)
                            lineTo(w, 0f)
                            lineTo(w, h)
                            lineTo(w * 0.90f, h)
                            close()
                        }
                        drawPath(waterPath, color = Color(0xFF0C2538))

                        val streetColor = Color(0xFF1E293B)
                        for (i in 1..8) {
                            val x = w * (i / 9f)
                            drawLine(streetColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.4f)
                        }
                        for (j in 1..6) {
                            val y = h * (j / 7f)
                            drawLine(streetColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.4f)
                        }

                        // Route Polyline Normalized Waypoints:
                        // [0] Driver Start -> [1] Waypoint -> [2] Pickup -> [3] Corridor -> [4] Turn -> [5] Dropoff
                        val waypoints = listOf(
                            Offset(w * 0.14f, h * 0.82f),
                            Offset(w * 0.24f, h * 0.64f),
                            Offset(w * 0.34f, h * 0.58f), // Pickup Pin
                            Offset(w * 0.52f, h * 0.44f),
                            Offset(w * 0.66f, h * 0.28f),
                            Offset(w * 0.82f, h * 0.20f)  // Dropoff Pin
                        )

                        // Draw full route shadow
                        val fullRoutePath = Path().apply {
                            moveTo(waypoints.first().x, waypoints.first().y)
                            waypoints.drop(1).forEach { pt -> lineTo(pt.x, pt.y) }
                        }
                        drawPath(
                            path = fullRoutePath,
                            color = Color(0xFF334155),
                            style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw active glowing polyline
                        drawPath(
                            path = fullRoutePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 7f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Dashed centerline for high-visibility navigation feel
                        drawPath(
                            path = fullRoutePath,
                            color = Color.White.copy(alpha = 0.65f),
                            style = Stroke(
                                width = 2f,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )
                        )

                        // Pickup Marker (Index 2)
                        val pickupOffset = waypoints[2]
                        drawCircle(color = secondaryColor.copy(alpha = 0.3f), radius = 24f, center = pickupOffset)
                        drawCircle(color = secondaryColor, radius = 11f, center = pickupOffset)
                        drawCircle(color = Color.White, radius = 4.5f, center = pickupOffset)

                        // Dropoff Marker (Index 5)
                        val dropoffOffset = waypoints[5]
                        drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 26f, center = dropoffOffset)
                        drawCircle(color = primaryColor, radius = 12f, center = dropoffOffset)
                        drawCircle(color = Color(0xFF0B0F17), radius = 5f, center = dropoffOffset)

                        // Interpolated Driver Vehicle Puck Position along the route
                        val effectiveFraction = when (activeOrder?.status) {
                            "ACCEPTED" -> (routeProgress.coerceIn(0f, 1f) * 0.40f) // Moving from 0 -> Pickup (0.4)
                            "ARRIVED_PICKUP" -> 0.40f
                            "IN_PROGRESS" -> 0.40f + (routeProgress.coerceIn(0f, 1f) * 0.60f) // Pickup -> Dropoff
                            else -> 0.65f
                        }

                        val totalSegments = waypoints.size - 1
                        val exactIndex = effectiveFraction * totalSegments
                        val segIdx = exactIndex.toInt().coerceIn(0, totalSegments - 1)
                        val segLocal = (exactIndex - segIdx).coerceIn(0f, 1f)
                        val startPt = waypoints[segIdx]
                        val endPt = waypoints[segIdx + 1]
                        val driverPos = Offset(
                            x = startPt.x + (endPt.x - startPt.x) * segLocal,
                            y = startPt.y + (endPt.y - startPt.y) * segLocal
                        )

                        // Driver Puck Halo & Beacon
                        drawCircle(
                            color = tertiaryColor.copy(alpha = 0.35f),
                            radius = 28f,
                            center = driverPos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 14f,
                            center = driverPos
                        )
                        drawCircle(
                            color = tertiaryColor,
                            radius = 9f,
                            center = driverPos
                        )
                    }

                    // Top-Left Polyline Geohash Overlay
                    Surface(
                        color = Color(0xD90F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "POLYLINE ENGINE • ${if (isLiveOrder) activeOrder?.status else "ROUTE PREVIEW"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Geohash: ${displayedOrder?.pickupGeohash ?: "9q8yyk8"} → ${displayedOrder?.dropoffGeohash ?: "9q8yyz2"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    // Bottom-Right ETA & Distance Overlay
                    Surface(
                        color = Color(0xD90F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = "Route Distance",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${GeoUtils.formatDistance(displayedOrder?.distanceKm ?: 4.2)} • ${displayedOrder?.estimatedMinutes ?: 12} min",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                // Route Progress Bar
                LinearProgressIndicator(
                    progress = { (if (isLiveOrder) routeProgress else 1f).coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // 3. Active Trip Execution Console OR Standby Dispatch Prompt
        if (isLiveOrder && activeOrder != null) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonPinCircle,
                                contentDescription = "Passenger",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = activeOrder.passengerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${activeOrder.passengerRating} • ${activeOrder.tierCategory.replace("_", " ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Rider Boarding Verification PIN Badge
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Rider Security PIN",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "PIN: ${activeOrder.riderPin}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Pickup & Dropoff summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PICKUP • ${activeOrder.pickupLatLngPoint}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = activeOrder.pickupAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "DROPOFF • ${activeOrder.dropoffLatLngPoint}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = activeOrder.dropoffAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "FK vehicle_id: ${activeOrder.vehicleId.take(8)}… • customer_id: ${activeOrder.customerId.take(8)}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "FINAL FARE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = GeoUtils.formatCurrency(activeOrder.totalPayout),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${activeOrder.cargoWeightKg}kg • ${GeoUtils.formatDistance(activeOrder.distanceKm)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Proof of Delivery (proof_of_delivery_url) Capture Section
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PROOF OF DELIVERY (trips.proof_of_delivery_url)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = activeOrder.proofOfDeliveryUrl ?: "Pending dropoff verification capture",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        podPhotoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.testTag("pod_photo_button")
                                ) {
                                    Text("Photo")
                                }
                                Button(
                                    onClick = {
                                        onAttachProofOfDelivery(
                                            activeOrder,
                                            "https://cdn.zaldi.io/pod/${activeOrder.orderId.take(8)}-signed.jpg"
                                        )
                                    },
                                    modifier = Modifier.testTag("pod_stamp_button")
                                ) {
                                    Text("Stamp POD")
                                }
                            }
                        }
                    }

                    // Action Buttons for Trip Progression (Previous Step + Next Step)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRevertOrderLifecycle(activeOrder) },
                            modifier = Modifier
                                .weight(0.28f)
                                .height(52.dp)
                                .testTag("map_previous_trip_step_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("← Prev", fontWeight = FontWeight.Bold)
                        }

                        if (activeOrder.status == "IN_PROGRESS") {
                            OutlinedButton(
                                onClick = onStepRouteForward,
                                modifier = Modifier
                                    .weight(0.24f)
                                    .height(52.dp)
                                    .testTag("step_route_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("+25%")
                            }
                        }

                        val primaryActionText = when (activeOrder.status) {
                            "ACCEPTED" -> "ARRIVED AT PICKUP"
                            "ARRIVED_PICKUP" -> "VERIFY PIN (${activeOrder.riderPin}) & START"
                            "IN_PROGRESS" -> "COMPLETE (${GeoUtils.formatCurrency(activeOrder.totalPayout)})"
                            else -> "NEXT STEP"
                        }

                        Button(
                            onClick = { onAdvanceOrderLifecycle(activeOrder) },
                            modifier = Modifier
                                .weight(if (activeOrder.status == "IN_PROGRESS") 0.48f else 0.72f)
                                .height(52.dp)
                                .testTag("advance_trip_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color(0xFF06281E)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Advance Trip"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = primaryActionText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        } else {
            // Standby Card when no active order is in progress
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
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Standby Route",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "NO ACTIVE TRIP • SHOWING LAST COMPLETED CORRIDOR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Dispatch a live order offer to test turn-by-turn polyline progression, Rider PIN verification, and automatic SQLite ledger settlement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRevertOrderLifecycle(null) },
                            modifier = Modifier
                                .weight(0.38f)
                                .height(50.dp)
                                .testTag("map_restore_previous_trip_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("← Prev Trip", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onRequestDispatchOrder,
                            modifier = Modifier
                                .weight(0.62f)
                                .height(50.dp)
                                .testTag("map_dispatch_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = "Dispatch")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DISPATCH NEW RIDE",
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
