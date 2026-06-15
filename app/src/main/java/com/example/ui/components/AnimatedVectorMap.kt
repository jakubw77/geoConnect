package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Friend
import com.example.viewmodel.UserLocation
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun AnimatedVectorMap(
    userLocation: UserLocation?,
    friends: List<Friend>,
    selectedFriend: Friend?,
    onMarkerClick: (Friend) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // Local state for zoom and center
    var zoomScale by remember { mutableStateOf(45000f) } // pixels per degree
    var centerLat by remember { mutableStateOf(52.2297) }
    var centerLng by remember { mutableStateOf(21.0122) }

    // On load, center around user or Warsaw
    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            centerLat = userLocation.latitude
            centerLng = userLocation.longitude
        }
    }

    // If selected friend changes, scale and fit viewport to show both user and friend!
    LaunchedEffect(selectedFriend, userLocation) {
        if (selectedFriend != null && userLocation != null) {
            // Find midpoint
            centerLat = (userLocation.latitude + selectedFriend.lat) / 2.0
            centerLng = (userLocation.longitude + selectedFriend.lng) / 2.0
            
            // Adjust zoom to fit
            val latDelta = abs(userLocation.latitude - selectedFriend.lat)
            val lngDelta = abs(userLocation.longitude - selectedFriend.lng)
            val maxDelta = max(latDelta, lngDelta)
            
            zoomScale = if (maxDelta > 0.001) {
                val calculated = (350f / maxDelta.toFloat()).coerceIn(15000f, 90000f)
                calculated
            } else {
                45000f
            }
        }
    }

    // Infinite transitions for radar pulsation and car animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 60f,
        animationSpec = infiniteSpec(1500),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteSpec(1500),
        label = "PulseAlpha"
    )

    val carProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CarProgress"
    )

    // Transformation support
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(12000f, 150000f)
        // Convert screen pixel changes back to latitude/longitude offsets
        centerLat -= offsetChange.y / zoomScale
        centerLng += offsetChange.x / (zoomScale * cos(centerLat * PI / 180.0))
    }

    // Themes
    val isSystemDark = MaterialTheme.colorScheme.background.red < 0.5f
    val gridColor = if (isSystemDark) Color(0xFF1E2836) else Color(0xFFE2E8F0)
    val lakeColor = if (isSystemDark) Color(0xFF0F1E36) else Color(0xFFD6E4FF)
    val parkColor = if (isSystemDark) Color(0xFF132D20) else Color(0xFFECFDF5)
    val mainRoadColor = if (isSystemDark) Color(0xFF2C3E50) else Color(0xFFF1F5F9)
    val beaconColor = Color(0xFF3B82F6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Drag pan mapping
                    centerLat += dragAmount.y / zoomScale
                    centerLng -= dragAmount.x / (zoomScale * cos(centerLat * PI / 180.0))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Helper Lambda to convert LatLng to screen coordinates
            val toPixels = { lat: Double, lng: Double ->
                val cosLat = cos(centerLat * PI / 180.0)
                val x = centerX + ((lng - centerLng) * zoomScale * cosLat).toFloat()
                val y = centerY - ((lat - centerLat) * zoomScale).toFloat()
                Offset(x, y)
            }

            // Helper Lambda to detect if point is inside viewport
            val inViewport = { offset: Offset ->
                offset.x >= -100f && offset.x <= canvasWidth + 100f &&
                        offset.y >= -100f && offset.y <= canvasHeight + 100f
            }

            // --- 1. WATERWAYS & PARKS BACKGROUND ---
            // Draw a diagonal large river for high depth
            val riverPath = Path().apply {
                val start = Offset(-100f, centerY * 0.3f)
                val end = Offset(canvasWidth + 100f, centerY * 1.5f)
                moveTo(start.x, start.y)
                cubicTo(
                    canvasWidth * 0.3f, centerY * 0.1f,
                    canvasWidth * 0.7f, centerY * 1.8f,
                    end.x, end.y
                )
            }
            drawPath(
                path = riverPath,
                color = lakeColor,
                style = Stroke(width = with(density) { 36.dp.toPx() })
            )

            // Draw a few green park islands
            drawCircle(
                color = parkColor,
                radius = with(density) { 80.dp.toPx() },
                center = Offset(centerX * 1.6f, centerY * 0.4f)
            )
            drawRoundRect(
                color = parkColor,
                topLeft = Offset(centerX * 0.3f, centerY * 1.4f),
                size = Size(with(density) { 140.dp.toPx() }, with(density) { 100.dp.toPx() }),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // --- 2. THE VECTOR GRID ---
            // Draw coordinate grid lines every 0.01 degree aligned steps
            val stepDeg = 0.01
            val minLat = centerLat - (centerY / zoomScale)
            val maxLat = centerLat + (centerY / zoomScale)
            val minLng = centerLng - (centerX / (zoomScale * cos(centerLat * PI / 180.0)))
            val maxLng = centerLng + (centerX / (zoomScale * cos(centerLat * PI / 180.0)))

            // Latitudes (Horizontal lines)
            val startLatIdx = (minLat / stepDeg).toInt() - 1
            val endLatIdx = (maxLat / stepDeg).toInt() + 1
            for (idx in startLatIdx..endLatIdx) {
                val gridLat = idx * stepDeg
                val pt1 = toPixels(gridLat, minLng)
                val pt2 = toPixels(gridLat, maxLng)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, pt1.y),
                    end = Offset(canvasWidth, pt2.y),
                    strokeWidth = 1f
                )
            }

            // Longitudes (Vertical lines)
            val startLngIdx = (minLng / stepDeg).toInt() - 1
            val endLngIdx = (maxLng / stepDeg).toInt() + 1
            for (idx in startLngIdx..endLngIdx) {
                val gridLng = idx * stepDeg
                val pt1 = toPixels(minLat, gridLng)
                val pt2 = toPixels(maxLat, gridLng)
                drawLine(
                    color = gridColor,
                    start = Offset(pt1.x, 0f),
                    end = Offset(pt2.x, canvasHeight),
                    strokeWidth = 1f
                )
            }

            // --- 3. HIGHWAYS AND INTERSECTIONS ---
            // Draw a main highway passing through the center horizontally
            drawLine(
                color = mainRoadColor,
                start = Offset(0f, centerY * 1.1f),
                end = Offset(canvasWidth, centerY * 0.9f),
                strokeWidth = with(density) { 8.dp.toPx() }
            )
            // Vertical primary expressway
            drawLine(
                color = mainRoadColor,
                start = Offset(centerX * 0.8f, 0f),
                end = Offset(centerX * 1.2f, canvasHeight),
                strokeWidth = with(density) { 8.dp.toPx() }
            )

            // Dynamic grid city blocks streets drawing
            for (offsetMultiplier in -3..3) {
                // Secondary minor roads
                val roadY = centerY + (offsetMultiplier * 180)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, roadY),
                    end = Offset(canvasWidth, roadY),
                    strokeWidth = 2f
                )
                val roadX = centerX + (offsetMultiplier * 240)
                drawLine(
                    color = gridColor,
                    start = Offset(roadX, 0f),
                    end = Offset(roadX, canvasHeight),
                    strokeWidth = 2f
                )
            }

            // --- 4. GLOWING NEON ROAD ROUTING (SELECTED FRIEND PATH) ---
            var routePointsCoordinates = emptyList<Offset>()
            if (selectedFriend != null && selectedFriend.isSharing && userLocation != null) {
                val pointsStr = selectedFriend.polylinePoints
                if (!pointsStr.isNullOrEmpty()) {
                    val decodedGps = decodePolylinePoints(pointsStr)
                    routePointsCoordinates = decodedGps.map { toPixels(it.first, it.second) }
                } else {
                    // Fallback visual line if polyline data hasn't finished calculating
                    routePointsCoordinates = listOf(
                        toPixels(userLocation.latitude, userLocation.longitude),
                        toPixels((userLocation.latitude + selectedFriend.lat)/2.0, (userLocation.longitude + selectedFriend.lng)/2.0),
                        toPixels(selectedFriend.lat, selectedFriend.lng)
                    )
                }

                if (routePointsCoordinates.size > 1) {
                    val glowPath = Path().apply {
                        moveTo(routePointsCoordinates.first().x, routePointsCoordinates.first().y)
                        for (i in 1 until routePointsCoordinates.size) {
                            lineTo(routePointsCoordinates[i].x, routePointsCoordinates[i].y)
                        }
                    }

                    // Neon outline shadow glow
                    drawPath(
                        path = glowPath,
                        color = Color(0x33EC4899), // Emerald or Fuchsia pink
                        style = Stroke(
                            width = with(density) { 8.dp.toPx() },
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                    // Core neon line
                    drawPath(
                        path = glowPath,
                        color = Color(0xFFEC4899),
                        style = Stroke(
                            width = with(density) { 3.5.dp.toPx() },
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )

                    // Draw an animated car indicator traveling along the route!
                    val totalSegments = routePointsCoordinates.size - 1
                    val progressIdx = (carProgress * totalSegments).toInt().coerceIn(0, totalSegments - 1)
                    val segmentProgress = (carProgress * totalSegments) - progressIdx
                    
                    val ptStart = routePointsCoordinates[progressIdx]
                    val ptEnd = routePointsCoordinates[progressIdx + 1]
                    val carOffset = Offset(
                        ptStart.x + (ptEnd.x - ptStart.x) * segmentProgress,
                        ptStart.y + (ptEnd.y - ptStart.y) * segmentProgress
                    )

                    // Animated Neon Target Circle (glowing car)
                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = carOffset
                    )
                    drawCircle(
                        color = Color(0xFFEC4899),
                        radius = 14f,
                        center = carOffset,
                        style = Stroke(width = 3f)
                    )
                }
            }

            // --- 5. CURRENT PHONE GPS LOCATION ---
            if (userLocation != null) {
                val userPx = toPixels(userLocation.latitude, userLocation.longitude)
                if (inViewport(userPx)) {
                    // Pulsating outer radar rings
                    drawCircle(
                        color = beaconColor.copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = userPx
                    )
                    drawCircle(
                        color = beaconColor.copy(alpha = 0.2f),
                        radius = 18f,
                        center = userPx
                    )
                    // Core beacon
                    drawCircle(
                        color = Color.White,
                        radius = 9f,
                        center = userPx
                    )
                    drawCircle(
                        color = beaconColor,
                        radius = 6f,
                        center = userPx
                    )
                }
            }

            // --- 6. FRIENDS LOCATIONS MARKERS ---
            friends.forEach { friend ->
                if (friend.isSharing) {
                    val friendPx = toPixels(friend.lat, friend.lng)
                    if (inViewport(friendPx)) {
                        val isSelected = selectedFriend?.id == friend.id
                        val markerBaseColor = if (isSelected) Color(0xFFEC4899) else Color(0xFF10B981)

                        // Marker anchor dot
                        drawCircle(
                            color = markerBaseColor,
                            radius = 6f,
                            center = friendPx
                        )

                        // Floating initial/avatar card block on the canvas!
                        val initial = friend.name.firstOrNull()?.toString() ?: "F"
                        val labelText = friend.name.split(" ").firstOrNull() ?: friend.name
                        
                        val pillWidth = with(density) { 72.dp.toPx() }
                        val pillHeight = with(density) { 26.dp.toPx() }
                        val topLeft = Offset(friendPx.x - pillWidth / 2f, friendPx.y - pillHeight - 20f)

                        // Drawing connection line
                        drawLine(
                            color = markerBaseColor,
                            start = friendPx,
                            end = Offset(friendPx.x, friendPx.y - 20f),
                            strokeWidth = 2f
                        )

                        // Draw card container
                        drawRoundRect(
                            color = if (isSystemDark) Color(0xFF1A1A1A) else Color.White,
                            topLeft = topLeft,
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = CornerRadius(20f, 20f),
                            style = androidx.compose.ui.graphics.drawscope.Fill
                        )
                        drawRoundRect(
                            color = markerBaseColor,
                            topLeft = topLeft,
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = CornerRadius(20f, 20f),
                            style = Stroke(width = 2f)
                        )

                        // Draw text initial avatar
                        val textStyle = TextStyle(
                            color = if (isSystemDark) Color.White else Color(0xFF1E293B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val textLayoutResult = textMeasurer.measure(
                            text = AnnotatedString(labelText),
                            style = textStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                                                val txtOffset = Offset(
                            topLeft.x + (pillWidth - textLayoutResult.size.width) / 2f,
                            topLeft.y + (pillHeight - textLayoutResult.size.height) / 2f
                        )
                        drawText(textLayoutResult = textLayoutResult, topLeft = txtOffset)
                    }
                }
            }
        }
    }
}

// Infinite specifications
private fun <T> infiniteSpec(duration: Int): InfiniteRepeatableSpec<T> {
    return infiniteRepeatable(
        animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Restart
    )
}

// Polyline Point list decoder
fun decodePolylinePoints(encoded: String): List<Pair<Double, Double>> {
    val poly = ArrayList<Pair<Double, Double>>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result ushr 1).inv() else result ushr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result ushr 1).inv() else result ushr 1
        lng += dlng

        val pLat = lat.toDouble() / 1E5
        val pLng = lng.toDouble() / 1E5
        poly.add(Pair(pLat, pLng))
    }
    return poly
}
