package com.z2ray.android.ui

import android.app.Activity
import android.content.Intent
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.z2ray.android.data.Subscription
import com.z2ray.android.data.VpnLog
import com.z2ray.android.data.VpnServer
import com.z2ray.android.ui.theme.*
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
    val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (showOnboarding) {
        AlertDialog(
            onDismissRequest = { viewModel.completeOnboarding() },
            title = { Text("Welcome to Z2ray", color = CyberTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "1) Import configs from QR, link, JSON, or Clash/Mihomo subscription.\n" +
                        "2) Test latency/TLS, then select the best config.\n" +
                        "3) Tune routing: Bypass Iran, Global, Direct, or custom rules.\n" +
                        "4) Keep GeoIP/GeoSite assets updated and use signed releases only.",
                    color = CyberTextSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.completeOnboarding() }, colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = CyberBlack)) {
                    Text("START")
                }
            },
            containerColor = CyberNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Z2ray",
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
                        ConnectionState.DISCONNECTED -> "NOT CONNECTED"
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
            val strings = AppStrings.current
            NavigationBar(
                containerColor = CyberNavy,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val items = listOf(
                    NavigationItem(strings.tabHome, Icons.Filled.Home, ScreenTab.HOME, "tab_home"),
                    NavigationItem(strings.tabConfigs, Icons.Filled.Dns, ScreenTab.CONFIGS, "tab_configs"),
                    NavigationItem(strings.tabSecurity, Icons.Filled.Settings, ScreenTab.SECURITY, "tab_security"),
                    NavigationItem(strings.tabSubs, Icons.Filled.Link, ScreenTab.SUBS, "tab_subscriptions"),
                    NavigationItem(strings.tabConsole, Icons.Filled.Terminal, ScreenTab.LOGS, "tab_logs")
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
                                ConnectionState.DISCONNECTED -> "Not Connected"
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

        // Speed Metrics Section: Download & Upload side-by-side (simplified Home Screen)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Download Speed Card
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
                                text = if (appLanguage == "fa") "دانلود" else if (appLanguage == "ru") "СКАЧАТЬ" else "DOWNLOAD",
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

                // Upload Speed Card
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
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = "Upload Icon",
                                tint = BentoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (appLanguage == "fa") "آپلود" else if (appLanguage == "ru") "ОТПРАВКА" else "UPLOAD",
                                color = BentoPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = if (connectionState == ConnectionState.CONNECTED) uSpeed else "0.0 B/s",
                            color = BentoTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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
                                text = selectedSrv?.let { "${it.protocol.lowercase()}-${it.networkType}" } ?: "Tap to choose a config",
                                color = BentoTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ExpandLess,
                        contentDescription = "Expand Configs list",
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

@Composable
fun CompactSelector(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            color = CyberCard,
            shape = RoundedCornerShape(10.dp),
            border = borderStroke(1.dp, CyberPrimary.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(label.uppercase(), color = CyberTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(value, color = CyberTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = CyberNavy) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = if (option == value) CyberPrimary else CyberTextPrimary, fontSize = 12.sp) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
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
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var inputLink by remember { mutableStateOf("") }
    var isLinkValid by remember { mutableStateOf<Boolean?>(null) }
    var showImportBackupDialog by remember { mutableStateOf(false) }
    var backupText by remember { mutableStateOf("") }
    var protocolFilter by remember { mutableStateOf("All") }
    var groupFilter by remember { mutableStateOf("All") }
    var sortMode by remember { mutableStateOf("Latency") }
    var detailServer by remember { mutableStateOf<VpnServer?>(null) }
    var editServer by remember { mutableStateOf<VpnServer?>(null) }
    var qrServer by remember { mutableStateOf<VpnServer?>(null) }
    var selectModeEnabled by remember { mutableStateOf(false) }
    var selectedServerIds by remember { mutableStateOf(setOf<Int>()) }
    var showManualCreateDialog by remember { mutableStateOf(false) }

    val protocols = remember(serversList) { listOf("All") + serversList.map { it.protocol }.distinct().sorted() }
    val groups = remember(serversList) { listOf("All") + serversList.map { it.groupName }.distinct().sorted() }
    val filteredServers = serversList.filter {
        val queryMatches = searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true) ||
            it.protocol.contains(searchQuery, ignoreCase = true) ||
            it.groupName.contains(searchQuery, ignoreCase = true) ||
            it.sni.contains(searchQuery, ignoreCase = true) ||
            it.host.contains(searchQuery, ignoreCase = true)
        queryMatches && (protocolFilter == "All" || it.protocol == protocolFilter) && (groupFilter == "All" || it.groupName == groupFilter)
    }.let { list ->
        when (sortMode) {
            "Name" -> list.sortedBy { it.name.lowercase() }
            "Protocol" -> list.sortedWith(compareBy<VpnServer> { it.protocol }.thenBy { it.name })
            "Group" -> list.sortedWith(compareBy<VpnServer> { it.groupName }.thenBy { it.name })
            "Newest" -> list.sortedByDescending { it.createdAt }
            else -> list.sortedWith(compareBy<VpnServer> { if (it.latency <= 0) Int.MAX_VALUE else it.latency }.thenBy { it.name })
        }
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
                    placeholder = { Text("Search configs...", color = CyberTextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("config_search_input"),
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

                Button(
                    onClick = { viewModel.selectBestServerByLatency(if (groupFilter == "All") null else groupFilter) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                    border = borderStroke(1.dp, SecureGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(10.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = "Select Fastest", tint = SecureGreen, modifier = Modifier.size(22.dp))
                }

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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactSelector("Protocol", protocolFilter, protocols, Modifier.weight(1f)) { protocolFilter = it }
                CompactSelector("Group", groupFilter, groups, Modifier.weight(1f)) { groupFilter = it }
                CompactSelector("Sort", sortMode, listOf("Latency", "Name", "Protocol", "Group", "Newest"), Modifier.weight(1f)) { sortMode = it }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row for quick import, manual create and selection delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = clipboard.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val text = clipData.getItemAt(0).text?.toString().orEmpty()
                            if (text.isNotBlank()) {
                                viewModel.addServerFromLink(text) { success ->
                                    if (success) {
                                        android.widget.Toast.makeText(context, if (appLanguage == "fa") "کانفیگ با موفقیت اضافه شد!" else "Import Success!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, if (appLanguage == "fa") "پیکربندی نامعتبر است!" else "Invalid config format!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                android.widget.Toast.makeText(context, if (appLanguage == "fa") "کلیپ‌بورد خالی است!" else "Clipboard is empty!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, if (appLanguage == "fa") "کلیپ‌بورد خالی است!" else "Clipboard is empty!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                    border = borderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = "Clipboard", tint = CyberPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (appLanguage == "fa") "کلیپ‌بورد" else if (appLanguage == "ru") "Из буфера" else "Clipboard", color = CyberTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showManualCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                    border = borderStroke(1.dp, SecureGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Manual Create", tint = SecureGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (appLanguage == "fa") "ساخت دستی" else if (appLanguage == "ru") "Вручную" else "Manual", color = CyberTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                val isSelectMode = selectModeEnabled
                Button(
                    onClick = {
                        selectModeEnabled = !selectModeEnabled
                        if (!selectModeEnabled) {
                            selectedServerIds = emptySet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSelectMode) DangerRed.copy(alpha = 0.2f) else CyberCard),
                    border = borderStroke(1.dp, if (isSelectMode) DangerRed else CyberTextSecondary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Select", tint = if (isSelectMode) DangerRed else CyberTextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSelectMode) {
                            if (appLanguage == "fa") "لغو انتخاب" else if (appLanguage == "ru") "Отмена" else "Cancel"
                        } else {
                            if (appLanguage == "fa") "انتخاب حذف" else if (appLanguage == "ru") "Выбрать" else "Select"
                        },
                        color = CyberTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Server count indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGS (${filteredServers.size})",
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
                                .testTag("clear_all_configs")
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
                            contentDescription = "Empty Configs",
                            tint = CyberCard,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No configs match your query" else "No configs added yet.",
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
                        val isBatchSelected = selectedServerIds.contains(server.id)
                        val cardBg = when {
                            isBatchSelected -> DangerRed.copy(alpha = 0.12f)
                            isSelected -> CyberNavy
                            else -> CyberCard
                        }
                        val cardBorder = when {
                            isBatchSelected -> DangerRed
                            isSelected -> CyberPrimary
                            else -> Color.Transparent
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = cardBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = borderStroke(
                                width = 1.dp,
                                color = cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectModeEnabled) {
                                        selectedServerIds = if (isBatchSelected) {
                                            selectedServerIds - server.id
                                        } else {
                                            selectedServerIds + server.id
                                        }
                                    } else {
                                        viewModel.selectServer(server)
                                    }
                                }
                                .testTag("config_item_${server.id}")
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
                                    if (selectModeEnabled) {
                                        Checkbox(
                                            checked = isBatchSelected,
                                            onCheckedChange = { checked ->
                                                selectedServerIds = if (checked == true) {
                                                    selectedServerIds + server.id
                                                } else {
                                                    selectedServerIds - server.id
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = DangerRed,
                                                uncheckedColor = CyberTextSecondary
                                            )
                                        )
                                    } else {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectServer(server) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = CyberPrimary,
                                                unselectedColor = CyberTextSecondary
                                            )
                                        )
                                    }
                                    
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
                                        onClick = { detailServer = server },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = "Config Details",
                                            tint = CyberPrimary.copy(alpha = 0.9f),
                                            modifier = Modifier.size(18.dp)
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
            Icon(Icons.Filled.Add, contentDescription = "Add config")
        }

        editServer?.let { server ->
            EditServerDialog(
                server = server,
                appLanguage = appLanguage,
                onDismiss = { editServer = null },
                onDelete = {
                    viewModel.deleteServer(server)
                    editServer = null
                    detailServer = null
                },
                onSave = {
                    viewModel.updateServer(it)
                    editServer = null
                    detailServer = it
                }
            )
        }

        qrServer?.let { server ->
            QrShareDialog(server = server, onDismiss = { qrServer = null })
        }

        if (selectModeEnabled && selectedServerIds.isNotEmpty()) {
            Surface(
                color = DangerRed.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "fa") "حذف ${selectedServerIds.size} کانفیگ انتخاب شده؟"
                               else if (appLanguage == "ru") "Удалить ${selectedServerIds.size} селектированных?"
                               else "Delete ${selectedServerIds.size} selected configs?",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            selectedServerIds.forEach { id ->
                                serversList.firstOrNull { it.id == id }?.let { server ->
                                    viewModel.deleteServer(server)
                                }
                            }
                            selectedServerIds = emptySet()
                            selectModeEnabled = false
                            android.widget.Toast.makeText(context, if (appLanguage == "fa") "کانفیگ‌ها حذف شدند" else "Deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DangerRed),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (appLanguage == "fa") "حذف قطعی" else if (appLanguage == "ru") "Да, Удалить" else "Confirm Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showManualCreateDialog) {
            var mName by remember { mutableStateOf("Manual Server") }
            var mProtocol by remember { mutableStateOf("VLESS") }
            var mAddress by remember { mutableStateOf("") }
            var mPort by remember { mutableStateOf("443") }
            var mUuid by remember { mutableStateOf("") }
            var mSecurity by remember { mutableStateOf("reality") }
            var mNetwork by remember { mutableStateOf("tcp") }
            var mSni by remember { mutableStateOf("") }
            var mPath by remember { mutableStateOf("") }
            var mPublicKey by remember { mutableStateOf("") }
            var mShortId by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showManualCreateDialog = false },
                title = { Text(if (appLanguage == "fa") "ساخت دستی کانفیگ" else "Create Manual Config", color = CyberTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                        item { EditField(if (appLanguage == "fa") "نام" else "Name", mName) { mName = it } }
                        item {
                            Column {
                                Text(if (appLanguage == "fa") "پروتکل" else "Protocol", color = CyberTextSecondary, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("VLESS", "VMESS", "TROJAN", "SHADOWSOCKS").forEach { p ->
                                        val isSelected = mProtocol == p
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) CyberPrimary else CyberCard)
                                                .clickable {
                                                    mProtocol = p
                                                    if (p == "SHADOWSOCKS") mSecurity = "aes-128-gcm"
                                                    else if (p == "TROJAN") mSecurity = "tls"
                                                    else mSecurity = "reality"
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(p, color = if (isSelected) CyberBlack else CyberTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        item { EditField(if (appLanguage == "fa") "آدرس سرور (IP/Host)" else "Server Address", mAddress) { mAddress = it } }
                        item { EditField(if (appLanguage == "fa") "پورت" else "Port", mPort) { mPort = it.filter { c -> c.isDigit() } } }
                        item { EditField(if (appLanguage == "fa") "شناسه UUID / رمز عبور" else "UUID / Password", mUuid) { mUuid = it } }
                        item { EditField(if (appLanguage == "fa") "امنیت (tls / reality / none)" else "Security", mSecurity) { mSecurity = it } }
                        item { EditField(if (appLanguage == "fa") "نوع شبکه (tcp / ws / grpc)" else "Transport Network", mNetwork) { mNetwork = it } }
                        item { EditField("SNI", mSni) { mSni = it } }
                        item { EditField(if (appLanguage == "fa") "مسیر (Path)" else "Path", mPath) { mPath = it } }
                        if (mSecurity.lowercase() == "reality") {
                            item { EditField(if (appLanguage == "fa") "کلید عمومی (Public Key)" else "Reality Public Key", mPublicKey) { mPublicKey = it } }
                            item { EditField(if (appLanguage == "fa") "شناسه کوتاه (Short ID)" else "Reality Short ID", mShortId) { mShortId = it } }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (mAddress.isNotBlank()) {
                                viewModel.addManualServer(
                                    VpnServer(
                                        name = mName.ifBlank { "Manual $mProtocol" },
                                        address = mAddress,
                                        port = mPort.toIntOrNull() ?: 443,
                                        uuid = mUuid,
                                        protocol = mProtocol,
                                        security = mSecurity,
                                        networkType = mNetwork,
                                        sni = mSni,
                                        path = mPath,
                                        publicKey = mPublicKey,
                                        shortId = mShortId,
                                        groupName = if (appLanguage == "fa") "ساخت دستی" else "Manual Configs",
                                        isCustom = true,
                                        originalLink = ""
                                    )
                                )
                                showManualCreateDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = CyberBlack),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (appLanguage == "fa") "ایجاد کانفیگ" else "CREATE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualCreateDialog = false }) {
                        Text(if (appLanguage == "fa") "لغو" else "CANCEL", color = CyberTextSecondary)
                    }
                },
                containerColor = CyberNavy,
                shape = RoundedCornerShape(20.dp)
            )
        }

        detailServer?.let { server ->
            ServerDetailsDialog(
                server = server,
                onDismiss = { detailServer = null },
                onCopy = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Z2ray config", server.originalLink.ifBlank { server.rawJson }))
                    android.widget.Toast.makeText(context, "Config copied", android.widget.Toast.LENGTH_SHORT).show()
                },
                onShare = {
                    val text = server.originalLink.ifBlank { server.rawJson }
                    if (text.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Z2ray config"))
                    }
                },
                onQr = { qrServer = server },
                onEdit = { editServer = server },
                onTlsTest = { viewModel.testTlsHandshake(server) },
                onUdpTest = { viewModel.testUdpProbe(server) },
                onDelete = {
                    viewModel.deleteServer(server)
                    detailServer = null
                }
            )
        }

        // Add Configuration Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Import Configuration", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                                "SCAN CONFIG QR CODE",
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

@Composable
fun ServerDetailsDialog(
    server: VpnServer,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onQr: () -> Unit,
    onEdit: () -> Unit,
    onTlsTest: () -> Unit,
    onUdpTest: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(server.name, color = CyberTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 420.dp)) {
                val rows = listOf(
                    "Protocol" to server.protocol,
                    "Group" to server.groupName,
                    "Address" to "${server.address}:${server.port}",
                    "Security" to server.security,
                    "Transport" to server.networkType,
                    "SNI" to server.sni,
                    "Host" to server.host,
                    "Path/Service" to server.path.ifBlank { server.serviceName },
                    "Flow" to server.flow,
                    "Fingerprint" to server.fingerprint,
                    "Public Key" to server.publicKey,
                    "Short ID" to server.shortId,
                    "ALPN" to server.alpn,
                    "Pinned Cert" to server.pinnedPeerCertSha256,
                    "Latency" to if (server.latency > 0) "${server.latency}ms" else "Not tested"
                ).filter { it.second.isNotBlank() }
                items(rows) { (key, value) ->
                    Column {
                        Text(key.uppercase(), color = CyberTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(value, color = CyberTextPrimary, fontSize = 12.sp, fontFamily = if (value.length > 24) FontFamily.Monospace else FontFamily.Default)
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onTlsTest) { Text("TLS", color = CyberPrimary) }
                    TextButton(onClick = onUdpTest) { Text("UDP", color = WarningOrange) }
                    TextButton(onClick = onEdit) { Text("EDIT", color = CyberPrimary) }
                    TextButton(onClick = onQr) { Text("QR", color = SecureGreen) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onShare) { Text("SHARE", color = SecureGreen) }
                    TextButton(onClick = onCopy) { Text("COPY", color = SecureGreen) }
                    TextButton(onClick = onDelete) { Text("DELETE", color = DangerRed) }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = CyberTextSecondary) } },
        containerColor = CyberNavy,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun QrShareDialog(server: VpnServer, onDismiss: () -> Unit) {
    val text = server.originalLink.ifBlank { server.rawJson }
    val bitmap = remember(text) { if (text.isNotBlank()) runCatching { QrCodeGenerator.create(text) }.getOrNull() else null }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share QR", color = CyberTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Configuration QR",
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Text("This profile has no original share link/raw JSON to encode.", color = DangerRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = CyberPrimary) } },
        containerColor = CyberNavy,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServerDialog(
    server: VpnServer,
    appLanguage: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (VpnServer) -> Unit
) {
    var name by remember(server.id) { mutableStateOf(server.name) }
    var address by remember(server.id) { mutableStateOf(server.address) }
    var port by remember(server.id) { mutableStateOf(server.port.toString()) }
    var uuid by remember(server.id) { mutableStateOf(server.uuid) }
    var protocol by remember(server.id) { mutableStateOf(server.protocol) }
    var security by remember(server.id) { mutableStateOf(server.security) }
    var network by remember(server.id) { mutableStateOf(server.networkType) }
    var sni by remember(server.id) { mutableStateOf(server.sni) }
    var host by remember(server.id) { mutableStateOf(server.host) }
    var path by remember(server.id) { mutableStateOf(server.path) }
    var flow by remember(server.id) { mutableStateOf(server.flow) }
    var publicKey by remember(server.id) { mutableStateOf(server.publicKey) }
    var shortId by remember(server.id) { mutableStateOf(server.shortId) }
    var fingerprint by remember(server.id) { mutableStateOf(server.fingerprint) }
    var alpn by remember(server.id) { mutableStateOf(server.alpn) }

    val titleStr = when (appLanguage) {
        "fa" -> "ویرایش پیکربندی"
        "ru" -> "Редактировать конфигурацию"
        else -> "Edit Configuration"
    }

    val saveStr = when (appLanguage) {
        "fa" -> "ذخیره"
        "ru" -> "СОХРАНИТЬ"
        else -> "SAVE"
    }

    val deleteStr = when (appLanguage) {
        "fa" -> "حذف"
        "ru" -> "УДАЛИТЬ"
        else -> "DELETE"
    }

    val closeStr = when (appLanguage) {
        "fa" -> "بستن"
        "ru" -> "ЗАКРЫТЬ"
        else -> "CLOSE"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleStr, color = CyberTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 500.dp)) {
                item { EditField(if (appLanguage == "fa") "نام" else if (appLanguage == "ru") "Имя" else "Name", name) { name = it } }
                item { EditField(if (appLanguage == "fa") "پروتکل" else if (appLanguage == "ru") "Протокол" else "Protocol", protocol) { protocol = it.uppercase() } }
                item { EditField(if (appLanguage == "fa") "آدرس سرور" else if (appLanguage == "ru") "Адрес" else "Address", address) { address = it } }
                item { EditField(if (appLanguage == "fa") "پورت" else if (appLanguage == "ru") "Порт" else "Port", port) { port = it.filter { c -> c.isDigit() } } }
                item { EditField(if (appLanguage == "fa") "شناسه UUID / رمز عبور" else if (appLanguage == "ru") "UUID / Пароль" else "UUID / Password", uuid) { uuid = it } }
                item { EditField(if (appLanguage == "fa") "امنیت (Security)" else if (appLanguage == "ru") "Безопасность" else "Security", security) { security = it.lowercase() } }
                item { EditField(if (appLanguage == "fa") "ترانسپورت (Network)" else if (appLanguage == "ru") "Транспорт" else "Transport", network) { network = it.lowercase() } }
                item { EditField("SNI", sni) { sni = it } }
                item { EditField(if (appLanguage == "fa") "هاست / Authority" else if (appLanguage == "ru") "Хост / Authority" else "Host / Authority", host) { host = it } }
                item { EditField(if (appLanguage == "fa") "مسیر / Service" else if (appLanguage == "ru") "Путь / Service" else "Path / Service", path) { path = it } }
                item { EditField("Flow", flow) { flow = it } }
                item { EditField(if (appLanguage == "fa") "کلید عمومی Reality" else if (appLanguage == "ru") "Reality Public Key" else "Reality Public Key", publicKey) { publicKey = it } }
                item { EditField(if (appLanguage == "fa") "شناسه کوتاه Reality Short ID" else if (appLanguage == "ru") "Reality Short ID" else "Reality Short ID", shortId) { shortId = it } }
                item { EditField("Fingerprint", fingerprint) { fingerprint = it } }
                item { EditField("ALPN", alpn) { alpn = it } }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        server.copy(
                            name = name.ifBlank { server.name },
                            address = address.ifBlank { server.address },
                            port = port.toIntOrNull() ?: server.port,
                            uuid = uuid,
                            protocol = protocol.ifBlank { server.protocol },
                            security = security.ifBlank { server.security },
                            networkType = network.ifBlank { server.networkType },
                            sni = sni,
                            host = host,
                            path = path,
                            serviceName = path,
                            flow = flow,
                            publicKey = publicKey,
                            shortId = shortId,
                            fingerprint = fingerprint,
                            alpn = alpn
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = CyberBlack),
                shape = RoundedCornerShape(10.dp)
            ) { Text(saveStr, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                ) {
                    Text(deleteStr, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberTextSecondary)
                ) {
                    Text(closeStr)
                }
            }
        },
        containerColor = CyberNavy,
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = CyberTextSecondary, fontSize = 11.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(viewModel: VpnViewModel) {
    val settings by viewModel.securitySettings.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val coreVersion by viewModel.coreVersion.collectAsStateWithLifecycle()
    val routingAssetsStatus by viewModel.routingAssetsStatus.collectAsStateWithLifecycle()
    val geoDataSource by viewModel.geoDataSource.collectAsStateWithLifecycle()
    val isUpdatingRoutingAssets by viewModel.isUpdatingRoutingAssets.collectAsStateWithLifecycle()
    val customRoutingRules by viewModel.customRoutingRules.collectAsStateWithLifecycle()
    val lastUrlTest by viewModel.lastUrlTestResult.collectAsStateWithLifecycle()
    val lastDownloadTest by viewModel.lastDownloadTestResult.collectAsStateWithLifecycle()
    val isRunningNetworkTest by viewModel.isRunningNetworkTest.collectAsStateWithLifecycle()
    var showRoutingImportDialog by remember { mutableStateOf(false) }
    var routingImportText by remember { mutableStateOf("") }
    var autoConnectOnBoot by remember { mutableStateOf(false) }
    var speedInNotification by remember { mutableStateOf(true) }
    var keepAliveActive by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (appLanguage == "fa") "تنظیمات پیشرفته هسته و سیستم" else if (appLanguage == "ru") "РАСШИРЕННЫЕ НАСТРОЙКИ ЯДРА" else "ADVANCED CORE & SYSTEM SETTINGS",
                color = CyberPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = if (appLanguage == "fa") "پیکربندی کامل پارامترهای پروتکل، اتصالات محلی و قوانین فیلترینگ منطبق بر ویتوری." 
                       else if (appLanguage == "ru") "Полная конфигурация локальных портов, стратегий маршрутизации, DNS, uTLS и MUX."
                       else "Configure routing domain strategies, local ports, DNS query, fakeDNS, MUX multiplexing, and certificate checks.",
                color = CyberTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Card 1: Local port and Connection Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Build, contentDescription = "Core Settings", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "اتصالات محلی و هسته" else if (appLanguage == "ru") "Локальные порты и Ядро" else "Core Connection & Port Settings",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // SOCKS Port
                    var tempPort by remember(settings.localSocksPort) { mutableStateOf(settings.localSocksPort) }
                    OutlinedTextField(
                        value = tempPort,
                        onValueChange = {
                            tempPort = it
                            viewModel.updateSecuritySettings(settings.copy(localSocksPort = it.filter { c -> c.isDigit() }))
                        },
                        label = { Text(if (appLanguage == "fa") "پورت محلی SOCKS" else if (appLanguage == "ru") "Локальный порт SOCKS" else "Local SOCKS Port", color = CyberTextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberCard,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        )
                    )

                    // Enable Sniffing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "ردیابی هوشمند مقصد (Traffic Sniffing)" else if (appLanguage == "ru") "Распознавание трафика (Sniffing)" else "Enable Traffic Sniffing",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "تشخیص مقصد دامنه‌ها در استریم برای هدایت دقیق ترافیک (توصیه شده)." else if (appLanguage == "ru") "Определяет доменные имена в потоке для точной маршрутизации." else "Detects domain names from raw stream to apply correct routing rules.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settings.enableSniffing,
                            onCheckedChange = { viewModel.updateSecuritySettings(settings.copy(enableSniffing = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    HorizontalDivider(color = CyberCard, thickness = 1.dp)

                    // Domain Strategy selector
                    Column {
                        Text(
                            text = if (appLanguage == "fa") "استراتژی مسیریابی دامنه" else if (appLanguage == "ru") "Стратегия разрешения доменов" else "Domain Resolution Strategy",
                            color = CyberTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("AsIs", "IPIfNonMatch", "IPOnDemand").forEach { strategy ->
                                val isSelected = settings.routingDnsStrategy == strategy
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberPrimary else CyberCard)
                                        .clickable { viewModel.updateSecuritySettings(settings.copy(routingDnsStrategy = strategy)) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(strategy, color = if (isSelected) CyberBlack else CyberTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CyberCard, thickness = 1.dp)

                    // Log level selector
                    Column {
                        Text(
                            text = if (appLanguage == "fa") "سطح ثبت گزارشات هسته" else if (appLanguage == "ru") "Уровень логов Ядра" else "Xray Core Log Level",
                            color = CyberTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("warning", "info", "debug", "none").forEach { level ->
                                val isSelected = settings.logLevel == level
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberPrimary else CyberCard)
                                        .clickable { viewModel.updateSecuritySettings(settings.copy(logLevel = level)) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(level.uppercase(), color = if (isSelected) CyberBlack else CyberTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 2: DNS & FakeDNS settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Dns, contentDescription = "DNS Settings", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "تنظیمات دی‌ان‌اس (DNS & FakeDNS)" else if (appLanguage == "ru") "Настройки DNS и FakeDNS" else "Secure DNS & FakeDNS Settings",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Enable FakeDNS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "فعال‌سازی FakeDNS" else if (appLanguage == "ru") "Включить FakeDNS сопоставление" else "Enable FakeDNS Mapping",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "کاهش بار فرآیند پاسخگویی DNS و رفع نشتی آی‌پی با ترسیم آدرس‌های موقت." else if (appLanguage == "ru") "Сопоставляет фейковые IP доменам для обхода задержек и утечек DNS." else "Maps fake IP addresses to domains to reduce query latency and bypass leaks.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settings.enableFakeDns,
                            onCheckedChange = { viewModel.updateSecuritySettings(settings.copy(enableFakeDns = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    HorizontalDivider(color = CyberCard, thickness = 1.dp)

                    // Remote DNS input
                    var tempRemoteDns by remember(settings.remoteDns) { mutableStateOf(settings.remoteDns) }
                    OutlinedTextField(
                        value = tempRemoteDns,
                        onValueChange = {
                            tempRemoteDns = it
                            viewModel.updateSecuritySettings(settings.copy(remoteDns = it))
                        },
                        label = { Text(if (appLanguage == "fa") "سرور DNS خارجی / امن (Remote DNS)" else if (appLanguage == "ru") "Удаленный DNS-сервер" else "Remote DNS Server", color = CyberTextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberCard,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        )
                    )

                    // Domestic DNS input
                    var tempDirectDns by remember(settings.directDns) { mutableStateOf(settings.directDns) }
                    OutlinedTextField(
                        value = tempDirectDns,
                        onValueChange = {
                            tempDirectDns = it
                            viewModel.updateSecuritySettings(settings.copy(directDns = it))
                        },
                        label = { Text(if (appLanguage == "fa") "سرور DNS داخلی / مستقیم (Domestic DNS)" else if (appLanguage == "ru") "Локальный/Домашний DNS-сервер" else "Direct/Domestic DNS Server", color = CyberTextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberCard,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        )
                    )
                }
            }
        }

        // Card 3: Advanced Optimization & MUX
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, contentDescription = "MUX Settings", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "تنظیمات بهینه‌سازی مالتی‌پلکس (MUX)" else if (appLanguage == "ru") "Оптимизация трафика и MUX" else "Advanced Optimization & MUX",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Enable MUX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "فعال‌سازی مالتی‌پلکسر MUX" else if (appLanguage == "ru") "Включить MUX мультиплексирование" else "Enable MUX Multiplexing",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "ادغام اتصال‌های TCP در کانال واحد برای کاهش سربار دست‌دهی (توصیه شده فقط برای VMess)." else if (appLanguage == "ru") "Объединяет несколько TCP-соединений в один канал для снижения задержки." else "Consolidates multiple TCP connections inside a single channel to reduce handshake overhead.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settings.enableMux,
                            onCheckedChange = { viewModel.updateSecuritySettings(settings.copy(enableMux = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    if (settings.enableMux) {
                        // Mux Concurrency
                        var tempMux by remember(settings.muxConcurrency) { mutableStateOf(settings.muxConcurrency) }
                        OutlinedTextField(
                            value = tempMux,
                            onValueChange = {
                                tempMux = it
                                viewModel.updateSecuritySettings(settings.copy(muxConcurrency = it.filter { c -> c.isDigit() }))
                            },
                            label = { Text(if (appLanguage == "fa") "حداکثر اتصالات همزمان MUX" else if (appLanguage == "ru") "Макс. параллельность MUX (1..16)" else "MUX Concurrency (1..16)", color = CyberTextSecondary, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberPrimary,
                                unfocusedBorderColor = CyberCard,
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = CyberCard, thickness = 1.dp)

                    // Skip cert verify / Allow Insecure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "اجازه اتصال‌های ناامن SSL (نامعتبر)" else if (appLanguage == "ru") "Разрешить небезопасные соединения" else "Allow Insecure Connections",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "نادیده گرفتن اعتبارسنجی گواهی‌های منقضی یا ساختگی SSL (جلوگیری از قطع اتصال با جعل‌های داخلی)." else if (appLanguage == "ru") "Пропускает валидацию SSL-сертификатов для предотвращения сбоев." else "Bypasses SSL certificate verification to avoid failures caused by localized fake cert injections.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settings.allowInsecure,
                            onCheckedChange = { viewModel.updateSecuritySettings(settings.copy(allowInsecure = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    HorizontalDivider(color = CyberCard, thickness = 1.dp)

                    // TLS Client Fingerprint selector
                    Column {
                        Text(
                            text = if (appLanguage == "fa") "اثرانگشت مرورگر (uTLS Fingerprint)" else if (appLanguage == "ru") "uTLS Иммитация Браузера" else "uTLS Client Fingerprint",
                            color = CyberTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("chrome", "firefox", "safari", "random").forEach { fp ->
                                val isSelected = settings.fingerprint == fp
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberPrimary else CyberCard)
                                        .clickable { viewModel.updateSecuritySettings(settings.copy(fingerprint = fp)) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(fp.uppercase(), color = if (isSelected) CyberBlack else CyberTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Anti-DPI & Packet Fragmentation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = "Fragment", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "تکه‌تکه‌کردن پکت‌ها (Anti-DPI / Fragment)" else if (appLanguage == "ru") "Фрагментация пакетов (Anti-DPI)" else "Anti-DPI Packet Fragmentation",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "فعال‌سازی مالتی‌پلکسر فرگمنت" else if (appLanguage == "ru") "Включить фрагментацию пакетов" else "Enable Handshake Fragmentation",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "تکه‌تکه‌کردن بسته ClientHello برای رد شدن از دیوار آتشین فیلترینگ شدید اپراتورها." else if (appLanguage == "ru") "Разделяет защищенный пакет ClientHello для нейтрализации систем DPI." else "Neutralizes SNI detectors by splitting initial secure handshakes.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settings.enableFragment,
                            onCheckedChange = { viewModel.updateSecuritySettings(settings.copy(enableFragment = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    if (settings.enableFragment) {
                        Spacer(modifier = Modifier.height(8.dp))
                        EditField(if (appLanguage == "fa") "محدوده اندازه (بایت)" else "Size Range (bytes)", settings.fragmentSize) {
                            viewModel.updateSecuritySettings(settings.copy(fragmentSize = it))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        EditField(if (appLanguage == "fa") "محدوده تاخیر (میلی‌ثانیه)" else "Delay Range (ms)", settings.fragmentInterval) {
                            viewModel.updateSecuritySettings(settings.copy(fragmentInterval = it))
                        }
                    }
                }
            }
        }

        // System and Connection Shield Preferences Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, contentDescription = "Preferences", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "تنظیمات اتصال و اولویت‌ها" else if (appLanguage == "ru") "Системные предпочтения подключения" else "Connection & System Preferences",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Auto Connect on Boot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "اتصال خودکار هنگام روشن شدن گوشی" else if (appLanguage == "ru") "Автоподключение при запуске устройства" else "Auto-Connect on Boot",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "اجرای خودکار تونل فیلترشکن هنگام روشن شدن سیستم‌عامل." else if (appLanguage == "ru") "Автоматически запускает защищенный туннель после загрузки ОС." else "Automatically start secure tunnel once the device boots up.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = autoConnectOnBoot,
                            onCheckedChange = { autoConnectOnBoot = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    HorizontalDivider(color = CyberCard, thickness = 1.dp)

                    // Keep Alive Active
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "سپر فعال زنده نگه‌داشتن اتصال (Keep-Alive)" else if (appLanguage == "ru") "Активная защита соединения (Keep-Alive)" else "Connection Keep-Alive Shield",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "بررسی خودکار اتصال و برقراری مجدد در صورت قطعی‌های ناگهانی اپراتور." else if (appLanguage == "ru") "Мониторит падение туннеля и автоматически перестраивает связь." else "Monitors connection drops and rebuilds tunnel automatically.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = keepAliveActive,
                            onCheckedChange = { keepAliveActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    HorizontalDivider(color = CyberCard, thickness = 1.dp)

                    // Speed in Status Notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "نمایش سرعت در بخش نوتیفیکیشن" else if (appLanguage == "ru") "Отображать скорость в Уведомлении" else "Show Speed in Status Bar",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "نمایش دائمی پهنای باند و میزان دانلود/آپلود در اعلان سیستم." else if (appLanguage == "ru") "Отображает текущую скорость скачивания и отдачи в строке уведомлений." else "Keeps active download and upload stats pinned on the status notification.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = speedInNotification,
                            onCheckedChange = { speedInNotification = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }

                    HorizontalDivider(color = CyberCard, thickness = 1.dp)

                    // Allow LAN (Share VPN)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "fa") "اشتراک‌گذاری اینترنت (Allow LAN)" else if (appLanguage == "ru") "Раздача интернета (Allow LAN)" else "Allow LAN / Share VPN Connection",
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLanguage == "fa") "اشتراک‌گذاری کانکشن فیلترشکن با لپ‌تاپ، تلویزیون هوشمند یا گوشی‌های دیگر در شبکه Wi-Fi." else if (appLanguage == "ru") "Позволяет другим устройствам сети (TV, PC, mobile) использовать данное подключение." else "Allow other local Wi-Fi devices (TV, PC, mobile) to use this VPN as SOCKS proxy.",
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settings.allowLan,
                            onCheckedChange = { viewModel.updateSecuritySettings(settings.copy(allowLan = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
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
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Leak Safeguards",
                            tint = CyberPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "محافظت نشت و ابزار تشخیصی" else if (appLanguage == "ru") "Диагностика и защита утечек" else "Connection Diagnostics & Leak Protection",
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
                                    text = if (appLanguage == "fa") "تشخیص کیفیت اتصال زنده (پینگ فعال)" else if (appLanguage == "ru") "Постоянная авто-диагностика (Пинг)" else "Active Diagnostics (Auto-Ping)",
                                    color = CyberTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (appLanguage == "fa") "اندازه‌گیری و همگام‌سازی خودکار تاخیر پاسخگویی سرورها برحسب میلی‌ثانیه." else if (appLanguage == "ru") "Регулярно пингует конфигурации для определения живой задержки." else "Periodically pings configurations every 20s to isolate live latencies.",
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
                                    text = if (appLanguage == "fa") "کلید قطع اضطراری محافظتی" else if (appLanguage == "ru") "Аварийный выключатель блокировки (Kill Switch)" else "Censorship Tunnel Kill Switch",
                                    color = CyberTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (appLanguage == "fa") "درصورت قطع اتصال تونل امن, تمام دسترسی به اینترنت را مسدود می‌کند تا از نشت داده‌ها جلوگیری کند." else if (appLanguage == "ru") "Блокирует незащищенный трафик интернета при обрыве туннеля VPN." else "Blocks all outbound unsecured traffic if secure tunnel drops.",
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
                                text = if (appLanguage == "fa") "پراکسی برنامه‌ها" else if (appLanguage == "ru") "Прокси для приложений" else "Per-App Proxy (Split Tunnel)",
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
                        text = if (appLanguage == "fa") "فقط برنامه‌های انتخاب شده را از فیلترشکن عبور دهید." else if (appLanguage == "ru") "Выбирайте приложения для отправки в туннель или прямого обхода." else "Selectively routing or bypassing specific apps on your device via secure tunnel corridors.",
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
                                    text = if (appLanguage == "fa") "دور زدن خودکار برنامه‌های داخلی" else if (appLanguage == "ru") "Обходить национальные приложения" else "Auto-Bypass Domestic Apps (Iran)",
                                    color = CyberTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (appLanguage == "fa") "اجرا برنامه‌های بانکی و اسنپ بدون فیلترشکن" else if (appLanguage == "ru") "Автоматический обход государственных, платежных и банковских приложений." else "Ensures local bank apps & Snapp execute clean without VPN interface interference.",
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
                            text = if (appLanguage == "fa") "برنامه‌های نصب شده" else if (appLanguage == "ru") "Список приложений для прокси" else "Configured Application Routing Corridors",
                            color = CyberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Render app list
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            appsList.take(60).forEach { app ->
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
                                                    if (appLanguage == "fa") "دور زدن اجباری بانک" else if (appLanguage == "ru") "Обход банковского софта" else "Forced Bypass (Local App)"
                                                } else if (app.isProxied) {
                                                    if (appLanguage == "fa") "مسیر عبور: تونل امن Z2ray" else if (appLanguage == "ru") "Прокси туннель Z2ray" else "Route: Tunnel Proxy Corridor"
                                                } else {
                                                    if (appLanguage == "fa") "مسیر عبور: اینترنت مستقیم" else if (appLanguage == "ru") "Прямой интернет" else "Route: Direct Internet"
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
                        Icon(Icons.Filled.Settings, contentDescription = "Routing", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "fa") "موتور هوشمند مدیریت مسیر" else if (appLanguage == "ru") "Умная маршрутизация трафика" else "Intelligent Routing Rule Engine",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
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

        // Custom Routing Rule Editor Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.List, contentDescription = "Custom Rules", tint = CyberPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Custom Routing Rules", color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("Import", color = CyberPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showRoutingImportDialog = true })
                    }
                    Text("Format: DOMAIN,direct,domain:example.com or IP,block,1.2.3.4/32. Rules are applied before built-in Iran/private rules.", color = CyberTextSecondary, fontSize = 10.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Block ads/malware via geosite:category-ads-all", color = CyberTextPrimary, fontSize = 12.sp)
                        Switch(
                            checked = settings.blockAds,
                            onCheckedChange = { viewModel.updateSecuritySettings(settings.copy(blockAds = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary)
                        )
                    }
                    if (customRoutingRules.isEmpty()) {
                        Text("No custom rules yet.", color = CyberTextSecondary, fontSize = 11.sp)
                    } else {
                        customRoutingRules.take(8).forEach { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCard)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.name, color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${rule.type} → ${rule.outboundTag} • ${rule.values.joinToString(", ").take(80)}", color = CyberTextSecondary, fontSize = 10.sp)
                                }
                                Switch(checked = rule.enabled, onCheckedChange = { viewModel.toggleCustomRoutingRule(rule.id) }, colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = CyberPrimary))
                                IconButton(onClick = { viewModel.deleteCustomRoutingRule(rule.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete rule", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        if (customRoutingRules.size > 8) Text("+${customRoutingRules.size - 8} more rules", color = CyberTextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        // Core & Assets card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavy),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke(1.dp, CyberCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = "Core", tint = CyberPrimary, modifier = Modifier.size(22.dp))
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
                    CompactSelector("GeoData", geoDataSource, listOf("ENHANCED", "OFFICIAL"), Modifier.fillMaxWidth()) { viewModel.updateRoutingAssets(it) }
                    Surface(
                        color = CyberCard,
                        shape = RoundedCornerShape(10.dp),
                        border = borderStroke(1.dp, CyberPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text("BUNDLED CORE", color = CyberTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("Build-time core: Xray or v2fly • current flavor shown above", color = CyberTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.runUrlConnectivityTest() },
                            enabled = !isRunningNetworkTest,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                            border = borderStroke(1.dp, SecureGreen.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("URL TEST", color = SecureGreen, fontSize = 11.sp) }
                        Button(
                            onClick = { viewModel.runDownloadSpeedTest() },
                            enabled = !isRunningNetworkTest,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                            border = borderStroke(1.dp, CyberPrimary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("SPEED TEST", color = CyberPrimary, fontSize = 11.sp) }
                    }
                    lastUrlTest?.let { Text("URL: ${it.message}", color = if (it.ok) SecureGreen else DangerRed, fontSize = 10.sp) }
                    lastDownloadTest?.let { Text("Speed: ${it.speedBytesPerSec / 1024} KB/s • ${it.message}", color = if (it.ok) SecureGreen else DangerRed, fontSize = 10.sp) }
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
                        label = { Text("Group Name (e.g. Free Config Pool)", color = CyberTextSecondary, fontSize = 11.sp) },
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
                        label = { Text("Subscription Link URL (must return base64/plain configs)", color = CyberTextSecondary, fontSize = 11.sp) },
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
                        text = if (appLanguage == "fa") "تعیین بازه زمانی خودکار برای همگام‌سازی و تحلیل لینک‌های اشتراک." else "Automatically crawl and refresh subscription feed configurations to keep configs healthy and bypass blocklists.",
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
                                Text("Configs: ${sub.serverCount}", color = CyberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
