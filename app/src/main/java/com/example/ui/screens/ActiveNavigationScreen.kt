package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveNavStatus
import com.example.data.model.LocationPoint
import com.example.data.model.MapStyle
import com.example.ui.NavViewModel
import com.example.ui.components.InteractiveMapView
import com.example.ui.components.MapViewController
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActiveNavigationScreen(
    viewModel: NavViewModel,
    mapController: MapViewController,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val strings = AppStrings.get(lang)
    val navStatus by viewModel.activeNavStatus.collectAsState()
    val mapStyle by viewModel.mapStyle.collectAsState()

    var showItineraryList by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    when (val state = navStatus) {
        is ActiveNavStatus.Navigating -> {
            val route = state.route
            val steps = route.steps
            val currentStep = if (state.currentStepIndex < steps.size) steps[state.currentStepIndex] else null
            val nextInstruction = currentStep?.instruction ?: "Proceed along route"
            val distToTurn = currentStep?.distanceMeters ?: 300.0

            val remainingKm = (state.remainingDistanceMeters / 100.0).toInt() / 10.0
            val remainingMin = Math.max(1, (state.remainingSeconds / 60.0).toInt())

            // ETA Calculation
            val arrivalTimeMs = System.currentTimeMillis() + (state.remainingSeconds * 1000).toLong()
            val etaStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(arrivalTimeMs))

            Box(modifier = modifier.fillMaxSize()) {
                // Background Map following vehicle
                InteractiveMapView(
                    controller = mapController,
                    currentLocation = LocationPoint("Current Vehicle", "", state.currentLat, state.currentLng),
                    selectedRoute = route,
                    mapStyle = mapStyle,
                    onMapClick = { _, _ -> }
                )

                // 1. Top High-Contrast Turn Maneuver Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .shadow(16.dp, RoundedCornerShape(20.dp))
                        .border(2.dp, Color(0xFF00D2FF), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A) // High contrast slate dark
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Maneuver Icon
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getManeuverIcon(currentStep?.modifier ?: ""),
                                contentDescription = "Maneuver",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "In ${formatDistance(distToTurn)}",
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = nextInstruction,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2
                            )
                        }

                        // Mute/Unmute toggle
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Sound",
                                tint = if (isMuted) Color.Gray else Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                // 2. Floating Speedometer & Speed Limit (Left center)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Current Speed Gauge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981)),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "${state.currentSpeedKmH}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "km/h",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Speed limit badge
                    if (state.speedLimitKmH > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(3.dp, Color.Red),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${state.speedLimitKmH}",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // 3. Floating Right Control Buttons (Re-center & Steps list)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Re-center on vehicle
                    MapControlFab(
                        icon = Icons.Default.NearMe,
                        contentDescription = "Recenter",
                        onClick = {
                            mapController.centerMap(state.currentLat, state.currentLng, 16)
                        }
                    )

                    // Steps List Sheet Toggle
                    MapControlFab(
                        icon = Icons.Default.FormatListBulleted,
                        contentDescription = "Turn Steps",
                        onClick = { showItineraryList = !showItineraryList }
                    )
                }

                // 4. Bottom HUD Card (Remaining km, time, ETA, and Exit Navigation)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .shadow(20.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color(0xFF0B132B)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$remainingMin min",
                                    color = Color(0xFF10B981),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "$remainingKm km • ETA $etaStr",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Exit Navigation Button
                            Button(
                                onClick = { viewModel.stopNavigation() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("exit_navigation_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.exitNavigation, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 5. Drawer for Turn-by-Turn Maneuver List
                AnimatedVisibility(
                    visible = showItineraryList,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 16.dp,
                        modifier = Modifier.height(350.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Upcoming Turns", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                IconButton(onClick = { showItineraryList = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                            LazyColumn {
                                items(steps) { step ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = getManeuverIcon(step.modifier),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(step.instruction, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(formatDistance(step.distanceMeters), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        is ActiveNavStatus.Finished -> {
            // Arrival Screen
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NearMe, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("You Have Arrived!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.route.destination.name,
                            color = Color(0xFF38BDF8),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.stopNavigation() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No navigation in progress.")
            }
        }
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        "${(meters / 100).toInt() / 10.0} km"
    } else {
        "${meters.toInt()} m"
    }
}

private fun getManeuverIcon(modifier: String): ImageVector {
    val m = modifier.lowercase()
    return when {
        m.contains("slight_left") -> Icons.Default.TurnSlightLeft
        m.contains("slight_right") -> Icons.Default.TurnSlightRight
        m.contains("sharp_left") -> Icons.Default.TurnSharpLeft
        m.contains("sharp_right") -> Icons.Default.TurnSharpRight
        m.contains("left") -> Icons.Default.TurnLeft
        m.contains("right") -> Icons.Default.TurnRight
        m.contains("u_turn") -> Icons.Default.TurnLeft
        else -> Icons.Default.VerticalAlignTop
    }
}
