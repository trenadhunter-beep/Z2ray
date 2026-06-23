package com.z2ray.android.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z2ray.android.data.*
import com.z2ray.android.vpn.Z2rayVpnService
import com.z2ray.android.vpn.XrayConfigBuilder
import com.z2ray.android.vpn.XrayAssetManager
import com.z2ray.android.vpn.XrayCoreBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject

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
    val activeFakeDns: Boolean = true,
    val blockAds: Boolean = true,
    val routingDnsStrategy: String = "IPIfNonMatch",
    val allowLan: Boolean = false,
    val localSocksPort: String = "10808",
    val enableSniffing: Boolean = true,
    val remoteDns: String = "https://1.1.1.1/dns-query",
    val directDns: String = "1.1.1.1",
    val enableFakeDns: Boolean = true,
    val enableMux: Boolean = false,
    val muxConcurrency: String = "8",
    val logLevel: String = "warning",
    val allowInsecure: Boolean = false,
    val fingerprint: String = "chrome"
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
    private val networkDiagnostics = NetworkDiagnostics()
    private val prefs = application.getSharedPreferences("z2ray_preferences", Context.MODE_PRIVATE)

    // STATE FLOWS
    val servers = repo.allServers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedServer = repo.selectedServer.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val subscriptions = repo.allSubscriptions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val logs = repo.allLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _showOnboarding = MutableStateFlow(!prefs.getBoolean("onboarding_seen", false))
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

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

    private val _coreVersion = MutableStateFlow("Checking core...")
    val coreVersion: StateFlow<String> = _coreVersion.asStateFlow()

    private val _routingAssetsStatus = MutableStateFlow("Checking assets...")
    val routingAssetsStatus: StateFlow<String> = _routingAssetsStatus.asStateFlow()

    private val _geoDataSource = MutableStateFlow(prefs.getString("geo_data_source", "ENHANCED") ?: "ENHANCED")
    val geoDataSource: StateFlow<String> = _geoDataSource.asStateFlow()

    private val _isUpdatingRoutingAssets = MutableStateFlow(false)
    val isUpdatingRoutingAssets: StateFlow<Boolean> = _isUpdatingRoutingAssets.asStateFlow()

    private val _customRoutingRules = MutableStateFlow(loadCustomRoutingRules())
    val customRoutingRules: StateFlow<List<CustomRoutingRule>> = _customRoutingRules.asStateFlow()

    private val _lastUrlTestResult = MutableStateFlow<UrlTestResult?>(null)
    val lastUrlTestResult: StateFlow<UrlTestResult?> = _lastUrlTestResult.asStateFlow()

    private val _lastDownloadTestResult = MutableStateFlow<UrlTestResult?>(null)
    val lastDownloadTestResult: StateFlow<UrlTestResult?> = _lastDownloadTestResult.asStateFlow()

    private val _isRunningNetworkTest = MutableStateFlow(false)
    val isRunningNetworkTest: StateFlow<Boolean> = _isRunningNetworkTest.asStateFlow()

    private val _perAppProxyEnabled = MutableStateFlow(prefs.getBoolean("per_app_proxy_enabled", false))
    val perAppProxyEnabled: StateFlow<Boolean> = _perAppProxyEnabled.asStateFlow()

    private val _bypassIranAppsEnabled = MutableStateFlow(prefs.getBoolean("bypass_iran_apps_enabled", true))
    val bypassIranAppsEnabled: StateFlow<Boolean> = _bypassIranAppsEnabled.asStateFlow()

    private val _appsList = MutableStateFlow<List<AppConfigItem>>(emptyList())
    val appsList: StateFlow<List<AppConfigItem>> = _appsList.asStateFlow()

    private val _subAutoUpdate = MutableStateFlow(prefs.getString("sub_auto_update", "On App Launch") ?: "On App Launch")
    val subAutoUpdate: StateFlow<String> = _subAutoUpdate.asStateFlow()

    private val _autoPingEnabled = MutableStateFlow(prefs.getBoolean("auto_ping_enabled", false))
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
    private var lastDownlinkBytes = 0L
    private var lastUplinkBytes = 0L
    private var lastStatsAt = 0L

    private val vpnStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Z2rayVpnService.ACTION_VPN_STATUS) return
            val running = intent.getBooleanExtra(Z2rayVpnService.EXTRA_RUNNING, false)
            val coreRunning = intent.getBooleanExtra(Z2rayVpnService.EXTRA_CORE_RUNNING, false)
            val downlink = intent.getLongExtra(Z2rayVpnService.EXTRA_DOWNLINK_BYTES, 0L)
            val uplink = intent.getLongExtra(Z2rayVpnService.EXTRA_UPLINK_BYTES, 0L)
            val message = intent.getStringExtra(Z2rayVpnService.EXTRA_STATUS_MESSAGE).orEmpty()
            handleVpnStatus(running, coreRunning, downlink, uplink, message)
        }
    }

    init {
        registerVpnStatusReceiver()
        // App starts clean without any pre-hardcoded/demo servers initially
        viewModelScope.launch {
            db.vpnDao().deleteServersByGroupName("z2ray Premium Nodes")
            db.vpnDao().deleteServersByGroupName("z2ray Premium Configs")
        }
        loadAppsList()
        refreshCoreAndAssetsStatus()
        viewModelScope.launch {
            delay(1500)
            triggerAutoUpdateOnStart()
            ensureRoutingAssetsAvailable()
            startScheduledSubUpdateLoop()
            if (_autoPingEnabled.value) startAutoPingLoop()
        }
    }

    private fun registerVpnStatusReceiver() {
        val app = getApplication<Application>()
        ContextCompat.registerReceiver(
            app,
            vpnStatusReceiver,
            IntentFilter(Z2rayVpnService.ACTION_VPN_STATUS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun handleVpnStatus(running: Boolean, coreRunning: Boolean, downlinkBytes: Long, uplinkBytes: Long, message: String) {
        val now = System.currentTimeMillis()
        if (running && coreRunning) {
            _connectionState.value = ConnectionState.CONNECTED
            _isKillSwitchActive.value = false
        } else if (!running) {
            _connectionState.value = ConnectionState.DISCONNECTED
            stopSimulations()
        }

        if (message.isNotBlank()) {
            viewModelScope.launch { repo.log("CORE", if (coreRunning) "SUCCESS" else "INFO", message) }
        }

        if (lastStatsAt > 0L) {
            val elapsedSeconds = ((now - lastStatsAt).coerceAtLeast(1L)) / 1000f
            val downSpeed = ((downlinkBytes - lastDownlinkBytes).coerceAtLeast(0L) / elapsedSeconds).toLong()
            val upSpeed = ((uplinkBytes - lastUplinkBytes).coerceAtLeast(0L) / elapsedSeconds).toLong()
            updateRealSpeed(downSpeed, upSpeed)
        }

        lastStatsAt = now
        lastDownlinkBytes = downlinkBytes
        lastUplinkBytes = uplinkBytes
    }

    private fun updateRealSpeed(downloadBytesPerSec: Long, uploadBytesPerSec: Long) {
        _downloadSpeed.value = formatBytesPerSecond(downloadBytesPerSec)
        _uploadSpeed.value = formatBytesPerSecond(uploadBytesPerSec)

        val currentHistory = _speedHistory.value.toMutableList()
        currentHistory.add(SpeedDataPoint(downloadBytesPerSec / 1024f, uploadBytesPerSec / 1024f))
        if (currentHistory.size > 60) {
            currentHistory.removeAt(0)
        }
        _speedHistory.value = currentHistory
    }

    private fun formatBytesPerSecond(bytesPerSecond: Long): String {
        val b = bytesPerSecond.toDouble()
        return when {
            b >= 1024 * 1024 -> String.format("%.2f MB/s", b / 1024.0 / 1024.0)
            b >= 1024 -> String.format("%.1f KB/s", b / 1024.0)
            else -> String.format("%d B/s", bytesPerSecond)
        }
    }

    fun refreshCoreAndAssetsStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            _routingAssetsStatus.value = XrayAssetManager.statusText(app)
            val core = XrayCoreBridge(app).runtimeInfo()
            _coreVersion.value = "${core.version} • ${core.provider}"
        }
    }

    fun updateRoutingAssets(sourceName: String = _geoDataSource.value) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_isUpdatingRoutingAssets.value) return@launch
            _isUpdatingRoutingAssets.value = true
            try {
                val app = getApplication<Application>()
                val source = runCatching { com.z2ray.android.vpn.GeoDataSource.valueOf(sourceName) }.getOrDefault(com.z2ray.android.vpn.GeoDataSource.ENHANCED)
                _geoDataSource.value = source.name
                prefs.edit().putString("geo_data_source", source.name).apply()
                repo.log("ASSETS", "INFO", "Updating geoip.dat and geosite.dat from ${source.title}...")
                val ok = XrayAssetManager.updateFromNetwork(app, source)
                _routingAssetsStatus.value = XrayAssetManager.statusText(app)
                repo.log("ASSETS", if (ok) "SUCCESS" else "WARN", "Routing assets update finished: ${_routingAssetsStatus.value}")
            } catch (e: Exception) {
                repo.log("ASSETS", "ERROR", "Failed to update routing assets: ${e.localizedMessage}")
            } finally {
                _isUpdatingRoutingAssets.value = false
            }
        }
    }

    private fun loadAppsList() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val pm = app.packageManager
            val packages = runCatching {
                pm.getInstalledPackages(0)
            }.getOrDefault(emptyList())

            val domesticKeywords = listOf(
                "ir.", "snapp", "divar", "rubika", "shaparak", "melli", "tejarat", "saman", 
                "shahr", "sadad", "tap30", "zarinpal", "bamilo", "digikala", "ap", 
                "asandardakht", "bank", "mellat", "keshavarzi", "pasargad", "ansar", "parsian"
            )

            val loadedList = packages
                .filter { pkg ->
                    pm.getLaunchIntentForPackage(pkg.packageName) != null
                }
                .map { pkg ->
                    val packageName = pkg.packageName
                    val name = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
                    val isIranian = domesticKeywords.any { packageName.contains(it, ignoreCase = true) || name.contains(it, ignoreCase = true) }
                    
                    val isProxiedDefault = !isIranian
                    val savedProxyState = prefs.getBoolean("app_proxy_$packageName", isProxiedDefault)
                    AppConfigItem(
                        packageName = packageName,
                        name = name,
                        isProxied = savedProxyState,
                        isIranian = isIranian
                    )
                }
                .sortedBy { it.name.lowercase() }

            _appsList.value = loadedList
        }
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

    fun completeOnboarding() {
        _showOnboarding.value = false
        prefs.edit().putBoolean("onboarding_seen", true).apply()
        viewModelScope.launch { repo.log("SYSTEM", "INFO", "Onboarding completed") }
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

    private fun ensureRoutingAssetsAvailable() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            if (XrayAssetManager.status(app).values.any { !it }) {
                repo.log("ASSETS", "INFO", "Routing assets are not bundled to keep APK small. Downloading in background...")
                val source = runCatching { com.z2ray.android.vpn.GeoDataSource.valueOf(_geoDataSource.value) }.getOrDefault(com.z2ray.android.vpn.GeoDataSource.ENHANCED)
                runCatching { XrayAssetManager.updateFromNetwork(app, source) }
                _routingAssetsStatus.value = XrayAssetManager.statusText(app)
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
                delay(5 * 60 * 1000L) // Conservative default: avoid battery/network load.
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
            activeFakeDns = prefs.getBoolean("active_fake_dns", true),
            blockAds = prefs.getBoolean("block_ads", true),
            routingDnsStrategy = prefs.getString("routing_dns_strategy", "IPIfNonMatch") ?: "IPIfNonMatch",
            allowLan = prefs.getBoolean("allow_lan", false),
            localSocksPort = prefs.getString("local_socks_port", "10808") ?: "10808",
            enableSniffing = prefs.getBoolean("enable_sniffing", true),
            remoteDns = prefs.getString("remote_dns", "https://1.1.1.1/dns-query") ?: "https://1.1.1.1/dns-query",
            directDns = prefs.getString("direct_dns", "1.1.1.1") ?: "1.1.1.1",
            enableFakeDns = prefs.getBoolean("enable_fakedns", true),
            enableMux = prefs.getBoolean("enable_mux", false),
            muxConcurrency = prefs.getString("mux_concurrency", "8") ?: "8",
            logLevel = prefs.getString("log_level", "warning") ?: "warning",
            allowInsecure = prefs.getBoolean("allow_insecure_connections", false),
            fingerprint = prefs.getString("fingerprint", "chrome") ?: "chrome"
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
            putBoolean("block_ads", newSettings.blockAds)
            putString("routing_dns_strategy", newSettings.routingDnsStrategy)
            putBoolean("allow_lan", newSettings.allowLan)
            putString("local_socks_port", newSettings.localSocksPort)
            putBoolean("enable_sniffing", newSettings.enableSniffing)
            putString("remote_dns", newSettings.remoteDns)
            putString("direct_dns", newSettings.directDns)
            putBoolean("enable_fakedns", newSettings.enableFakeDns)
            putBoolean("enable_mux", newSettings.enableMux)
            putString("mux_concurrency", newSettings.muxConcurrency)
            putString("log_level", newSettings.logLevel)
            putBoolean("allow_insecure_connections", newSettings.allowInsecure)
            putString("fingerprint", newSettings.fingerprint)
            apply()
        }
        viewModelScope.launch {
            repo.log("SECURITY", "INFO", "Updated security preferences: Routing=${newSettings.routingMode}")
        }
    }

    private fun loadCustomRoutingRules(): List<CustomRoutingRule> =
        CustomRoutingRule.listFromJson(prefs.getString("custom_routing_rules", "[]") ?: "[]")

    private fun persistCustomRoutingRules(rules: List<CustomRoutingRule>) {
        _customRoutingRules.value = rules
        prefs.edit().putString("custom_routing_rules", CustomRoutingRule.listToJson(rules)).apply()
    }

    fun addCustomRoutingRule(rule: CustomRoutingRule) {
        persistCustomRoutingRules(_customRoutingRules.value + rule)
        viewModelScope.launch { repo.log("ROUTING", "SUCCESS", "Added custom routing rule '${rule.name}' -> ${rule.outboundTag}") }
    }

    fun deleteCustomRoutingRule(ruleId: String) {
        val rule = _customRoutingRules.value.firstOrNull { it.id == ruleId }
        persistCustomRoutingRules(_customRoutingRules.value.filterNot { it.id == ruleId })
        viewModelScope.launch { repo.log("ROUTING", "INFO", "Deleted custom routing rule '${rule?.name ?: ruleId}'") }
    }

    fun toggleCustomRoutingRule(ruleId: String) {
        val updated = _customRoutingRules.value.map { if (it.id == ruleId) it.copy(enabled = !it.enabled) else it }
        persistCustomRoutingRules(updated)
    }

    fun importCustomRoutingRules(text: String, onComplete: (Int) -> Unit = {}) {
        val imported = CustomRoutingRule.fromImportText(text)
        if (imported.isNotEmpty()) {
            persistCustomRoutingRules((_customRoutingRules.value + imported).distinctBy { it.id })
        }
        viewModelScope.launch { repo.log("ROUTING", if (imported.isNotEmpty()) "SUCCESS" else "WARN", "Imported ${imported.size} custom routing rules") }
        onComplete(imported.size)
    }

    fun exportCustomRoutingRules(): String = CustomRoutingRule.listToJson(_customRoutingRules.value)

    fun runUrlConnectivityTest(url: String = NetworkDiagnostics.DEFAULT_TEST_URL) {
        viewModelScope.launch {
            if (_isRunningNetworkTest.value) return@launch
            _isRunningNetworkTest.value = true
            repo.log("TEST", "INFO", "Running active URL test: $url")
            val result = if (_connectionState.value == ConnectionState.CONNECTED) {
                networkDiagnostics.urlTestThroughLocalSocks(url, com.z2ray.android.vpn.XrayConfigBuilder.LOCAL_SOCKS_PORT)
            } else {
                networkDiagnostics.urlTest(url)
            }
            _lastUrlTestResult.value = result
            repo.log("TEST", if (result.ok) "SUCCESS" else "ERROR", "URL test: ${result.message}")
            _isRunningNetworkTest.value = false
        }
    }

    fun runDownloadSpeedTest(url: String = NetworkDiagnostics.DEFAULT_DOWNLOAD_TEST_URL) {
        viewModelScope.launch {
            if (_isRunningNetworkTest.value) return@launch
            _isRunningNetworkTest.value = true
            repo.log("TEST", "INFO", "Running download speed test...")
            val result = if (_connectionState.value == ConnectionState.CONNECTED) {
                networkDiagnostics.downloadSpeedTestThroughLocalSocks(url, port = com.z2ray.android.vpn.XrayConfigBuilder.LOCAL_SOCKS_PORT)
            } else {
                networkDiagnostics.downloadSpeedTest(url)
            }
            _lastDownloadTestResult.value = result
            repo.log("TEST", if (result.ok) "SUCCESS" else "ERROR", "Download test: ${result.message}, speed=${formatBytesPerSecond(result.speedBytesPerSec)}")
            _isRunningNetworkTest.value = false
        }
    }

    fun testTlsHandshake(server: VpnServer) {
        viewModelScope.launch {
            repo.log("TLS", "INFO", "Testing TLS/REALITY front handshake for ${server.name}...")
            val result = networkDiagnostics.tlsHandshake(server)
            val pin = if (result.peerSha256.isNotBlank()) " sha256=${result.peerSha256.take(16)}..." else ""
            repo.log("TLS", if (result.ok) "SUCCESS" else "ERROR", "${server.name}: ${result.message}$pin")
        }
    }

    fun testUdpProbe(server: VpnServer) {
        viewModelScope.launch {
            repo.log("UDP", "INFO", "Sending UDP probe to ${server.name}...")
            val result = networkDiagnostics.udpProbe(server)
            repo.log("UDP", if (result.ok) "SUCCESS" else "ERROR", "${server.name}: ${result.message}")
        }
    }

    fun updateServer(server: VpnServer) {
        viewModelScope.launch {
            repo.updateServer(server)
            repo.log("SERVER", "SUCCESS", "Updated server profile: ${server.name}")
        }
    }

    fun selectBestServerByLatency(groupName: String? = null) {
        viewModelScope.launch {
            val candidates = servers.value.filter { (groupName == null || it.groupName == groupName) && it.latency > 0 }
            val best = candidates.minByOrNull { it.latency }
            if (best != null) {
                repo.selectServer(best.id)
                repo.log("BALANCER", "SUCCESS", "Selected fastest config: ${best.name} (${best.latency}ms)")
            } else {
                repo.log("BALANCER", "WARN", "No tested healthy config available. Run batch latency first.")
            }
        }
    }

    // ACTIONS
    fun addManualServer(server: VpnServer) {
        viewModelScope.launch {
            repo.insertServer(server)
            repo.log("SERVER", "SUCCESS", "Created manual server: ${server.name}")
        }
    }

    fun addServerFromLink(link: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val parsed = VpnParser.parseMany(link, "Manual Link")
            if (parsed.isNotEmpty()) {
                parsed.forEach { repo.insertServer(it) }
                repo.log("SERVER", "SUCCESS", "Imported ${parsed.size} configuration(s) from manual input")
                onComplete(true)
            } else {
                repo.log("SERVER", "ERROR", "Failed to parse input: Format unrecognized")
                onComplete(false)
            }
        }
    }


    fun exportAllConfigsText(): String {
        return servers.value
            .mapNotNull { server -> server.originalLink.takeIf { it.isNotBlank() } }
            .distinct()
            .joinToString("\n")
    }

    fun importConfigsFromText(text: String, groupName: String = "Backup Import", onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val parsed = VpnParser.parseMany(text, groupName)
                .distinctBy { it.originalLink.ifBlank { "${it.protocol}-${it.address}-${it.port}-${it.uuid}-${it.networkType}" } }

            parsed.forEach { repo.insertServer(it) }
            repo.log("BACKUP", "SUCCESS", "Imported ${parsed.size} configs from backup text")
            onComplete(parsed.size)
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
            val limiter = Semaphore(8)
            currentList.map { srv ->
                launch(Dispatchers.IO) {
                    limiter.withPermit {
                        repo.testServerLatency(srv)
                    }
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
                ConnectionState.DISCONNECTED -> connectSelectedServer()
                ConnectionState.CONNECTING -> {
                    connectionJob?.cancel()
                    stopVpnService()
                    _connectionState.value = ConnectionState.DISCONNECTED
                    repo.log("CORE", "WARN", "Connection initiation aborted by user.")
                }
                ConnectionState.CONNECTED -> {
                    stopSimulations()
                    stopVpnService()
                    _connectionState.value = ConnectionState.DISCONNECTED
                    repo.log("CORE", "INFO", "Tunnel closed successfully. System safe.")
                }
            }
        }
    }

    fun onVpnPermissionDenied() {
        viewModelScope.launch {
            repo.log("CORE", "ERROR", "Android VPN permission was denied by user.")
        }
    }

    private suspend fun connectSelectedServer() {
        val srv = selectedServer.value
        if (srv == null) {
            repo.log("CORE", "ERROR", "Cannot connect: No configuration profile selected.")
            return
        }

        _isKillSwitchActive.value = false
        _connectionState.value = ConnectionState.CONNECTING
        repo.log("CORE", "INFO", "Initiating Android VPN service for ${srv.protocol} profile...")
        repo.log("CORE", "INFO", "Applying secure routing rules: ${securitySettings.value.routingMode} mode")

        if (securitySettings.value.enableFragment) {
            repo.log("SECURITY", "SUCCESS", "Anti-DPI packet fragmentation profile loaded (${securitySettings.value.fragmentSize} bytes, delay ${securitySettings.value.fragmentInterval}ms)")
        }

        repo.log("DNS", "INFO", "Resolving security tunnels via ${securitySettings.value.dnsMode}...")
        startVpnService(srv)

        connectionJob = viewModelScope.launch {
            val steps = mutableListOf<String>()
            steps.add("Creating Android protected VPN interface...")
            steps.add("Loading selected endpoint ${srv.address}:${srv.port}...")

            if (srv.security.lowercase() == "reality") {
                steps.add("Validated REALITY TLS parameters for SNI: ${srv.sni.ifEmpty { "unspecified" }}")
            } else if (srv.protocol.uppercase() == "TROJAN") {
                steps.add("Prepared Trojan credential headers.")
            } else if (srv.protocol.uppercase() == "SHADOWSOCKS" || srv.protocol.uppercase() == "SS") {
                steps.add("Prepared Shadowsocks cipher profile.")
            } else if (srv.protocol.uppercase() == "VMESS") {
                steps.add("Prepared VMess request metadata.")
            } else if (srv.protocol.uppercase() == "HYSTERIA2" || srv.protocol.uppercase() == "HY2") {
                steps.add("Prepared Hysteria2 QUIC transport metadata.")
            } else if (srv.protocol.uppercase() == "CUSTOM_JSON") {
                steps.add("Loaded raw Xray JSON profile without rewriting outbounds.")
            } else if (srv.protocol.uppercase() == "SOCKS" || srv.protocol.uppercase() == "SOCKS5") {
                steps.add("Prepared SOCKS5 upstream profile.")
            } else {
                steps.add("Prepared VLESS flow control state.")
            }

            steps.add("Waiting for Xray core startup confirmation...")

            for (step in steps) {
                delay(250)
                repo.log("CORE", "INFO", step)
            }

            startSimulations()
        }
    }

    private fun startVpnService(server: VpnServer) {
        val app = getApplication<Application>()
        val dns = when (securitySettings.value.dnsMode) {
            "Google DoH" -> "8.8.8.8"
            "Shecan" -> "178.22.122.100"
            "System" -> "1.1.1.1"
            else -> "1.1.1.1"
        }
        val configJson = XrayConfigBuilder.build(
            server = server,
            dnsServer = dns,
            routingMode = securitySettings.value.routingMode,
            customRules = _customRoutingRules.value,
            blockAds = securitySettings.value.blockAds,
            domainStrategy = securitySettings.value.routingDnsStrategy,
            enableFragment = securitySettings.value.enableFragment,
            fragmentSize = securitySettings.value.fragmentSize,
            fragmentInterval = securitySettings.value.fragmentInterval,
            fragmentPackets = securitySettings.value.fragmentPackets,
            allowLan = securitySettings.value.allowLan
        )
        val allowedApps = if (_perAppProxyEnabled.value) {
            ArrayList(_appsList.value.filter { it.isProxied }.map { it.packageName })
        } else {
            arrayListOf<String>()
        }
        val intent = Intent(app, Z2rayVpnService::class.java).apply {
            action = Z2rayVpnService.ACTION_CONNECT
            putExtra(Z2rayVpnService.EXTRA_SESSION_NAME, "Z2ray - ${server.name}")
            putExtra(Z2rayVpnService.EXTRA_DNS, dns)
            putExtra(Z2rayVpnService.EXTRA_CONFIG_JSON, configJson)
            putStringArrayListExtra(Z2rayVpnService.EXTRA_ALLOWED_APPS, allowedApps)
        }
        ContextCompat.startForegroundService(app, intent)
    }

    private fun stopVpnService() {
        val app = getApplication<Application>()
        val intent = Intent(app, Z2rayVpnService::class.java).apply {
            action = Z2rayVpnService.ACTION_DISCONNECT
        }
        app.startService(intent)
    }

    private fun startSimulations() {
        _elapsedSeconds.value = 0
        lastStatsAt = 0L
        lastDownlinkBytes = 0L
        lastUplinkBytes = 0L
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value += 1
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
        runCatching { getApplication<Application>().unregisterReceiver(vpnStatusReceiver) }
        super.onCleared()
        stopSimulations()
    }
}
