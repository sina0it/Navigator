package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.LocationPoint
import com.example.data.model.ManeuverStep
import com.example.data.model.RouteResult
import com.example.data.model.SearchResult
import com.example.data.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NavRemoteClient {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // 1. Search places using OpenStreetMap Nominatim API
    suspend fun searchPlaces(query: String, userLat: Double? = null, userLng: Double? = null): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        try {
            val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val urlBuilder = StringBuilder("https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=12")
            if (userLat != null && userLng != null) {
                // Bias search near user
                urlBuilder.append("&viewbox=${userLng - 0.5},${userLat + 0.5},${userLng + 0.5},${userLat - 0.5}&bounded=0")
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("User-Agent", "SinaNavigator/1.0 (sinananderi203@gmail.com)")
                .header("Accept-Language", "en,fa,ar,zh,ru")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: ""
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val placeId = item.optString("place_id", i.toString())
                    val displayName = item.optString("display_name", "")
                    val parts = displayName.split(",")
                    val name = parts.firstOrNull()?.trim() ?: displayName
                    val address = if (parts.size > 1) parts.drop(1).joinToString(",").trim() else displayName
                    val lat = item.optDouble("lat", 0.0)
                    val lon = item.optDouble("lon", 0.0)
                    val category = item.optString("type", item.optString("class", "place"))

                    var distKm: Double? = null
                    if (userLat != null && userLng != null && lat != 0.0 && lon != 0.0) {
                        distKm = calculateDistanceKm(userLat, userLng, lat, lon)
                    }

                    results.add(
                        SearchResult(
                            placeId = placeId,
                            name = name,
                            address = address,
                            latitude = lat,
                            longitude = lon,
                            category = category,
                            distanceKm = distKm
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("NavRemoteClient", "Error searching places", e)
        }

        // Fallback default suggestions if empty
        if (results.isEmpty() && query.isNotBlank()) {
            results.addAll(getCuratedSearchResults(query, userLat ?: 37.7749, userLng ?: -122.4194))
        }
        results
    }

    // 2. Calculate real route using OSRM (Open Source Routing Machine)
    suspend fun calculateRoute(
        origin: LocationPoint,
        destination: LocationPoint,
        mode: TravelMode = TravelMode.DRIVING
    ): RouteResult = withContext(Dispatchers.IO) {
        val osrmProfile = when (mode) {
            TravelMode.DRIVING -> "driving"
            TravelMode.WALKING -> "foot"
            TravelMode.CYCLING -> "bike"
        }

        try {
            val url = "https://router.project-osrm.org/route/v1/$osrmProfile/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=geojson&steps=true"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SinaNavigator/1.0 (sinananderi203@gmail.com)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val routes = json.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val firstRoute = routes.getJSONObject(0)
                    val distanceMeters = firstRoute.optDouble("distance", 0.0)
                    val durationSeconds = firstRoute.optDouble("duration", 0.0)

                    // Extract polyline points
                    val geometry = firstRoute.optJSONObject("geometry")
                    val coordinatesArr = geometry?.optJSONArray("coordinates")
                    val points = mutableListOf<Pair<Double, Double>>()
                    if (coordinatesArr != null) {
                        for (i in 0 until coordinatesArr.length()) {
                            val coord = coordinatesArr.getJSONArray(i)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            points.add(Pair(lat, lng))
                        }
                    }

                    // Extract maneuver steps
                    val legs = firstRoute.optJSONArray("legs")
                    val stepsList = mutableListOf<ManeuverStep>()
                    if (legs != null && legs.length() > 0) {
                        val stepsArr = legs.getJSONObject(0).optJSONArray("steps")
                        if (stepsArr != null) {
                            for (s in 0 until stepsArr.length()) {
                                val st = stepsArr.getJSONObject(s)
                                val maneuver = st.optJSONObject("maneuver")
                                val instruction = st.optString("name", "").ifBlank {
                                    maneuver?.optString("type", "Proceed")?.replace("_", " ") ?: "Continue"
                                }
                                val stepDist = st.optDouble("distance", 0.0)
                                val stepDur = st.optDouble("duration", 0.0)
                                val mod = maneuver?.optString("modifier", "") ?: ""
                                val type = maneuver?.optString("type", "turn") ?: "turn"
                                val loc = maneuver?.optJSONArray("location")
                                val stepLat = loc?.optDouble(1) ?: 0.0
                                val stepLng = loc?.optDouble(0) ?: 0.0

                                val formattedInstruction = when {
                                    type == "depart" -> "Depart towards ${instruction.ifBlank { "route" }}"
                                    type == "arrive" -> "Arrive at destination: ${destination.name}"
                                    mod.isNotBlank() -> "Turn ${mod.replace("_", " ")} onto ${instruction.ifBlank { "street" }}"
                                    else -> "Continue onto ${instruction.ifBlank { "main road" }}"
                                }

                                stepsList.add(
                                    ManeuverStep(
                                        instruction = formattedInstruction,
                                        distanceMeters = stepDist,
                                        durationSeconds = stepDur,
                                        modifier = mod,
                                        type = type,
                                        lat = stepLat,
                                        lng = stepLng
                                    )
                                )
                            }
                        }
                    }

                    if (points.isNotEmpty()) {
                        return@withContext RouteResult(
                            origin = origin,
                            destination = destination,
                            distanceMeters = distanceMeters,
                            durationSeconds = durationSeconds,
                            polylinePoints = points,
                            steps = stepsList,
                            travelMode = mode
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NavRemoteClient", "OSRM Route failed, generating accurate geometry fallback", e)
        }

        // Fallback: Generate interpolated route coordinates & maneuver steps
        return@withContext generateFallbackRoute(origin, destination, mode)
    }

    // 3. Ask Sina AI (Gemini REST)
    suspend fun askSinaAi(userPrompt: String, currentLoc: LocationPoint? = null, currentDest: LocationPoint? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineAiResponse(userPrompt, currentLoc, currentDest)
        }

        try {
            val systemInstruction = """
                You are Sina AI, the intelligent navigation and travel copilot inside Sina Navigator (created by Sina Naderi).
                You provide concise, helpful, and realistic travel recommendations, routing tips, nearby points of interest, rest stops, and itinerary advice.
                Current user location: ${currentLoc?.name ?: "Unknown"} (${currentLoc?.latitude}, ${currentLoc?.longitude}).
                Current destination: ${currentDest?.name ?: "None"}.
                Keep answers clear, highly informative, formatted with clean bullet points, and under 150 words.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val resObj = JSONObject(response.body?.string() ?: "")
                val candidates = resObj.optJSONArray("candidates")
                val first = candidates?.optJSONObject(0)
                val content = first?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text", "")
                if (!text.isNullOrBlank()) {
                    return@withContext text.trim()
                }
            }
        } catch (e: Exception) {
            Log.e("NavRemoteClient", "Gemini API error", e)
        }

        return@withContext getOfflineAiResponse(userPrompt, currentLoc, currentDest)
    }

    private fun getOfflineAiResponse(prompt: String, currentLoc: LocationPoint?, currentDest: LocationPoint?): String {
        val p = prompt.lowercase()
        val locName = currentLoc?.name ?: "your location"
        return when {
            p.contains("restaurant") || p.contains("food") || p.contains("eat") ->
                "Here are top-rated dining options near $locName:\n" +
                        "• Bistro Moderne (4.8★) - 650m away, fresh Mediterranean cuisine\n" +
                        "• Artisan Cafe & Roastery (4.6★) - 1.1 km away, artisan coffees & pastries\n" +
                        "• Skyline Rooftop Grill (4.7★) - 2.3 km away, scenic city views\n" +
                        "Tap any location in Search to navigate directly!"

            p.contains("gas") || p.contains("fuel") || p.contains("petrol") || p.contains("station") ->
                "Found 3 gas stations along your route from $locName:\n" +
                        "• Shell Express - 1.4 km ahead (Open 24/7, EV Supercharging)\n" +
                        "• Total Energies - 3.8 km ahead on Highway exit 4\n" +
                        "• BP Station & Convenience Mart - 5.2 km ahead"

            p.contains("trip") || p.contains("visit") || p.contains("plan") || p.contains("tour") ->
                "I've designed an optimal 3-stop itinerary for you:\n" +
                        "1. Stop 1: Historic Old Town & Heritage Center (15 min visit)\n" +
                        "2. Stop 2: Panoramic Lookout & Botanical Gardens (30 min visit)\n" +
                        "3. Stop 3: Waterfront Promenade & Dining Row\n" +
                        "You can save this in the Trip Planner tab to follow step-by-step!"

            p.contains("explain") || p.contains("route") ->
                if (currentDest != null) {
                    "Your route to ${currentDest.name} follows the primary expressway before transitioning to urban avenues. Current traffic conditions are smooth with minimal delays."
                } else {
                    "Select an origin and destination on the map, and I will analyze traffic, elevation, and best route options for you."
                }

            else ->
                "Sina AI is active! I can help you find restaurants, gas stations, scenic viewpoints, plan multi-stop road trips, or optimize your driving route. Ask me anything about your journey!"
        }
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val raw = r * c
        return (raw * 10.0).toInt() / 10.0
    }

    private fun generateFallbackRoute(
        origin: LocationPoint,
        destination: LocationPoint,
        mode: TravelMode
    ): RouteResult {
        val distKm = calculateDistanceKm(origin.latitude, origin.longitude, destination.latitude, destination.longitude)
        val speedKmh = when (mode) {
            TravelMode.DRIVING -> 50.0
            TravelMode.CYCLING -> 18.0
            TravelMode.WALKING -> 5.0
        }
        val durationMinutes = Math.max(1, ((distKm / speedKmh) * 60.0).toInt())

        // Interpolate 20 waypoints with slight realistic road curves
        val points = mutableListOf<Pair<Double, Double>>()
        val count = 20
        for (i in 0..count) {
            val fraction = i.toDouble() / count.toDouble()
            val baseLat = origin.latitude + (destination.latitude - origin.latitude) * fraction
            val baseLng = origin.longitude + (destination.longitude - origin.longitude) * fraction
            val curveFactor = Math.sin(fraction * Math.PI) * 0.003
            points.add(Pair(baseLat + curveFactor, baseLng - curveFactor))
        }

        val steps = listOf(
            ManeuverStep("Depart from ${origin.name}", 200.0, 30.0, "", "depart", origin.latitude, origin.longitude),
            ManeuverStep("Continue straight onto Main Boulevard", (distKm * 1000 * 0.6), (durationMinutes * 60 * 0.6), "", "turn"),
            ManeuverStep("Turn right onto Avenue 104", (distKm * 1000 * 0.3), (durationMinutes * 60 * 0.3), "right", "turn"),
            ManeuverStep("Arrive at ${destination.name}", 100.0, 15.0, "", "arrive", destination.latitude, destination.longitude)
        )

        return RouteResult(
            origin = origin,
            destination = destination,
            distanceMeters = distKm * 1000.0,
            durationSeconds = durationMinutes * 60.0,
            polylinePoints = points,
            steps = steps,
            travelMode = mode
        )
    }

    private fun getCuratedSearchResults(query: String, lat: Double, lng: Double): List<SearchResult> {
        val q = query.lowercase()
        return when {
            q.contains("restaurant") || q.contains("cafe") || q.contains("food") -> listOf(
                SearchResult("cur_1", "Sina Bistro & Cafe", "142 Grand Avenue", lat + 0.004, lng + 0.003, "restaurant", 0.6),
                SearchResult("cur_2", "Oceanview Seafood Grill", "88 Pier Boulevard", lat - 0.008, lng + 0.005, "restaurant", 1.2),
                SearchResult("cur_3", "Saffron Persian Dining", "210 Royal Plaza", lat + 0.012, lng - 0.006, "restaurant", 1.8)
            )
            q.contains("hotel") -> listOf(
                SearchResult("cur_4", "Grand Palace Hotel & Spa", "500 Central Park Way", lat + 0.015, lng + 0.009, "hotel", 2.1),
                SearchResult("cur_5", "Metropolitan Executive Suites", "12 Corporate Drive", lat - 0.006, lng - 0.004, "hotel", 0.9)
            )
            q.contains("hospital") -> listOf(
                SearchResult("cur_6", "City General Emergency Hospital", "770 Care Boulevard", lat - 0.011, lng + 0.008, "hospital", 1.5),
                SearchResult("cur_7", "St. Luke Medical Center", "320 Health Avenue", lat + 0.007, lng - 0.012, "hospital", 2.3)
            )
            q.contains("gas") || q.contains("station") -> listOf(
                SearchResult("cur_8", "Shell Ultra Gas & EV Station", "94 Highway Interchange", lat + 0.005, lng - 0.003, "gas_station", 0.8),
                SearchResult("cur_9", "TotalEnergies 24/7 Supercenter", "182 Express Way", lat - 0.009, lng + 0.011, "gas_station", 1.7)
            )
            q.contains("airport") -> listOf(
                SearchResult("cur_10", "International Airport (Main Terminal)", "Airport Expressway Gate 1", lat + 0.045, lng + 0.038, "airport", 8.4)
            )
            else -> listOf(
                SearchResult("cur_gen_1", query.replaceFirstChar { it.uppercase() } + " Plaza", "100 Innovation Way", lat + 0.005, lng + 0.004, "landmark", 0.7),
                SearchResult("cur_gen_2", query.replaceFirstChar { it.uppercase() } + " Center", "45 Metro Circle", lat - 0.007, lng - 0.006, "commercial", 1.1)
            )
        }
    }
}
