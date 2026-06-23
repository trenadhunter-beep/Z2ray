package com.z2ray.android.vpn

import android.content.Context
import android.net.VpnService
import com.z2ray.android.BuildConfig
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

data class CoreRuntimeInfo(
    val available: Boolean,
    val version: String,
    val provider: String,
    val updatePolicy: String
)

class XrayCoreBridge(private val context: Context) {

    private var controller: Any? = null

    fun isAvailable(): Boolean = runCatching {
        Class.forName("libv2ray.Libv2ray")
        Class.forName("libv2ray.CoreCallbackHandler")
        true
    }.getOrDefault(false)

    fun start(configJson: String, tunFd: Int, onStatus: (String) -> Unit): Boolean {
        return runCatching {
            val libClass = Class.forName("libv2ray.Libv2ray")
            val callbackInterface = Class.forName("libv2ray.CoreCallbackHandler")

            val callback = Proxy.newProxyInstance(
                callbackInterface.classLoader,
                arrayOf(callbackInterface)
            ) { _, method, args ->
                when (method.name) {
                    "startup" -> {
                        onStatus("Xray core started")
                        0L
                    }
                    "shutdown" -> {
                        onStatus("Xray core stopped")
                        0L
                    }
                    "onEmitStatus" -> {
                        onStatus(args?.getOrNull(1)?.toString() ?: "Xray status changed")
                        0L
                    }
                    "protect", "protectFd", "onProtect" -> protectSocket(args)
                    else -> {
                        // Some libv2ray/AndroidLibXray builds expose callback names with
                        // slightly different casing/signatures. If it looks like a protect
                        // callback, protect the socket to prevent VPN recursion.
                        if (method.name.contains("protect", ignoreCase = true)) protectSocket(args) else 0L
                    }
                }
            }

            val assetDir = XrayAssetManager.prepare(context).absolutePath
            libClass.requireMethod("initCoreEnv", 2).invoke(null, assetDir, "Z2ray")

            controller = libClass.requireMethod("newCoreController", 1).invoke(null, callback)
                ?: error("newCoreController returned null")

            controller!!.javaClass.requireMethod("startLoop", 2).invoke(controller, configJson, tunFd)
            true
        }.onFailure { error ->
            onStatus("Xray core failed: ${error.rootMessage()}")
            controller = null
        }.getOrDefault(false)
    }

    fun stop(onStatus: (String) -> Unit = {}) {
        runCatching {
            val current = controller ?: return@runCatching
            current.javaClass.findMethod("stopLoop", 0)?.invoke(current)
            onStatus("Xray core stopped")
        }
        controller = null
    }

    fun queryTraffic(): Pair<Long, Long> {
        val current = controller ?: return 0L to 0L
        return runCatching {
            val method = current.javaClass.requireMethod("queryStats", 2)
            val down = method.invoke(current, "proxy", "downlink").toLongSafe()
            val up = method.invoke(current, "proxy", "uplink").toLongSafe()
            down to up
        }.getOrDefault(0L to 0L)
    }

    fun checkVersion(): String = runCatching {
        val libClass = Class.forName("libv2ray.Libv2ray")
        libClass.requireMethod("checkVersionX", 0).invoke(null)?.toString() ?: "Xray core version unavailable"
    }.getOrDefault("Xray core not installed")

    fun runtimeInfo(): CoreRuntimeInfo {
        val available = isAvailable()
        val version = checkVersion()
        return CoreRuntimeInfo(
            available = available,
            version = version,
            provider = if (available) "libv2ray / ${BuildConfig.CORE_FLAVOR.uppercase()} bundled core" else "missing libv2ray.aar",
            updatePolicy = "Core binary is selected at build time with -PCORE_FLAVOR=xray or -PCORE_FLAVOR=v2fly."
        )
    }

    private fun protectSocket(args: Array<Any?>?): Any {
        val fd = args?.firstOrNull { it is Int || it is Long || it is Number }.toLongSafe().toInt()
        val ok = fd > 0 && (context as? VpnService)?.protect(fd) == true
        return if (ok) 0L else -1L
    }

    private fun Class<*>.requireMethod(name: String, paramCount: Int): Method {
        return findMethod(name, paramCount) ?: error("Method not found: ${this.name}.$name/$paramCount")
    }

    private fun Class<*>.findMethod(name: String, paramCount: Int): Method? {
        return methods.firstOrNull { it.name == name && it.parameterTypes.size == paramCount }
    }

    private fun Any?.toLongSafe(): Long = when (this) {
        is Long -> this
        is Int -> this.toLong()
        is Number -> this.toLong()
        else -> 0L
    }

    private fun Throwable.rootMessage(): String {
        val root = if (this is InvocationTargetException) targetException ?: this else this
        return root.message ?: root.javaClass.simpleName
    }
}
