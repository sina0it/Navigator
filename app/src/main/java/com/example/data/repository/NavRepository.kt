package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.FavoritePlaceEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.RecentDestinationEntity
import com.example.data.local.entity.RouteHistoryEntity
import com.example.data.local.entity.SavedTripEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.LocationPoint
import com.example.data.model.RouteResult
import com.example.data.model.SearchResult
import com.example.data.model.TravelMode
import com.example.data.remote.NavRemoteClient
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NavRepository(private val database: AppDatabase) {
    val userDao = database.userDao()
    val favoriteDao = database.favoritePlaceDao()
    val recentDao = database.recentDestinationDao()
    val routeDao = database.routeHistoryDao()
    val tripDao = database.savedTripDao()
    val notificationDao = database.notificationDao()
    val auditLogDao = database.auditLogDao()
    val settingDao = database.appSettingDao()

    // Users
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    fun getUserCount(): Flow<Int> = userDao.getUserCount()
    suspend fun getUserById(id: Long): UserEntity? = userDao.getUserById(id)
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)

    suspend fun registerUser(username: String, email: String, password: String,fullName: String): Result<UserEntity> {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists"))
        }
        val salt = UUID.randomUUID().toString().substring(0, 8)
        val hash = AppDatabase.hashPassword(password, salt)
        val user = UserEntity(
            username = username,
            email = email,
            passwordHash = hash,
            salt = salt,
            fullName = fullName,
            role = "USER"
        )
        val newId = userDao.insertUser(user)
        logAudit(newId, email, "USER_REGISTERED", "User $username registered successfully")
        return Result.success(user.copy(id = newId))
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = userDao.getUserByEmail(email) ?: return Result.failure(Exception("Invalid email or password"))
        val computedHash = AppDatabase.hashPassword(password, user.salt)
        if (computedHash != user.passwordHash) {
            return Result.failure(Exception("Invalid email or password"))
        }
        if (!user.isActive) {
            return Result.failure(Exception("Your account is deactivated. Please contact admin."))
        }
        logAudit(user.id, user.email, "USER_LOGIN", "User logged in")
        return Result.success(user)
    }

    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun deleteUser(id: Long) = userDao.deleteUserById(id)

    // Favorites
    fun getFavorites(userId: Long): Flow<List<FavoritePlaceEntity>> = favoriteDao.getFavoritesByUser(userId)
    fun getTotalFavoritesCount(): Flow<Int> = favoriteDao.getTotalFavoritesCount()
    suspend fun addFavorite(fav: FavoritePlaceEntity): Long = favoriteDao.insertFavorite(fav)
    suspend fun updateFavorite(fav: FavoritePlaceEntity) = favoriteDao.updateFavorite(fav)
    suspend fun deleteFavorite(id: Long, userId: Long) = favoriteDao.deleteFavoriteById(id, userId)

    // Recent destinations
    fun getRecentDestinations(userId: Long): Flow<List<RecentDestinationEntity>> = recentDao.getRecentDestinations(userId)
    suspend fun addRecentDestination(recent: RecentDestinationEntity) = recentDao.insertRecent(recent)
    suspend fun deleteRecent(id: Long, userId: Long) = recentDao.deleteById(id, userId)
    suspend fun clearRecents(userId: Long) = recentDao.clearAll(userId)

    // Routes
    fun getRouteHistory(userId: Long): Flow<List<RouteHistoryEntity>> = routeDao.getRouteHistory(userId)
    fun getTotalRoutesCount(): Flow<Int> = routeDao.getTotalRoutesCount()
    suspend fun recordRouteHistory(route: RouteHistoryEntity) = routeDao.insertRoute(route)
    suspend fun deleteRouteHistory(id: Long) = routeDao.deleteRouteById(id)

    // Trips
    fun getTrips(userId: Long): Flow<List<SavedTripEntity>> = tripDao.getTripsByUser(userId)
    fun getTotalTripsCount(): Flow<Int> = tripDao.getTotalTripsCount()
    suspend fun saveTrip(trip: SavedTripEntity) = tripDao.insertTrip(trip)
    suspend fun updateTrip(trip: SavedTripEntity) = tripDao.updateTrip(trip)
    suspend fun deleteTrip(id: Long, userId: Long) = tripDao.deleteTrip(id, userId)

    // Notifications
    fun getNotifications(userId: Long): Flow<List<NotificationEntity>> = notificationDao.getNotificationsByUser(userId)
    fun getUnreadCount(userId: Long): Flow<Int> = notificationDao.getUnreadCount(userId)
    suspend fun sendNotification(notif: NotificationEntity) = notificationDao.insertNotification(notif)
    suspend fun markNotificationRead(id: Long) = notificationDao.markAsRead(id)
    suspend fun markAllNotificationsRead(userId: Long) = notificationDao.markAllAsRead(userId)

    // Audit logs
    fun getAuditLogs(): Flow<List<AuditLogEntity>> = auditLogDao.getRecentAuditLogs()
    suspend fun logAudit(userId: Long, email: String, action: String, details: String) {
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = userId,
                userEmail = email,
                action = action,
                details = details
            )
        )
    }

    // App Settings
    suspend fun getSetting(key: String): String? = settingDao.getSetting(key)
    suspend fun setSetting(key: String, value: String) = settingDao.setSetting(AppSettingEntity(key, value))

    // Remote Operations
    suspend fun searchPlaces(query: String, lat: Double?, lng: Double?): List<SearchResult> {
        return NavRemoteClient.searchPlaces(query, lat, lng)
    }

    suspend fun calculateRoute(origin: LocationPoint, destination: LocationPoint, mode: TravelMode): RouteResult {
        return NavRemoteClient.calculateRoute(origin, destination, mode)
    }

    suspend fun askSinaAi(prompt: String, currentLoc: LocationPoint?, currentDest: LocationPoint?): String {
        return NavRemoteClient.askSinaAi(prompt, currentLoc, currentDest)
    }
}
