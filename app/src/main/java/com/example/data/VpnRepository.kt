package com.example.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class VpnRepository(private val vpnDao: VpnDao, private val context: android.content.Context) {

    val allServers: Flow<List<VpnServer>> = vpnDao.getAllServers()
    val selectedServer: Flow<VpnServer?> = vpnDao.getSelectedServer()
    val allSubscriptions: Flow<List<Subscription>> = vpnDao.getAllSubscriptions()
    val allLogs: Flow<List<VpnLog>> = vpnDao.getAllLogs()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun insertServer(server: VpnServer) {
        vpnDao.insertServer(server)
        log("SERVER", "SUCCESS", "Added server: ${server.name} (${server.protocol})")
    }

    suspend fun updateServer(server: VpnServer) {
        vpnDao.updateServer(server)
    }

    suspend fun deleteServer(server: VpnServer) {
        vpnDao.deleteServer(server)
        log("SERVER", "INFO", "Deleted server: ${server.name}")
    }

    suspend fun selectServer(serverId: Int) {
        vpnDao.selectServer(serverId)
        log("CORE", "INFO", "Selected server configuration ID: $serverId")
    }

    suspend fun clearAllServers() {
        vpnDao.deleteAllServers()
        log("SYSTEM", "WARN", "Cleared all server configurations")
    }

    suspend fun addSubscription(name: String, url: String) {
        val sub = Subscription(name = name, url = url)
        vpnDao.insertSubscription(sub)
        log("SUBSCRIPTION", "SUCCESS", "Added subscription: $name")
        fetchSubscription(sub)
    }

    suspend fun deleteSubscription(sub: Subscription) {
        vpnDao.deleteSubscription(sub)
        vpnDao.deleteServersByGroupName(sub.name)
        log("SUBSCRIPTION", "INFO", "Deleted subscription: ${sub.name}")
    }

    suspend fun fetchSubscription(sub: Subscription) = withContext(Dispatchers.IO) {
        log("SUBSCRIPTION", "INFO", "Fetching subscription: ${sub.name}...")
        try {
            val request = Request.Builder().url(sub.url).header("User-Agent", "z2ray/1.0").build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    log("SUBSCRIPTION", "ERROR", "Failed to fetch ${sub.name}. HTTP code: ${response.code}")
                    return@withContext
                }

                val payload = response.body?.string() ?: ""
                if (payload.isEmpty()) {
                    log("SUBSCRIPTION", "WARN", "Empty response from subscription: ${sub.name}")
                    return@withContext
                }

                // Subscriptions are usually base64 encoded strings of multiple lines.
                val decoded = VpnParser.parseLine(payload) // Some subscriptions might not be base64, check first line or full decode
                val linesStr = try {
                    val rawDecoded = Base64.decode(payload.trim(), Base64.DEFAULT)
                    String(rawDecoded, Charsets.UTF_8)
                } catch (e: Exception) {
                    // Try parsing as raw lines
                    payload
                }

                val lines = linesStr.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
                val parsedServers = mutableListOf<VpnServer>()

                for (line in lines) {
                    val parsed = VpnParser.parseLine(line, sub.name)
                    if (parsed != null) {
                        parsedServers.add(parsed.copy(isCustom = false))
                    }
                }

                if (parsedServers.isNotEmpty()) {
                    // Clear previous configs from this subscription
                    vpnDao.deleteServersByGroupName(sub.name)
                    vpnDao.insertServers(parsedServers)
                    
                    // Update subscription metadata
                    vpnDao.insertSubscription(sub.copy(
                        lastUpdated = System.currentTimeMillis(),
                        serverCount = parsedServers.size
                    ))
                    log("SUBSCRIPTION", "SUCCESS", "Successfully refreshed '${sub.name}': parsed ${parsedServers.size} servers")
                } else {
                    log("SUBSCRIPTION", "WARN", "No valid server configurations parsed from subscription")
                }
            }
        } catch (e: Exception) {
            log("SUBSCRIPTION", "ERROR", "Network error updating ${sub.name}: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    suspend fun testServerLatency(server: VpnServer): Int = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val socket = Socket()
            val socketAddress = InetSocketAddress(server.address, server.port)
            socket.connect(socketAddress, 2500) // 2.5s connection timeout
            socket.close()
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime).toInt()
            vpnDao.updateServerLatency(server.id, duration)
            duration
        } catch (e: Exception) {
            vpnDao.updateServerLatency(server.id, -1)
            -1
        }
    }

    suspend fun log(tag: String, level: String, message: String) {
        vpnDao.insertLog(VpnLog(tag = tag, level = level, message = message))
    }

    suspend fun clearLogs() {
        vpnDao.clearLogs()
    }
}
