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
                trimmed.startsWith("vless://") -> parseVless(trimmed, defaultGroup)
                trimmed.startsWith("vmess://") -> parseVmess(trimmed, defaultGroup)
                trimmed.startsWith("trojan://") -> parseTrojan(trimmed, defaultGroup)
                trimmed.startsWith("ss://") -> parseShadowsocks(trimmed, defaultGroup)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeBase64Safe(input: String): String {
        return try {
            val cleaned = input.replace("-", "+").replace("_", "/").trim()
            val padded = when (cleaned.length % 4) {
                2 -> "$cleaned=="
                3 -> "$cleaned="
                else -> cleaned
            }
            val decodedBytes = Base64.decode(padded, Base64.DEFAULT)
            String(decodedBytes, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseVless(link: String, groupName: String): VpnServer? {
        // vless://uuid@host:port?param=val#description
        val uriStr = link.substring(8)
        val hashIndex = uriStr.indexOf('#')
        val rawDesc = if (hashIndex != -1) uriStr.substring(hashIndex + 1) else "VLESS Server"
        val desc = try { URLDecoder.decode(rawDesc, "UTF-8") } catch (e: Exception) { rawDesc }
        
        val mainPart = if (hashIndex != -1) uriStr.substring(0, hashIndex) else uriStr
        val queryIndex = mainPart.indexOf('?')
        val queryParams = if (queryIndex != -1) mainPart.substring(queryIndex + 1) else ""
        
        val credAndSocket = if (queryIndex != -1) mainPart.substring(0, queryIndex) else mainPart
        val atIndex = credAndSocket.indexOf('@')
        if (atIndex == -1) return null
        
        val uuid = credAndSocket.substring(0, atIndex)
        val socketPart = credAndSocket.substring(atIndex + 1)
        val colonIndex = socketPart.indexOf(':')
        if (colonIndex == -1) return null
        
        val address = socketPart.substring(0, colonIndex)
        val port = socketPart.substring(colonIndex + 1).toIntOrNull() ?: 443
        
        // Parse Query Parameters
        var security = "none"
        var path = ""
        var host = ""
        var netType = "tcp"
        var sni = ""
        
        if (queryParams.isNotEmpty()) {
            val pairs = queryParams.split("&")
            for (pair in pairs) {
                val parts = pair.split("=")
                if (parts.size == 2) {
                    val key = parts[0].lowercase()
                    val value = try { URLDecoder.decode(parts[1], "UTF-8") } catch (e: Exception) { parts[1] }
                    when (key) {
                        "security" -> security = value
                        "path" -> path = value
                        "host" -> host = value
                        "type" -> netType = value
                        "sni" -> sni = value
                    }
                }
            }
        }
        
        return VpnServer(
            name = desc,
            address = address,
            port = port,
            uuid = uuid,
            protocol = "VLESS",
            security = security,
            sni = if (sni.isNotEmpty()) sni else host,
            path = path,
            host = host,
            networkType = netType,
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseVmess(link: String, groupName: String): VpnServer? {
        // vmess://base64EncodedJson
        val encodedPayload = link.substring(8)
        val jsonStr = decodeBase64Safe(encodedPayload)
        if (jsonStr.isEmpty()) return null
        
        val json = JSONObject(jsonStr)
        val ps = json.optString("ps", "VMess Server")
        val add = json.optString("add", "")
        val port = json.optInt("port", 443)
        val id = json.optString("id", "")
        val net = json.optString("net", "tcp")
        val path = json.optString("path", "")
        val host = json.optString("host", "")
        val tls = json.optString("tls", "")
        val sni = json.optString("sni", "")
        
        if (add.isEmpty() || id.isEmpty()) return null

        return VpnServer(
            name = ps,
            address = add,
            port = port,
            uuid = id,
            protocol = "VMESS",
            security = if (tls == "tls") "tls" else "none",
            sni = if (sni.isNotEmpty()) sni else host,
            path = path,
            host = host,
            networkType = net,
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseTrojan(link: String, groupName: String): VpnServer? {
        // trojan://password@host:port?param=val#description
        val uriStr = link.substring(9)
        val hashIndex = uriStr.indexOf('#')
        val rawDesc = if (hashIndex != -1) uriStr.substring(hashIndex + 1) else "Trojan Server"
        val desc = try { URLDecoder.decode(rawDesc, "UTF-8") } catch (e: Exception) { rawDesc }
        
        val mainPart = if (hashIndex != -1) uriStr.substring(0, hashIndex) else uriStr
        val queryIndex = mainPart.indexOf('?')
        val queryParams = if (queryIndex != -1) mainPart.substring(queryIndex + 1) else ""
        
        val credAndSocket = if (queryIndex != -1) mainPart.substring(0, queryIndex) else mainPart
        val atIndex = credAndSocket.indexOf('@')
        if (atIndex == -1) return null
        
        val password = credAndSocket.substring(0, atIndex)
        val socketPart = credAndSocket.substring(atIndex + 1)
        val colonIndex = socketPart.indexOf(':')
        if (colonIndex == -1) return null
        
        val address = socketPart.substring(0, colonIndex)
        val port = socketPart.substring(colonIndex + 1).toIntOrNull() ?: 443
        
        var security = "tls"
        var path = ""
        var host = ""
        var sni = ""
        var netType = "tcp"
        
        if (queryParams.isNotEmpty()) {
            val pairs = queryParams.split("&")
            for (pair in pairs) {
                val parts = pair.split("=")
                if (parts.size == 2) {
                    val key = parts[0].lowercase()
                    val value = try { URLDecoder.decode(parts[1], "UTF-8") } catch (e: Exception) { parts[1] }
                    when (key) {
                        "security" -> security = value
                        "path" -> path = value
                        "host" -> host = value
                        "sni" -> sni = value
                        "type" -> netType = value
                    }
                }
            }
        }
        
        return VpnServer(
            name = desc,
            address = address,
            port = port,
            uuid = password,
            protocol = "TROJAN",
            security = security,
            sni = if (sni.isNotEmpty()) sni else host,
            path = path,
            host = host,
            networkType = netType,
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseShadowsocks(link: String, groupName: String): VpnServer? {
        // ss://base64_of_method_password@host:port#description
        val uriStr = link.substring(5)
        val hashIndex = uriStr.indexOf('#')
        val rawDesc = if (hashIndex != -1) uriStr.substring(hashIndex + 1) else "SS Server"
        val desc = try { URLDecoder.decode(rawDesc, "UTF-8") } catch (e: Exception) { rawDesc }
        
        val mainPart = if (hashIndex != -1) uriStr.substring(0, hashIndex) else uriStr
        
        // Let's check for standard ss format: ss://base64(method:password@host:port) or ss://base64(method:password)@host:port
        val atIndex = mainPart.indexOf('@')
        if (atIndex != -1) {
            val base64Part = mainPart.substring(0, atIndex)
            val socketPart = mainPart.substring(atIndex + 1)
            val colonIndex = socketPart.indexOf(':')
            if (colonIndex == -1) return null
            
            val address = socketPart.substring(0, colonIndex)
            val port = socketPart.substring(colonIndex + 1).toIntOrNull() ?: 8388
            val decodedCreds = decodeBase64Safe(base64Part)
            
            return VpnServer(
                name = desc,
                address = address,
                port = port,
                uuid = decodedCreds, // stores method:password
                protocol = "SHADOWSOCKS",
                security = "none",
                groupName = groupName,
                originalLink = link
            )
        } else {
            // Whole thing base64 encoded
            val decodedFull = decodeBase64Safe(mainPart)
            if (decodedFull.isEmpty()) return null
            // Form: method:password@host:port
            val innerAt = decodedFull.indexOf('@')
            if (innerAt == -1) return null
            
            val methodPass = decodedFull.substring(0, innerAt)
            val socketPart = decodedFull.substring(innerAt + 1)
            val colonIndex = socketPart.indexOf(':')
            if (colonIndex == -1) return null
            
            val address = socketPart.substring(0, colonIndex)
            val port = socketPart.substring(colonIndex + 1).toIntOrNull() ?: 8388
            
            return VpnServer(
                name = desc,
                address = address,
                port = port,
                uuid = methodPass,
                protocol = "SHADOWSOCKS",
                security = "none",
                groupName = groupName,
                originalLink = link
            )
        }
    }
}
