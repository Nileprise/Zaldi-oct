package com.example.features.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.errors.DiagnosticEvent
import com.example.core.utils.GeoUtils
import com.example.features.authentication.ZaldiAuthSessionState
import com.example.services.localstorage.DriverProfileEntity
import com.example.services.localstorage.ReviewEntity
import com.example.services.localstorage.RideOrderEntity
import com.example.services.localstorage.VehicleEntity

@Composable
fun ProfileScreen(
    profile: DriverProfileEntity?,
    authSessionState: ZaldiAuthSessionState,
    vehicles: List<VehicleEntity>,
    trips: List<RideOrderEntity>,
    reviews: List<ReviewEntity>,
    diagnosticEvents: List<DiagnosticEvent>,
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: (makeAndModel: String, plateNumber: String, capacityKg: Int, insuranceExpiry: String, tier: String) -> Unit,
    onUpdateBackendUrl: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onSaveDriverProfile: (firstName: String, lastName: String, phone: String, email: String) -> Unit,
    onTriggerGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showEditProfileForm by remember { mutableStateOf(false) }
    var editFirstName by remember(profile?.firstName) { mutableStateOf(profile?.firstName ?: "Mateo") }
    var editLastName by remember(profile?.lastName) { mutableStateOf(profile?.lastName ?: "Vance") }
    var editPhone by remember(profile?.phoneNumber) { mutableStateOf(profile?.phoneNumber ?: "+1 (415) 890-4210") }
    var editEmail by remember(profile?.email) { mutableStateOf(profile?.email ?: "mateo.vance@zaldi.fleet") }
    var selectedErdTable by remember { mutableStateOf("DRIVERS") }
    var socketUrlInput by remember(profile?.backendSocketUrl) {
        mutableStateOf(profile?.backendSocketUrl ?: "ws://10.0.2.2:3000")
    }

    var vMakeModel by remember { mutableStateOf("2025 Rivian R1S Dual Max") }
    var vPlate by remember { mutableStateOf("ZLD-771R") }
    var vCapacityKg by remember { mutableStateOf("850") }
    var vInsuranceExpiry by remember { mutableStateOf("2028-06-15") }
    var vTier by remember { mutableStateOf("ZALDI_XL") }
    var vError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Driver Table Record Summary Header
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${profile?.firstName ?: "Mateo"} ${profile?.lastName ?: "Vance"}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "UUID: ${profile?.driverId?.take(18) ?: "d4f8a920-7c1b"}… • ${profile?.phoneNumber ?: "+1 (415) 890-4210"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (profile?.isDarkMode != false) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Switch Theme",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (profile?.isDarkMode != false) "Day" else "Night")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    KpiStatBox(label = "RATING", value = "${profile?.rating ?: 4.94f} ★")
                    KpiStatBox(label = "TRIPS (1:M)", value = "${trips.size} / ${profile?.totalTrips ?: 648}")
                    KpiStatBox(label = "WALLET", value = GeoUtils.formatCurrency(profile?.walletBalance ?: 428.60))
                }

                OutlinedButton(
                    onClick = { showEditProfileForm = !showEditProfileForm },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_driver_profile_button")
                ) {
                    Text(if (showEditProfileForm) "Close Room Profile Editor" else "Edit Driver Profile in Room SQLite")
                }

                if (showEditProfileForm) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editFirstName,
                                onValueChange = { editFirstName = it },
                                label = { Text("first_name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editLastName,
                                onValueChange = { editLastName = it },
                                label = { Text("last_name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("phone_number (Unique)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSaveDriverProfile(editFirstName, editLastName, editPhone, editEmail)
                                showEditProfileForm = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_driver_profile_button")
                        ) {
                            Text("Save Driver Profile to Room Database")
                        }
                    }
                }
            }
        }

        // 1B. Firebase Authentication & Google Sign-In (Credential Manager) Status Card
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
                        imageVector = Icons.Default.Security,
                        contentDescription = "Firebase Auth & Google Sign-In",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "FIREBASE AUTH & GOOGLE SIGN-IN (CREDENTIAL MANAGER)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Provider: ${authSessionState.authProvider} • UID: ${authSessionState.firebaseUid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Identity: ${authSessionState.displayName} (${authSessionState.email})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Credential Manager: ${authSessionState.credentialManagerStatus}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dependencies: firebase-auth • androidx.credentials • credentials-play-services-auth • googleid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTriggerGoogleSignIn,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_google_sign_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Google Sign-In",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google Sign-In")
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_firebase_auth_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Switch Account",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch Account")
                    }
                }
            }
        }

        // 2. Interactive 3-Table Relational Schema Inspector (Drivers 1:M Vehicles 1:M Trips)
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
                        imageVector = Icons.Default.Schema,
                        contentDescription = "Relational Schema ERD",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text(
                            text = "RELATIONAL ERD INSPECTOR (1:M SQLITE TABLES)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Drivers (PK UUID) 1──M Vehicles (FK) 1──M Trips (FK)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "DRIVERS" to "Drivers (1)",
                        "VEHICLES" to "Vehicles (${vehicles.size})",
                        "TRIPS" to "Trips (${trips.size})"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedErdTable == key,
                            onClick = { selectedErdTable = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("erd_tab_$key")
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (selectedErdTable) {
                            "DRIVERS" -> {
                                Text(
                                    text = "TABLE: drivers (PK: id UUID, UNIQUE: phone_number)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• id (PK): ${profile?.driverId}\n" +
                                        "• first_name: ${profile?.firstName} | last_name: ${profile?.lastName}\n" +
                                        "• phone_number (Unique): ${profile?.phoneNumber}\n" +
                                        "• kyc_status: ${profile?.kycStatus} | is_online: ${profile?.isOnline}\n" +
                                        "• current_lat: ${profile?.currentLat} | current_lng: ${profile?.currentLng}\n" +
                                        "• wallet_balance: ${GeoUtils.formatCurrency(profile?.walletBalance ?: 0.0)} | rating: ${profile?.rating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            "VEHICLES" -> {
                                Text(
                                    text = "TABLE: vehicles (PK: id UUID, FK: driver_id, UNIQUE: plate_number)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                vehicles.forEach { v ->
                                    Text(
                                        text = "• id: ${v.id.take(12)}… | FK driver_id: ${v.driverId.take(10)}…\n" +
                                            "  make_and_model: ${v.makeAndModel} | tier: ${v.vehicleTier}\n" +
                                            "  plate_number: ${v.plateNumber} | capacity_kg: ${v.capacityKg}kg\n" +
                                            "  insurance_expiry: ${v.insuranceExpiry} | is_active: ${v.isActive}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            "TRIPS" -> {
                                Text(
                                    text = "TABLE: trips (PK: id UUID, FKs: driver_id, vehicle_id, customer_id)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                trips.take(4).forEach { t ->
                                    Text(
                                        text = "• id: ${t.orderId.take(10)}… | status: ${t.status}\n" +
                                            "  FK driver_id: ${t.driverId.take(8)}… | FK vehicle_id: ${t.vehicleId.take(8)}…\n" +
                                            "  FK customer_id: ${t.customerId.take(8)}… | dist: ${t.distanceKm}km | final_fare: $${t.totalPayout}\n" +
                                            "  pickup_lat_lng: ${t.pickupLatLngPoint} -> dropoff: ${t.dropoffLatLngPoint}\n" +
                                            "  proof_of_delivery_url: ${t.proofOfDeliveryUrl ?: "NULL"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Vehicles Table Fleet Management (`features/profile`)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Vehicles Table",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "VEHICLES TABLE (${vehicles.size} LINKED TO DRIVER)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showAddVehicleDialog = true },
                        modifier = Modifier.testTag("add_vehicle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Vehicle",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Vehicle")
                    }
                }

                vehicles.forEach { vehicle ->
                    Surface(
                        color = if (vehicle.isActive) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (vehicle.isActive) 1.5.dp else 1.dp,
                                color = if (vehicle.isActive) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectVehicle(vehicle.id) }
                            .testTag("vehicle_item_${vehicle.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vehicle.makeAndModel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Plate: ${vehicle.plateNumber} • Capacity: ${vehicle.capacityKg}kg • Tier: ${vehicle.vehicleTier}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Insurance Expiry: ${vehicle.insuranceExpiry} • UUID: ${vehicle.id.take(8)}…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (vehicle.isActive) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Activate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Passenger Reviews & Socket Config
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
                        imageVector = Icons.Default.Star,
                        contentDescription = "Reviews",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "PASSENGER & CARGO REVIEWS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                reviews.forEachIndexed { idx, rev ->
                    if (idx > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${rev.passengerName} • ${"★".repeat(rev.rating)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = rev.timestampLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = rev.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsEthernet,
                        contentDescription = "Socket Config",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "BACKEND WEBSOCKET & DIAGNOSTICS",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = socketUrlInput,
                        onValueChange = { socketUrlInput = it },
                        label = { Text("Zaldi Backend WebSocket Endpoint") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onUpdateBackendUrl(socketUrlInput) },
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Apply")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Telemetry Logs",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LIVE SQLITE & SOCKET TELEMETRY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                diagnosticEvents.take(4).forEach { ev ->
                    Text(
                        text = "[${ev.timestamp}] [${ev.module}] ${ev.message}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out of Partner Session",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showAddVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("Insert into Vehicles Table") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vMakeModel,
                        onValueChange = { vMakeModel = it },
                        label = { Text("make_and_model") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vPlate,
                            onValueChange = {
                                vPlate = it
                                vError = null
                            },
                            label = { Text("plate_number (Unique)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.55f)
                        )
                        OutlinedTextField(
                            value = vCapacityKg,
                            onValueChange = { vCapacityKg = it.filter { ch -> ch.isDigit() }.take(5) },
                            label = { Text("capacity_kg") },
                            singleLine = true,
                            modifier = Modifier.weight(0.45f)
                        )
                    }
                    OutlinedTextField(
                        value = vInsuranceExpiry,
                        onValueChange = { vInsuranceExpiry = it },
                        label = { Text("insurance_expiry (YYYY-MM-DD)") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ZALDI_EV", "ZALDI_VAN", "ZALDI_XL").forEach { tier ->
                            FilterChip(
                                selected = vTier == tier,
                                onClick = { vTier = tier },
                                label = { Text(tier.replace("ZALDI_", "")) }
                            )
                        }
                    }
                    if (vError != null) {
                        Text(
                            text = vError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!GeoUtils.isValidLicensePlate(vPlate)) {
                            vError = "Enter a valid 4-10 character unique plate_number."
                        } else {
                            onAddVehicle(
                                vMakeModel.ifBlank { "2025 Tesla Semi Lite" },
                                vPlate,
                                vCapacityKg.toIntOrNull() ?: 750,
                                vInsuranceExpiry.ifBlank { "2028-01-01" },
                                vTier
                            )
                            showAddVehicleDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_vehicle_button")
                ) {
                    Text("Insert & Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun KpiStatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
