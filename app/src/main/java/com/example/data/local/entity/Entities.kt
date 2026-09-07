package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val fullName: String,
    val avatarUrl: String = "",
    val role: String = "USER", // "USER", "ADMIN", "SUPER_ADMIN", "MODERATOR"
    val preferredLanguage: String = "en", // "en", "fa", "ar", "zh", "ru"
    val themePreference: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val voiceGuidance: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoritePlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val address: String,
    val category: String = "FAVORITE", // "HOME", "WORK", "FAVORITE", "CUSTOM"
    val latitude: Double,
    val longitude: Double,
    val iconName: String = "star",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_destinations")
data class RecentDestinationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val travelMode: String = "DRIVING",
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "routes")
data class RouteHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val originName: String,
    val originLat: Double,
    val originLng: Double,
    val destinationName: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val distanceKm: Double,
    val durationMin: Int,
    val travelMode: String = "DRIVING",
    val polylinePointsJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trips")
data class SavedTripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val description: String = "",
    val travelMode: String = "DRIVING",
    val stopsJson: String, // JSON array of stops
    val totalDistanceKm: Double = 0.0,
    val totalDurationMin: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val type: String = "SYSTEM", // "TRIP_REMINDER", "SYSTEM", "TRAFFIC", "UPDATE"
    val isRead: Boolean = false,
    val actionUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,
    val userEmail: String,
    val action: String,
    val details: String,
    val ipAddress: String = "127.0.0.1",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
