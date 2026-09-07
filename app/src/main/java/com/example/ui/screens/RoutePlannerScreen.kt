package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SavedTripEntity
import com.example.data.model.LocationPoint
import com.example.data.model.TravelMode
import com.example.ui.NavScreen
import com.example.ui.NavViewModel
import com.example.ui.i18n.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerScreen(
    viewModel: NavViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val strings = AppStrings.get(lang)
    val origLoc by viewModel.originLocation.collectAsState()
    val destLoc by viewModel.destinationLocation.collectAsState()
    val stops by viewModel.tripStops.collectAsState()
    val travelMode by viewModel.travelMode.collectAsState()
    val savedTrips by viewModel.savedTrips.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddStopDialog by remember { mutableStateOf(false) }
    var showSaveTripDialog by remember { mutableStateOf(false) }

    var stopNameInput by remember { mutableStateOf("") }
    var stopAddressInput by remember { mutableStateOf("") }
    var tripTitleInput by remember { mutableStateOf("") }
    var tripDescInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(NavScreen.MAP) },
                        modifier = Modifier.testTag("route_planner_back")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.routePlannerTab,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(strings.tripItinerary) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("${strings.myTrips} (${savedTrips.size})") }
                    )
                }
            }
        }

        if (selectedTab == 0) {
            // Tab 0: Multi-Stop Itinerary Planner
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Transport Mode Selector
                item {
                    Text(
                        text = "Travel Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TravelModeChip(
                            title = strings.modeDriving,
                            icon = Icons.Default.DirectionsCar,
                            isSelected = travelMode == TravelMode.DRIVING,
                            onClick = { viewModel.setTravelMode(TravelMode.DRIVING) }
                        )
                        TravelModeChip(
                            title = strings.modeWalking,
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            isSelected = travelMode == TravelMode.WALKING,
                            onClick = { viewModel.setTravelMode(TravelMode.WALKING) }
                        )
                        TravelModeChip(
                            title = strings.modeCycling,
                            icon = Icons.AutoMirrored.Filled.DirectionsBike,
                            isSelected = travelMode == TravelMode.CYCLING,
                            onClick = { viewModel.setTravelMode(TravelMode.CYCLING) }
                        )
                    }
                }

                // Origin
                item {
                    ItineraryWaypointCard(
                        title = origLoc.name,
                        subtitle = origLoc.address.ifBlank { "Starting Location" },
                        badge = "Start",
                        badgeColor = Color(0xFF10B981),
                        canDelete = false,
                        onDelete = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Dynamic Stops
                itemsIndexed(stops) { index, stop ->
                    ItineraryWaypointCard(
                        title = stop.name,
                        subtitle = stop.address.ifBlank { "Waypoint Stop ${index + 1}" },
                        badge = "Stop ${index + 1}",
                        badgeColor = Color(0xFFF59E0B),
                        canDelete = true,
                        onDelete = { viewModel.removeTripStop(stop.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add Stop Button
                item {
                    Button(
                        onClick = { showAddStopDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.addStop, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Destination
                item {
                    destLoc?.let { dest ->
                        ItineraryWaypointCard(
                            title = dest.name,
                            subtitle = dest.address.ifBlank { "Final Destination" },
                            badge = "End",
                            badgeColor = Color(0xFFEF4444),
                            canDelete = false,
                            onDelete = {}
                        )
                    } ?: run {
                        Surface(
                            onClick = { viewModel.navigateTo(NavScreen.SEARCH) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Tap to select Destination", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Actions: Compute & Save Trip
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (destLoc != null) {
                                    viewModel.computeRoute()
                                    viewModel.navigateTo(NavScreen.MAP)
                                } else {
                                    viewModel.showBanner("Please set a destination first.")
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.calculateRoute, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showSaveTripDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.saveTrip, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Tab 1: Saved Trips
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (savedTrips.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No saved trips yet. Plan an itinerary and tap 'Save Trip'!")
                        }
                    }
                } else {
                    items(savedTrips) { trip ->
                        SavedTripCard(
                            trip = trip,
                            onLoad = {
                                viewModel.loadTripToMap(trip)
                            },
                            onDelete = {
                                viewModel.deleteTrip(trip)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // Add Stop Dialog
    if (showAddStopDialog) {
        AlertDialog(
            onDismissRequest = { showAddStopDialog = false },
            title = { Text(strings.addStop) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stopNameInput,
                        onValueChange = { stopNameInput = it },
                        label = { Text("Stop / Place Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stopAddressInput,
                        onValueChange = { stopAddressInput = it },
                        label = { Text("Address / City") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (stopNameInput.isNotBlank()) {
                            // Use slight offset from current location
                            val offsetLat = origLoc.latitude + (stops.size + 1) * 0.008
                            val offsetLng = origLoc.longitude - (stops.size + 1) * 0.006
                            viewModel.addTripStop(stopNameInput, stopAddressInput, offsetLat, offsetLng)
                            stopNameInput = ""
                            stopAddressInput = ""
                            showAddStopDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStopDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Save Trip Dialog
    if (showSaveTripDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTripDialog = false },
            title = { Text(strings.saveTrip) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tripTitleInput,
                        onValueChange = { tripTitleInput = it },
                        label = { Text("Trip Title") },
                        placeholder = { Text("e.g. Weekend Coastal Tour") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tripDescInput,
                        onValueChange = { tripDescInput = it },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("Notes about restaurants, views...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tripTitleInput.isNotBlank()) {
                            viewModel.saveCurrentTrip(tripTitleInput, tripDescInput)
                            tripTitleInput = ""
                            tripDescInput = ""
                            showSaveTripDialog = false
                            selectedTab = 1
                        }
                    }
                ) {
                    Text(strings.saveTrip)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTripDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ItineraryWaypointCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(badge.take(2), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
            }

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SavedTripCard(
    trip: SavedTripEntity,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(trip.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }

            if (trip.description.isNotBlank()) {
                Text(trip.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
            }

            Text(
                text = "${trip.travelMode} • ${trip.totalDistanceKm} km • ~${trip.totalDurationMin} min",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Button(
                onClick = onLoad,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Navigate This Itinerary", fontSize = 13.sp)
            }
        }
    }
}
