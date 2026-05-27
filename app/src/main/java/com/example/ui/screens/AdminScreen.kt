package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.FirebaseManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var activeAdminTab by remember { mutableStateOf(0) } // 0: FCM Sender, 1: Accounts, 2: Feedback

    // Change admin password dialog state
    var showChangePassDialog by remember { mutableStateOf(false) }
    var currentPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }

    // Live Flow Collections
    val usersList by FirebaseManager.allUsersFlow.collectAsStateWithLifecycle()
    val feedbackList by FirebaseManager.allFeedbackFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        // Fetch up-to-date values on opening
        FirebaseManager.fetchAllUsers {}
        FirebaseManager.fetchAllFeedback {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Admin Control Panel",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close admin panel")
                    }
                },
                actions = {
                    // Password configuration option
                    IconButton(onClick = { showChangePassDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Config admin keys",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Elegant Tab Selectors
            TabRow(selectedTabIndex = activeAdminTab) {
                Tab(
                    selected = activeAdminTab == 0,
                    onClick = { activeAdminTab = 0 },
                    text = { Text("FCM Broadcaster", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Upload, contentDescription = "FCM Broadcast") }
                )
                Tab(
                    selected = activeAdminTab == 1,
                    onClick = { activeAdminTab = 1 },
                    text = { Text("Users (${usersList.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Users count") }
                )
                Tab(
                    selected = activeAdminTab == 2,
                    onClick = { activeAdminTab = 2 },
                    text = { Text("Feedback (${feedbackList.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Feedbacks list") }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (activeAdminTab) {
                    0 -> FcmNotificationSenderSection()
                    1 -> RegisteredUsersAnalyticsSection()
                    2 -> DirectFeedbackSection()
                }
            }
        }
    }

    // Modal to Change Admin Access Passcode
    if (showChangePassDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePassDialog = false
                currentPassInput = ""
                newPassInput = ""
            },
            title = {
                Text(
                    text = "🔐 Change Admin Passcode",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Modify the active administrator passcode. Default is '9242505224'. Ensure this is secure.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it },
                        label = { Text("Current Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text("New Security Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val realPass = FirebaseManager.getAdminPassword()
                        if (currentPassInput != realPass) {
                            Toast.makeText(context, "Current password incorrect!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassInput.trim().isEmpty()) {
                            Toast.makeText(context, "New password cannot be empty!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        FirebaseManager.updateAdminPassword(newPassInput.trim()) { success ->
                            if (success) {
                                showChangePassDialog = false
                                currentPassInput = ""
                                newPassInput = ""
                                Toast.makeText(context, "Admin Passcode updated to '$newPassInput'!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("SAVE PASSCODE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePassDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

// Subcomponents definitions

@Composable
fun FcmNotificationSenderSection() {
    val context = LocalContext.current
    var fcmTitle by remember { mutableStateOf("") }
    var fcmText by remember { mutableStateOf("") }
    var targetSelectionAll by remember { mutableStateOf(true) } // true for All, false for Specific
    var targetEmailInput by remember { mutableStateOf("") }
    var isBroadcasting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "FCM push loader",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text("Firebase Cloud Messaging Portal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Pushes foreground/background deep alerts to users instantly.", fontSize = 11.sp)
                }
            }
        }

        OutlinedTextField(
            value = fcmTitle,
            onValueChange = { fcmTitle = it },
            label = { Text("Notification Title") },
            placeholder = { Text("e.g., Hydration Milestone Reached!") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fcmText,
            onValueChange = { fcmText = it },
            label = { Text("Notification Body Text") },
            placeholder = { Text("e.g., Drink 3 liters of warm ginger tea to expand lower back comfort today.") },
            minLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Target Picker Radio Buttons
        Column {
            Text("Target Audience Select:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { targetSelectionAll = true }
            ) {
                RadioButton(selected = targetSelectionAll, onClick = { targetSelectionAll = true })
                Text("Broadcast to All Registered Devices", fontSize = 13.sp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { targetSelectionAll = false }
            ) {
                RadioButton(selected = !targetSelectionAll, onClick = { targetSelectionAll = false })
                Text("Target Specific User Account via Email", fontSize = 13.sp)
            }
        }

        AnimatedVisibility(visible = !targetSelectionAll) {
            OutlinedTextField(
                value = targetEmailInput,
                onValueChange = { targetEmailInput = it },
                label = { Text("Target User Email Address") },
                placeholder = { Text("e.g. priti.sen@gmail.com") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                val target = if (targetSelectionAll) "All" else targetEmailInput.trim()
                if (target == "") {
                    Toast.makeText(context, "Target email is required!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isBroadcasting = true
                FirebaseManager.broadcastAdminNotification(fcmTitle.trim(), fcmText.trim(), target) { success ->
                    isBroadcasting = false
                    if (success) {
                        fcmTitle = ""
                        fcmText = ""
                        targetEmailInput = ""
                        Toast.makeText(context, "FCM Broadcast Triggered Successfully!", Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = !isBroadcasting && fcmTitle.trim().isNotEmpty() && fcmText.trim().isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isBroadcasting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("LAUNCH PUSH NOTIFICATION", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RegisteredUsersAnalyticsSection() {
    val usersList by FirebaseManager.allUsersFlow.collectAsStateWithLifecycle()

    if (usersList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No registered users detected yet in analytics collection.")
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Total Registered Accounts: ${usersList.size}",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Table Header Row
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Name",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Email Address",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "Joined Timestamp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            // Table Body List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(usersList) { u ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = u.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = u.email,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.4f)
                                )
                                Text(
                                    text = u.registrationDate,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
                
                // Bottom row rounding
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun DirectFeedbackSection() {
    val feedbackList by FirebaseManager.allFeedbackFlow.collectAsStateWithLifecycle()

    if (feedbackList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No feedback recorded.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(feedbackList) { f ->
                val friendlyTime = remember(f.timestamp) {
                    try {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        sdf.format(Date(f.timestamp))
                    } catch (e: Exception) {
                        ""
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = f.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Text(text = f.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(text = friendlyTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = f.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
