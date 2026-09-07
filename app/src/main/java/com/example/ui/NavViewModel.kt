package com.example.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FavoritePlaceEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.RecentDestinationEntity
import com.example.data.local.entity.RouteHistoryEntity
import com.example.data.local.entity.SavedTripEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.ActiveNavStatus
import com.example.data.model.AiChatMessage
import com.example.data.model.LocationPoint
import com.example.data.model.MapStyle
import com.example.data.model.RouteResult
import com.example.data.model.SearchResult
import com.example.data.model.TravelMode
import com.example.data.model.TripStop
import com.example.data.repository.NavRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class NavScreen {
    MAP,
    SEARCH,
    ROUTE_PLANNER,
    NAVIGATION_HUD,
    FAVORITES,
    RECENT_DESTINATIONS,
    TRIPS,
    AI_ASSISTANT,
    PROFILE,
    SETTINGS,
    NOTIFICATIONS,
    AUTH,
    ADMIN_DASHBOARD
}

class NavViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = NavRepository(database)

    // Current User
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Navigation Screen Router
    private val _currentScreen = MutableStateFlow(NavScreen.MAP)
    val currentScreen: StateFlow<NavScreen> = _currentScreen.asStateFlow()

    // Language & Theme
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Map & Routing State
    private val defaultLoc = LocationPoint("San Francisco Hub", "Market Street", 37.774929, -122.419416)
    private val _currentLocation = MutableStateFlow(defaultLoc)
    val currentLocation: StateFlow<LocationPoint> = _currentLocation.asStateFlow()

    private val _originLocation = MutableStateFlow(defaultLoc)
    val originLocation: StateFlow<LocationPoint> = _originLocation.asStateFlow()

    private val _destinationLocation = MutableStateFlow<LocationPoint?>(null)
    val destinationLocation: StateFlow<LocationPoint?> = _destinationLocation.asStateFlow()

    private val _selectedRoute = MutableStateFlow<RouteResult?>(null)
    val selectedRoute: StateFlow<RouteResult?> = _selectedRoute.asStateFlow()

    private val _isCalculatingRoute = MutableStateFlow(false)
    val isCalculatingRoute: StateFlow<Boolean> = _isCalculatingRoute.asStateFlow()

    private val _travelMode = MutableStateFlow(TravelMode.DRIVING)
    val travelMode: StateFlow<TravelMode> = _travelMode.asStateFlow()

    private val _mapStyle = MutableStateFlow(MapStyle.CARTO_DARK)
    val mapStyle: StateFlow<MapStyle> = _mapStyle.asStateFlow()

    // Active Navigation HUD simulation
    private val _activeNavStatus = MutableStateFlow<ActiveNavStatus>(ActiveNavStatus.Idle)
    val activeNavStatus: StateFlow<ActiveNavStatus> = _activeNavStatus.asStateFlow()

    private var navigationSimulationJob: Job? = null
    private var textToSpeech: TextToSpeech? = null

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Trip Planner Stops
    private val _tripStops = MutableStateFlow<List<TripStop>>(emptyList())
    val tripStops: StateFlow<List<TripStop>> = _tripStops.asStateFlow()

    // AI Chat
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                text = "Hello! I am Sina AI, your navigation and travel copilot. How can I assist your journey today? You can ask me to find restaurants, plan multi-stop road trips, or optimize your driving route.",
                isUser = false
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // UI Feedback Banner
    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    // Flows from DB
    val favorites = repository.getFavorites(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDestinations = repository.getRecentDestinations(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routeHistory = repository.getRouteHistory(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTrips = repository.getTrips(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications = repository.getNotifications(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifCount = repository.getUnreadCount(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allUsers = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs = repository.getAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUsersCount = repository.getUserCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val totalRoutesCount = repository.getTotalRoutesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val totalFavoritesCount = repository.getTotalFavoritesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val totalTripsCount = repository.getTotalTripsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    init {
        // Initialize default user
        viewModelScope.launch {
            val user = repository.getUserById(2)
            if (user != null) {
                _currentUser.value = user
                _currentLanguage.value = AppStrings.fromCode(user.preferredLanguage)
                _themeMode.value = user.themePreference
            }
        }
        initTts(application)
    }

    private fun initTts(context: Context) {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                }
            }
        } catch (e: Exception) {
            // TTS optional
        }
    }

    fun speakGuidance(text: String) {
        if (_currentUser.value?.voiceGuidance == true) {
            try {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SINA_NAV_GUIDE")
            } catch (e: Exception) {}
        }
    }

    fun navigateTo(screen: NavScreen) {
        _currentScreen.value = screen
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val updated = user.copy(preferredLanguage = lang.code)
                _currentUser.value = updated
                repository.updateUser(updated)
            }
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        if (mode == "DARK") {
            _mapStyle.value = MapStyle.CARTO_DARK
        } else if (mode == "LIGHT") {
            _mapStyle.value = MapStyle.CARTO_POSITRON
        }
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val updated = user.copy(themePreference = mode)
                _currentUser.value = updated
                repository.updateUser(updated)
            }
        }
    }

    fun setMapStyle(style: MapStyle) {
        _mapStyle.value = style
    }

    fun setTravelMode(mode: TravelMode) {
        _travelMode.value = mode
        if (_destinationLocation.value != null) {
            computeRoute()
        }
    }

    fun useCurrentLocationAsOrigin() {
        _originLocation.value = _currentLocation.value
        if (_destinationLocation.value != null) {
            computeRoute()
        }
    }

    fun swapOriginAndDestination() {
        val dest = _destinationLocation.value ?: return
        val orig = _originLocation.value
        _originLocation.value = dest
        _destinationLocation.value = orig
        computeRoute()
    }

    fun setOrigin(loc: LocationPoint) {
        _originLocation.value = loc
        if (_destinationLocation.value != null) {
            computeRoute()
        }
    }

    fun setDestination(loc: LocationPoint) {
        _destinationLocation.value = loc
        computeRoute()
        // Save to recent destinations
        viewModelScope.launch {
            val userId = _currentUser.value?.id ?: 2
            repository.addRecentDestination(
                RecentDestinationEntity(
                    userId = userId,
                    name = loc.name,
                    address = loc.address,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    travelMode = _travelMode.value.name
                )
            )
        }
    }

    fun computeRoute() {
        val orig = _originLocation.value
        val dest = _destinationLocation.value ?: return

        viewModelScope.launch {
            _isCalculatingRoute.value = true
            try {
                val result = repository.calculateRoute(orig, dest, _travelMode.value)
                _selectedRoute.value = result

                // Record route history
                val userId = _currentUser.value?.id ?: 2
                repository.recordRouteHistory(
                    RouteHistoryEntity(
                        userId = userId,
                        originName = orig.name,
                        originLat = orig.latitude,
                        originLng = orig.longitude,
                        destinationName = dest.name,
                        destinationLat = dest.latitude,
                        destinationLng = dest.longitude,
                        distanceKm = result.distanceKm,
                        durationMin = result.durationMinutes,
                        travelMode = _travelMode.value.name
                    )
                )
            } catch (e: Exception) {
                showBanner("Error computing route: ${e.message}")
            } finally {
                _isCalculatingRoute.value = false
            }
        }
    }

    fun startNavigation() {
        val route = _selectedRoute.value ?: return
        _activeNavStatus.value = ActiveNavStatus.Navigating(
            route = route,
            currentStepIndex = 0,
            currentSpeedKmH = if (_travelMode.value == TravelMode.WALKING) 5 else if (_travelMode.value == TravelMode.CYCLING) 18 else 48,
            speedLimitKmH = if (_travelMode.value == TravelMode.DRIVING) 50 else 0,
            remainingDistanceMeters = route.distanceMeters,
            remainingSeconds = route.durationSeconds,
            currentLat = route.origin.latitude,
            currentLng = route.origin.longitude
        )
        _currentScreen.value = NavScreen.NAVIGATION_HUD

        val firstStep = route.steps.firstOrNull()?.instruction ?: "Starting journey towards ${route.destination.name}"
        speakGuidance(firstStep)

        // Start real-time simulation along route polyline
        startNavigationSimulation(route)
    }

    private fun startNavigationSimulation(route: RouteResult) {
        navigationSimulationJob?.cancel()
        navigationSimulationJob = viewModelScope.launch {
            val points = route.polylinePoints
            if (points.isEmpty()) return@launch

            var stepIdx = 0
            val totalPoints = points.size
            for (i in 0 until totalPoints) {
                delay(2000) // update every 2s
                val currentPoint = points[i]
                val fractionRemaining = 1.0 - (i.toDouble() / totalPoints.toDouble())
                val remainingDist = route.distanceMeters * fractionRemaining
                val remainingTime = route.durationSeconds * fractionRemaining

                if (i % 5 == 0 && stepIdx < route.steps.size - 1) {
                    stepIdx++
                    speakGuidance(route.steps[stepIdx].instruction)
                }

                _activeNavStatus.value = ActiveNavStatus.Navigating(
                    route = route,
                    currentStepIndex = stepIdx,
                    currentSpeedKmH = if (_travelMode.value == TravelMode.WALKING) 5 else 46 + (i % 6),
                    speedLimitKmH = 50,
                    remainingDistanceMeters = remainingDist,
                    remainingSeconds = remainingTime,
                    currentLat = currentPoint.first,
                    currentLng = currentPoint.second
                )
            }

            speakGuidance("You have arrived at your destination: ${route.destination.name}")
            _activeNavStatus.value = ActiveNavStatus.Finished(route)
        }
    }

    fun stopNavigation() {
        navigationSimulationJob?.cancel()
        _activeNavStatus.value = ActiveNavStatus.Idle
        _currentScreen.value = NavScreen.MAP
    }

    fun performSearch(query: String, category: String = "") {
        _searchQuery.value = query
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val fullQuery = if (category.isNotBlank() && !query.contains(category)) "$category $query" else query
                val results = repository.searchPlaces(
                    fullQuery,
                    _currentLocation.value.latitude,
                    _currentLocation.value.longitude
                )
                _searchResults.value = results
            } catch (e: Exception) {
                showBanner("Search failed: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Favorites
    fun saveFavorite(name: String, address: String, category: String, lat: Double, lng: Double, notes: String = "") {
        viewModelScope.launch {
            val userId = _currentUser.value?.id ?: 2
            repository.addFavorite(
                FavoritePlaceEntity(
                    userId = userId,
                    name = name,
                    address = address,
                    category = category,
                    latitude = lat,
                    longitude = lng,
                    notes = notes
                )
            )
            showBanner("Place saved to Favorites!")
        }
    }

    fun deleteFavorite(fav: FavoritePlaceEntity) {
        viewModelScope.launch {
            repository.deleteFavorite(fav.id, fav.userId)
        }
    }

    fun updateFavorite(fav: FavoritePlaceEntity) {
        viewModelScope.launch {
            repository.updateFavorite(fav)
            showBanner("Place updated!")
        }
    }

    fun clearRecentDestinations() {
        viewModelScope.launch {
            val userId = _currentUser.value?.id ?: 2
            repository.clearRecents(userId)
        }
    }

    fun deleteRecentDestination(id: Long) {
        viewModelScope.launch {
            val userId = _currentUser.value?.id ?: 2
            repository.deleteRecent(id, userId)
        }
    }

    // Trip Planner
    fun addTripStop(name: String, address: String, lat: Double, lng: Double) {
        val current = _tripStops.value.toMutableList()
        current.add(
            TripStop(
                name = name,
                address = address,
                latitude = lat,
                longitude = lng,
                order = current.size + 1
            )
        )
        _tripStops.value = current
    }

    fun removeTripStop(stopId: String) {
        _tripStops.value = _tripStops.value.filter { it.id != stopId }
    }

    fun saveCurrentTrip(title: String, description: String) {
        viewModelScope.launch {
            val stops = _tripStops.value
            if (stops.isEmpty()) {
                showBanner("Please add at least one stop to your trip.")
                return@launch
            }
            val stopsArray = JSONArray()
            stops.forEach {
                val o = JSONObject()
                o.put("name", it.name)
                o.put("address", it.address)
                o.put("lat", it.latitude)
                o.put("lng", it.longitude)
                o.put("order", it.order)
                stopsArray.put(o)
            }
            val userId = _currentUser.value?.id ?: 2
            repository.saveTrip(
                SavedTripEntity(
                    userId = userId,
                    title = title,
                    description = description,
                    travelMode = _travelMode.value.name,
                    stopsJson = stopsArray.toString(),
                    totalDistanceKm = (stops.size * 4.5),
                    totalDurationMin = (stops.size * 12)
                )
            )
            showBanner("Trip '$title' saved successfully!")
            _tripStops.value = emptyList()
        }
    }

    fun deleteTrip(trip: SavedTripEntity) {
        viewModelScope.launch {
            repository.deleteTrip(trip.id, trip.userId)
        }
    }

    fun loadTripToMap(trip: SavedTripEntity) {
        try {
            val arr = JSONArray(trip.stopsJson)
            if (arr.length() > 0) {
                val first = arr.getJSONObject(0)
                val last = arr.getJSONObject(arr.length() - 1)
                _originLocation.value = LocationPoint(first.getString("name"), first.optString("address", ""), first.getDouble("lat"), first.getDouble("lng"))
                _destinationLocation.value = LocationPoint(last.getString("name"), last.optString("address", ""), last.getDouble("lat"), last.getDouble("lng"))
                _travelMode.value = try { TravelMode.valueOf(trip.travelMode) } catch(e: Exception) { TravelMode.DRIVING }
                computeRoute()
                _currentScreen.value = NavScreen.MAP
            }
        } catch (e: Exception) {
            showBanner("Failed to load trip: ${e.message}")
        }
    }

    // AI Chat
    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = AiChatMessage(text = prompt, isUser = true)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            try {
                val reply = repository.askSinaAi(prompt, _currentLocation.value, _destinationLocation.value)
                val aiMsg = AiChatMessage(text = reply, isUser = false)
                _aiMessages.value = _aiMessages.value + aiMsg
            } catch (e: Exception) {
                _aiMessages.value = _aiMessages.value + AiChatMessage(
                    text = "Sina AI encountered an error: ${e.message}",
                    isUser = false,
                    isError = true
                )
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    // Notifications
    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            val userId = _currentUser.value?.id ?: 2
            repository.markAllNotificationsRead(userId)
        }
    }

    // Auth & Profile
    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.login(email, pass)
            res.onSuccess { user ->
                _currentUser.value = user
                _currentLanguage.value = AppStrings.fromCode(user.preferredLanguage)
                _themeMode.value = user.themePreference
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Authentication failed")
            }
        }
    }

    fun register(username: String, email: String, pass: String, fullName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.registerUser(username, email, pass, fullName)
            res.onSuccess { user ->
                _currentUser.value = user
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Fallback back to demo user
            val demo = repository.getUserById(2)
            _currentUser.value = demo
            _currentScreen.value = NavScreen.MAP
            showBanner("Logged out successfully.")
        }
    }

    fun updateProfile(fullName: String, email: String, voice: Boolean, tolls: Boolean, highways: Boolean) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val updated = user.copy(
                    fullName = fullName,
                    email = email,
                    voiceGuidance = voice,
                    avoidTolls = tolls,
                    avoidHighways = highways,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateUser(updated)
                _currentUser.value = updated
                showBanner("Profile settings updated!")
            }
        }
    }

    fun switchAccountToAdmin() {
        viewModelScope.launch {
            val admin = repository.getUserById(1)
            if (admin != null) {
                _currentUser.value = admin
                _currentScreen.value = NavScreen.ADMIN_DASHBOARD
                showBanner("Switched to Super Admin (Sina Naderi)")
            }
        }
    }

    // Admin Actions
    fun toggleUserActiveStatus(user: UserEntity) {
        viewModelScope.launch {
            val updated = user.copy(isActive = !user.isActive)
            repository.updateUser(updated)
            repository.logAudit(
                _currentUser.value?.id ?: 1,
                _currentUser.value?.email ?: "admin",
                "USER_STATUS_TOGGLE",
                "User ${user.email} status toggled to ${if (updated.isActive) "ACTIVE" else "DEACTIVATED"}"
            )
        }
    }

    fun deleteUserByAdmin(userId: Long) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            repository.logAudit(
                _currentUser.value?.id ?: 1,
                _currentUser.value?.email ?: "admin",
                "USER_DELETED",
                "User ID $userId deleted by admin"
            )
        }
    }

    fun broadcastNotification(title: String, message: String) {
        viewModelScope.launch {
            val all = database.userDao().getUserById(2)
            repository.sendNotification(
                NotificationEntity(
                    userId = 2,
                    title = title,
                    message = message,
                    type = "SYSTEM"
                )
            )
            repository.logAudit(
                _currentUser.value?.id ?: 1,
                _currentUser.value?.email ?: "admin",
                "NOTIFICATION_BROADCAST",
                "Broadcast: $title"
            )
            showBanner("Notification broadcast successfully dispatched!")
        }
    }

    fun showBanner(msg: String) {
        _bannerMessage.value = msg
        viewModelScope.launch {
            delay(3500)
            if (_bannerMessage.value == msg) {
                _bannerMessage.value = null
            }
        }
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        navigationSimulationJob?.cancel()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
