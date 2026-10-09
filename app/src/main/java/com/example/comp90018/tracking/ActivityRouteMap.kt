package com.example.comp90018.ui.screens.activity

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import java.io.File
import android.util.Log
import android.graphics.Color
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import com.example.comp90018.tracking.TrackPoint
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


//Create a dedicated tool for managing the Map Network, one-time network configuration
private object TrailwiseMapNetwork {
    private var configured = false

    // Prevents multiple threads from configuring the network client at the same time.
    @Synchronized
    fun configure(context: Context) {
        if (configured) return

        val client = OkHttpClient.Builder()
            .cache(
                Cache(
                    directory = File(
                        context.applicationContext.cacheDir,
                        "map-http-cache"
                    ),
                    maxSize = 50L * 1024L * 1024L // Maximum cache size: 50 MB

                )
            )
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header(
                        "User-Agent",
                        "Trailwise/1.0 (com.example.comp90018)"
                    )
                    .build()

                val response = chain.proceed(request)

                Log.d(
                    "TrailwiseMap",
                    "host=${request.url.host}, " +
                            "userAgent=${request.header("User-Agent")}, " +
                            "status=${response.code}, " +
                            "network=${response.networkResponse?.code}, " +
                            "cache=${response.cacheResponse?.code}"
                )

                response
            }
            .build()

        HttpRequestUtil.setOkHttpClient(client)
        configured = true
    }
}

private const val ROUTE_SOURCE_ID = "activity-route-source"
private const val ROUTE_LAYER_ID = "activity-route-layer"

private const val START_SOURCE_ID = "activity-start-source"
private const val START_LAYER_ID = "activity-start-layer"

private const val END_SOURCE_ID = "activity-end-source"
private const val END_LAYER_ID = "activity-end-layer"

private fun emptyFeatures(): FeatureCollection {
    return FeatureCollection.fromFeatures(
        emptyArray<Feature>()
    )
}
// Adds the route line, start marker, and end marker layers to the map style.
private fun installRouteLayers(style: Style) {
    if (style.getSource(ROUTE_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                ROUTE_SOURCE_ID,
                emptyFeatures()
            )
        )
    }

    if (style.getLayer(ROUTE_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(
                ROUTE_LAYER_ID,
                ROUTE_SOURCE_ID
            ).withProperties(
                PropertyFactory.lineColor(
                    Color.parseColor("#0B5D3B")
                ),
                PropertyFactory.lineWidth(6f),
                PropertyFactory.lineCap(
                    Property.LINE_CAP_ROUND
                ),
                PropertyFactory.lineJoin(
                    Property.LINE_JOIN_ROUND
                )
            )
        )
    }

    if (style.getSource(START_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                START_SOURCE_ID,
                emptyFeatures()
            )
        )
    }

    if (style.getLayer(START_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(
                START_LAYER_ID,
                START_SOURCE_ID
            ).withProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleColor(
                    Color.WHITE
                ),
                PropertyFactory.circleStrokeWidth(4f),
                PropertyFactory.circleStrokeColor(
                    Color.parseColor("#21844A")
                )
            )
        )
    }

    if (style.getSource(END_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                END_SOURCE_ID,
                emptyFeatures()
            )
        )
    }

    if (style.getLayer(END_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(
                END_LAYER_ID,
                END_SOURCE_ID
            ).withProperties(
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleColor(
                    Color.parseColor("#E85D55")
                ),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(
                    Color.WHITE
                )
            )
        )
    }
}

