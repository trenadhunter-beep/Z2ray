package com.example.data

import android.util.Base64
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.Charset

object VpnParser {

    fun parseLine(line: String, defaultGroup: String = "Imported"): VpnServer? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        return try {
            when {
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed, defaultGroup)
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed, defaultGroup)
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed, defaultGroup)
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed, defaultGroup)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64Safe(input: String): String {
        return try {
            val cleaned = input
                .substringBefore("#")
                .replace("-", "+")
                .replace("_", "/")
                .replace("\n", "")
                .replace("\r", "")
                .trim()
            val padded = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
            val decodedBytes = Base64.decode(padded, Base64.DEFAULT)
            String(decodedBytes, Charset.forName("UTF-8"))
        } catch (_: Exception) {
            ""
        }
    }

    private fun decodeUrl(value: String): String = try {
        URLDecoder.decode(value, "UTF-8")
    } catch (_: Exception) {
        value
    }

    private fun queryMap(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&")
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.isEmpty() || parts[0].isBlank()) null else {
                    parts[0].lowercase() to decodeUrl(parts.getOrElse(1) { "" })
                }
            }
            .toMap()
    }

    private fun splitHostPort(socketPart: String, defaultPort: Int): Pair<String, Int>? {
        val cleaned = socketPart.trim()
        if (cleaned.isBlank()) return null
        return if (cleaned.startsWith("[")) {
            val end = cleaned.indexOf(']')
            if (end == -1) return null
            val host = cleaned.substring(1, end)
            val port = cleaned.substring(end + 1).removePrefix(":").toIntOrNull() ?: defaultPort
            host to port
        } else {
            val colon = cleaned.lastIndexOf(':')
            if (colon == -1) cleaned to defaultPort else {
                val host = cleaned.substring(0, colon)
                val port = cleaned.substring(colon + 1).toIntOrNull() ?: defaultPort
                host to port
            }
        }
    }

    private fun parseVless(link: String, groupName: String): VpnServer? {
        val uriStr = link.substringAfter("://")
        val rawDesc = uriStr.substringAfter("#", "VLESS Server")
        val desc = decodeUrl(rawDesc).ifBlank { "VLESS Server" }
        val mainPart = uriStr.substringBefore("#")
        val queryParams = mainPart.substringAfter("?", "")
        val credAndSocket = mainPart.substringBefore("?")
        val atIndex = credAndSocket.indexOf('@')
        if (atIndex == -1) return null

        val uuid = decodeUrl(credAndSocket.substring(0, atIndex))
        val socket = splitHostPort(credAndSocket.substring(atIndex + 1), 443) ?: return null
        val params = queryMap(queryParams)

        val security = params["security"] ?: "none"
        val host = params["host"] ?: ""
        val sni = params["sni"] ?: params["servername"] ?: host
        val flow = params["flow"] ?: ""
        val publicKey = params["pbk"] ?: params["publickey"] ?: ""
        val shortId = params["sid"] ?: params["shortid"] ?: ""
        val fingerprint = params["fp"] ?: params["fingerprint"] ?: "chrome"
        val spiderX = params["spx"] ?: params["spiderx"] ?: ""
        val alpn = params["alpn"] ?: ""
        val allowInsecure = params["allowinsecure"] == "1" || params["allowinsecure"]?.equals("true", true) == true

        return VpnServer(
            name = desc,
            address = socket.first,
            port = socket.second,
            uuid = uuid,
            protocol = "VLESS",
            security = security,
            sni = sni,
            path = params["path"] ?: params["serviceName".lowercase()] ?: "",
            host = host,
            networkType = params["type"] ?: "tcp",
            flow = flow,
            publicKey = publicKey,
            shortId = shortId,
            fingerprint = fingerprint,
            spiderX = spiderX,
            alpn = alpn,
            allowInsecure = allowInsecure,
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseVmess(link: String, groupName: String): VpnServer? {
        val encodedPayload = link.substringAfter("://")
        val jsonStr = decodeBase64Safe(encodedPayload)
        if (jsonStr.isEmpty()) return null

        val json = JSONObject(jsonStr)
        val ps = json.optString("ps", "VMess Server")
        val add = json.optString("add", "")
        val port = json.optString("port", "443").toIntOrNull() ?: json.optInt("port", 443)
        val id = json.optString("id", "")
        val net = json.optString("net", "tcp")
        val path = json.optString("path", "")
        val host = json.optString("host", "")
        val tls = json.optString("tls", "")
        val sni = json.optString("sni", "").ifBlank { json.optString("serverName", "") }
        val alpn = json.optString("alpn", "")
        val fingerprint = json.optString("fp", "").ifBlank { json.optString("fingerprint", "chrome") }
        val allowInsecure = json.optString("allowInsecure", "false").equals("true", true)

        if (add.isEmpty() || id.isEmpty()) return null

        return VpnServer(
            name = ps.ifBlank { "VMess Server" },
            address = add,
            port = port,
            uuid = id,
            protocol = "VMESS",
            security = if (tls.equals("tls", true)) "tls" else "none",
            sni = sni.ifBlank { host },
            path = path,
            host = host,
            networkType = net.ifBlank { "tcp" },
            fingerprint = fingerprint,
            alpn = alpn,
            allowInsecure = allowInsecure,
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseTrojan(link: String, groupName: String): VpnServer? {
        val uriStr = link.substringAfter("://")
        val rawDesc = uriStr.substringAfter("#", "Trojan Server")
        val desc = decodeUrl(rawDesc).ifBlank { "Trojan Server" }
        val mainPart = uriStr.substringBefore("#")
        val queryParams = mainPart.substringAfter("?", "")
        val credAndSocket = mainPart.substringBefore("?")
        val atIndex = credAndSocket.indexOf('@')
        if (atIndex == -1) return null

        val password = decodeUrl(credAndSocket.substring(0, atIndex))
        val socket = splitHostPort(credAndSocket.substring(atIndex + 1), 443) ?: return null
        val params = queryMap(queryParams)
        val host = params["host"] ?: ""
        val sni = params["sni"] ?: params["peer"] ?: host
        val fingerprint = params["fp"] ?: params["fingerprint"] ?: "chrome"
        val alpn = params["alpn"] ?: ""
        val allowInsecure = params["allowinsecure"] == "1" || params["allowinsecure"]?.equals("true", true) == true

        return VpnServer(
            name = desc,
            address = socket.first,
            port = socket.second,
            uuid = password,
            protocol = "TROJAN",
            security = params["security"] ?: "tls",
            sni = sni,
            path = params["path"] ?: "",
            host = host,
            networkType = params["type"] ?: "tcp",
            fingerprint = fingerprint,
            alpn = alpn,
            allowInsecure = allowInsecure,
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseShadowsocks(link: String, groupName: String): VpnServer? {
        val uriStr = link.substringAfter("://")
        val rawDesc = uriStr.substringAfter("#", "Shadowsocks Server")
        val desc = decodeUrl(rawDesc).ifBlank { "Shadowsocks Server" }
        val mainPart = uriStr.substringBefore("#").substringBefore("?")

        val decoded = if (mainPart.contains('@')) {
            val encodedMethodPass = mainPart.substringBefore('@')
            val methodPass = decodeBase64Safe(encodedMethodPass).ifBlank { decodeUrl(encodedMethodPass) }
            val socketPart = mainPart.substringAfter('@')
            "$methodPass@$socketPart"
        } else {
            decodeBase64Safe(mainPart)
        }

        val atIndex = decoded.indexOf('@')
        if (atIndex == -1) return null
        val methodPass = decoded.substring(0, atIndex)
        val method = methodPass.substringBefore(':', "")
        val password = methodPass.substringAfter(':', "")
        if (method.isBlank() || password.isBlank()) return null

        val socket = splitHostPort(decoded.substring(atIndex + 1), 8388) ?: return null

        return VpnServer(
            name = desc,
            address = socket.first,
            port = socket.second,
            uuid = password,
            protocol = "SHADOWSOCKS",
            security = method,
            groupName = groupName,
            originalLink = link
        )
    }
}
