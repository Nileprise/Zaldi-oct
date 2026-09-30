package com.example.features.authentication

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.utils.GeoUtils

@Composable
fun AuthenticationScreen(
    initialPhone: String,
    initialName: String,
    pendingOtpCode: String?,
    authSessionState: ZaldiAuthSessionState,
    onSignInWithGoogle: (context: Context, driverName: String, driverEmail: String, phone: String) -> Unit,
    onSignInWithFirebaseEmail: (email: String, password: String, driverName: String, phone: String, isRegister: Boolean) -> Unit,
    onRequestOtp: (String) -> Unit,
    onVerifyOtp: (phone: String, name: String, otp: String) -> Boolean
) {
    val context = LocalContext.current
    var authMethod by rememberSaveable { mutableStateOf("GOOGLE_FIREBASE") }
    var driverName by rememberSaveable { mutableStateOf(initialName.ifBlank { "Mateo Vance" }) }
    var driverEmail by rememberSaveable { mutableStateOf(authSessionState.email.ifBlank { "mateo.vance@zaldi.fleet" }) }
    var firebasePassword by rememberSaveable { mutableStateOf("ZaldiDriver#2026") }
    var isCreateNewAccount by rememberSaveable { mutableStateOf(false) }

    var phoneNumber by rememberSaveable { mutableStateOf(initialPhone.ifBlank { "+1 (415) 890-4210" }) }
    var otpInput by rememberSaveable { mutableStateOf("") }
    var otpStepActive by rememberSaveable { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Emblem Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Zaldi Driver Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ZALDI DRIVER",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Firebase Authentication & Credential Manager Google Sign-In",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Partner Security",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "Secure Driver Account Access",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Powered by Firebase Auth & AndroidX Credential Manager",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mode Switcher: Google / Firebase Auth vs SMS OTP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = authMethod == "GOOGLE_FIREBASE",
                            onClick = {
                                authMethod = "GOOGLE_FIREBASE"
                                errorText = null
                            },
                            label = { Text("Google & Firebase Auth") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Google and Firebase Auth",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("auth_mode_google_firebase")
                        )
                        FilterChip(
                            selected = authMethod == "PHONE_OTP",
                            onClick = {
                                authMethod = "PHONE_OTP"
                                errorText = null
                            },
                            label = { Text("Mobile SMS OTP") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Mobile SMS OTP",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("auth_mode_phone_otp")
                        )
                    }

                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { driverName = it },
                        label = { Text("Partner Full Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Partner Name"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input")
                    )

                    if (authMethod == "GOOGLE_FIREBASE") {
                        OutlinedTextField(
                            value = driverEmail,
                            onValueChange = {
                                driverEmail = it
                                errorText = null
                            },
                            label = { Text("Driver Google / Firebase Email") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Icon"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firebase_email_input")
                        )

                        // Primary Action: Google Sign-In via Credential Manager (GetGoogleIdOption)
                        Button(
                            onClick = {
                                errorText = null
                                onSignInWithGoogle(
                                    context,
                                    driverName,
                                    driverEmail,
                                    phoneNumber
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color(0xFF0B0F17)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF1A73E8),
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google (Credential Manager)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Text(
                            text = "OR SIGN IN WITH FIREBASE EMAIL & PASSWORD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = firebasePassword,
                            onValueChange = {
                                firebasePassword = it
                                errorText = null
                            },
                            label = { Text("Firebase Account Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Password Key"
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firebase_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (!driverEmail.contains("@")) {
                                        errorText = "Please enter a valid driver email address."
                                    } else if (firebasePassword.length < 6) {
                                        errorText = "Password must be at least 6 characters."
                                    } else {
                                        onSignInWithFirebaseEmail(
                                            driverEmail,
                                            firebasePassword,
                                            driverName,
                                            phoneNumber,
                                            isCreateNewAccount
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("firebase_email_signin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Firebase Email Auth",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCreateNewAccount) "Create Driver Account" else "Sign In with Email",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilterChip(
                                selected = isCreateNewAccount,
                                onClick = { isCreateNewAccount = !isCreateNewAccount },
                                label = { Text(if (isCreateNewAccount) "New Driver" else "Existing") },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("toggle_firebase_register_mode")
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it
                                errorText = null
                            },
                            label = { Text("Registered Mobile Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Phone Icon"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_phone_input")
                        )

                        AnimatedVisibility(visible = otpStepActive) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (pendingOtpCode != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Sms,
                                                    contentDescription = "SMS Code",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "SMS OTP: $pendingOtpCode",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { otpInput = pendingOtpCode },
                                                modifier = Modifier.testTag("autofill_otp_button")
                                            ) {
                                                Text("Auto-Fill")
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = {
                                        if (it.length <= 6) otpInput = it.filter { ch -> ch.isDigit() }
                                        errorText = null
                                    },
                                    label = { Text("6-Digit Verification OTP") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "OTP Lock"
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_otp_input")
                                )
                            }
                        }

                        if (!otpStepActive) {
                            Button(
                                onClick = {
                                    if (!GeoUtils.isValidPhone(phoneNumber)) {
                                        errorText = "Please enter a valid 8-15 digit mobile number."
                                    } else {
                                        onRequestOtp(phoneNumber)
                                        otpInput = "482910"
                                        otpStepActive = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("request_otp_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = "Send Verification OTP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    val ok = onVerifyOtp(phoneNumber, driverName, otpInput)
                                    if (!ok) {
                                        errorText = "Invalid 6-digit OTP code. Use 482910."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("verify_otp_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verify"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verify & Launch Cockpit",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // SDK Status Inspector Card
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
                                text = "CONFIGURED AUTHENTICATION DEPENDENCIES",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• com.google.firebase:firebase-auth (via firebase-bom:34.17.0)\n" +
                                    "• androidx.credentials:credentials:1.5.0\n" +
                                    "• androidx.credentials:credentials-play-services-auth:1.5.0\n" +
                                    "• com.google.android.libraries.identity.googleid:googleid:1.1.1",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Status: ${authSessionState.credentialManagerStatus}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
