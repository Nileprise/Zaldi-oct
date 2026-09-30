package com.example.features.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.network.DispatchEndpointTrace
import com.example.core.utils.GeoUtils
import com.example.services.localstorage.DriverProfileEntity
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CustomerBookRideSection(
    activeDriver: DriverProfileEntity?,
    allDrivers: List<DriverProfileEntity>,
    lastDispatchTrace: DispatchEndpointTrace?,
    simulateRaceLockConflict: Boolean,
    onToggleRaceLockConflict: (Boolean) -> Unit,
    onBookRideViaDispatchEndpoint: (
        customerName: String,
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropoffAddress: String,
        dropoffLat: Double,
        dropoffLng: Double,
        cargoDetails: String,
        cargoWeightKg: Int,
        vehicleTier: String,
        guaranteedFare: Double,
        distanceKm: Double
    ) -> Unit
) {
    var customerName by rememberSaveable { mutableStateOf("Vikram Aditya (Customer App)") }
    var pickupAddress by rememberSaveable { mutableStateOf("Clock Tower Center, Nalgonda") }
    var pickupLatInput by rememberSaveable { mutableStateOf("17.0500") }
    var pickupLngInput by rememberSaveable { mutableStateOf("79.2667") }

    var dropoffAddress by rememberSaveable { mutableStateOf("SLN Terminus, Gachibowli, Hyderabad") }
    var dropoffLatInput by rememberSaveable { mutableStateOf("17.4401") }
    var dropoffLngInput by rememberSaveable { mutableStateOf("78.3489") }

    var cargoDetails by rememberSaveable { mutableStateOf("Fragile Load") }
    var cargoWeightInput by rememberSaveable { mutableStateOf("320") }
    var vehicleTier by rememberSaveable { mutableStateOf("ZALDI_VAN") }
    var guaranteedFareInput by rememberSaveable { mutableStateOf("145.00") }
    var distanceKmInput by rememberSaveable { mutableStateOf("105.0") }
    var showRawJsonPayload by rememberSaveable { mutableStateOf(true) }

    val pickupLat = pickupLatInput.toDoubleOrNull() ?: 17.0500
    val pickupLng = pickupLngInput.toDoubleOrNull() ?: 79.2667
    val dropoffLat = dropoffLatInput.toDoubleOrNull() ?: 17.4401
    val dropoffLng = dropoffLngInput.toDoubleOrNull() ?: 78.3489
    val pickupGeohash = remember(pickupLat, pickupLng) {
        GeoUtils.encodeGeohash(pickupLat, pickupLng, 7).uppercase()
    }
    val dropoffGeohash = remember(dropoffLat, dropoffLng) {
        GeoUtils.encodeGeohash(dropoffLat, dropoffLng, 7).uppercase()
    }
    val onlineDrivers = remember(allDrivers) { allDrivers.filter { it.isOnline } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_book_ride_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Customer 'Book Ride' Screen Header & Route Visualizer Card
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Book Ride",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "BOOK RIDE • CUSTOMER APP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "POST /api/internal/dispatch",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Enter pickup & drop-off locations to trigger Redis 5km lookup & zaldi_ping",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Live Route & Nearby Drivers Radar Canvas
                Surface(
                    color = Color(0xFF0B0F17),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val emerald = MaterialTheme.colorScheme.secondary
                        val amber = MaterialTheme.colorScheme.primary
                        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Subtle coordinate grid
                            for (i in 1..5) {
                                val x = w * (i / 6f)
                                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                            }
                            for (j in 1..2) {
                                val y = h * (j / 3f)
                                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                            }

                            val pickupOffset = Offset(w * 0.18f, h * 0.68f)
                            val dropoffOffset = Offset(w * 0.82f, h * 0.30f)

                            // 5km Redis GEORADIUS ring around pickup
                            drawCircle(
                                color = emerald.copy(alpha = 0.12f),
                                radius = 48.dp.toPx(),
                                center = pickupOffset
                            )
                            drawCircle(
                                color = emerald.copy(alpha = 0.35f),
                                radius = 48.dp.toPx(),
                                center = pickupOffset,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                )
                            )

                            // Route Line from Pickup to Drop-off
                            drawLine(
                                color = amber,
                                start = pickupOffset,
                                end = dropoffOffset,
                                strokeWidth = 4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))
                            )

                            // Nearby online driver dots around pickup
                            val driverOffsets = listOf(
                                Offset(w * 0.12f, h * 0.42f),
                                Offset(w * 0.25f, h * 0.82f),
                                Offset(w * 0.28f, h * 0.48f)
                            )
                            driverOffsets.take(onlineDrivers.size.coerceAtLeast(1)).forEach { drvPos ->
                                drawCircle(color = emerald.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = drvPos)
                                drawCircle(color = emerald, radius = 4.dp.toPx(), center = drvPos)
                            }

                            // Pickup Pin (Emerald)
                            drawCircle(color = emerald.copy(alpha = 0.28f), radius = 12.dp.toPx(), center = pickupOffset)
                            drawCircle(color = emerald, radius = 6.dp.toPx(), center = pickupOffset)

                            // Dropoff Pin (Amber)
                            drawCircle(color = amber.copy(alpha = 0.28f), radius = 12.dp.toPx(), center = dropoffOffset)
                            drawCircle(color = amber, radius = 6.dp.toPx(), center = dropoffOffset)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PICKUP [$pickupGeohash] → DROPOFF [$dropoffGeohash]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${onlineDrivers.size} Drivers in 5km GEO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = pickupAddress.take(28),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${distanceKmInput} km • $${guaranteedFareInput}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = dropoffAddress.take(28),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Route Corridor Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = pickupAddress.contains("Nalgonda", ignoreCase = true),
                        onClick = {
                            customerName = "Vikram Aditya (Nalgonda Hub)"
                            pickupAddress = "Clock Tower Center, Nalgonda"
                            pickupLatInput = "17.0500"
                            pickupLngInput = "79.2667"
                            dropoffAddress = "SLN Terminus, Gachibowli, Hyderabad"
                            dropoffLatInput = "17.4401"
                            dropoffLngInput = "78.3489"
                            cargoDetails = "Fragile Load"
                            cargoWeightInput = "320"
                            vehicleTier = "ZALDI_VAN"
                            distanceKmInput = "105.0"
                            guaranteedFareInput = "145.00"
                        },
                        label = { Text("Nalgonda → Hyderabad (105km)") },
                        modifier = Modifier.testTag("preset_nalgonda_hyderabad")
                    )
                    FilterChip(
                        selected = pickupAddress.contains("Mission", ignoreCase = true),
                        onClick = {
                            customerName = "Elena Rostova (Biotech Lab)"
                            pickupAddress = "450 Mission St, SF Transit Tower"
                            pickupLatInput = "37.7897"
                            pickupLngInput = "-122.4014"
                            dropoffAddress = "1600 Owens St, Mission Bay Hub"
                            dropoffLatInput = "37.7675"
                            dropoffLngInput = "-122.3921"
                            cargoDetails = "Cold Chain"
                            cargoWeightInput = "180"
                            vehicleTier = "ZALDI_EV"
                            distanceKmInput = "6.8"
                            guaranteedFareInput = "48.50"
                        },
                        label = { Text("SF Mission → Biotech Hub (6.8km)") },
                        modifier = Modifier.testTag("preset_mission_biotech")
                    )
                    FilterChip(
                        selected = pickupAddress.contains("Hitech", ignoreCase = true),
                        onClick = {
                            customerName = "Ananya Reddy (Enterprise)"
                            pickupAddress = "Cyber Towers, Hitech City, Hyderabad"
                            pickupLatInput = "17.4504"
                            pickupLngInput = "78.3808"
                            dropoffAddress = "RGIA Airport Cargo Terminal, Shamshabad"
                            dropoffLatInput = "17.2403"
                            dropoffLngInput = "78.4294"
                            cargoDetails = "Heavy Pallet"
                            cargoWeightInput = "540"
                            vehicleTier = "ZALDI_PRIME"
                            distanceKmInput = "32.4"
                            guaranteedFareInput = "76.00"
                        },
                        label = { Text("Hitech City → RGIA Cargo (32km)") },
                        modifier = Modifier.testTag("preset_hitech_rgia")
                    )
                }

                // Pickup Location Input + Coordinates
                OutlinedTextField(
                    value = pickupAddress,
                    onValueChange = { pickupAddress = it },
                    label = { Text("Pickup Location (Address / Hub)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Pickup Location",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_pickup_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pickupLatInput,
                        onValueChange = { pickupLatInput = it },
                        label = { Text("Pickup Lat") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("booking_pickup_lat_input")
                    )
                    OutlinedTextField(
                        value = pickupLngInput,
                        onValueChange = { pickupLngInput = it },
                        label = { Text("Pickup Lng") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("booking_pickup_lng_input")
                    )
                    OutlinedButton(
                        onClick = {
                            val tmpAddr = pickupAddress
                            val tmpLat = pickupLatInput
                            val tmpLng = pickupLngInput
                            pickupAddress = dropoffAddress
                            pickupLatInput = dropoffLatInput
                            pickupLngInput = dropoffLngInput
                            dropoffAddress = tmpAddr
                            dropoffLatInput = tmpLat
                            dropoffLngInput = tmpLng
                        },
                        modifier = Modifier.testTag("swap_pickup_dropoff_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap Pickup and Drop-off",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Swap")
                    }
                }

                // Drop-off Location Input + Coordinates
                OutlinedTextField(
                    value = dropoffAddress,
                    onValueChange = { dropoffAddress = it },
                    label = { Text("Drop-off Location (Destination Address)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Drop-off Location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_dropoff_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dropoffLatInput,
                        onValueChange = { dropoffLatInput = it },
                        label = { Text("Drop-off Lat") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("booking_dropoff_lat_input")
                    )
                    OutlinedTextField(
                        value = dropoffLngInput,
                        onValueChange = { dropoffLngInput = it },
                        label = { Text("Drop-off Lng") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("booking_dropoff_lng_input")
                    )
                    OutlinedButton(
                        onClick = {
                            val calcDist = GeoUtils.calculateDistanceKm(
                                pickupLat,
                                pickupLng,
                                dropoffLat,
                                dropoffLng
                            ).coerceAtLeast(1.2)
                            val roundedDist = (calcDist * 10.0).roundToInt() / 10.0
                            distanceKmInput = String.format(Locale.US, "%.1f", roundedDist)
                            val tierRate = when (vehicleTier) {
                                "ZALDI_VAN" -> 1.35
                                "ZALDI_PRIME" -> 1.15
                                else -> 0.95
                            }
                            val estFare = (12.0 + roundedDist * tierRate).coerceAtLeast(15.0)
                            guaranteedFareInput = String.format(Locale.US, "%.2f", estFare)
                        },
                        modifier = Modifier.testTag("recalculate_fare_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Auto Calculate Distance",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Calc Fare")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Customer Name + Vehicle Tier Selector
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name / Account") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_customer_name_input")
                )

                Text(
                    text = "SELECT VEHICLE TIER & CARGO CLASSIFICATION:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("ZALDI_EV", "Zaldi EV", "550kg"),
                        Triple("ZALDI_PRIME", "Zaldi Prime", "750kg"),
                        Triple("ZALDI_VAN", "Zaldi Van", "1200kg")
                    ).forEach { (tierCode, label, capLabel) ->
                        val selected = vehicleTier == tierCode
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { vehicleTier = tierCode }
                                .testTag("tier_select_$tierCode")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = capLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Fragile Load", "Heavy Pallet", "Cold Chain", "Express Ride").forEach { load ->
                        FilterChip(
                            selected = cargoDetails == load,
                            onClick = { cargoDetails = load },
                            label = { Text(load) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = guaranteedFareInput,
                        onValueChange = { guaranteedFareInput = it },
                        label = { Text("Fare ($)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = distanceKmInput,
                        onValueChange = { distanceKmInput = it },
                        label = { Text("Distance (km)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cargoWeightInput,
                        onValueChange = { cargoWeightInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // PostgreSQL Race-Condition Lock Simulator Switch
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simulate PostgreSQL Race-Condition Lock (55P03)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Test competing driver claiming SELECT FOR UPDATE NOWAIT lock first",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = simulateRaceLockConflict,
                            onCheckedChange = onToggleRaceLockConflict,
                            modifier = Modifier.testTag("race_lock_toggle")
                        )
                    }
                }

                // Primary Book Ride CTA Button
                Button(
                    onClick = {
                        onBookRideViaDispatchEndpoint(
                            customerName,
                            pickupAddress,
                            pickupLat,
                            pickupLng,
                            dropoffAddress,
                            dropoffLat,
                            dropoffLng,
                            cargoDetails,
                            cargoWeightInput.toIntOrNull() ?: 320,
                            vehicleTier,
                            guaranteedFareInput.toDoubleOrNull() ?: 145.0,
                            distanceKmInput.toDoubleOrNull() ?: 105.0
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("book_custom_ride_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF0B0F17)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = "Book Ride & Trigger Driver Ping")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BOOK RIDE & PING DRIVER (/API/INTERNAL/DISPATCH)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 2. Live `/api/internal/dispatch` Endpoint Response & Payload Inspector
        if (lastDispatchTrace != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.large)
                    .testTag("dispatch_endpoint_trace_card"),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Dispatch Response",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = lastDispatchTrace.statusLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${lastDispatchTrace.endpointUrl} • ${lastDispatchTrace.latencyMs}ms • ${lastDispatchTrace.timestampFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilterChip(
                            selected = showRawJsonPayload,
                            onClick = { showRawJsonPayload = !showRawJsonPayload },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Toggle JSON",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text(if (showRawJsonPayload) "Hide JSON" else "View JSON") }
                        )
                    }

                    Text(
                        text = "Pinged ${lastDispatchTrace.driversPingedCount} online drivers (${activeDriver?.fullName ?: "Mateo Vance"}) for ${lastDispatchTrace.pickupAddress} → ${lastDispatchTrace.dropoffAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    AnimatedVisibility(visible = showRawJsonPayload) {
                        Surface(
                            color = Color(0xFF0B0F17),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "// Request Body -> POST /api/internal/dispatch",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = lastDispatchTrace.requestJson,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0),
                                    fontFamily = FontFamily.Monospace
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Text(
                                    text = "// Response Body <- 200 OK (zaldi_ping broadcasted)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = lastDispatchTrace.responseJson,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF34D399),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
