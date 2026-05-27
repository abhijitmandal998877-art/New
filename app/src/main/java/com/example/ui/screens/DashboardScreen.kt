package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PeriodLog
import com.example.firebase.FirebaseManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val currentUser by FirebaseManager.currentUserFlow.collectAsStateWithLifecycle()
    val logs by FirebaseManager.periodLogs.collectAsStateWithLifecycle()

    var showLogDialog by remember { mutableStateOf(false) }

    // Math metrics
    val latestLog = logs.firstOrNull()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Compute cycle state
    val cycleLength = latestLog?.cycleLength ?: 28
    val periodLength = latestLog?.periodLength ?: 5
    
    // Calculate current day of cycle
    val (currentDayOfCycle, daysRemaining, statusMsg, colorAccent, ringProgress) = remember(logs) {
        if (latestLog == null) {
            // Default baseline predictions
            return@remember TripleFive(12, 16, "Follicular Phase", Color(0xFF9C27B0), 12f / 28f)
        }
        try {
            val startDate = sdf.parse(latestLog.startDate) ?: Date()
            val today = Date()
            val diffInMs = today.time - startDate.time
            val diffInDays = (diffInMs / (1000 * 60 * 60 * 24)).toInt()
            
            val currentDay = (diffInDays % cycleLength) + 1
            val remaining = if (currentDay <= periodLength) {
                0
            } else {
                cycleLength - currentDay + 1
            }

            // Status checks
            val status: String
            val accent: Color
            when {
                currentDay <= periodLength -> {
                    status = "Menstruation (Flow Phase)"
                    accent = Color(0xFFE65275) // Primary pink
                }
                currentDay <= 10 -> {
                    status = "Follicular Phase (Normal)"
                    accent = Color(0xFFFBC02D) // Soft Gold
                }
                currentDay in 11..16 -> {
                    status = "High Fertility (Ovulation Period)"
                    accent = Color(0xFF9C27B0) // Lavender
                }
                else -> {
                    status = "Luteal Phase (PMS Forecast)"
                    accent = Color(0xFF0288D1) // Calming Blue
                }
            }
            
            val progress = currentDay.toFloat() / cycleLength.toFloat()
            TripleFive(currentDay, remaining, status, accent, progress)
        } catch (e: Exception) {
            TripleFive(1, 27, "Follicular Phase", Color(0xFF9C27B0), 1f/28f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome and Ring Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Hello, ${currentUser?.name ?: "Guest"}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Welcome to your health dashboard",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Care icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Predictive Ring Circular Display
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw predictive arc with custom Canvas
            val progressColor = colorAccent
            val baseRingColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawCircle(
                    color = baseRingColor,
                    radius = (size.minDimension / 2f) - 16.dp.toPx(),
                    style = Stroke(width = 18.dp.toPx())
                )
                // Prediction Arc
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress,
                    useCenter = false,
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
                    size = size.copy(
                        width = size.width - 32.dp.toPx(),
                        height = size.height - 32.dp.toPx()
                    ),
                    topLeft = Offset(16.dp.toPx(), 16.dp.toPx())
                )
            }

            // Centered Analytics Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Cycle Day",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    text = currentDayOfCycle.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = progressColor
                )
                Text(
                    text = statusMsg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = progressColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next Cycle prediction countdown card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendar prediction",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    val prompt = if (daysRemaining == 0) "Period Active Today" else "Next Period in $daysRemaining Days"
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Text(
                        text = "Calculated using a $cycleLength-day cycle interval.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cycle Quick Status Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ovulation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ovulation Prediction",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Day 14 (±2d)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Fertile Window Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fertile Window",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Days 10 to 16",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logging Period call to action
        Button(
            onClick = { showLogDialog = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Log period")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "LOG LATEST PERIOD DATE",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Symptoms Logging Preview (if logged in latest cycle)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Latest Logged Symptoms",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (latestLog != null && latestLog.symptoms.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        latestLog.symptoms.forEach { symptom ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "🌸 $symptom",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No symptoms recorded in latest cycle. Log your period to add them!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // Elegant Dialog to Log menstrual status
    if (showLogDialog) {
        var startLogDate by remember { mutableStateOf("") }
        var endLogDate by remember { mutableStateOf("") }
        var selectedCycleLen by remember { mutableStateOf(28f) }
        var selectedPeriodLen by remember { mutableStateOf(5f) }
        
        // Symptoms selector states
        val symptomsList = listOf("Cramps", "Headache", "Bloating", "Acne", "Fatigue", "Mood swings", "Backache", "Insomnia", "Nausea")
        val chosenSymptoms = remember { mutableStateListOf<String>() }

        val isReady = startLogDate.isNotEmpty() && endLogDate.isNotEmpty()

        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = {
                Text(
                    text = "🌸 Record Menstrual Period",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Input your dates and cycle lengths. Our algorithm uses this to dynamically compute fertile and ovulation predictions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Period Start Date picker
                    val calendarStart = Calendar.getInstance()
                    val startPickerDialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            // Normalize format
                            startLogDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        },
                        calendarStart.get(Calendar.YEAR),
                        calendarStart.get(Calendar.MONTH),
                        calendarStart.get(Calendar.DAY_OF_MONTH)
                    )

                    OutlinedTextField(
                        value = startLogDate,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Period Start Date") },
                        trailingIcon = {
                            IconButton(onClick = { startPickerDialog.show() }) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar Picker")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Period End Date picker
                    val calendarEnd = Calendar.getInstance()
                    val endPickerDialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            endLogDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        },
                        calendarEnd.get(Calendar.YEAR),
                        calendarEnd.get(Calendar.MONTH),
                        calendarEnd.get(Calendar.DAY_OF_MONTH)
                    )

                    OutlinedTextField(
                        value = endLogDate,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Period End Date") },
                        trailingIcon = {
                            IconButton(onClick = { endPickerDialog.show() }) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar Picker")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Avg Cycle Length Slider
                    Column {
                        Text(
                            text = "Average Cycle Length: ${selectedCycleLen.toInt()} days",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = selectedCycleLen,
                            onValueChange = { selectedCycleLen = it },
                            valueRange = 21f..35f,
                            steps = 13
                        )
                    }

                    // Period Flow Length Slider
                    Column {
                        Text(
                            text = "Menstruation Flow Duration: ${selectedPeriodLen.toInt()} days",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = selectedPeriodLen,
                            onValueChange = { selectedPeriodLen = it },
                            valueRange = 3f..10f,
                            steps = 6
                        )
                    }

                    // Symptoms check grid
                    Column {
                        Text(
                            text = "Feelings & Health Symptoms",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        symptomsList.forEach { symptom ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (chosenSymptoms.contains(symptom)) {
                                            chosenSymptoms.remove(symptom)
                                        } else {
                                            chosenSymptoms.add(symptom)
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = chosenSymptoms.contains(symptom),
                                    onCheckedChange = { checked ->
                                        if (checked == true) chosenSymptoms.add(symptom)
                                        else chosenSymptoms.remove(symptom)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(symptom, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseManager.addPeriodLog(
                            startDate = startLogDate,
                            endDate = endLogDate,
                            cycleLength = selectedCycleLen.toInt(),
                            periodLength = selectedPeriodLen.toInt(),
                            symptoms = chosenSymptoms.toList()
                        ) { success ->
                            if (success) {
                                showLogDialog = false
                                Toast.makeText(context, "Calculations Refreshed!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Log save error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = isReady
                ) {
                    Text("SAVE LOG")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

// Data holder helper to bypass Triple limitation
private data class TripleFive(
    val currentDay: Int,
    val remainingDays: Int,
    val statusMsg: String,
    val accent: Color,
    val progress: Float
)
