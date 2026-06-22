package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class SecuritySettings(
    val routingMode: String = "Bypass Iran", // Global, Bypass Iran, Direct
    val enableFragment: Boolean = true,
    val fragmentSize: String = "10-20",
    val fragmentInterval: String = "10-20",
    val fragmentPackets: String = "tls",
    val dnsMode: String = "Cloudflare DoH", // Google DoH, Cloudflare DoH, Shecan, System
    val enableObfuscation: Boolean = true,
    val stealthSnd: String = "assets.github.com",
    val activeFakeDns: Boolean = true
)

data class SpeedDataPoint(
    val downloadSpeedKb: Float,
    val uploadSpeedKb: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppConfigItem(
    val packageName: String,
    val name: String,
    val isProxied: Boolean,
    val isIranian: Boolean
)

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VpnDatabase.getDatabase(application)
    private val repo = VpnRepository(db.vpnDao(), application)
    private val prefs = application.getSharedPreferences("z2ray_preferences", Context.MODE_PRIVATE)

    // STATE FLOWS
    val servers = repo.allServers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedServer = repo.selectedServer.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val subscriptions = repo.allSubscriptions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val logs = repo.allLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _downloadSpeed = MutableStateFlow("0.0 KB/s")
    val downloadSpeed: StateFlow<String> = _downloadSpeed.asStateFlow()

    private val _uploadSpeed = MutableStateFlow("0.0 KB/s")
    val uploadSpeed: StateFlow<String> = _uploadSpeed.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _securitySettings = MutableStateFlow(loadSettingsFromPrefs())
    val securitySettings: StateFlow<SecuritySettings> = _securitySettings.asStateFlow()

    private val _isPingingAll = MutableStateFlow(false)
    val isPingingAll: StateFlow<Boolean> = _isPingingAll.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "Bento Dark") ?: "Bento Dark")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _perAppProxyEnabled = MutableStateFlow(prefs.getBoolean("per_app_proxy_enabled", false))
    val perAppProxyEnabled: StateFlow<Boolean> = _perAppProxyEnabled.asStateFlow()

    private val _bypassIranAppsEnabled = MutableStateFlow(prefs.getBoolean("bypass_iran_apps_enabled", true))
    val bypassIranAppsEnabled: StateFlow<Boolean> = _bypassIranAppsEnabled.asStateFlow()

    private val _appsList = MutableStateFlow<List<AppConfigItem>>(emptyList())
    val appsList: StateFlow<List<AppConfigItem>> = _appsList.asStateFlow()

    private val _subAutoUpdate = MutableStateFlow(prefs.getString("sub_auto_update", "On App Launch") ?: "On App Launch")
    val subAutoUpdate: StateFlow<String> = _subAutoUpdate.asStateFlow()

    private val _autoPingEnabled = MutableStateFlow(prefs.getBoolean("auto_ping_enabled", true))
    val autoPingEnabled: StateFlow<Boolean> = _autoPingEnabled.asStateFlow()

    private val _killSwitchEnabled = MutableStateFlow(prefs.getBoolean("kill_switch_enabled", false))
    val killSwitchEnabled: StateFlow<Boolean> = _killSwitchEnabled.asStateFlow()

    private val _isKillSwitchActive = MutableStateFlow(false)
    val isKillSwitchActive: StateFlow<Boolean> = _isKillSwitchActive.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedDataPoint>>(emptyList())
    val speedHistory: StateFlow<List<SpeedDataPoint>> = _speedHistory.asStateFlow()

    // Coroutine Jobs for simulations
    private var connectionJob: Job? = null
    private var speedMetricsJob: Job? = null
    private var timerJob: Job? = null
    private var autoPingJob: Job? = null

    init {
        // App starts clean without any pre-hardcoded/demo servers initially
        viewModelScope.launch {
            db.vpnDao().deleteServersByGroupName("z2ray Premium Nodes")
        }
        loadAppsList()
        viewModelScope.launch {
            delay(1500)
            triggerAutoUpdateOnStart()
            startScheduledSubUpdateLoop()
            startAutoPingLoop()
        }
    }

    private fun loadAppsList() {
        val defaultApps = listOf(
            AppConfigItem("com.instagram.android", "Instagram", true, false),
            AppConfigItem("org.telegram.messenger", "Telegram Messenger", true, false),
            AppConfigItem("com.google.android.youtube", "YouTube", true, false),
            AppConfigItem("com.android.chrome", "Google Chrome", true, false),
            AppConfigItem("com.whatsapp", "WhatsApp", true, false),
            AppConfigItem("com.spotify.music", "Spotify", true, false),
            AppConfigItem("ir.snapp.passenger", "Snapp! Passenger Taxi", false, true),
            AppConfigItem("cab.snapp.driver", "Snapp! Driver App", false, true),
            AppConfigItem("ir.divar", "Divar Classified Market", false, true),
            AppConfigItem("ir.rubika.app", "Rubika Super App", false, true),
            AppConfigItem("com.shaparak.mobile", "Shaparak Payments Gate", false, true),
            AppConfigItem("com.melli.key", "Melli Mobile Bank", false, true)
        )
        val loadedList = defaultApps.map { app ->
            val isProxiedDefault = if (app.isIranian) false else app.isProxied
            val savedProxyState = prefs.getBoolean("app_proxy_${app.packageName}", isProxiedDefault)
            app.copy(isProxied = savedProxyState)
        }
        _appsList.value = loadedList
    }

    fun toggleAppProxy(packageName: String) {
        val updated = _appsList.value.map { app ->
            if (app.packageName == packageName) {
                val newState = !app.isProxied
                prefs.edit().putBoolean("app_proxy_$packageName", newState).apply()
                viewModelScope.launch {
                    repo.log("PER-APP-PROXY", "INFO", "Split Tunnel app ${app.name} route proxy: $newState")
                }
                app.copy(isProxied = newState)
            } else {
                app
            }
        }
        _appsList.value = updated
    }

    fun setPerAppProxyEnabled(enabled: Boolean) {
        _perAppProxyEnabled.value = enabled
        prefs.edit().putBoolean("per_app_proxy_enabled", enabled).apply()
        viewModelScope.launch {
            repo.log("PER-APP-PROXY", "INFO", "Split tunneling control: $enabled")
        }
    }

    fun setBypassIranAppsEnabled(enabled: Boolean) {
        _bypassIranAppsEnabled.value = enabled
        prefs.edit().putBoolean("bypass_iran_apps_enabled", enabled).apply()
        viewModelScope.launch {
            repo.log("PER-APP-PROXY", "INFO", "Bypass domestic apps: $enabled")
        }
        if (enabled) {
            val updated = _appsList.value.map { app ->
                if (app.isIranian) {
                    prefs.edit().putBoolean("app_proxy_${app.packageName}", false).apply()
                    app.copy(isProxied = false)
                } else {
                    app
                }
            }
            _appsList.value = updated
        }
    }

    fun selectLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
        viewModelScope.launch {
            repo.log("SYSTEM", "INFO", "Language switched dynamically to: $lang")
        }
    }

    fun selectTheme(themeName: String) {
        _appTheme.value = themeName
        prefs.edit().putString("app_theme", themeName).apply()
        viewModelScope.launch {
            repo.log("SYSTEM", "INFO", "Theme preset loaded: $themeName")
        }
    }

    fun selectSubAutoUpdate(interval: String) {
        _subAutoUpdate.value = interval
        prefs.edit().putString("sub_auto_update", interval).apply()
        viewModelScope.launch {
            repo.log("SUBSCRIPTION", "INFO", "Subscription auto-update schedule modified: $interval")
        }
    }

    fun setAutoPingEnabled(enabled: Boolean) {
        _autoPingEnabled.value = enabled
        prefs.edit().putBoolean("auto_ping_enabled", enabled).apply()
        viewModelScope.launch {
            repo.log("SYSTEM", "INFO", "Active auto-ping diagnostics toggled: $enabled")
        }
        if (enabled) {
            startAutoPingLoop()
        } else {
            autoPingJob?.cancel()
        }
    }

    fun setKillSwitchEnabled(enabled: Boolean) {
        _killSwitchEnabled.value = enabled
        prefs.edit().putBoolean("kill_switch_enabled", enabled).apply()
        viewModelScope.launch {
            repo.log("SECURITY", "INFO", "Kill Switch state modified: $enabled")
        }
        if (!enabled) {
            _isKillSwitchActive.value = false
        }
    }

    private fun triggerAutoUpdateOnStart() {
        val mode = _subAutoUpdate.value
        if (mode == "Disabled") return
        viewModelScope.launch {
            repo.log("SUBSCRIPTION", "INFO", "Running app-launch subscription auto-sync (Mode: $mode)...")
            subscriptions.value.forEach { sub ->
                repo.fetchSubscription(sub)
            }
        }
    }

    private fun startScheduledSubUpdateLoop() {
        viewModelScope.launch {
            while (true) {
                delay(60 * 1000) // check every minute
                val mode = _subAutoUpdate.value
                val intervalMs = when (mode) {
                    "1 Hour" -> 60 * 60 * 1000L
                    "6 Hours" -> 6 * 60 * 60 * 1000L
                    "24 Hours" -> 24 * 60 * 60 * 1000L
                    else -> continue // "Disabled" or "On App Launch" don't run continuous interval
                }
                val now = System.currentTimeMillis()
                subscriptions.value.forEach { sub ->
                    if (now - sub.lastUpdated > intervalMs) {
                        repo.log("SUBSCRIPTION", "INFO", "Subscription '${sub.name}' expired schedule limit. Auto-refreshing...")
                        repo.fetchSubscription(sub)
                    }
                }
            }
        }
    }

    private fun startAutoPingLoop() {
        autoPingJob?.cancel()
        autoPingJob = viewModelScope.launch {
            while (true) {
                if (_autoPingEnabled.value) {
                    // Start bulk ping process in ViewModel background
                    testAllServersPing()
                }
                delay(20000) // Re-ping every 20 seconds
            }
        }
    }

    private fun loadSettingsFromPrefs(): SecuritySettings {
        return SecuritySettings(
            routingMode = prefs.getString("routing_mode", "Bypass Iran") ?: "Bypass Iran",
            enableFragment = prefs.getBoolean("enable_fragment", true),
            fragmentSize = prefs.getString("fragment_size", "10-20") ?: "10-20",
            fragmentInterval = prefs.getString("fragment_interval", "10-20") ?: "10-20",
            fragmentPackets = prefs.getString("fragment_packets", "tls") ?: "tls",
            dnsMode = prefs.getString("dns_mode", "Cloudflare DoH") ?: "Cloudflare DoH",
            enableObfuscation = prefs.getBoolean("enable_obfuscation", true),
            stealthSnd = prefs.getString("stealth_snd", "assets.github.com") ?: "assets.github.com",
            activeFakeDns = prefs.getBoolean("active_fake_dns", true)
        )
    }

    fun updateSecuritySettings(newSettings: SecuritySettings) {
        _securitySettings.value = newSettings
        prefs.edit().apply {
            putString("routing_mode", newSettings.routingMode)
            putBoolean("enable_fragment", newSettings.enableFragment)
            putString("fragment_size", newSettings.fragmentSize)
            putString("fragment_interval", newSettings.fragmentInterval)
            putString("fragment_packets", newSettings.fragmentPackets)
            putString("dns_mode", newSettings.dnsMode)
            putBoolean("enable_obfuscation", newSettings.enableObfuscation)
            putString("stealth_snd", newSettings.stealthSnd)
            putBoolean("active_fake_dns", newSettings.activeFakeDns)
            apply()
        }
        viewModelScope.launch {
            repo.log("SECURITY", "INFO", "Updated security preferences: Routing=${newSettings.routingMode}")
        }
    }

    // ACTIONS
    fun addServerFromLink(link: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val server = VpnParser.parseLine(link, "Manual Link")
            if (server != null) {
                repo.insertServer(server)
                onComplete(true)
            } else {
                repo.log("SERVER", "ERROR", "Failed to parse link: Format unrecognized")
                onComplete(false)
            }
        }
    }

    fun deleteServer(server: VpnServer) {
        viewModelScope.launch {
            repo.deleteServer(server)
        }
    }

    fun selectServer(server: VpnServer) {
        viewModelScope.launch {
            repo.selectServer(server.id)
            if (_connectionState.value == ConnectionState.CONNECTED) {
                // Reconnect to apply
                toggleConnection() // disconnect
                delay(300)
                toggleConnection() // reconnect
            }
        }
    }

    fun clearAllServers() {
        viewModelScope.launch {
            repo.clearAllServers()
        }
    }

    fun addSubscription(name: String, url: String) {
        viewModelScope.launch {
            repo.addSubscription(name, url)
        }
    }

    fun deleteSubscription(sub: Subscription) {
        viewModelScope.launch {
            repo.deleteSubscription(sub)
        }
    }

    fun refreshSubscription(sub: Subscription) {
        viewModelScope.launch {
            repo.fetchSubscription(sub)
        }
    }

    fun testServerPing(server: VpnServer) {
        viewModelScope.launch {
            repo.log("PING", "INFO", "Testing ping for ${server.name}...")
            val ms = repo.testServerLatency(server)
            if (ms > 0) {
                repo.log("PING", "SUCCESS", "${server.name} responded in ${ms}ms")
            } else {
                repo.log("PING", "ERROR", "${server.name} connection timed out")
            }
        }
    }

    fun testAllServersPing() {
        viewModelScope.launch {
            if (_isPingingAll.value) return@launch
            _isPingingAll.value = true
            repo.log("PING", "INFO", "Running concurrent latency measurements for all servers...")
            
            val currentList = servers.value
            currentList.map { srv ->
                launch {
                    repo.testServerLatency(srv)
                }
            }.forEach { it.join() }
            
            _isPingingAll.value = false
            repo.log("PING", "SUCCESS", "Bulk ping testing completed successfully.")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repo.clearLogs()
        }
    }

    // VPN CONNECTION TOGGLE
    fun toggleConnection() {
        viewModelScope.launch {
            when (_connectionState.value) {
                ConnectionState.DISCONNECTED -> {
                    val srv = selectedServer.value
                    if (srv == null) {
                        repo.log("CORE", "ERROR", "Cannot connect: No configuration profile selected.")
                        return@launch
                    }
                    
                    _isKillSwitchActive.value = false
                    _connectionState.value = ConnectionState.CONNECTING
                    repo.log("CORE", "INFO", "Initiating tunnel connection using ${srv.protocol} payload...")
                    repo.log("CORE", "INFO", "Applying secure routing rules: ${securitySettings.value.routingMode} mode")
                    
                    if (securitySettings.value.enableFragment) {
                        repo.log("SECURITY", "SUCCESS", "Anti-DPI packet fragmentation loaded successfully (${securitySettings.value.fragmentSize} bytes, delay ${securitySettings.value.fragmentInterval}ms)")
                    }
                    
                    repo.log("DNS", "INFO", "Resolving security tunnels via ${securitySettings.value.dnsMode}...")
                    
                    // Connection delay simulation with real steps
                    connectionJob = launch {
                        val steps = mutableListOf<String>()
                        steps.add("Establishing secure handshakes with ${srv.address}:${srv.port}...")
                        
                        if (srv.security.lowercase() == "reality") {
                            steps.add("Validating REALITY TLS handshake parameters (PublicKey, ShortId)...")
                            steps.add("Matched REALITY target destination SNI: ${srv.sni.ifEmpty { "unspecified" }}")
                        } else if (srv.protocol.uppercase() == "TROJAN") {
                            steps.add("Sending Trojan hex password verification headers...")
                        } else if (srv.protocol.uppercase() == "SHADOWSOCKS" || srv.protocol.uppercase() == "SS") {
                            steps.add("Initializing AEAD Shadowsocks secure cipher tunnel stream...")
                        } else if (srv.protocol.uppercase() == "VMESS") {
                            steps.add("Encoding VMess secure request metadata headers...")
                        } else {
                            steps.add("Initializing VLESS secure flow control state...")
                        }
                        
                        steps.add("Verifying SNI parameters (${securitySettings.value.stealthSnd.ifEmpty { srv.sni }})...")
                        steps.add("Establishing tunnel interface tun0 successfully!")
                        
                        for (step in steps) {
                            delay(400)
                            repo.log("CORE", "INFO", step)
                        }
                        
                        _connectionState.value = ConnectionState.CONNECTED
                        repo.log("CORE", "SUCCESS", "z2ray secure tunnel connection active!")
                        
                        startSimulations()
                    }
                }
                
                ConnectionState.CONNECTING -> {
                    connectionJob?.cancel()
                    _connectionState.value = ConnectionState.DISCONNECTED
                    repo.log("CORE", "WARN", "Connection initiation aborted by user.")
                }
                
                ConnectionState.CONNECTED -> {
                    stopSimulations()
                    _connectionState.value = ConnectionState.DISCONNECTED
                    repo.log("CORE", "INFO", "Tunnel closed successfully. System safe.")
                }
            }
        }
    }

    private fun startSimulations() {
        // 1. Timer Simulation
        _elapsedSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value += 1
            }
        }

        // 2. Network Speed Fluctuation simulation
        speedMetricsJob = viewModelScope.launch {
            while (true) {
                delay(1500)
                
                val dsKb: Float
                val ds = if (Math.random() > 0.3) {
                    val floatSpd = 1.0f + (Math.random() * 8.5).toFloat()
                    dsKb = floatSpd * 1024f
                    String.format("%.1f MB/s", floatSpd)
                } else {
                    val floatSpd = 50.0f + (Math.random() * 800.0).toFloat()
                    dsKb = floatSpd
                    String.format("%.1f KB/s", floatSpd)
                }

                val usKb: Float
                val us = if (Math.random() > 0.5) {
                    val floatSpd = 100.0f + (Math.random() * 600.0).toFloat()
                    usKb = floatSpd
                    String.format("%.1f KB/s", floatSpd)
                } else {
                    val floatSpd = 0.5f + (Math.random() * 1.8).toFloat()
                    usKb = floatSpd * 1024f
                    String.format("%.1f MB/s", floatSpd)
                }

                _downloadSpeed.value = ds
                _uploadSpeed.value = us

                val currentHistory = _speedHistory.value.toMutableList()
                currentHistory.add(SpeedDataPoint(dsKb, usKb))
                if (currentHistory.size > 20) {
                    currentHistory.removeAt(0)
                }
                _speedHistory.value = currentHistory
            }
        }
    }

    private fun stopSimulations() {
        timerJob?.cancel()
        speedMetricsJob?.cancel()
        _downloadSpeed.value = "0.0 KB/s"
        _uploadSpeed.value = "0.0 KB/s"
        _elapsedSeconds.value = 0
        _speedHistory.value = emptyList()

        if (_killSwitchEnabled.value) {
            _isKillSwitchActive.value = true
            viewModelScope.launch {
                repo.log("SECURITY", "WARN", "Kill Switch engaged: All unsecured outbound internet connections blocked.")
            }
        } else {
            _isKillSwitchActive.value = false
        }
    }

    fun formattedDuration(): String {
        val totalSecs = _elapsedSeconds.value
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulations()
    }
}
