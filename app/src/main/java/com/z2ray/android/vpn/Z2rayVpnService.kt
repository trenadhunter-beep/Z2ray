package com.z2ray.android.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.z2ray.android.MainActivity
import com.z2ray.android.R
import java.io.IOException

class Z2rayVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val xrayCore by lazy { XrayCoreBridge(this) }
    private val statsHandler = Handler(Looper.getMainLooper())
    private var isCoreRunning = false
    private var totalDownlinkBytes = 0L
    private var totalUplinkBytes = 0L

    private val statsRunnable = object : Runnable {
        override fun run() {
            val (downlinkDelta, uplinkDelta) = if (isCoreRunning) xrayCore.queryTraffic() else 0L to 0L
            totalDownlinkBytes += downlinkDelta
            totalUplinkBytes += uplinkDelta
            broadcastStatus(running = vpnInterface != null, coreRunning = isCoreRunning, downlink = totalDownlinkBytes, uplink = totalUplinkBytes)
            statsHandler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CONNECT -> startVpn(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn(intent: Intent) {
        startForeground(NOTIFICATION_ID, buildNotification("Starting secure tunnel..."))

        val sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "Z2ray"
        val dns = intent.getStringExtra(EXTRA_DNS) ?: "1.1.1.1"
        val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON).orEmpty()
        val allowedApps = intent.getStringArrayListExtra(EXTRA_ALLOWED_APPS).orEmpty()
        val hasCore = xrayCore.isAvailable() && configJson.isNotBlank()
        if (!hasCore) {
            broadcastStatus(false, false, 0L, 0L, "Xray core is not available. Rebuild with libv2ray.aar.")
            stopSelf()
            return
        }

        try {
            stopVpn()

            totalDownlinkBytes = 0L
            totalUplinkBytes = 0L

            val builder = Builder()
                .setSession(sessionName)
                .setMtu(1500)
                .addAddress("10.10.0.2", 32)
                .addDnsServer(dns)
                .setBlocking(false)

            runCatching { builder.addDisallowedApplication(packageName) }
            if (allowedApps.isNotEmpty()) {
                allowedApps.forEach { packageName ->
                    if (packageName != this.packageName) {
                        runCatching { builder.addAllowedApplication(packageName) }
                    }
                }
            }

            builder.addRoute("0.0.0.0", 0)
            builder.addRoute("::", 0)

            vpnInterface = builder.establish()

            if (hasCore && vpnInterface != null) {
                isCoreRunning = xrayCore.start(configJson, vpnInterface!!.fd) { status ->
                    startForeground(NOTIFICATION_ID, buildNotification(status))
                    broadcastStatus(vpnInterface != null, isCoreRunning, 0L, 0L, status)
                }
            }

            if (!isCoreRunning) {
                broadcastStatus(false, false, 0L, 0L, "Xray core failed to start")
                stopVpn()
                stopSelf()
                return
            }

            val status = "Xray core is running"
            startForeground(NOTIFICATION_ID, buildNotification(status))
            broadcastStatus(vpnInterface != null, true, 0L, 0L, status)
            statsHandler.removeCallbacks(statsRunnable)
            statsHandler.post(statsRunnable)
        } catch (e: Exception) {
            broadcastStatus(false, false, 0L, 0L, "VPN start failed: ${e.message}")
            stopVpn()
            stopSelf()
        }
    }

    private fun stopVpn() {
        statsHandler.removeCallbacks(statsRunnable)
        xrayCore.stop()
        isCoreRunning = false
        try {
            vpnInterface?.close()
        } catch (_: IOException) {
        } finally {
            vpnInterface = null
            broadcastStatus(false, false, 0L, 0L, "VPN stopped")
        }
    }

    private fun broadcastStatus(
        running: Boolean,
        coreRunning: Boolean,
        downlink: Long,
        uplink: Long,
        message: String = ""
    ) {
        val intent = Intent(ACTION_VPN_STATUS).apply {
            setPackage(packageName)
            putExtra(EXTRA_RUNNING, running)
            putExtra(EXTRA_CORE_RUNNING, coreRunning)
            putExtra(EXTRA_DOWNLINK_BYTES, downlink)
            putExtra(EXTRA_UPLINK_BYTES, uplink)
            putExtra(EXTRA_STATUS_MESSAGE, message)
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Z2ray")
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Z2ray VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_CONNECT = "com.z2ray.android.action.CONNECT"
        const val ACTION_DISCONNECT = "com.z2ray.android.action.DISCONNECT"
        const val ACTION_VPN_STATUS = "com.z2ray.android.action.STATUS"
        const val EXTRA_SESSION_NAME = "session_name"
        const val EXTRA_DNS = "dns"
        const val EXTRA_CONFIG_JSON = "config_json"
        const val EXTRA_ALLOWED_APPS = "allowed_apps"
        const val EXTRA_RUNNING = "running"
        const val EXTRA_CORE_RUNNING = "core_running"
        const val EXTRA_DOWNLINK_BYTES = "downlink_bytes"
        const val EXTRA_UPLINK_BYTES = "uplink_bytes"
        const val EXTRA_STATUS_MESSAGE = "status_message"
        private const val CHANNEL_ID = "z2ray_vpn"
        private const val NOTIFICATION_ID = 2001
    }
}
