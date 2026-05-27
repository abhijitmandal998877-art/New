package com.example.ui.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.FirebaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onAdminUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var isLoginTab by remember { mutableStateOf(true) }
    
    // Connection mode state linking to FirebaseManager
    var connectionModeOnline by remember { mutableStateOf(FirebaseManager.isFirebaseOnline) }
    
    // Form States
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Admin Dialog state
    var showAdminDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }

    // Validation computed properties
    val isEmailValid = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val passwordStrength = checkPasswordStrength(password) // 0 to 3

    val canSubmitSignUp = fullName.trim().isNotEmpty() && isEmailValid && password.length >= 6
    val canSubmitLogin = isEmailValid && password.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Heart & Petal Header Visual
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Heart Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Period Care",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = "Track, Predict, Calm & Manage Your Cycle",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Dual Tab Controls in a stylized card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isLoginTab = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LOGIN",
                        fontWeight = FontWeight.Bold,
                        color = if (isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isLoginTab = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SIGN UP",
                        fontWeight = FontWeight.Bold,
                        color = if (!isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection Mode State Banner
            Surface(
                color = if (connectionModeOnline) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (connectionModeOnline) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (connectionModeOnline) Color(0xFF43A047) else Color(0xFFF57C00),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = if (connectionModeOnline) "Production Cloud Mode" else "Offline Sandbox Mode (Active)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (connectionModeOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (connectionModeOnline) {
                                "Connected securely to Firebase services."
                            } else {
                                "Dummy key detected. Using secure local sandbox offline."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    if (connectionModeOnline) {
                        TextButton(
                            onClick = {
                                FirebaseManager.isFirebaseOnline = false
                                connectionModeOnline = false
                                Toast.makeText(context, "Switched to offline Demo Sandbox!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("SANDBOX", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Using Sandbox Mode so you can use the app without any Firebase keys. Feel free to configure your Google Firebase key later inside google-services.json context.", Toast.LENGTH_LONG).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info icon Details",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Fields
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isLoginTab) "Welcome Back" else "Create Account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    // FULL NAME (Sign Up only)
                    AnimatedVisibility(visible = !isLoginTab) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User Icon") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // EMAIL
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = email.isNotEmpty() && !isEmailValid,
                        supportingText = {
                            if (email.isNotEmpty() && !isEmailValid) {
                                Text("Invalid email format (needs @ and domain)", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // PASSWORD
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = !isLoginTab && password.isNotEmpty() && password.length < 6,
                        supportingText = {
                            if (!isLoginTab && password.isNotEmpty() && password.length < 6) {
                                Text("Password must be at least 6 characters.", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Strength Indicator (Sign Up only)
                    AnimatedVisibility(visible = !isLoginTab && password.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Password Strength: ${getStrengthLabel(passwordStrength)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = getStrengthColor(passwordStrength)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(3) { index ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (index < passwordStrength) getStrengthColor(passwordStrength)
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Submission Button
                    Button(
                        onClick = {
                            isLoading = true
                            if (isLoginTab) {
                                FirebaseManager.login(email.trim(), password) { success, msg ->
                                    if (success) {
                                        isLoading = false
                                        onAuthSuccess()
                                    } else {
                                        if (msg.contains("API key", ignoreCase = true)) {
                                            FirebaseManager.isFirebaseOnline = false
                                            connectionModeOnline = false
                                            Toast.makeText(context, "Firebase API Key is Invalid. Transitioning to local Sandbox Mode...", Toast.LENGTH_LONG).show()
                                            // Retry immediately in local Sandbox mode
                                            FirebaseManager.login(email.trim(), password) { subSuccess, subMsg ->
                                                isLoading = false
                                                if (subSuccess) {
                                                    onAuthSuccess()
                                                } else {
                                                    Toast.makeText(context, subMsg, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                FirebaseManager.signUp(fullName.trim(), email.trim(), password) { success, msg ->
                                    if (success) {
                                        isLoading = false
                                        onAuthSuccess()
                                    } else {
                                        if (msg.contains("API key", ignoreCase = true)) {
                                            FirebaseManager.isFirebaseOnline = false
                                            connectionModeOnline = false
                                            Toast.makeText(context, "Firebase API Key is Invalid. Transitioning to local Sandbox Mode...", Toast.LENGTH_LONG).show()
                                            // Retry immediately in local Sandbox mode
                                            FirebaseManager.signUp(fullName.trim(), email.trim(), password) { subSuccess, subMsg ->
                                                isLoading = false
                                                if (subSuccess) {
                                                    onAuthSuccess()
                                                } else {
                                                    Toast.makeText(context, subMsg, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && (if (isLoginTab) canSubmitLogin else canSubmitSignUp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = if (isLoginTab) "LOG IN" else "SIGN UP & CREATE PROFILE",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Secure Admin Entry Point Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showAdminDialog = true }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Admin Entry",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Developer & Access Panel",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }

    // Custom dialog to login as Admin
    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminDialog = false
                adminPasswordInput = ""
            },
            title = {
                Text(
                    text = "🔒 Admin Control Login",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Please enter the secure administrative access key to unlock analytics, feedback, and cloud broadcasts.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        label = { Text("Admin Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeAdminPass = FirebaseManager.getAdminPassword()
                        if (adminPasswordInput == activeAdminPass) {
                            showAdminDialog = false
                            adminPasswordInput = ""
                            onAdminUnlocked()
                        } else {
                            Toast.makeText(context, "Wrong access passcode!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("UNLOCK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

// Password Strength helpers
private fun checkPasswordStrength(password: String): Int {
    if (password.length < 6) return 1
    var score = 1
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score
}

private fun getStrengthLabel(strength: Int): String {
    return when (strength) {
        1 -> "Weak (Needs 6+ characters)"
        2 -> "Medium (Add numbers/special signs)"
        else -> "Strong"
    }
}

private fun getStrengthColor(strength: Int): Color {
    return when (strength) {
        1 -> Color(0xFFE53935) // Red
        2 -> Color(0xFFFFB300) // Orange/Yellow
        else -> Color(0xFF43A047) // Green
    }
}
