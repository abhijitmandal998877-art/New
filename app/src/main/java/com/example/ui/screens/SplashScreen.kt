package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.FirebaseManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateCheck: (Boolean) -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scaleValue by animateFloatAsState(
        targetValue = if (startAnimation) 1.15f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alphaValue by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(1500, easing = EaseInOutSine),
        label = "alpha"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
        // Check if user already logged in
        val isLoggedIn = FirebaseManager.currentUserFlow.value != null
        onNavigateCheck(isLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Symbolic Animated Care Logo Drawn with Canvas for crisp resolution
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scaleValue)
                    .alpha(alphaValue),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Outer healing halo
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.15f),
                        radius = size.minDimension / 2.2f,
                        center = center
                    )
                    
                    // Outlined bloom curves
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.3f),
                        radius = size.minDimension / 2.7f,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    
                    // Center core caring heart
                    val width = size.width
                    val height = size.height
                    val heartPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width / 2f, height / 1.7f)
                        cubicTo(
                            width / 3f, height / 2.8f,
                            width / 4.5f, height / 2.1f,
                            width / 2f, height * 0.72f
                        )
                        cubicTo(
                            width / 1.25f, height / 2.1f,
                            width * (2f/3f), height / 2.8f,
                            width / 2f, height / 1.7f
                        )
                    }
                    drawPath(
                        path = heartPath,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name with high-fidelity letter spacing
            Text(
                text = "Period Care",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alphaValue)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Calming subtitle
            Text(
                text = "Your Gentle Menstrual Wellness Companion",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alphaValue)
            )
        }
    }
}
