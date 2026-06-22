package com.example.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Subscription
import com.example.data.VpnLog
import com.example.data.VpnServer
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Dynamic Theme getters resolving to AppTheme of selected theme
val CyberBlack: Color @Composable get() = AppTheme.colors.background
val CyberNavy: Color @Composable get() = AppTheme.colors.card
val CyberCard: Color @Composable get() = AppTheme.colors.accentCard
val CyberPrimary: Color @Composable get() = AppTheme.colors.primary
val CyberSecondary: Color @Composable get() = AppTheme.colors.textPrimary
val CyberAccent: Color @Composable get() = AppTheme.colors.accentCard
val CyberTextPrimary: Color @Composable get() = AppTheme.colors.textPrimary
val CyberTextSecondary: Color @Composable get() = AppTheme.colors.textSecondary

val BentoBlack: Color @Composable get() = AppTheme.colors.background
val BentoNavy: Color @Composable get() = AppTheme.colors.card
val BentoCard: Color @Composable get() = AppTheme.colors.card
val BentoAccentCard: Color @Composable get() = AppTheme.colors.accentCard
val BentoPrimary: Color @Composable get() = AppTheme.colors.primary
val BentoSecondary: Color @Composable get() = AppTheme.colors.textPrimary
val BentoAccent: Color @Composable get() = AppTheme.colors.accentCard
val BentoBorder: Color @Composable get() = AppTheme.colors.border
val BentoTextPrimary: Color @Composable get() = AppTheme.colors.textPrimary
val BentoTextSecondary: Color @Composable get() = AppTheme.colors.textSecondary

enum class ScreenTab {
    HOME,
    CONFIGS,
    SECURITY,
    SUBS,
    LOGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnAppScreen(viewModel: VpnViewModel, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(ScreenTab.HOME) }
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "z2ray.com",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            "Android Xray Client",
                            color = CyberPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    val statusText = when (connectionState) {
                        ConnectionState.CONNECTED -> "SECURE • CONNECTED"
                        ConnectionState.CONNECTING -> "TUNNELING..."
                        ConnectionState.DISCONNECTED -> "SHIELD INACTIVE"
                    }
                    val statusColor = when (connectionState) {
                        ConnectionState.CONNECTED -> SecureGreen
                        ConnectionState.CONNECTING -> WarningOrange
                        ConnectionState.DISCONNECTED -> DangerRed
                    }
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = borderStroke(1.dp, statusColor.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberNavy,
                    titleContentColor = CyberTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberNavy,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val items = listOf(
                    NavigationItem("Home", Icons.Filled.Home, ScreenTab.HOME, "tab_home"),
                    NavigationItem("Nodes", Icons.Filled.Dns, ScreenTab.CONFIGS, "tab_configs"),
                    NavigationItem("Security", Icons.Filled.Security, ScreenTab.SECURITY, "tab_security"),
                    NavigationItem("Subs", Icons.Filled.Link, ScreenTab.SUBS, "tab_subscriptions"),
                    NavigationItem("Console", Icons.Filled.Terminal, ScreenTab.LOGS, "tab_logs")
                )

                items.forEach { item ->
                    val selected = selectedTab == item.tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = item.tab },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) CyberPrimary else CyberTextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                color = if (selected) CyberPrimary else CyberTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = CyberCard
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBlack)
        ) {
            // Screen switching with AnimatedVisibility
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    ScreenTab.HOME -> HomeScreen(viewModel, onNavigateToConfigs = { selectedTab = ScreenTab.CONFIGS })
                    ScreenTab.CONFIGS -> ServerListScreen(viewModel)
                    ScreenTab.SECURITY -> SecurityScreen(viewModel)
                    ScreenTab.SUBS -> SubscriptionScreen(viewModel)
                    ScreenTab.LOGS -> LogsConsoleScreen(viewModel)
                }
            }
        }
    }
}

// NAVIGATION STATE HOLDER
data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val tab: ScreenTab,
    val testTag: String
)

// Helper function to create clean custom borders
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)

// Flag helper mapping names/codes to flag emojis
fun getCountryFlagSymbol(name: String): String {
    val uppercase = name.uppercase()
    return when {
        uppercase.contains("GERMANY") || uppercase.contains("FRANKFURT") || uppercase.contains("DE") -> "🇩🇪"
        uppercase.contains("IRAN") || uppercase.contains("TEHRAN") || uppercase.contains("IR") -> "🇮🇷"
        uppercase.contains("FINLAND") || uppercase.contains("FI") -> "🇫🇮"
        uppercase.contains("NETHERLANDS") || uppercase.contains("NL") || uppercase.contains("AMSTERDAM") -> "🇳🇱"
        uppercase.contains("USA") || uppercase.contains("US") || uppercase.contains("AMERICA") || uppercase.contains("NEW YORK") -> "🇺🇸"
        uppercase.contains("TURKEY") || uppercase.contains("TR") || uppercase.contains("ISTANBUL") -> "🇹🇷"
        uppercase.contains("UNITED KINGDOM") || uppercase.contains("UK") || uppercase.contains("LONDON") -> "🇬🇧"
        uppercase.contains("SINGAPORE") || uppercase.contains("SG") -> "🇸🇬"
        uppercase.contains("RUSSIA") || uppercase.contains("RU") -> "🇷🇺"
        else -> "🌐"
    }
}

