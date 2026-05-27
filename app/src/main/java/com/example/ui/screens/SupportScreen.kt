package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.FirebaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val currentUser by FirebaseManager.currentUserFlow.collectAsStateWithLifecycle()

    var activePasswordChange by remember { mutableStateOf(false) }
    var changePwdInput by remember { mutableStateOf("") }
    var confirmPwdInput by remember { mutableStateOf("") }
    var isChangingPwd by remember { mutableStateOf(false) }

    var customFeedbackMsg by remember { mutableStateOf("") }
    var isSubmittingFeedback by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ---- 1. User Profile Subsection ----
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Avatar bullet
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentUser?.name ?: "Guest User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Registered on: ${currentUser?.registrationDate ?: ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Change password collapse control
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activePasswordChange = !activePasswordChange }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockReset, contentDescription = "Lock", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Auth Password", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Icon(
                            imageVector = if (activePasswordChange) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown indicator"
                        )
                    }

                    AnimatedVisibility(visible = activePasswordChange) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = changePwdInput,
                                onValueChange = { changePwdInput = it },
                                label = { Text("New Security Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPwdInput,
                                onValueChange = { confirmPwdInput = it },
                                label = { Text("Confirm Security Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (changePwdInput.length < 6) {
                                        Toast.makeText(context, "Must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (changePwdInput != confirmPwdInput) {
                                        Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isChangingPwd = true
                                    FirebaseManager.changePassword(changePwdInput) { success, msg ->
                                        isChangingPwd = false
                                        if (success) {
                                            changePwdInput = ""
                                            confirmPwdInput = ""
                                            activePasswordChange = false
                                            Toast.makeText(context, "Password securely changed!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isChangingPwd && changePwdInput.isNotEmpty() && confirmPwdInput.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                if (isChangingPwd) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("UPDATE PASSWORD")
                                }
                            }
                        }
                    }
                }

                // Log out action button
                Button(
                    onClick = {
                        FirebaseManager.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Exit icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SECURELY LOG OUT", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---- 2. Support Form Sub-Panel (Firestore) ----
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "📨 Contact Support Portal",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Send a direct support query or feedback regarding menstrual forecasts, predictions, calculations, or features. Developer team responds within 24 hours.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = customFeedbackMsg,
                    onValueChange = { customFeedbackMsg = it },
                    label = { Text("Write your message here...") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        isSubmittingFeedback = true
                        FirebaseManager.sendFeedback(customFeedbackMsg) { success, msg ->
                            isSubmittingFeedback = false
                            if (success) {
                                customFeedbackMsg = ""
                                Toast.makeText(context, "Message Saved!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSubmittingFeedback && customFeedbackMsg.trim().isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmittingFeedback) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("SUBMIT LIVE MESSAGE TO DEVELOPER", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---- 3. About Developer Subsection ----
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💻 About Developer",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                
                DetailRow(label = "Developer Name", value = "Abhijit Mandal")
                DetailRow(label = "Location", value = "Rampurhat, Birbhum, West Bengal")
                DetailRow(label = "Current Status", value = "Student")

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Email Deep-link Launcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:imm.abhijit@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Period Care Android App Support")
                            }
                            context.startActivity(Intent.createChooser(intent, "Send Email"))
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mail, contentDescription = "Mail", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Contact Email", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("imm.abhijit@gmail.com", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // WhatsApp Deep-link Launcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val url = "https://api.whatsapp.com/send?phone=919242505224&text=Hello%20Abhijit,%20I'm%20using%20the%20Period%20Care%20Android%20Application."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AppShortcut, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("WhatsApp Contact", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("+91 9242505224", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
