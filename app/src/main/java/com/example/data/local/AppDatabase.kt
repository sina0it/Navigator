package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AppSettingDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.FavoritePlaceDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.RecentDestinationDao
import com.example.data.local.dao.RouteHistoryDao
import com.example.data.local.dao.SavedTripDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.FavoritePlaceEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.RecentDestinationEntity
import com.example.data.local.entity.RouteHistoryEntity
import com.example.data.local.entity.SavedTripEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Database(
    entities = [
        UserEntity::class,
        FavoritePlaceEntity::class,
        RecentDestinationEntity::class,
        RouteHistoryEntity::class,
        SavedTripEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun favoritePlaceDao(): FavoritePlaceDao
    abstract fun recentDestinationDao(): RecentDestinationDao
    abstract fun routeHistoryDao(): RouteHistoryDao
    abstract fun savedTripDao(): SavedTripDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sina_navigator_db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun hashPassword(password: String, salt: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((password + salt).toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(db: AppDatabase) {
                val userDao = db.userDao()
                val favDao = db.favoritePlaceDao()
                val notifDao = db.notificationDao()
                val recentDao = db.recentDestinationDao()
                val tripDao = db.savedTripDao()
                val routeDao = db.routeHistoryDao()
                val logDao = db.auditLogDao()

                // Seed Super Admin: Sina Naderi
                val adminSalt = "sina_salt_2026"
                val adminId = userDao.insertUser(
                    UserEntity(
                        id = 1,
                        username = "sina_admin",
                        email = "sinananderi203@gmail.com",
                        passwordHash = hashPassword("admin123", adminSalt),
                        salt = adminSalt,
                        fullName = "Sina Naderi",
                        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                        role = "SUPER_ADMIN",
                        preferredLanguage = "en",
                        themePreference = "DARK"
                    )
                )

                // Seed Demo User
                val userSalt = "alex_salt_2026"
                val demoUserId = userDao.insertUser(
                    UserEntity(
                        id = 2,
                        username = "alex_navigator",
                        email = "user@example.com",
                        passwordHash = hashPassword("password123", userSalt),
                        salt = userSalt,
                        fullName = "Alex Morgan",
                        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                        role = "USER",
                        preferredLanguage = "en",
                        themePreference = "SYSTEM"
                    )
                )

                // Seed Favorites
                favDao.insertFavorite(
                    FavoritePlaceEntity(
                        userId = demoUserId,
                        name = "Home",
                        address = "452 Market Street, Downtown",
                        category = "HOME",
                        latitude = 37.789172,
                        longitude = -122.401449,
                        iconName = "home",
                        notes = "Primary residence"
                    )
                )
                favDao.insertFavorite(
                    FavoritePlaceEntity(
                        userId = demoUserId,
                        name = "Tech Hub Work",
                        address = "100 Innovation Boulevard, Silicon Park",
                        category = "WORK",
                        latitude = 37.774929,
                        longitude = -122.419416,
                        iconName = "work",
                        notes = "Office Building 4"
                    )
                )
                favDao.insertFavorite(
                    FavoritePlaceEntity(
                        userId = demoUserId,
                        name = "Grand Central Station",
                        address = "Central Plaza & Transit Way",
                        category = "FAVORITE",
                        latitude = 37.783333,
                        longitude = -122.416667,
                        iconName = "train",
                        notes = "High-speed rail connection"
                    )
                )
                favDao.insertFavorite(
                    FavoritePlaceEntity(
                        userId = demoUserId,
                        name = "Milad Tower & Scenic Park",
                        address = "Heights District, Skyline Way",
                        category = "CUSTOM",
                        latitude = 35.7448,
                        longitude = 51.3753,
                        iconName = "landmark",
                        notes = "Panoramic city view"
                    )
                )

                // Seed Recent Destinations
                recentDao.insertRecent(
                    RecentDestinationEntity(
                        userId = demoUserId,
                        name = "City General Hospital",
                        address = "789 Health Avenue",
                        latitude = 37.7650,
                        longitude = -122.4200,
                        travelMode = "DRIVING"
                    )
                )
                recentDao.insertRecent(
                    RecentDestinationEntity(
                        userId = demoUserId,
                        name = "Metropolitan Airport (Terminal 2)",
                        address = "Aviation Highway Exit 7",
                        latitude = 37.6213,
                        longitude = -122.3790,
                        travelMode = "DRIVING"
                    )
                )

                // Seed Welcome Notifications
                notifDao.insertNotification(
                    NotificationEntity(
                        userId = demoUserId,
                        title = "Welcome to Sina Navigator",
                        message = "Experience high-precision routing, Sina AI assistance, and seamless multilingual navigation.",
                        type = "SYSTEM"
                    )
                )
                notifDao.insertNotification(
                    NotificationEntity(
                        userId = demoUserId,
                        title = "Live Navigation Ready",
                        message = "GPS and OpenStreetMap vector routing engine configured and operational.",
                        type = "TRIP_REMINDER"
                    )
                )

                // Seed Saved Trip
                val demoStopsJson = """
                    [
                        {"name":"Central Station","address":"Downtown Hub","lat":37.7833,"lng":-122.4166,"order":1},
                        {"name":"Civic Center & Arts Plaza","address":"Civic Way","lat":37.7792,"lng":-122.4191,"order":2},
                        {"name":"Sunset Coastline Bay","address":"Coastal Blvd","lat":37.7690,"lng":-122.4467,"order":3}
                    ]
                """.trimIndent()
                tripDao.insertTrip(
                    SavedTripEntity(
                        userId = demoUserId,
                        title = "City Tour & Coastline Itinerary",
                        description = "Scenic weekend 3-stop exploration",
                        travelMode = "DRIVING",
                        stopsJson = demoStopsJson,
                        totalDistanceKm = 14.8,
                        totalDurationMin = 28
                    )
                )

                // Seed Route History
                routeDao.insertRoute(
                    RouteHistoryEntity(
                        userId = demoUserId,
                        originName = "Home (Market St)",
                        originLat = 37.789172,
                        originLng = -122.401449,
                        destinationName = "Tech Hub Work",
                        destinationLat = 37.774929,
                        destinationLng = -122.419416,
                        distanceKm = 4.2,
                        durationMin = 12,
                        travelMode = "DRIVING"
                    )
                )

                // Seed Audit Log
                logDao.insertLog(
                    AuditLogEntity(
                        userId = adminId,
                        userEmail = "sinananderi203@gmail.com",
                        action = "SYSTEM_INITIALIZATION",
                        details = "Sina Navigator database initialized with default security policies and Super Admin role.",
                        ipAddress = "127.0.0.1"
                    )
                )
            }
        }
    }
}
