package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationPoint
import com.example.data.model.MapStyle
import com.example.data.model.TravelMode
import com.example.ui.NavScreen
import com.example.ui.NavViewModel
import com.example.ui.components.InteractiveMapView
import com.example.ui.components.MapViewController
import com.example.ui.i18n.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapScreen(
    viewModel: NavViewModel,
    mapController: MapViewController,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val strings = AppStrings.get(lang)
    val curLoc by viewModel.currentLocation.collectAsState()
    val origLoc by viewModel.originLocation.collectAsState()
    val destLoc by viewModel.destinationLocation.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val isCalculating by viewModel.isCalculatingRoute.collectAsState()
    val travelMode by viewModel.travelMode.collectAsState()
    val mapStyle by viewModel.mapStyle.collectAsState()
    val unreadNotifs by viewModel.unreadNotifCount.collectAsState()

    var showLayersMenu by remember { mutableStateOf(false) }
    var isRouteInputExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Interactive Map Engine
        InteractiveMapView(
            controller = mapController,
            currentLocation = curLoc,
            selectedRoute = selectedRoute,
            mapStyle = mapStyle,
            onMapClick = { lat, lng ->
                // Quick tap sets destination
                val point = LocationPoint(
                    name = "Selected Pin (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})",
                    address = "Lat: $lat, Lng: $lng",
                    latitude = lat,
                    longitude = lng
                )
                viewModel.setDestination(point)
            }
        )

        // 2. Top Header & Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Branding Icon & App Name
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00D2FF), Color(0xFF0066FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Search Input trigger
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { viewModel.navigateTo(NavScreen.SEARCH) }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = destLoc?.name ?: strings.searchPlaceholder,
                                    color = if (destLoc != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Route toggle button
                        IconButton(
                            onClick = { isRouteInputExpanded = !isRouteInputExpanded },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isRouteInputExpanded) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    CircleShape
                                )
                                .testTag("toggle_route_inputs")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = "Route Input",
                                tint = if (isRouteInputExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Notifications Icon with badge
                        IconButton(
                            onClick = { viewModel.navigateTo(NavScreen.NOTIFICATIONS) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge { Text("$unreadNotifs") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alerts",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Expandable Origin & Destination routing bar
                    AnimatedVisibility(
                        visible = isRouteInputExpanded,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            // Origin row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Origin",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = origLoc.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = { viewModel.useCurrentLocationAsOrigin() },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(strings.useMyLocation, fontSize = 11.sp)
                                }
                            }

                            // Destination row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Destination",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = destLoc?.name ?: strings.destinationPlaceholder,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.navigateTo(NavScreen.SEARCH) },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (destLoc != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                IconButton(
                                    onClick = { viewModel.swapOriginAndDestination() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = strings.swapLocations,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Travel Mode Selector (Driving, Walking, Cycling)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
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
                    }
                }
            }
        }

        // 3. Floating Right Map Action Controls (Layers, Re-center, Zoom In, Zoom Out)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Map Layers picker
            Box {
                MapControlFab(
                    icon = Icons.Default.Layers,
                    contentDescription = strings.mapLayers,
                    onClick = { showLayersMenu = true }
                )
                DropdownMenu(
                    expanded = showLayersMenu,
                    onDismissRequest = { showLayersMenu = false }
                ) {
                    MapStyle.values().forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.title) },
                            onClick = {
                                viewModel.setMapStyle(style)
                                mapController.setMapStyle(style)
                                showLayersMenu = false
                            }
                        )
                    }
                }
            }

            // Re-center on user GPS location
            MapControlFab(
                icon = Icons.Default.NearMe,
                contentDescription = strings.reCenter,
                onClick = {
                    mapController.centerMap(curLoc.latitude, curLoc.longitude, 15)
                }
            )

            // Zoom In
            MapControlFab(
                icon = Icons.Default.Add,
                contentDescription = "Zoom In",
                onClick = { mapController.zoomIn() }
            )

            // Zoom Out
            MapControlFab(
                icon = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                onClick = { mapController.zoomOut() }
            )
        }

        // 4. Quick Category Shortcuts Floating Bar (Sina AI, Favorites, Trip Planner, Search)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = if (selectedRoute != null) 148.dp else 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionPill(
                label = strings.sinaAiTitle,
                icon = Icons.Default.AutoAwesome,
                gradient = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))),
                onClick = { viewModel.navigateTo(NavScreen.AI_ASSISTANT) },
                modifier = Modifier.weight(1f)
            )

            QuickActionPill(
                label = strings.favoritesTab,
                icon = Icons.Default.Star,
                gradient = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                onClick = { viewModel.navigateTo(NavScreen.FAVORITES) },
                modifier = Modifier.weight(1f)
            )

            QuickActionPill(
                label = strings.routePlannerTab,
                icon = Icons.Default.Route,
                gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                onClick = { viewModel.navigateTo(NavScreen.ROUTE_PLANNER) },
                modifier = Modifier.weight(1f)
            )
        }

        // 5. Active Route Calculation Summary Card (Bottom Sheet Card)
        AnimatedVisibility(
            visible = selectedRoute != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedRoute?.let { route ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = route.destination.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${strings.distance}: ${route.distanceKm} km • ${strings.estimatedTime}: ${route.durationMinutes} min",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Dismiss route button
                            IconButton(
                                onClick = {
                                    viewModel.setDestination(LocationPoint("", "", 0.0, 0.0))
                                    mapController.setRoute(null)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Route",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large prominent "Start Navigation" Button
                        Button(
                            onClick = { viewModel.startNavigation() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_navigation_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7)
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.startNavigation,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 6. Loading Indicator when calculating route
        if (isCalculating) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = strings.calculateRoute, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun MapControlFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun TravelModeChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 6.dp,
        modifier = modifier.height(42.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
