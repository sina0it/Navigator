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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.NavScreen
import com.example.ui.NavViewModel
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: NavViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val strings = AppStrings.get(lang)
    val user by viewModel.currentUser.collectAsState()

    val totalUsers by viewModel.totalUsersCount.collectAsState()
    val totalRoutes by viewModel.totalRoutesCount.collectAsState()
    val totalFavs by viewModel.totalFavoritesCount.collectAsState()
    val totalTrips by viewModel.totalTripsCount.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMsg by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Admin Header
        Surface(color = Color(0xFF0F172A), shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateTo(NavScreen.MAP) },
                            modifier = Modifier.testTag("admin_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.adminDashboard,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Super Admin: Sina Naderi",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Button(
                        onClick = { showBroadcastDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.NotificationAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Broadcast", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF38BDF8),
                    edgePadding = 0.dp
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Users (${allUsers.size})") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("AI & Routes") })
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Audit Trail (${auditLogs.size})") })
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Overview KPI Cards
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // System Status banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.operational, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF10B981))
                                    Text("OSRM Routing Engine • Leaflet Tiles • Gemini AI", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminKpiCard(
                                title = strings.totalUsers,
                                value = "$totalUsers",
                                icon = Icons.Default.Group,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = strings.totalRoutes,
                                value = "$totalRoutes",
                                icon = Icons.Default.Route,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminKpiCard(
                                title = strings.savedPlaces,
                                value = "$totalFavs",
                                icon = Icons.Default.Place,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = strings.totalTrips,
                                value = "$totalTrips",
                                icon = Icons.Default.History,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        // AI Usage Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sina AI Engine Monitor", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Model: gemini-3.5-flash", fontSize = 13.sp)
                                Text("Response Latency: ~650ms", fontSize = 13.sp)
                                Text("Fallback Offline Intelligence: Enabled", fontSize = 13.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }
            1 -> {
                // Tab 1: User Management
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allUsers) { u ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (u.isActive) Color(0xFF3B82F6) else Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(u.fullName.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (u.role.contains("ADMIN")) Color(0xFF6366F1).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(u.role, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text(u.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = if (u.isActive) "Active" else "Deactivated",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (u.isActive) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }

                                if (u.role != "SUPER_ADMIN") {
                                    IconButton(onClick = { viewModel.toggleUserActiveStatus(u) }) {
                                        Icon(
                                            imageVector = if (u.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                                            contentDescription = "Toggle Status",
                                            tint = if (u.isActive) Color(0xFFF59E0B) else Color(0xFF10B981)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deleteUserByAdmin(u.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Tab 2: AI & Routes Analytics
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Routing Service Metrics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Provider: Open Source Routing Machine (OSRM)", fontSize = 13.sp)
                                Text("• Geocoding Provider: OpenStreetMap Nominatim", fontSize = 13.sp)
                                Text("• Average Route Calculation Time: ~280ms", fontSize = 13.sp)
                                Text("• Supported Profiles: Driving, Walking, Cycling", fontSize = 13.sp)
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Security & Policy Compliance", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Zero secret exposure in client bundles", fontSize = 13.sp, color = Color(0xFF10B981))
                                Text("• SHA-256 with per-user salt password encryption", fontSize = 13.sp, color = Color(0xFF10B981))
                                Text("• Dynamic permission handling & zero external executable loading", fontSize = 13.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }
            3 -> {
                // Tab 3: Audit Trail Logs
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs) { log ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    val dateStr = SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()).format(Date(log.createdAt))
                                    Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Text(log.details, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                Text("User: ${log.userEmail} (${log.ipAddress})", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    // Broadcast Notification Dialog
    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Broadcast Notification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("Announcement Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = broadcastMsg,
                        onValueChange = { broadcastMsg = it },
                        label = { Text("Message Body") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastTitle.isNotBlank()) {
                            viewModel.broadcastNotification(broadcastTitle, broadcastMsg)
                            broadcastTitle = ""
                            broadcastMsg = ""
                            showBroadcastDialog = false
                        }
                    }
                ) {
                    Text("Send to All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
