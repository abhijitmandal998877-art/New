package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.YogaPose

@Composable
fun YogaScreen() {
    val poses = remember {
        listOf(
            YogaPose(
                title = "Child's Pose (Balasana)",
                description = "A deeply relaxing resting pose that gently stretches the lower back muscles, helping to soothe severe uterine cramps and quiet the nervous system.",
                duration = "5 - 10 Minutes",
                benefits = "Relieves lower back pressure, opens hips, reduces mental stress and fatigue.",
                steps = listOf(
                    "Kneel on your mat, sit back on your heels, with big toes touching and knees hip-width apart.",
                    "Exhale and lower your torso down forward, extending your arms flat out in front of you.",
                    "Rest your forehead gently onto the mat, relaxing your neck completely.",
                    "Take slow, deep bellied breaths, focusing on expanding your lower back on each inhale."
                )
            ),
            YogaPose(
                title = "Reclined Bound Angle (Supta Baddha Konasana)",
                description = "Excellent feminine health restorative posture. Opens the pelvic region, stimulates ovaries, and alleviates abdominal tension organically.",
                duration = "8 - 12 Minutes",
                benefits = "Enhances circulation in pelvis, stretches groin thighs, relieves cramp spasms.",
                steps = listOf(
                    "Sit vertically, bend knees, and bring the soles of your feet together, letting knees fall open.",
                    "Slowly lower your spine backward onto your elbows and then completely flat onto your mat.",
                    "Place one hand on your heart and one on your lower belly.",
                    "Relax your jaw, close your eyes, and sink deeper into the mat with every outgoing breath."
                )
            ),
            YogaPose(
                title = "Cat-Cow Flow (Chakravakasana)",
                description = "Gentle dynamic pelvic tilting. Helps stretch the front and back of the torso, releasing rigid spinal congestion.",
                duration = "3 - 5 Minutes",
                benefits = "Improves spinal coordination, relieves tight lower back fascia, regulates pelvic flow.",
                steps = listOf(
                    "Start on all fours with hands directly below shoulders and knees below hips.",
                    "Inhale to Cow: Drop your belly towards the mat, lift your chest, chin, and tailbone up.",
                    "Exhale to Cat: Draw your navel tightly to your spine, rounding your back upward, tucking chin."
                )
            ),
            YogaPose(
                title = "Cobra Pose (Bhujangasana)",
                description = "A gentle therapeutic backbend that stimulates organs in the abdomen and helps alleviate overall body fatigue.",
                duration = "2 - 3 Minutes",
                benefits = "Opens chest and shoulders, strengthens spinal columns, relieves pelvic compression.",
                steps = listOf(
                    "Lie flat on your belly with legs extended straight and tops of the feet resting on the floor.",
                    "Place your palms on the floor beside your ribs with elbows tucked close to your torso.",
                    "Inhale, press down with feet and lift your head, neck, and upper chest up without straightening arms completely."
                )
            )
        )
    }

    var selectedPose by remember { mutableStateOf<YogaPose?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Yoga & Pelvic Comfort",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = "Gentle yoga flow and stretching routines curated by medical insights to soothe menstrual spasms, reduce lower back strain, and encourage relaxation.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(poses) { pose ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPose = pose }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Lavender Lotus silhouette bullet
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = "Yoga pose",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pose.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pose.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Duration info",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = pose.duration,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Pop-up sheets
    if (selectedPose != null) {
        val active = selectedPose!!
        AlertDialog(
            onDismissRequest = { selectedPose = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = "Flower icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = active.title,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(
                        text = "Calming Benefits:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = active.benefits,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Step-By-step Guide:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        active.steps.forEachIndexed { i, step ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (i + 1).toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = step,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedPose = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CLOSE GUIDE")
                }
            }
        )
    }
}