@Composable
fun HomeScreen(viewModel: VpnViewModel, onNavigateToConfigs: () -> Unit) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedSrv by viewModel.selectedServer.collectAsStateWithLifecycle()
    val dSpeed by viewModel.downloadSpeed.collectAsStateWithLifecycle()
    val uSpeed by viewModel.uploadSpeed.collectAsStateWithLifecycle()
    val seconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val securitySet by viewModel.securitySettings.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleConnection()
        } else {
            viewModel.onVpnPermissionDenied()
        }
    }

    fun requestVpnOrToggle() {
        if (connectionState == ConnectionState.DISCONNECTED) {
            val permissionIntent = VpnService.prepare(context)
            if (permissionIntent != null) {
                vpnPermissionLauncher.launch(permissionIntent)
            } else {
                viewModel.toggleConnection()
            }
        } else {
            viewModel.toggleConnection()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val isKillSwitchActive by viewModel.isKillSwitchActive.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isKillSwitchActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(20.dp),
                    border = borderStroke(1.dp, DangerRed.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Kill Switch Alert",
                            tint = DangerRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "کلید قطع اضطراری فعال است" else "KILL SWITCH ENGAGED",
                                color = DangerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (appLanguage == "fa") "تمام ترافیک ناامن وب قطع شده است تا نشت داده رخ ندهد." else "All unsecured outbound internet traffic is fully blocked to prevent IP exposure leaks.",
                                color = BentoTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Primary Status Card (Bento Area 1)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BentoAccentCard),
                shape = RoundedCornerShape(32.dp),
                border = borderStroke(1.dp, BentoBorder.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Protected Tunnel",
                            color = BentoPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> "Connected"
                                ConnectionState.CONNECTING -> "Connecting..."
                                ConnectionState.DISCONNECTED -> "Shield Inactive"
                            },
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Huge Connection Toggle Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .testTag("power_connect_button")
                            .clickable(
                                onClick = { requestVpnOrToggle() },
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = ripple(
                                    bounded = false,
                                    radius = 70.dp
                                )
                            )
                    ) {
                        // Ambient Glowing Pulse
                        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
                            val pulseColor = BentoPrimary
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            pulseColor.copy(alpha = pulseAlpha * 0.4f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = size.width / 2f
                                    )
                                )
                            }
                        }

                        // Inner Action Toggle Circle
                        Surface(
                            shape = CircleShape,
                            color = if (connectionState == ConnectionState.CONNECTED) BentoPrimary else BentoCard,
                            border = borderStroke(3.dp, if (connectionState == ConnectionState.CONNECTING) WarningOrange else BentoPrimary),
                            modifier = Modifier.size(112.dp),
                            shadowElevation = 8.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PowerSettingsNew,
                                    contentDescription = "Power Shield Switch",
                                    tint = if (connectionState == ConnectionState.CONNECTED) BentoAccentCard else BentoPrimary,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                    }

                    // Network IP Address Pill Indicator
                    Surface(
                        color = BentoBorder.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            val dotColor = when (connectionState) {
                                ConnectionState.CONNECTED -> SecureGreen
                                ConnectionState.CONNECTING -> WarningOrange
                                ConnectionState.DISCONNECTED -> DangerRed
                            }
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = dotColor)
                            }
                            Text(
                                text = if (selectedSrv != null && connectionState == ConnectionState.CONNECTED) {
                                    "${selectedSrv!!.address}:${selectedSrv!!.port}"
                                } else if (connectionState == ConnectionState.CONNECTING) {
                                    "Configuring rules..."
                                } else {
                                    "Stealth Shield Offline"
                                },
                                color = BentoTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Bento Grid Section (2 Rows, 2 Columns)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latency Bento Card
                Card(
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCard),
                    shape = RoundedCornerShape(24.dp),
                    border = borderStroke(1.dp, BentoBorder.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = "Latency Icon",
                                tint = BentoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "LATENCY",
                                color = BentoPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        val latency = selectedSrv?.latency ?: 0
                        val (latencyColor, valStr) = when {
                            latency == 0 -> Pair(BentoTextSecondary, "Check")
                            latency < 0 -> Pair(DangerRed, "Timeout")
                            else -> Pair(BentoTextPrimary, "$latency")
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = valStr,
                                color = latencyColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (latency > 0) {
                                Text(
                                    text = "ms",
                                    color = BentoTextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Encryption / Security Bento Card
                Card(
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCard),
                    shape = RoundedCornerShape(24.dp),
                    border = borderStroke(1.dp, BentoBorder.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Security Icon",
                                tint = BentoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "ENCRYPTION",
                                color = BentoPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        val cipherType = selectedSrv?.protocol?.uppercase() ?: "REALITY"
                        Text(
                            text = if (connectionState == ConnectionState.CONNECTED) "TLS 1.3 / $cipherType" else "TLS 1.3 / X25519",
                            color = BentoTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Download Speed Bento Card
                Card(
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCard),
                    shape = RoundedCornerShape(24.dp),
                    border = borderStroke(1.dp, BentoBorder.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "Download Icon",
                                tint = BentoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "DOWNLOAD",
                                color = BentoPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = if (connectionState == ConnectionState.CONNECTED) dSpeed else "0.0 B/s",
                            color = BentoTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Node/Routing Mode Bento Card
                Card(
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoBorder.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(24.dp),
                    border = borderStroke(1.dp, BentoPrimary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Dns,
                                contentDescription = "Mode Icon",
                                tint = BentoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "MODE",
                                color = BentoPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        val modeStr = if (securitySet.routingMode == "Bypass Iran") "Smart Route" else "Global Proxy"
                        Text(
                            text = modeStr,
                            color = BentoTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Live Speed Metrics Real-time Chart (Recharts style visualization)
        item {
            SpeedChart(viewModel = viewModel)
        }

        // Animated Active Server Picker Bar (Bottom Bento Layer)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BentoCard),
                shape = RoundedCornerShape(20.dp),
                border = borderStroke(1.dp, BentoBorder.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToConfigs() }
                    .testTag("active_server_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Country Flag Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BentoBorder.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val flagSymbol = selectedSrv?.let { getCountryFlagSymbol(it.name) } ?: "🌐"
                            Text(
                                text = flagSymbol,
                                fontSize = 22.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedSrv?.name ?: "No Server Selected",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = selectedSrv?.let { "${it.protocol.lowercase()}-${it.networkType}" } ?: "Tap to choose a node",
                                color = BentoTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ExpandLess,
                        contentDescription = "Expand Nodes list",
                        tint = BentoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedChart(viewModel: VpnViewModel, modifier: Modifier = Modifier) {
    val speedHistory by viewModel.speedHistory.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    if (speedHistory.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            colors = CardDefaults.cardColors(containerColor = BentoCard),
            shape = RoundedCornerShape(24.dp),
            border = borderStroke(1.dp, BentoBorder.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (appLanguage == "fa") "در انتظار اتصال برای ترسیم نمودار..." else "Waiting for active tunnel connections...",
                    color = BentoTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCard),
        shape = RoundedCornerShape(24.dp),
        border = borderStroke(1.dp, BentoBorder.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Graph",
                        tint = BentoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (appLanguage == "fa") "نمودار زنده پهنای باند" else "REAL-TIME BANDWIDTH MONITOR",
                        color = BentoPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                
                // Indicators / Legends
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BentoPrimary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (appLanguage == "fa") "دانلود" else "Down",
                            color = BentoTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SecureGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (appLanguage == "fa") "آپلود" else "Up",
                            color = BentoTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val primaryColor = BentoPrimary
                val secondaryColor = SecureGreen
                val gridColor = BentoBorder.copy(alpha = 0.2f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // 1. Draw helper grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = (height / gridLines) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    if (speedHistory.size < 2) return@Canvas

                    // 2. Find max value to auto-scale charts dynamically (min scale 500 KB/s)
                    val maxVal = maxOf(
                        500f,
                        speedHistory.maxOf { it.downloadSpeedKb },
                        speedHistory.maxOf { it.uploadSpeedKb }
                    ) * 1.15f // 15% head padding

                    val pointsCount = speedHistory.size
                    val stepX = width / (pointsCount - 1)

                    // Draw download speed path
                    val dlPath = androidx.compose.ui.graphics.Path()
                    val dlFillPath = androidx.compose.ui.graphics.Path()
                    
                    // Draw upload speed path
                    val ulPath = androidx.compose.ui.graphics.Path()
                    val ulFillPath = androidx.compose.ui.graphics.Path()

                    for (idx in 0 until pointsCount) {
                        val point = speedHistory[idx]
                        val x = idx * stepX
                        // Convert value to y coordinates (y = 0 at top, y = height at bottom)
                        val dy = height - ((point.downloadSpeedKb / maxVal) * height)
                        val uy = height - ((point.uploadSpeedKb / maxVal) * height)

                        if (idx == 0) {
                            dlPath.moveTo(x, dy)
                            dlFillPath.moveTo(x, height)
                            dlFillPath.lineTo(x, dy)

                            ulPath.moveTo(x, uy)
                            ulFillPath.moveTo(x, height)
                            ulFillPath.lineTo(x, uy)
                        } else {
                            dlPath.lineTo(x, dy)
                            dlFillPath.lineTo(x, dy)

                            ulPath.lineTo(x, uy)
                            ulFillPath.lineTo(x, uy)
                        }

                        if (idx == pointsCount - 1) {
                            dlFillPath.lineTo(x, height)
                            dlFillPath.close()

                            ulFillPath.lineTo(x, height)
                            ulFillPath.close()
                        }
                    }

                    // Render Fill under Down
                    drawPath(
                        path = dlFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )

                    // Render Fill under Up
                    drawPath(
                        path = ulFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(secondaryColor.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )

                    // Render Line Down
                    drawPath(
                        path = dlPath,
                        color = primaryColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 6f
                        )
                    )

                    // Render Line Up
                    drawPath(
                        path = ulPath,
                        color = secondaryColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 5f
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            // Axis labels or bottom text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (appLanguage == "fa") "۳۰ ثانیه قبل" else "30s ago",
                    color = BentoTextSecondary,
                    fontSize = 9.sp
                )
                Text(
                    text = if (appLanguage == "fa") "زنده" else "Live",
                    color = SecureGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(viewModel: VpnViewModel) {
    val scope = rememberCoroutineScope()
    val serversList by viewModel.servers.collectAsStateWithLifecycle()
    val selectedSrv by viewModel.selectedServer.collectAsStateWithLifecycle()
    val isPinging by viewModel.isPingingAll.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var inputLink by remember { mutableStateOf("") }
    var isLinkValid by remember { mutableStateOf<Boolean?>(null) }
    var showImportBackupDialog by remember { mutableStateOf(false) }
    var backupText by remember { mutableStateOf("") }

    val filteredServers = serversList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.address.contains(searchQuery, ignoreCase = true) ||
        it.protocol.contains(searchQuery, ignoreCase = true) ||
        it.groupName.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Search & Bulk Ping Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search nodes...", color = CyberTextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("node_search_input"),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = CyberPrimary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = CyberCard,
                        focusedContainerColor = CyberNavy,
                        unfocusedContainerColor = CyberNavy,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Bulk Ping button
                Button(
                    onClick = { viewModel.testAllServersPing() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                    border = borderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("bulk_ping_button"),
                    enabled = !isPinging
                ) {
                    if (isPinging) {
                        CircularProgressIndicator(color = CyberPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.NetworkCheck, contentDescription = "Bulk Ping", tint = CyberPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Server count indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURATIONS (${filteredServers.size})",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Import",
                        color = CyberPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showImportBackupDialog = true }
                    )
                    if (serversList.isNotEmpty()) {
                        Text(
                            text = "Export",
                            color = SecureGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val exported = viewModel.exportAllConfigsText()
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Z2ray configs", exported))
                                android.widget.Toast.makeText(context, "Configs copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        Text(
                            text = "Clear All",
                            color = DangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.clearAllServers() }
                                .testTag("clear_all_nodes")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredServers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Dns,
                            contentDescription = "Empty Nodes",
                            tint = CyberCard,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No nodes match your query" else "No server configurations added yet.",
                            color = CyberTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredServers, key = { it.id }) { server ->
                        val isSelected = selectedSrv?.id == server.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CyberNavy else CyberCard
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = borderStroke(
                                width = 1.dp,
                                color = if (isSelected) CyberPrimary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectServer(server) }
                                .testTag("node_item_${server.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Status selected radio
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectServer(server) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = CyberPrimary,
                                            unselectedColor = CyberTextSecondary
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.width(4.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = server.name,
                                            color = CyberTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = CyberAccent.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = server.protocol,
                                                    color = CyberPrimary,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "${server.address}:${server.port}",
                                                color = CyberTextSecondary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Group: ${server.groupName}",
                                            color = CyberPrimary.copy(alpha = 0.7f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Latency Ping action & info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val (color, text) = when {
                                        server.latency == 0 -> Pair(CyberTextSecondary, "Check")
                                        server.latency < 0 -> Pair(DangerRed, "Timeout")
                                        server.latency < 150 -> Pair(SecureGreen, "${server.latency}ms")
                                        server.latency < 300 -> Pair(WarningOrange, "${server.latency}ms")
                                        else -> Pair(DangerRed, "${server.latency}ms")
                                    }

                                    Surface(
                                        color = color.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { viewModel.testServerPing(server) }
                                    ) {
                                        Text(
                                            text = text,
                                            color = color,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteServer(server) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Server",
                                            tint = DangerRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating button to add configuration paste link
        FloatingActionButton(
            onClick = {
                inputLink = ""
                isLinkValid = null
                showAddDialog = true
            },
            containerColor = CyberPrimary,
            contentColor = CyberBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_config_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add node configuration")
        }

        // Add Configuration Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Import Secure Configuration", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                showAddDialog = false
                                showQrScanner = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPrimary.copy(alpha = 0.12f),
                                contentColor = CyberPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = borderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("scan_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = "Scan QR",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SCAN SECURE QR CODE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Paste a vmess://, vless://, trojan:// or ss:// shareable configuration link below. Z2ray will automatically parse and save the config parameters locally.",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = inputLink,
                            onValueChange = {
                                inputLink = it
                                isLinkValid = null
                            },
                            placeholder = { Text("vmess://... or vless://...", color = CyberTextSecondary, fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("paste_config_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberPrimary,
                                unfocusedBorderColor = CyberCard,
                                focusedContainerColor = CyberBlack,
                                unfocusedContainerColor = CyberBlack,
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (isLinkValid == false) {
                            Text(
                                "Invalid config link. Unrecognized layout format.",
                                color = DangerRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (isLinkValid == true) {
                            Text(
                                "Configuration Parsed Successfully!",
                                color = SecureGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputLink.trim().isNotEmpty()) {
                                viewModel.addServerFromLink(inputLink.trim()) { success ->
                                    isLinkValid = success
                                    if (success) {
                                        scope.launch {
                                            delay(700)
                                            showAddDialog = false
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = CyberBlack),
                        modifier = Modifier.testTag("submit_config_link_btn")
                    ) {
                        Text("IMPORT")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("CANCEL", color = CyberTextSecondary)
                    }
                },
                containerColor = CyberNavy,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showImportBackupDialog) {
        AlertDialog(
            onDismissRequest = { showImportBackupDialog = false },
            title = { Text("Import Config Backup", color = CyberTextPrimary) },
            text = {
                OutlinedTextField(
                    value = backupText,
                    onValueChange = { backupText = it },
                    label = { Text("Paste vless/vmess/trojan/ss lines or base64 subscription") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = CyberCard,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importConfigsFromText(backupText) { count ->
                        android.widget.Toast.makeText(context, "Imported $count configs", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    backupText = ""
                    showImportBackupDialog = false
                }) { Text("Import", color = CyberPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showImportBackupDialog = false }) { Text("Cancel", color = CyberTextSecondary) }
            },
            containerColor = CyberNavy
        )
    }

    if (showQrScanner) {
            CameraQrScannerDialog(
                onDismiss = { showQrScanner = false },
                onQrScanned = { result ->
                    inputLink = result
                    showQrScanner = false
                    showAddDialog = true
                    viewModel.addServerFromLink(result.trim()) { success ->
                        isLinkValid = success
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(viewModel: VpnViewModel) {
    val settings by viewModel.securitySettings.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val coreVersion by viewModel.coreVersion.collectAsStateWithLifecycle()
    val routingAssetsStatus by viewModel.routingAssetsStatus.collectAsStateWithLifecycle()
    val isUpdatingRoutingAssets by viewModel.isUpdatingRoutingAssets.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (appLanguage == "fa") "پروتکل‌های پیشرفته ضد فیلتر" else "ADVANCED CIRCUMVENTION PROTOCOLS",
                color = CyberPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = if (appLanguage == "fa") "بهینه‌سازی ضافیلترینگ و لایه‌های رمزنگاری ترافیک شبکه همراه و خانگی." else "Optimize anti-filtering and active traffic encryption specifically configured for domestic networks.",
                color = CyberTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }



        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Memory, contentDescription = "Core", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "هسته و فایل‌های مسیریابی" else "Core & Routing Assets",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(coreVersion, color = CyberTextSecondary, fontSize = 11.sp)
                    Text(routingAssetsStatus, color = CyberTextSecondary, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.refreshCoreAndAssetsStatus() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                            border = borderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(if (appLanguage == "fa") "بررسی" else "Check", color = CyberPrimary, fontSize = 12.sp) }
                        Button(
                            onClick = { viewModel.updateRoutingAssets() },
                            enabled = !isUpdatingRoutingAssets,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = CyberBlack),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isUpdatingRoutingAssets) {
                                CircularProgressIndicator(color = CyberBlack, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (appLanguage == "fa") "آپدیت Geo" else "Update Geo", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Theme & Language Settings Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Palette, contentDescription = "Theme", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "شخصی‌سازی و زبان پویا" else "Dynamic Theme & Localization",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Language segment
                    Text(
                        text = if (appLanguage == "fa") "زبان سیستم" else "Interface Language",
                        color = CyberPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf("en" to "English 🇺🇸", "fa" to "فارسی 🇮🇷", "ru" to "Русский 🇷🇺")
                        languages.forEach { (code, label) ->
                            val isSelected = appLanguage == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CyberPrimary else CyberCard)
                                    .clickable { viewModel.selectLanguage(code) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) CyberBlack else CyberTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Theme segment
                    Text(
                        text = if (appLanguage == "fa") "تم رنگی" else "Visual Design Theme",
                        color = CyberPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val themesList = listOf(
                            "Bento Dark" to "Bento Lavender",
                            "Cyberpunk Neon" to "Cyber Neon",
                            "Emerald Secure" to "Emerald Gate",
                            "Midnight Black" to "Midnight OLED"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themesList.take(2).forEach { (themeId, label) ->
                                val isSelected = appTheme == themeId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CyberPrimary else CyberCard)
                                        .clickable { viewModel.selectTheme(themeId) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) CyberBlack else CyberTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themesList.drop(2).forEach { (themeId, label) ->
                                val isSelected = appTheme == themeId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CyberPrimary else CyberCard)
                                        .clickable { viewModel.selectTheme(themeId) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) CyberBlack else CyberTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Connection Diagnostics & Leak Safeguards Card
        item {
            val autoPing by viewModel.autoPingEnabled.collectAsStateWithLifecycle()
            val killSwitch by viewModel.killSwitchEnabled.collectAsStateWithLifecycle()
            
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Leak Safeguards",
                            tint = CyberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "محافظت نشت و ابزار تشخیصی" else "Connection Diagnostics & Leak Protection",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Auto-ping Diagnostic Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = "Active Ping",
                                tint = CyberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (appLanguage == "fa") "تشخیص کیفیت اتصال زنده (پینگ فعال)" else "Active Diagnostics (Auto-Ping)",
                                    color = CyberTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (appLanguage == "fa") "اندازه‌گیری و همگام‌سازی خودکار تاخیر پاسخگویی سرورها برحسب میلی‌ثانیه." else "Periodically pings configurations every 20s to isolate live latencies.",
                                    color = CyberTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Switch(
                            checked = autoPing,
                            onCheckedChange = { viewModel.setAutoPingEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    HorizontalDivider(
                        color = CyberCard,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Kill Switch Safeguard Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = "Kill Switch",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (appLanguage == "fa") "کلید قطع اضطراری محافظتی" else "Censorship Tunnel Kill Switch",
                                    color = CyberTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (appLanguage == "fa") "درصورت قطع اتصال تونل امن، تمام دسترسی به اینترنت را مسدود می‌کند تا از نشت داده‌ها جلوگیری کند." else "Blocks all outbound unsecured traffic if secure tunnel drops.",
                                    color = CyberTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Switch(
                            checked = killSwitch,
                            onCheckedChange = { viewModel.setKillSwitchEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }
                }
            }
        }

        // Per-App Proxy (Split Tunneling) Card
        item {
            val perAppProxyEnabled by viewModel.perAppProxyEnabled.collectAsStateWithLifecycle()
            val bypassIranAppsEnabled by viewModel.bypassIranAppsEnabled.collectAsStateWithLifecycle()
            val appsList by viewModel.appsList.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Apps, contentDescription = "Per-App Proxy", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == "fa") "پراکسی برنامه‌ها" else "Per-App Proxy (Split Tunnel)",
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Switch(
                            checked = perAppProxyEnabled,
                            onCheckedChange = { viewModel.setPerAppProxyEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (appLanguage == "fa") "فقط برنامه‌های انتخاب شده را از فیلترشکن عبور دهید." else "Selectively routing or bypassing specific apps on your device via secure tunnel corridors.",
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )

                    if (perAppProxyEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = CyberCard, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Bypass Domestic Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (appLanguage == "fa") "دور زدن خودکار برنامه‌های داخلی" else "Auto-Bypass Domestic Apps (Iran)",
                                    color = CyberTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (appLanguage == "fa") "اجرا برنامه‌های بانکی و اسنپ بدون فیلترشکن" else "Ensures local bank apps & Snapp execute clean without VPN interface interference.",
                                    color = CyberTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = bypassIranAppsEnabled,
                                onCheckedChange = { viewModel.setBypassIranAppsEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (appLanguage == "fa") "برنامه‌های نصب شده" else "Configured Application Routing Corridors",
                            color = CyberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Render app list
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            appsList.forEach { app ->
                                val isDisabled = bypassIranAppsEnabled && app.isIranian
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDisabled) CyberCard.copy(alpha = 0.4f) else CyberCard)
                                        .clickable(enabled = !isDisabled) {
                                            viewModel.toggleAppProxy(app.packageName)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CyberBlack),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val iconLetter = app.name.take(1)
                                            Text(iconLetter, color = CyberPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = app.name,
                                                color = if (isDisabled) CyberTextSecondary else CyberTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (isDisabled) {
                                                    if (appLanguage == "fa") "دور زدن اجباری بانک" else "Forced Bypass (Local App)"
                                                } else if (app.isProxied) {
                                                    if (appLanguage == "fa") "مسیر عبور: تونل امن Z2ray" else "Route: Tunnel Proxy Corridor"
                                                } else {
                                                    if (appLanguage == "fa") "مسیر عبور: اینترنت مستقیم" else "Route: Direct Internet"
                                                },
                                                color = if (app.isProxied && !isDisabled) SecureGreen else CyberTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Checkbox(
                                        checked = app.isProxied && !isDisabled,
                                        onCheckedChange = { if (!isDisabled) viewModel.toggleAppProxy(app.packageName) },
                                        enabled = !isDisabled,
                                        colors = CheckboxDefaults.colors(checkedColor = CyberPrimary, uncheckedColor = CyberTextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Routing Mode Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = "Routing", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Intelligent Routing Rule Engine", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val routingModes = listOf("Bypass Iran", "Global Proxy", "Direct (Disabled)")
                    routingModes.forEach { mode ->
                        val selected = settings.routingMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSecuritySettings(settings.copy(routingMode = mode))
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(mode, color = CyberTextPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                val desc = when (mode) {
                                    "Bypass Iran" -> "Bypasses domestic websites (.ir) so banking/shopping flows stay direct."
                                    "Global Proxy" -> "Routes all applications and IPs through the server tunnel interface."
                                    else -> "Temporarily disables tunneling client features."
                                }
                                Text(desc, color = CyberTextSecondary, fontSize = 10.sp)
                            }
                            RadioButton(
                                selected = selected,
                                onClick = { viewModel.updateSecuritySettings(settings.copy(routingMode = mode)) },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberPrimary)
                            )
                        }
                    }
                }
            }
        }

        // SNI / Stealth Overrides Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Input, contentDescription = "SNI Stealth", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stealth SNI (Active Payload Cloaking)", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Manipulates the TLS ClientHello Server Name Indication (SNI) header to bypass deep packet filters on Irancell networks.",
                        color = CyberTextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    var inputSni by remember(settings.stealthSnd) { mutableStateOf(settings.stealthSnd) }

                    OutlinedTextField(
                        value = inputSni,
                        onValueChange = {
                            inputSni = it
                            viewModel.updateSecuritySettings(settings.copy(stealthSnd = it))
                        },
                        label = { Text("Stealth SNI Overrides", color = CyberTextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberCard,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Popular bypass SNIs: images.apple.com, assets.github.com, speedtest.net", color = CyberPrimary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Anti-DPI Fragment Packets Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ViewModule, contentDescription = "Fragment", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Anti-DPI Packet Fragmentation", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Switch(
                            checked = settings.enableFragment,
                            onCheckedChange = { viewModel.updateSecuritySettings(settings.copy(enableFragment = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Splits secure ClientHello handshake TLS records into segments which neutralizes Iranian SNI detection scanners.",
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )

                    if (settings.enableFragment) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var sizeVal by remember(settings.fragmentSize) { mutableStateOf(settings.fragmentSize) }
                            var intervalVal by remember(settings.fragmentInterval) { mutableStateOf(settings.fragmentInterval) }

                            OutlinedTextField(
                                value = sizeVal,
                                onValueChange = {
                                    sizeVal = it
                                    viewModel.updateSecuritySettings(settings.copy(fragmentSize = it))
                                },
                                label = { Text("Size Range (bytes)", fontSize = 10.sp, color = CyberTextSecondary) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimary,
                                    unfocusedBorderColor = CyberCard,
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = intervalVal,
                                onValueChange = {
                                    intervalVal = it
                                    viewModel.updateSecuritySettings(settings.copy(fragmentInterval = it))
                                },
                                label = { Text("Delay Range (ms)", fontSize = 10.sp, color = CyberTextSecondary) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimary,
                                    unfocusedBorderColor = CyberCard,
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }
        }

        // Secure DNS Over HTTPS Providers Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SettingsInputHdmi, contentDescription = "DoH DNS", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Secure DNS & DNS-over-HTTPS", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val dnsModes = listOf("Cloudflare DoH", "Google DoH", "Shecan Anti-Sanction", "System DNS")
                    dnsModes.forEach { mode ->
                        val selected = settings.dnsMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSecuritySettings(settings.copy(dnsMode = mode))
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(mode, color = CyberTextPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                val desc = when (mode) {
                                    "Cloudflare DoH" -> "Encrypted queries to Cloudflare securely (highly unblockable)."
                                    "Google DoH" -> "Queries Google DoH endpoints anonymously."
                                    "Shecan Anti-Sanction" -> "Popular Iranian DNS which bypasses foreign tech sanctions."
                                    else -> "Requests default local cellular network routers."
                                }
                                Text(desc, color = CyberTextSecondary, fontSize = 10.sp)
                            }
                            RadioButton(
                                selected = selected,
                                onClick = { viewModel.updateSecuritySettings(settings.copy(dnsMode = mode)) },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberPrimary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionScreen(viewModel: VpnViewModel) {
    val subs by viewModel.subscriptions.collectAsStateWithLifecycle()
    var subName by remember { mutableStateOf("") }
    var subUrl by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SUBSCRIPTION LIST GROUP MANAGER",
                color = CyberPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                "Manage groups of server configs. Decodes base64 sub links dynamically.",
                color = CyberTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Add subscription section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add New Config Subscription", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        IconButton(
                            onClick = { showQrScanner = true },
                            modifier = Modifier.size(24.dp).testTag("scan_sub_qr_icon_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = "Scan subscription QR link",
                                tint = CyberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Group Name (e.g. Free Node Pool)", color = CyberTextSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("sub_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberBlack,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = subUrl,
                        onValueChange = { subUrl = it },
                        label = { Text("Subscription Link URL (must return base64/plain nodes)", color = CyberTextSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("sub_url_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberBlack,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (subName.isNotEmpty() && subUrl.isNotEmpty()) {
                                viewModel.addSubscription(subName, subUrl)
                                subName = ""
                                subUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = CyberBlack),
                        modifier = Modifier.fillMaxWidth().testTag("add_sub_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("IMPORT SUBSCRIPTION FEED")
                    }
                }
            }
        }

        // Automatic Sync Schedule Picker Card
        item {
            val autoUpdateMode by viewModel.subAutoUpdate.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Sync Schedule",
                            tint = CyberPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "برنامه‌ریزی به‌روزرسانی خودکار" else "Automatic Sync Schedule",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (appLanguage == "fa") "تعیین بازه زمانی خودکار برای همگام‌سازی و تحلیل لینک‌های اشتراک." else "Automatically crawl and refresh subscription feed configurations to keep nodes healthy and bypass blocklists.",
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val options = listOf("Disabled", "On App Launch", "1 Hour", "6 Hours", "24 Hours")
                    Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.take(3).forEach { option ->
                            val isSelected = autoUpdateMode == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyberPrimary else CyberCard)
                                    .clickable { viewModel.selectSubAutoUpdate(option) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    color = if (isSelected) CyberBlack else CyberTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.drop(3).forEach { option ->
                            val isSelected = autoUpdateMode == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyberPrimary else CyberCard)
                                    .clickable { viewModel.selectSubAutoUpdate(option) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    color = if (isSelected) CyberBlack else CyberTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Current subscriptions list
        item {
            Text(
                "SAVED FEEDS (${subs.size})",
                color = CyberTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (subs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No automatic server subscriptions added.",
                        color = CyberTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        } else {
            items(subs) { sub ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(12.dp),
                    border = borderStroke(1.dp, CyberCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.name, color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(sub.url, color = CyberTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Nodes: ${sub.serverCount}", color = CyberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (sub.lastUpdated > 0) {
                                    val formattedTime = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(sub.lastUpdated))
                                    Text("Updated: $formattedTime", color = CyberTextSecondary, fontSize = 10.sp)
                                } else {
                                    Text("Status: Never updated", color = WarningOrange, fontSize = 10.sp)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.refreshSubscription(sub) },
                                modifier = Modifier.testTag("refresh_sub_${sub.id}")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh Feed", tint = CyberPrimary)
                            }
                            IconButton(onClick = { viewModel.deleteSubscription(sub) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Feed", tint = DangerRed)
                            }
                        }
                    }
                }
            }
        }
    }

        if (showQrScanner) {
        CameraQrScannerDialog(
            onDismiss = { showQrScanner = false },
            onQrScanned = { result ->
                subUrl = result
                showQrScanner = false
                if (subName.isEmpty()) {
                    subName = "Scanned Feed"
                }
            }
        )
    }
}

@Composable
fun LogsConsoleScreen(viewModel: VpnViewModel) {
    val logsList by viewModel.logs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "DIAGNOSTIC TELEMETRY CONSOLE",
                    color = CyberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    "Realtime encryption handshakes and censorship circumvention diagnostic details.",
                    color = CyberTextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { viewModel.clearLogs() },
                modifier = Modifier.testTag("clear_logs_button")
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear logs", tint = DangerRed)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = CyberBlack,
            shape = RoundedCornerShape(12.dp),
            border = borderStroke(1.dp, CyberCard),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (logsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Console waiting for tunnel session...",
                        color = CyberTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    items(logsList) { log ->
                        val levelColor = when (log.level) {
                            "SUCCESS" -> SecureGreen
                            "WARN" -> WarningOrange
                            "ERROR" -> DangerRed
                            else -> CyberTextSecondary
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "[${log.formattedTime}] ",
                                color = CyberTextSecondary.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${log.tag} ",
                                color = CyberPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "-> ${log.message}",
                                color = levelColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
