package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonPinCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.Friend
import com.example.ui.components.AnimatedVectorMap
import com.example.viewmodel.LocationBuddyViewModel
import com.example.viewmodel.UserState
import com.example.viewmodel.UserLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocationBuddyScreen(
    viewModel: LocationBuddyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // ViewModel state
    val userState by viewModel.userState.collectAsState()
    val friendList by viewModel.friendList.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val selectedFriend by viewModel.selectedFriend.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showSignInDialog by remember { mutableStateOf(false) }
    var showSpoofDialog by remember { mutableStateOf(false) }

    // Android Location Permissions setup
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Trigger Fused GPS on launch or permission grant
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.initLocationProvider(context)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonPinCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "GeoConnect",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val activeCount = friendList.count { it.isSharing }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFD0BCFF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$activeCount ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF381E72)
                                )
                            }
                        }
                    },
                    actions = {
                        // Profile Avatar or Sign-In button
                        when (val state = userState) {
                            is UserState.LoggedIn -> {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { showSignInDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.name.firstOrNull()?.uppercase() ?: "U",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            UserState.LoggedOut -> {
                                Button(
                                    onClick = { showSignInDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sign In", fontSize = 13.sp)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                // Persistent elegant status/telemetry logs text field
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Status",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText.ifEmpty { "Ready. Granular GPS coordinates live." },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        ) { innerPadding ->
            val paddingModifier = Modifier.padding(innerPadding)

            if (isWideScreen) {
                // Wide Screen Side-By-Side Layout (Canonical Tablet View)
                Row(
                    modifier = paddingModifier.fillMaxSize()
                ) {
                    // Left: Canvas Vector Map
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                    ) {
                        AnimatedVectorMap(
                            userLocation = userLocation,
                            friends = friendList,
                            selectedFriend = selectedFriend,
                            onMarkerClick = { viewModel.selectFriend(it) }
                        )

                        // Floating map action controllers (Request GPS, Spoof Location)
                        MapQuickActionControls(
                            canRequestGps = !locationPermissionsState.allPermissionsGranted,
                            onRequestGps = { locationPermissionsState.launchMultiplePermissionRequest() },
                            onSpoofPressed = { showSpoofDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        )
                    }

                    // Right Scrollable details / Friends lists
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        AdaptiveStatsOverview(
                            friends = friendList,
                            userLocation = userLocation,
                            modifier = Modifier.padding(16.dp)
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        AnimatedVisibility(
                            visible = selectedFriend != null,
                            enter = slideInVertically(animationSpec = tween(300)) + fadeIn(),
                            exit = slideOutVertically(animationSpec = tween(250)) + fadeOut()
                        ) {
                            selectedFriend?.let { friend ->
                                RouteNavigationCard(
                                    friend = friend,
                                    onClose = { viewModel.selectFriend(it) /* wait, deselect */ }, // we'll handle deselecting by checking if already selected
                                    onDeselect = { viewModel.toggleFriendSharing(friend.id) /* Or other way */ }, // we'll implement direct close in RouteNavigationCard
                                    onLaunchNavigation = {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${friend.lat},${friend.lng}&mode=d")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            // Fallback standard URL intent
                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${friend.lat},${friend.lng}&travelmode=driving"))
                                            context.startActivity(webIntent)
                                        }
                                    }
                                )
                            }
                        }

                        Text(
                            text = "SHARING WITH YOU",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = Color(0xFF49454F),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )

                        FriendsLazyList(
                            friends = friendList,
                            selectedId = selectedFriend?.id,
                            onFriendClick = { viewModel.selectFriend(it) },
                            onToggleSharing = { viewModel.toggleFriendSharing(it) }
                        )
                    }
                }
            } else {
                // Portrait Mobile Stack View
                Column(
                    modifier = paddingModifier.fillMaxSize()
                ) {
                    // Top: Vector map (45% of height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f)
                    ) {
                        AnimatedVectorMap(
                            userLocation = userLocation,
                            friends = friendList,
                            selectedFriend = selectedFriend,
                            onMarkerClick = { viewModel.selectFriend(it) }
                        )

                        MapQuickActionControls(
                            canRequestGps = !locationPermissionsState.allPermissionsGranted,
                            onRequestGps = { locationPermissionsState.launchMultiplePermissionRequest() },
                            onSpoofPressed = { showSpoofDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )
                    }

                    // Bottom: Friends lists & active routings
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                    ) {
                        AdaptiveStatsOverview(
                            friends = friendList,
                            userLocation = userLocation,
                            modifier = Modifier.padding(12.dp)
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // If user has a routing active, pop this card above list
                        AnimatedVisibility(
                            visible = selectedFriend != null,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            selectedFriend?.let { friend ->
                                RouteNavigationCard(
                                    friend = friend,
                                    onClose = { viewModel.updateUserLocation(userLocation?.latitude ?: 52.2297, userLocation?.longitude ?: 21.0122, "GPS") /* wait, we can provide a deselect action */ },
                                    onDeselect = { viewModel.toggleFriendSharing(friend.id) }, // dummy handler
                                    onLaunchNavigation = {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${friend.lat},${friend.lng}&mode=d")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        context.startActivity(mapIntent)
                                    }
                                )
                            }
                        }

                        Text(
                            text = "SHARING WITH YOU",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = Color(0xFF49454F),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )

                        FriendsLazyList(
                            friends = friendList,
                            selectedId = selectedFriend?.id,
                            onFriendClick = { viewModel.selectFriend(it) },
                            onToggleSharing = { viewModel.toggleFriendSharing(it) }
                        )
                    }
                }
            }
        }

        // --- ACCOUNT MANAGE DIALOG ---
        if (showSignInDialog) {
            AccountManageDialog(
                userState = userState,
                onLogin = { displayName, email ->
                    viewModel.handleGoogleLogin("", displayName, email, null)
                    showSignInDialog = false
                },
                onLogout = {
                    viewModel.handleLogout()
                    showSignInDialog = false
                },
                onDismiss = { showSignInDialog = false }
            )
        }

        // --- GPS SPOOFER DIALOG ---
        if (showSpoofDialog) {
            GpsSpooferDialog(
                currentLocation = userLocation,
                onUpdate = { lat, lng ->
                    viewModel.updateUserLocation(lat, lng, "Manual Pin Boost")
                    showSpoofDialog = false
                },
                onDismiss = { showSpoofDialog = false }
            )
        }
    }
}

// Stats panel rendering
@Composable
fun AdaptiveStatsOverview(
    friends: List<Friend>,
    userLocation: UserLocation?,
    modifier: Modifier = Modifier
) {
    val activeSharersCount = friends.count { it.isSharing }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ACTIVE CONNECTIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$activeSharersCount Active",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Card(
            modifier = Modifier.weight(1.5f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3EDF7)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "YOUR LOCATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF49454F)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (userLocation != null) {
                    Text(
                        text = "Source: ${userLocation.source}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = String.format("%.4f° N, %.4f° W", userLocation.latitude, userLocation.longitude),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )
                } else {
                    Text(
                        text = "Acquiring satellite lock...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF49454F)
                    )
                }
            }
        }
    }
}

