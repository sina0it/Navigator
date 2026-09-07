package com.example.data.model

import com.squareup.moshi.JsonClass

enum class TravelMode(val apiProfile: String, val label: String) {
    DRIVING("driving", "Driving"),
    WALKING("walking", "Walking"),
    CYCLING("cycling", "Cycling")
}

data class LocationPoint(
    val name: String,
    val address: String = "",
    val latitude: Double,
    val longitude: Double
)

data class ManeuverStep(
    val instruction: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val modifier: String = "",
    val type: String = "turn",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class RouteResult(
    val origin: LocationPoint,
    val destination: LocationPoint,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val polylinePoints: List<Pair<Double, Double>>, // (lat, lng) pairs
    val steps: List<ManeuverStep>,
    val travelMode: TravelMode
) {
    val distanceKm: Double get() = (distanceMeters / 100.0).toInt() / 10.0
    val durationMinutes: Int get() = Math.max(1, (durationSeconds / 60.0).toInt())
}

data class SearchResult(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String = "general",
    val distanceKm: Double? = null
)

data class TripStop(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedPlace: SearchResult? = null,
    val isError: Boolean = false
)

enum class MapStyle(val title: String, val tileUrl: String) {
    CARTO_POSITRON("Light (Carto)", "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"),
    CARTO_DARK("Dark Matter", "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"),
    OPENSTREETMAP("Standard OSM", "https://tile.openstreetmap.org/{z}/{x}/{y}.png"),
    CARTO_VOYAGER("Voyager Outdoors", "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png")
}

sealed class ActiveNavStatus {
    object Idle : ActiveNavStatus()
    data class Navigating(
        val route: RouteResult,
        val currentStepIndex: Int = 0,
        val currentSpeedKmH: Int = 46,
        val speedLimitKmH: Int = 50,
        val remainingDistanceMeters: Double,
        val remainingSeconds: Double,
        val currentLat: Double,
        val currentLng: Double,
        val isMuted: Boolean = false
    ) : ActiveNavStatus()
    data class Finished(val route: RouteResult) : ActiveNavStatus()
}