// Displays the activity route on a MapLibre map.
@Composable
fun ActivityRouteMap(
    segments: List<List<TrackPoint>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Stores the previous number of route points.
    val lastCameraPointCount = remember {
        intArrayOf(-1)
    }

    var currentMap by remember {
        mutableStateOf<MapLibreMap?>(null)
    }

    var currentStyle by remember {
        mutableStateOf<Style?>(null)
    }

    val mapView = remember(context, lifecycleOwner) {
        MapLibre.getInstance(context.applicationContext)
        TrailwiseMapNetwork.configure(context)

        MapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        var disposed = false
        var started = false
        var resumed = false

        fun stopMap() {
            if (resumed) {
                mapView.onPause()
                resumed = false
            }

            if (started) {
                mapView.onStop()
                started = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (!started) {
                        mapView.onStart()
                        started = true
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (!resumed) {
                        mapView.onResume()
                        resumed = true
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    if (resumed) {
                        mapView.onPause()
                        resumed = false
                    }
                }

                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> stopMap()

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        mapView.getMapAsync { map ->
            currentMap = map
            if (!disposed) {
                // Initial preview: central Melbourne.
                // This is not the user's detected position.
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(-37.8136, 144.9631))
                    .zoom(14.0)
                    .build()

                map.setStyle(
                    Style.Builder().fromUri(
                        "https://tiles.openfreemap.org/styles/liberty"
                    )
                ) { style ->
                    installRouteLayers(style)
                    currentStyle = style
                }
            }
        }

        onDispose {
            disposed = true
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopMap()
            mapView.onDestroy()
        }
    }


    LaunchedEffect(
        currentMap,
        currentStyle,
        segments
    ) {
        val map = currentMap ?: return@LaunchedEffect
        val style = currentStyle ?: return@LaunchedEffect

        val pointCount = segments.sumOf { it.size }

        updateRoute(
            map = map,
            style = style,
            segments = segments,
            moveCamera =
                pointCount != lastCameraPointCount[0]
        )

        lastCameraPointCount[0] = pointCount
    }


    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}

private fun updateRoute(
    map: MapLibreMap,
    style: Style,
    segments: List<List<TrackPoint>>,
    moveCamera: Boolean
) {
    installRouteLayers(style)

    val validSegments = segments.filter {
        it.size >= 2
    }

    val routeFeatures = validSegments.map { segment ->
        val points = segment.map { point ->
            Point.fromLngLat(
                point.longitude,
                point.latitude
            )
        }

        Feature.fromGeometry(
            LineString.fromLngLats(points)
        )
    }

    val routeSource =
        style.getSource(ROUTE_SOURCE_ID) as? GeoJsonSource

    routeSource?.setGeoJson(
        FeatureCollection.fromFeatures(
            routeFeatures.toTypedArray()
        )
    )

    val allPoints = segments.flatten()

    val startSource =
        style.getSource(START_SOURCE_ID) as? GeoJsonSource

    val endSource =
        style.getSource(END_SOURCE_ID) as? GeoJsonSource

    if (allPoints.isEmpty()) {
        startSource?.setGeoJson(emptyFeatures())
        endSource?.setGeoJson(emptyFeatures())
        return
    }

    val first = allPoints.first()
    val last = allPoints.last()

    startSource?.setGeoJson(
        Point.fromLngLat(
            first.longitude,
            first.latitude
        )
    )

    if (allPoints.size >= 2) {
        endSource?.setGeoJson(
            Point.fromLngLat(
                last.longitude,
                last.latitude
            )
        )
    } else {
        endSource?.setGeoJson(emptyFeatures())
    }

    if (!moveCamera) return

    if (allPoints.size == 1) {
        map.cameraPosition = CameraPosition.Builder()
            .target(
                LatLng(
                    first.latitude,
                    first.longitude
                )
            )
            .zoom(16.0)
            .build()

        return
    }

    val boundsBuilder = LatLngBounds.Builder()

    allPoints.forEach { point ->
        boundsBuilder.include(
            LatLng(
                point.latitude,
                point.longitude
            )
        )
    }

    val bounds = boundsBuilder.build()

    map.getCameraForLatLngBounds(
        bounds,
        intArrayOf(80, 80, 80, 80)
    )?.let { camera ->
        map.cameraPosition = camera
    }
}