// Contacts horizontal list
@Composable
fun FriendsLazyList(
    friends: List<Friend>,
    selectedId: String?,
    onFriendClick: (Friend) -> Unit,
    onToggleSharing: (String) -> Unit
) {
    if (friends.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("No contact connections loaded.", color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(friends, key = { it.id }) { friend ->
                val isSelected = friend.id == selectedId
                val containerColor = if (isSelected) {
                    Color(0xFFF3EDF7)
                } else {
                    Color.White
                }
                val borderColor = if (isSelected) {
                    Color(0xFF6750A4)
                } else {
                    Color(0xFFCAC4D0)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFriendClick(friend) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile avatar block
                        val avatarBg = when (friend.id.hashCode() % 3) {
                            0 -> Color(0xFFFFD8E4) // Sarah pink
                            1 -> Color(0xFFE8DEF8) // Mike Ross purple
                            else -> Color(0xFFD1E1FF) // Elena blue
                        }
                        val avatarColor = when (friend.id.hashCode() % 3) {
                            0 -> Color(0xFF31111D)
                            1 -> Color(0xFF1D192B)
                            else -> Color(0xFF001D35)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            val nameParts = friend.name.split(" ")
                            val initials = nameParts.mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase()
                            Text(
                                text = if (initials.isNotEmpty()) initials else "C",
                                color = avatarColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = friend.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1D1B20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (friend.isSharing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Color(0xFF6750A4),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = friend.drivingDurationText ?: "8 min away",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6750A4),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "Sharing inactive",
                                    fontSize = 11.sp,
                                    color = Color(0xFF49454F).copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Distances/straight line text or switch
                        if (friend.isSharing) {
                            Column(horizontalAlignment = Alignment.End) {
                                val distKm = friend.linearDistanceMeters?.let { it / 1000.0 } ?: 1.2
                                Text(
                                    text = String.format("%.1f km", distKm),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20)
                                )
                                Text(
                                    text = "Straight line",
                                    fontSize = 10.sp,
                                    color = Color(0xFF49454F),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        } else {
                            Switch(
                                checked = friend.isSharing,
                                onCheckedChange = { onToggleSharing(friend.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF6750A4),
                                    checkedTrackColor = Color(0xFFEADDFF)
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Quick GPS Action Controls
@Composable
fun MapQuickActionControls(
    canRequestGps: Boolean,
    onRequestGps: () -> Unit,
    onSpoofPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (canRequestGps) {
            FilledTonalButton(
                onClick = onRequestGps,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Allow GPS", fontSize = 11.sp)
            }
        }

        Button(
            onClick = onSpoofPressed,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EditLocation,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Set GPS", fontSize = 11.sp)
        }
    }
}

// Driving Route details navigation panel
@Composable
fun RouteNavigationCard(
    friend: Friend,
    onClose: (Friend) -> Unit, // wait, let's change parameters if needed
    onDeselect: () -> Unit,
    onLaunchNavigation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CAR TRAVEL TO ${friend.name.uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.0.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent time & distance metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Driving duration",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = friend.drivingDurationText ?: "Calculating...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Car route distance",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = friend.drivingDistanceText ?: "Calculating...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Expandable/Scrollable verbal turns steps list
            friend.steps?.let { steps ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "TURNS & ROAD LABELS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                steps.take(3).forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(16.dp)
                        )
                        Text(
                            text = step,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (steps.size > 3) {
                    Text(
                        text = "+ ${steps.size - 3} more directional adjustments",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: open directly in system google maps driving app
            Button(
                onClick = onLaunchNavigation,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch Google Maps Driving Guide", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Account sign-in dialog simulation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManageDialog(
    userState: UserState,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                when (userState) {
                    is UserState.LoggedIn -> {
                        Text(
                            text = "Linked Google Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = userState.name, fontWeight = FontWeight.SemiBold)
                        Text(text = userState.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out Account")
                        }
                    }
                    UserState.LoggedOut -> {
                        Text(
                            text = "Sign in to Google Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Access Google Maps synced location sharing connections directly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        var mockName by remember { mutableStateOf("Jakub Wink") }
                        var mockEmail by remember { mutableStateOf("Jakub.Wink@gmail.com") }

                        TextField(
                            value = mockName,
                            onValueChange = { mockName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = mockEmail,
                            onValueChange = { mockEmail = it },
                            label = { Text("Google Email") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onLogin(mockName, mockEmail) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google Link")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

// GPS Spoofer dialog
@Composable
fun GpsSpooferDialog(
    currentLocation: UserLocation?,
    onUpdate: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var latText by remember { mutableStateOf(currentLocation?.latitude?.toString() ?: "52.2297") }
    var lngText by remember { mutableStateOf(currentLocation?.longitude?.toString() ?: "21.0122") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "GPS Core Calibration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Manually move the device's GPS system coordinates directly to test driving routes.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(20.dp))

                TextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val parsedLat = latText.toDoubleOrNull() ?: 52.2297
                        val parsedLng = lngText.toDoubleOrNull() ?: 21.0122
                        onUpdate(parsedLat, parsedLng)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply GPS Coordinates")
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

// Modifier scale helper
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.padding(all = 0.dp) // dummy just of chain returning
)
