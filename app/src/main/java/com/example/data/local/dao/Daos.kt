package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.FavoritePlaceEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.RecentDestinationEntity
import com.example.data.local.entity.RouteHistoryEntity
import com.example.data.local.entity.SavedTripEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("SELECT COUNT(*) FROM users")
    fun getUserCount(): Flow<Int>
}

@Dao
interface FavoritePlaceDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getFavoritesByUser(userId: Long): Flow<List<FavoritePlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoritePlaceEntity): Long

    @Update
    suspend fun updateFavorite(favorite: FavoritePlaceEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoritePlaceEntity)

    @Query("DELETE FROM favorites WHERE id = :id AND userId = :userId")
    suspend fun deleteFavoriteById(id: Long, userId: Long)

    @Query("SELECT COUNT(*) FROM favorites")
    fun getTotalFavoritesCount(): Flow<Int>
}

@Dao
interface RecentDestinationDao {
    @Query("SELECT * FROM recent_destinations WHERE userId = :userId ORDER BY visitedAt DESC LIMIT 25")
    fun getRecentDestinations(userId: Long): Flow<List<RecentDestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(destination: RecentDestinationEntity): Long

    @Query("DELETE FROM recent_destinations WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: Long, userId: Long)

    @Query("DELETE FROM recent_destinations WHERE userId = :userId")
    suspend fun clearAll(userId: Long)
}

@Dao
interface RouteHistoryDao {
    @Query("SELECT * FROM routes WHERE userId = :userId ORDER BY createdAt DESC LIMIT 30")
    fun getRouteHistory(userId: Long): Flow<List<RouteHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteHistoryEntity): Long

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRouteById(id: Long)

    @Query("SELECT COUNT(*) FROM routes")
    fun getTotalRoutesCount(): Flow<Int>
}

@Dao
interface SavedTripDao {
    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getTripsByUser(userId: Long): Flow<List<SavedTripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: Long): SavedTripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: SavedTripEntity): Long

    @Update
    suspend fun updateTrip(trip: SavedTripEntity)

    @Query("DELETE FROM trips WHERE id = :id AND userId = :userId")
    suspend fun deleteTrip(id: Long, userId: Long)

    @Query("SELECT COUNT(*) FROM trips")
    fun getTotalTripsCount(): Flow<Int>
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsByUser(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCount(userId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 50")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long
}

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM user_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)
}
