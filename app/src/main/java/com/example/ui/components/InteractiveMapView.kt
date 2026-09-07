package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.LocationPoint
import com.example.data.model.MapStyle
import com.example.data.model.RouteResult
import org.json.JSONArray
import org.json.JSONObject

class MapWebBridge(
    private val onMapClick: (Double, Double) -> Unit
) {
    @JavascriptInterface
    fun onMapTapped(lat: Double, lng: Double) {
        onMapClick(lat, lng)
    }
}

class MapViewController(val context: Context) {
    var webView: WebView? = null
    var isMapReady = false
    private val pendingCommands = mutableListOf<String>()

    fun setLocation(lat: Double, lng: Double, name: String = "My Location") {
        executeJs("window.updateUserLocation($lat, $lng, '$name');")
    }

    fun setRoute(route: RouteResult?) {
        if (route == null) {
            executeJs("window.clearRoute();")
            return
        }
        val coordsArr = JSONArray()
        for (pt in route.polylinePoints) {
            val p = JSONArray()
            p.put(pt.first)
            p.put(pt.second)
            coordsArr.put(p)
        }
        val origObj = JSONObject().apply {
            put("name", route.origin.name)
            put("lat", route.origin.latitude)
            put("lng", route.origin.longitude)
        }
        val destObj = JSONObject().apply {
            put("name", route.destination.name)
            put("lat", route.destination.latitude)
            put("lng", route.destination.longitude)
        }
        val script = "window.renderRoute($coordsArr, $origObj, $destObj);"
        executeJs(script)
    }

    fun centerMap(lat: Double, lng: Double, zoom: Int = 14) {
        executeJs("window.centerMap($lat, $lng, $zoom);")
    }

    fun setMapStyle(style: MapStyle) {
        executeJs("window.setMapTileStyle('${style.tileUrl}');")
    }

    fun zoomIn() {
        executeJs("window.zoomIn();")
    }

    fun zoomOut() {
        executeJs("window.zoomOut();")
    }

    fun onMapReady() {
        isMapReady = true
        for (cmd in pendingCommands) {
            webView?.evaluateJavascript(cmd, null)
        }
        pendingCommands.clear()
    }

    private fun executeJs(script: String) {
        if (isMapReady && webView != null) {
            webView?.post { webView?.evaluateJavascript(script, null) }
        } else {
            pendingCommands.add(script)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMapView(
    controller: MapViewController,
    currentLocation: LocationPoint,
    selectedRoute: RouteResult?,
    mapStyle: MapStyle,
    onMapClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bridge = remember { MapWebBridge(onMapClick) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                addJavascriptInterface(bridge, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        controller.onMapReady()
                        controller.setLocation(currentLocation.latitude, currentLocation.longitude, currentLocation.name)
                        if (selectedRoute != null) {
                            controller.setRoute(selectedRoute)
                        } else {
                            controller.centerMap(currentLocation.latitude, currentLocation.longitude, 13)
                        }
                    }
                }

                val html = getMapHtmlTemplate(currentLocation.latitude, currentLocation.longitude, mapStyle.tileUrl)
                loadDataWithBaseURL("https://leafletjs.com/", html, "text/html", "UTF-8", null)
                controller.webView = this
            }
        },
        update = {
            controller.setMapStyle(mapStyle)
            controller.setLocation(currentLocation.latitude, currentLocation.longitude, currentLocation.name)
            if (selectedRoute != null) {
                controller.setRoute(selectedRoute)
            }
        },
        modifier = modifier.fillMaxSize()
    )

    LaunchedEffect(mapStyle) {
        controller.setMapStyle(mapStyle)
    }
}

