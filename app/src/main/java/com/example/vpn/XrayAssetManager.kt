package com.example.vpn

import android.content.Context
import java.io.File
import java.net.URL

object XrayAssetManager {
    private val requiredAssets = listOf("geoip.dat", "geosite.dat")
    private const val GEOIP_URL = "https://github.com/Loyalsoldier/geoip/releases/latest/download/geoip.dat"
    private const val GEOSITE_URL = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat"

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
                runCatching {
                    context.assets.open(name).use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                }
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

    fun statusText(context: Context): String {
        val status = status(context)
        return status.entries.joinToString(" • ") { (name, ok) -> "$name: ${if (ok) "ready" else "missing"}" }
    }

    fun updateFromNetwork(context: Context): Boolean {
        val dir = assetsDir(context)
        downloadToFile(GEOIP_URL, File(dir, "geoip.dat"))
        downloadToFile(GEOSITE_URL, File(dir, "geosite.dat"))
        return status(context).values.all { it }
    }

    private fun downloadToFile(url: String, output: File) {
        val temp = File(output.parentFile, "${output.name}.tmp")
        URL(url).openStream().use { input ->
            temp.outputStream().use { out -> input.copyTo(out) }
        }
        if (temp.length() <= 0L) error("Downloaded empty file: ${output.name}")
        if (output.exists()) output.delete()
        temp.renameTo(output)
    }
}
