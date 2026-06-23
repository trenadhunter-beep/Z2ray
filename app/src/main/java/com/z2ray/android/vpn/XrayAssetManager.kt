package com.z2ray.android.vpn

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class RoutingAssetStatus(
    val name: String,
    val ready: Boolean,
    val sizeBytes: Long,
    val sha256: String,
    val source: String,
    val message: String = ""
)

enum class GeoDataSource(val title: String, val geoipUrl: String, val geositeUrl: String) {
    ENHANCED(
        "Enhanced (Loyalsoldier, v2rayNG default-style)",
        "https://github.com/Loyalsoldier/geoip/releases/latest/download/geoip.dat",
        "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat"
    ),
    OFFICIAL(
        "Official v2fly community data",
        "https://github.com/v2fly/geoip/releases/latest/download/geoip.dat",
        "https://github.com/v2fly/domain-list-community/releases/latest/download/dlc.dat"
    )
}

object XrayAssetManager {
    private val requiredAssets = listOf("geoip.dat", "geosite.dat")

    fun assetsDir(context: Context): File {
        val dir = File(context.filesDir, "xray_assets")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun prepare(context: Context): File {
        val dir = assetsDir(context)
        requiredAssets.forEach { name ->
            val out = File(dir, name)
            if (!out.exists() || out.length() == 0L) {
                copyBundledAsset(context, name, out)
            }
        }
        return dir
    }

    fun status(context: Context): Map<String, Boolean> {
        val dir = assetsDir(context)
        return requiredAssets.associateWith { name ->
            File(dir, name).let { file -> file.exists() && file.length() > 0L }
        }
    }

    fun detailedStatus(context: Context): List<RoutingAssetStatus> {
        val dir = assetsDir(context)
        return requiredAssets.map { name ->
            val file = File(dir, name)
            if (file.exists() && file.length() > 0L) {
                RoutingAssetStatus(
                    name = name,
                    ready = true,
                    sizeBytes = file.length(),
                    sha256 = file.sha256Safe(),
                    source = "app-files",
                    message = "ready"
                )
            } else {
                val bundled = runCatching { context.assets.open(name).use { it.available() > 0 } }.getOrDefault(false)
                RoutingAssetStatus(
                    name = name,
                    ready = false,
                    sizeBytes = 0L,
                    sha256 = "",
                    source = if (bundled) "bundled-fallback-available" else "missing",
                    message = if (bundled) "missing in files; bundled fallback can be copied" else "missing"
                )
            }
        }
    }

    fun statusText(context: Context): String {
        return detailedStatus(context).joinToString(" • ") { item ->
            val size = if (item.sizeBytes > 0) "${item.sizeBytes / 1024}KB" else "0KB"
            item.name + ": " + if (item.ready) "ready $size ${item.sha256.take(8)}" else item.message
        }
    }

    fun updateFromNetwork(context: Context, source: GeoDataSource = GeoDataSource.ENHANCED): Boolean {
        val dir = assetsDir(context)
        val results = listOf(
            downloadWithFallback(context, source.geoipUrl, File(dir, "geoip.dat")),
            downloadWithFallback(context, source.geositeUrl, File(dir, "geosite.dat"))
        )
        return results.all { it } && status(context).values.all { it }
    }

    private fun downloadWithFallback(context: Context, url: String, output: File): Boolean {
        return runCatching {
            downloadToFile(url, output)
            output.exists() && output.length() > 0L
        }.getOrElse {
            // If network download fails, preserve current file. If current file is missing, try bundled assets.
            if (!output.exists() || output.length() == 0L) {
                copyBundledAsset(context, output.name, output)
            }
            output.exists() && output.length() > 0L
        }
    }

    private fun copyBundledAsset(context: Context, name: String, output: File): Boolean = runCatching {
        if (!output.parentFile.exists()) output.parentFile.mkdirs()
        context.assets.open(name).use { input ->
            val temp = File(output.parentFile, "${output.name}.bundled.tmp")
            temp.outputStream().use { out -> input.copyTo(out) }
            if (temp.length() <= 0L) error("Bundled asset is empty: $name")
            if (output.exists()) output.delete()
            temp.renameTo(output)
        }
        true
    }.getOrDefault(false)

    private fun downloadToFile(url: String, output: File) {
        if (!output.parentFile.exists()) output.parentFile.mkdirs()
        val temp = File(output.parentFile, "${output.name}.download.tmp")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Z2ray-Asset-Updater/1.0")
        }
        connection.inputStream.use { input ->
            temp.outputStream().use { out -> input.copyTo(out) }
        }
        if (temp.length() <= 0L) error("Downloaded empty file: ${output.name}")
        if (output.exists()) output.delete()
        if (!temp.renameTo(output)) error("Failed to replace ${output.name}")
    }

    private fun File.sha256Safe(): String = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrDefault("")
}