private fun getMapHtmlTemplate(initLat: Double, initLng: Double, tileUrl: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        body, html, #map {
            margin: 0; padding: 0; width: 100%; height: 100%; background: #0B1329; overflow: hidden;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        .user-pulse {
            width: 22px; height: 22px; background: #00D2FF; border-radius: 50%;
            border: 3px solid #FFFFFF; box-shadow: 0 0 16px #00D2FF, 0 0 30px rgba(0,210,255,0.6);
            animation: pulse-ring 2s infinite ease-out;
        }
        @keyframes pulse-ring {
            0% { transform: scale(0.9); opacity: 1; }
            50% { transform: scale(1.15); opacity: 0.8; }
            100% { transform: scale(0.9); opacity: 1; }
        }
        .pin-marker {
            display: flex; align-items: center; justify-content: center;
            font-weight: bold; font-size: 11px; color: white; border-radius: 20px;
            padding: 4px 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.4);
        }
        .origin-pin { background: #10B981; border: 2px solid white; }
        .dest-pin { background: #EF4444; border: 2px solid white; }
        .leaflet-bar { display: none !important; } /* Hide default controls, we use native Compose buttons */
        .leaflet-control-attribution { font-size: 9px !important; background: rgba(0,0,0,0.5) !important; color: #aaa !important; }
        .leaflet-control-attribution a { color: #60A5FA !important; text-decoration: none; }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map', {
            center: [$initLat, $initLng],
            zoom: 13,
            zoomControl: false,
            attributionControl: true
        });

        var currentTileLayer = L.tileLayer('$tileUrl', {
            maxZoom: 19,
            attribution: '© OpenStreetMap contributors, CartoDB'
        }).addTo(map);

        var userMarker = null;
        var originMarker = null;
        var destMarker = null;
        var routePolyline = null;
        var routeBorderPolyline = null;

        function updateUserLocation(lat, lng, name) {
            if (userMarker) {
                userMarker.setLatLng([lat, lng]);
            } else {
                var userIcon = L.divIcon({
                    className: 'user-marker-container',
                    html: '<div class="user-pulse"></div>',
                    iconSize: [22, 22],
                    iconAnchor: [11, 11]
                });
                userMarker = L.marker([lat, lng], {icon: userIcon}).addTo(map);
            }
        }

        function renderRoute(coords, origin, dest) {
            clearRoute();
            if (!coords || coords.length === 0) return;

            // Route outline
            routeBorderPolyline = L.polyline(coords, {
                color: '#0369A1',
                weight: 8,
                opacity: 0.9,
                lineCap: 'round',
                lineJoin: 'round'
            }).addTo(map);

            // Glowing main route polyline
            routePolyline = L.polyline(coords, {
                color: '#00D2FF',
                weight: 5,
                opacity: 1.0,
                lineCap: 'round',
                lineJoin: 'round'
            }).addTo(map);

            // Origin Pin
            if (origin && origin.lat && origin.lng) {
                var oIcon = L.divIcon({
                    className: '',
                    html: '<div class="pin-marker origin-pin">🟢 ' + (origin.name || 'Start') + '</div>',
                    iconSize: [100, 30],
                    iconAnchor: [50, 15]
                });
                originMarker = L.marker([origin.lat, origin.lng], {icon: oIcon}).addTo(map);
            }

            // Destination Pin
            if (dest && dest.lat && dest.lng) {
                var dIcon = L.divIcon({
                    className: '',
                    html: '<div class="pin-marker dest-pin">📍 ' + (dest.name || 'Destination') + '</div>',
                    iconSize: [120, 30],
                    iconAnchor: [60, 15]
                });
                destMarker = L.marker([dest.lat, dest.lng], {icon: dIcon}).addTo(map);
            }

            map.fitBounds(routePolyline.getBounds(), {
                padding: [60, 60],
                maxZoom: 16
            });
        }

        function clearRoute() {
            if (routePolyline) { map.removeLayer(routePolyline); routePolyline = null; }
            if (routeBorderPolyline) { map.removeLayer(routeBorderPolyline); routeBorderPolyline = null; }
            if (originMarker) { map.removeLayer(originMarker); originMarker = null; }
            if (destMarker) { map.removeLayer(destMarker); destMarker = null; }
        }

        function centerMap(lat, lng, zoom) {
            map.flyTo([lat, lng], zoom || 14, { duration: 1.2 });
        }

        function setMapTileStyle(url) {
            if (currentTileLayer) {
                map.removeLayer(currentTileLayer);
            }
            currentTileLayer = L.tileLayer(url, {
                maxZoom: 19,
                attribution: '© OpenStreetMap contributors, CartoDB'
            }).addTo(map);
        }

        function zoomIn() { map.zoomIn(); }
        function zoomOut() { map.zoomOut(); }

        map.on('click', function(e) {
            if (window.AndroidBridge && window.AndroidBridge.onMapTapped) {
                window.AndroidBridge.onMapTapped(e.latlng.lat, e.latlng.lng);
            }
        });
    </script>
</body>
</html>
    """.trimIndent()
}
