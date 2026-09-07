package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MapViewController
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.ActiveNavigationScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.MainMapScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecentDestinationsScreen
import com.example.ui.screens.RoutePlannerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SinaAiAssistantScreen

private val SinaDarkColors = darkColorScheme(
    primary = Color(0xFF00D2FF),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF004D73),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFF070D1E),
    surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val SinaLightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF6366F1),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun SinaNavigatorApp(
    viewModel: NavViewModel = viewModel()
) {
    val context = LocalContext.current
    val mapController = remember { MapViewController(context) }

    val lang by viewModel.currentLanguage.collectAsState()
    val strings = AppStrings.get(lang)
    val themePref by viewModel.themeMode.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val bannerMsg by viewModel.bannerMessage.collectAsState()

    val isDark = when (themePref) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    // Permission launcher for Location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.showBanner("GPS Location enabled!")
        }
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineLocationGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Wrap in Material 3 theme and dynamic LayoutDirection (RTL for Persian/Arabic, LTR for English/Chinese/Russian)
    MaterialTheme(
        colorScheme = if (isDark) SinaDarkColors else SinaLightColors
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides lang.layoutDirection) {
            val hideBottomBar = currentScreen == NavScreen.NAVIGATION_HUD || currentScreen == NavScreen.AUTH

            Scaffold(
                bottomBar = {
                    if (!hideBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == NavScreen.MAP,
                                onClick = { viewModel.navigateTo(NavScreen.MAP) },
                                icon = { Icon(Icons.Default.Map, contentDescription = strings.mapTab) },
                                label = { Text(strings.mapTab, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavScreen.SEARCH,
                                onClick = { viewModel.navigateTo(NavScreen.SEARCH) },
                                icon = { Icon(Icons.Default.Search, contentDescription = strings.searchTab) },
                                label = { Text(strings.searchTab, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavScreen.ROUTE_PLANNER,
                                onClick = { viewModel.navigateTo(NavScreen.ROUTE_PLANNER) },
                                icon = { Icon(Icons.Default.Route, contentDescription = strings.routePlannerTab) },
                                label = { Text(strings.routePlannerTab, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavScreen.FAVORITES,
                                onClick = { viewModel.navigateTo(NavScreen.FAVORITES) },
                                icon = { Icon(Icons.Default.Star, contentDescription = strings.favoritesTab) },
                                label = { Text(strings.favoritesTab, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavScreen.AI_ASSISTANT,
                                onClick = { viewModel.navigateTo(NavScreen.AI_ASSISTANT) },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = strings.aiTab, tint = Color(0xFF8B5CF6)) },
                                label = { Text(strings.aiTab, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6)) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavScreen.RECENT_DESTINATIONS,
                                onClick = { viewModel.navigateTo(NavScreen.RECENT_DESTINATIONS) },
                                icon = { Icon(Icons.Default.History, contentDescription = strings.historyTab) },
                                label = { Text(strings.historyTab, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavScreen.PROFILE,
                                onClick = { viewModel.navigateTo(NavScreen.PROFILE) },
                                icon = { Icon(Icons.Default.Person, contentDescription = strings.profileTab) },
                                label = { Text(strings.profileTab, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        NavScreen.MAP -> MainMapScreen(viewModel = viewModel, mapController = mapController)
                        NavScreen.SEARCH -> SearchScreen(viewModel = viewModel)
                        NavScreen.ROUTE_PLANNER -> RoutePlannerScreen(viewModel = viewModel)
                        NavScreen.NAVIGATION_HUD -> ActiveNavigationScreen(viewModel = viewModel, mapController = mapController)
                        NavScreen.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                        NavScreen.RECENT_DESTINATIONS -> RecentDestinationsScreen(viewModel = viewModel)
                        NavScreen.AI_ASSISTANT -> SinaAiAssistantScreen(viewModel = viewModel)
                        NavScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                        NavScreen.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                        NavScreen.AUTH -> AuthScreen(viewModel = viewModel)
                        NavScreen.ADMIN_DASHBOARD -> AdminDashboardScreen(viewModel = viewModel)
                        else -> MainMapScreen(viewModel = viewModel, mapController = mapController)
                    }

                    // Floating Notification Banner
                    AnimatedVisibility(
                        visible = bannerMsg != null,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        bannerMsg?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.inverseSurface,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.inversePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = msg,
                                        color = MaterialTheme.colorScheme.inverseOnSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
