package com.z2ray.android.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.Locale

object VpnParser {

    fun parseLine(line: String, defaultGroup: String = "Imported"): VpnServer? {
        val trimmed = line.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) return null

        return try {
            val lower = trimmed.lowercase(Locale.US)
            val startIndex = when {
                trimmed.startsWith("{") -> 0
                lower.contains("vless://") -> trimmed.indexOf("vless://", ignoreCase = true)
                lower.contains("vmess://") -> trimmed.indexOf("vmess://", ignoreCase = true)
                lower.contains("trojan://") -> trimmed.indexOf("trojan://", ignoreCase = true)
                lower.contains("ss://") -> trimmed.indexOf("ss://", ignoreCase = true)
                lower.contains("hysteria2://") -> trimmed.indexOf("hysteria2://", ignoreCase = true)
                lower.contains("hy2://") -> trimmed.indexOf("hy2://", ignoreCase = true)
                lower.contains("tuic://") -> trimmed.indexOf("tuic://", ignoreCase = true)
                lower.contains("socks://") -> trimmed.indexOf("socks://", ignoreCase = true)
                lower.contains("socks5://") -> trimmed.indexOf("socks5://", ignoreCase = true)
                else -> -1
            }

            if (startIndex == -1) return null
            val configPart = trimmed.substring(startIndex)

            // Extract only the link (until first space, tab, newline, or quote)
            val cleanConfig = configPart.split(Regex("[\\s\"']")).first()

            when {
                cleanConfig.startsWith("{", ignoreCase = false) -> parseRawJsonConfig(cleanConfig, defaultGroup)
                cleanConfig.startsWith("vless://", ignoreCase = true) -> parseVless(cleanConfig, defaultGroup)
                cleanConfig.startsWith("vmess://", ignoreCase = true) -> parseVmess(cleanConfig, defaultGroup)
                cleanConfig.startsWith("trojan://", ignoreCase = true) -> parseTrojan(cleanConfig, defaultGroup)
                cleanConfig.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(cleanConfig, defaultGroup)
                cleanConfig.startsWith("hysteria2://", ignoreCase = true) || cleanConfig.startsWith("hy2://", ignoreCase = true) -> parseHysteria2(cleanConfig, defaultGroup)
                cleanConfig.startsWith("tuic://", ignoreCase = true) -> parseTuic(cleanConfig, defaultGroup)
                cleanConfig.startsWith("socks://", ignoreCase = true) || cleanConfig.startsWith("socks5://", ignoreCase = true) -> parseSocks(cleanConfig, defaultGroup)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses common subscription payloads:
     * - plain line-based share links
     * - base64 line-based subscriptions
     * - raw Xray JSON configs
     * - SIP008 Shadowsocks JSON
     * - a pragmatic subset of Clash/Mihomo YAML proxies
     */
    fun parseMany(text: String, defaultGroup: String = "Imported"): List<VpnServer> {
        val raw = text.trim().removePrefix("\uFEFF")
        if (raw.isBlank()) return emptyList()

        val decoded = decodeBase64Safe(raw)
        val candidates = listOf(raw, decoded).filter { it.isNotBlank() }.distinct()

        for (candidate in candidates) {
            val parsedJson = parseJsonPayload(candidate, defaultGroup)
            if (parsedJson.isNotEmpty()) return parsedJson.distinctServers()

            val parsedClash = parseClashYaml(candidate, defaultGroup)
            if (parsedClash.isNotEmpty()) return parsedClash.distinctServers()

            val lines = candidate
                .replace("\r", "\n")
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
            val parsedLines = lines.mapNotNull { parseLine(it, defaultGroup) }
            if (parsedLines.isNotEmpty()) return parsedLines.distinctServers()
        }

        return emptyList()
    }

    fun decodeBase64Safe(input: String): String {
        return try {
            val decodedInput = decodeUrl(input)
            val cleaned = decodedInput
                .substringBefore("#")
                .substringBefore("?")
                .replace("-", "+")
                .replace("_", "/")
                .replace("\n", "")
                .replace("\r", "")
                .trim()
            if (cleaned.isBlank() || cleaned.any { it == ':' || it == '{' || it == '[' }) return ""
            val padded = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
            val decodedBytes = Base64.decode(padded, Base64.DEFAULT)
            String(decodedBytes, Charset.forName("UTF-8"))
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseJsonPayload(payload: String, groupName: String): List<VpnServer> {
        val trimmed = payload.trim()
        if (!trimmed.startsWith("{")) return emptyList()
        return try {
            val json = JSONObject(trimmed)
            when {
                json.has("outbounds") || json.has("inbounds") -> listOf(parseRawJsonConfig(trimmed, groupName)!!)
                json.has("servers") -> parseSip008(json, groupName)
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseRawJsonConfig(json: String, groupName: String): VpnServer? {
        val obj = JSONObject(json)
        val remarks = obj.optString("remarks", obj.optString("name", "Custom Xray JSON"))
        return VpnServer(
            name = remarks.ifBlank { "Custom Xray JSON" },
            address = "custom-json.local",
            port = 0,
            uuid = "",
            protocol = "CUSTOM_JSON",
            groupName = groupName,
            originalLink = json,
            rawJson = json
        )
    }

    private fun parseSip008(json: JSONObject, groupName: String): List<VpnServer> {
        val group = json.optString("airport", groupName).ifBlank { groupName }
        val arr = json.optJSONArray("servers") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { index ->
            val item = arr.optJSONObject(index) ?: return@mapNotNull null
            val method = item.optString("method", "")
            val password = item.optString("password", "")
            val address = item.optString("server", item.optString("address", ""))
            val port = item.optInt("server_port", item.optInt("port", 8388))
            if (address.isBlank() || method.isBlank() || password.isBlank()) return@mapNotNull null
            VpnServer(
                name = item.optString("remarks", item.optString("name", "Shadowsocks Server")),
                address = address,
                port = port,
                uuid = password,
                protocol = "SHADOWSOCKS",
                security = method,
                groupName = group,
                isCustom = false,
                originalLink = item.toString()
            )
        }
    }

    private fun parseClashYaml(yaml: String, groupName: String): List<VpnServer> {
        if (!yaml.contains("proxies:") && !yaml.contains("- name:")) return emptyList()
        val blocks = mutableListOf<MutableMap<String, String>>()
        var current: MutableMap<String, String>? = null
        var inHeaders = false
        var inReality = false
        var inWs = false
        var inGrpc = false

        yaml.replace("\r", "\n").lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) return@forEach
            if (line.startsWith("- {")) {
                inlineYamlMap(line.removePrefix("- ")).takeIf { it.isNotEmpty() }?.let { blocks.add(it.toMutableMap()) }
                current = null
                return@forEach
            }
            if (line.startsWith("- name:")) {
                current?.let { if (it.isNotEmpty()) blocks.add(it) }
                current = mutableMapOf("name" to cleanYamlValue(line.substringAfter(":")))
                inHeaders = false; inReality = false; inWs = false; inGrpc = false
                return@forEach
            }
            val map = current ?: return@forEach
            when (line.removeSuffix(":")) {
                "headers" -> { inHeaders = true; return@forEach }
                "reality-opts", "reality_opts" -> { inReality = true; inHeaders = false; return@forEach }
                "ws-opts", "ws_opts" -> { inWs = true; inHeaders = false; return@forEach }
                "grpc-opts", "grpc_opts" -> { inGrpc = true; inHeaders = false; return@forEach }
            }
            if (!line.contains(":")) return@forEach
            val keyRaw = line.substringBefore(":").trim().lowercase(Locale.US).replace("-", "_")
            val value = cleanYamlValue(line.substringAfter(":"))
            val key = when {
                inHeaders && keyRaw == "host" -> "host"
                inReality && keyRaw == "public_key" -> "pbk"
                inReality && keyRaw == "short_id" -> "sid"
                inWs && keyRaw == "path" -> "path"
                inWs && keyRaw == "headers" -> "headers"
                inGrpc && keyRaw == "grpc_service_name" -> "service_name"
                else -> keyRaw
            }
            map[key] = value
        }
        current?.let { if (it.isNotEmpty()) blocks.add(it) }

        return blocks.mapNotNull { map -> clashMapToServer(map, groupName) }
    }

    private fun inlineYamlMap(value: String): Map<String, String> {
        val trimmed = value.trim().removePrefix("{").removeSuffix("}")
        if (trimmed.isBlank()) return emptyMap()
        return trimmed.split(",")
            .mapNotNull { part ->
                val pieces = part.split(":", limit = 2)
                if (pieces.size != 2) null else pieces[0].trim().lowercase(Locale.US).replace("-", "_") to cleanYamlValue(pieces[1])
            }
            .toMap()
    }

    private fun clashMapToServer(map: Map<String, String>, groupName: String): VpnServer? {
        val type = map["type"]?.uppercase(Locale.US) ?: return null
        val address = map["server"] ?: map["address"] ?: return null
        val port = map["port"]?.toIntOrNull() ?: map["server_port"]?.toIntOrNull() ?: defaultPortFor(type)
        val tls = map["tls"].asBool() || map["network"] == "reality"
        val reality = map["pbk"] != null || map["public_key"] != null || map["reality_opts"] != null
        val network = map["network"] ?: map["net"] ?: map["type"]?.takeIf { it in listOf("ws", "grpc", "h2", "http", "httpupgrade", "xhttp") } ?: "tcp"
        return VpnServer(
            name = map["name"] ?: "$type Server",
            address = address,
            port = port,
            uuid = map["uuid"] ?: map["password"] ?: map["username"] ?: "",
            protocol = when (type) {
                "SS" -> "SHADOWSOCKS"
                "HY2" -> "HYSTERIA2"
                else -> type
            },
            security = when {
                type == "SHADOWSOCKS" || type == "SS" -> map["cipher"] ?: map["method"] ?: "aes-128-gcm"
                reality -> "reality"
                tls -> "tls"
                else -> map["security"] ?: "none"
            },
            sni = map["servername"] ?: map["sni"] ?: map["server_name"] ?: "",
            host = map["host"] ?: map["ws_host"] ?: "",
            path = map["path"] ?: "",
            networkType = network,
            flow = map["flow"] ?: "",
            publicKey = map["pbk"] ?: map["public_key"] ?: "",
            shortId = map["sid"] ?: map["short_id"] ?: "",
            fingerprint = map["client_fingerprint"] ?: map["fingerprint"] ?: map["fp"] ?: "chrome",
            alpn = map["alpn"] ?: "",
            encryption = map["encryption"] ?: map["scy"] ?: "",
            alterId = map["alterId"]?.toIntOrNull() ?: map["alterid"]?.toIntOrNull() ?: map["aid"]?.toIntOrNull() ?: 0,
            serviceName = map["service_name"] ?: map["grpc_service_name"] ?: "",
            authority = map["authority"] ?: "",
            obfsType = map["obfs"] ?: map["obfs_type"] ?: "",
            obfsPassword = map["obfs_password"] ?: "",
            congestion = map["congestion_control"] ?: map["congestion"] ?: "",
            upMbps = map["up"]?.filter { it.isDigit() }?.toIntOrNull() ?: map["up_mbps"]?.toIntOrNull() ?: 0,
            downMbps = map["down"]?.filter { it.isDigit() }?.toIntOrNull() ?: map["down_mbps"]?.toIntOrNull() ?: 0,
            udpRelayMode = map["udp_relay_mode"] ?: "",
            allowInsecure = map["skip_cert_verify"].asBool() || map["allow_insecure"].asBool(),
            groupName = groupName,
            isCustom = false,
            originalLink = map.entries.joinToString(prefix = "clash:{", postfix = "}") { "${it.key}=${it.value}" }
        )
    }

    private fun cleanYamlValue(value: String): String = value.trim().trim('"', '\'', ' ', ',')

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
                    parts[0].lowercase(Locale.US) to decodeUrl(parts.getOrElse(1) { "" })
                }
            }
            .toMap()
    }

    private fun splitHostPort(socketPart: String, defaultPort: Int): Pair<String, Int>? {
        var cleaned = socketPart.trim()
        if (cleaned.isBlank()) return null
        
        // Remove trailing slashes and any path
        cleaned = cleaned.substringBefore("/").trim()
        
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
        val host = params["host"] ?: params["authority"] ?: ""
        val sni = params["sni"] ?: params["servername"] ?: params["peer"] ?: host
        val network = params["type"] ?: params["network"] ?: "tcp"

        return VpnServer(
            name = desc,
            address = socket.first,
            port = socket.second,
            uuid = uuid,
            protocol = "VLESS",
            security = security,
            sni = sni,
            path = params["path"] ?: params["servicename"] ?: "",
            host = host,
            networkType = network,
            flow = params["flow"] ?: "",
            publicKey = params["pbk"] ?: params["publickey"] ?: params["public_key"] ?: params["public-key"] ?: "",
            shortId = params["sid"] ?: params["shortid"] ?: params["short_id"] ?: params["short-id"] ?: "",
            fingerprint = params["fp"] ?: params["fingerprint"] ?: "chrome",
            spiderX = params["spx"] ?: params["spiderx"] ?: params["spider_x"] ?: params["spider-x"] ?: "",
            alpn = params["alpn"] ?: "",
            encryption = params["encryption"] ?: "none",
            packetEncoding = params["packetencoding"] ?: "",
            headerType = params["headertype"] ?: params["header"] ?: "",
            serviceName = params["servicename"] ?: "",
            authority = params["authority"] ?: host,
            mode = params["mode"] ?: "",
            quicSecurity = params["quicsecurity"] ?: params["quic_security"] ?: "",
            quicKey = params["key"] ?: "",
            seed = params["seed"] ?: "",
            pinnedPeerCertSha256 = params["pinnedpeercertsha256"] ?: "",
            verifyPeerCertByName = params["verifypeercertbyname"] ?: "",
            echConfigList = params["ech"] ?: params["echconfiglist"] ?: "",
            allowInsecure = params.isInsecure(),
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseVmess(link: String, groupName: String): VpnServer? {
        val encodedPayload = link.substringAfter("://")
        val jsonStr = decodeBase64Safe(encodedPayload)
        if (jsonStr.isEmpty()) return null

        val json = JSONObject(jsonStr)
        val add = json.optString("add", "")
        val port = json.optString("port", "443").toIntOrNull() ?: json.optInt("port", 443)
        val id = json.optString("id", json.optString("uuid", ""))
        if (add.isEmpty() || id.isEmpty()) return null

        val net = json.optString("net", "tcp")
        val path = json.optString("path", "")
        val host = json.optString("host", "")
        
        val tlsObj = json.opt("tls")
        val securityVal = json.optString("security", "")
        val security = when {
            tlsObj == "reality" || securityVal.equals("reality", true) -> "reality"
            tlsObj == "tls" || tlsObj == true || tlsObj == "true" || securityVal.equals("tls", true) -> "tls"
            else -> "none"
        }
        
        val sni = json.optString("sni", "").ifBlank { json.optString("serverName", "") }
        val fingerprint = json.optString("fp", "").ifBlank { json.optString("fingerprint", "chrome") }

        return VpnServer(
            name = json.optString("ps", "VMess Server").ifBlank { "VMess Server" },
            address = add,
            port = port,
            uuid = id,
            protocol = "VMESS",
            security = security,
            sni = sni.ifBlank { host },
            path = path,
            host = host,
            networkType = net.ifBlank { "tcp" },
            encryption = json.optString("scy", json.optString("security", "auto")).ifBlank { "auto" },
            alterId = json.optString("aid", "0").toIntOrNull() ?: json.optInt("aid", 0),
            fingerprint = fingerprint,
            alpn = json.optString("alpn", ""),
            headerType = json.optString("type", ""),
            serviceName = json.optString("serviceName", ""),
            authority = json.optString("authority", ""),
            allowInsecure = json.optBoolean("allowInsecure", false) ||
                            json.optString("allowInsecure", "false").equals("true", true) ||
                            json.optBoolean("skip-cert-verify", false) ||
                            json.optString("skip-cert-verify", "false").equals("true", true),
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
        val host = params["host"] ?: params["authority"] ?: ""
        val sni = params["sni"] ?: params["peer"] ?: params["servername"] ?: host

        return VpnServer(
            name = desc,
            address = socket.first,
            port = socket.second,
            uuid = password,
            protocol = "TROJAN",
            security = params["security"] ?: "tls",
            sni = sni,
            path = params["path"] ?: params["servicename"] ?: "",
            host = host,
            networkType = params["type"] ?: params["network"] ?: "tcp",
            fingerprint = params["fp"] ?: params["fingerprint"] ?: "chrome",
            alpn = params["alpn"] ?: "",
            headerType = params["headertype"] ?: params["header"] ?: "",
            serviceName = params["servicename"] ?: "",
            authority = params["authority"] ?: host,
            pinnedPeerCertSha256 = params["pinnedpeercertsha256"] ?: "",
            verifyPeerCertByName = params["verifypeercertbyname"] ?: "",
            echConfigList = params["ech"] ?: params["echconfiglist"] ?: "",
            allowInsecure = params.isInsecure(),
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

    private fun parseHysteria2(link: String, groupName: String): VpnServer? {
        val uriStr = link.substringAfter("://")
        val rawDesc = uriStr.substringAfter("#", "Hysteria2 Server")
        val desc = decodeUrl(rawDesc).ifBlank { "Hysteria2 Server" }
        val mainPart = uriStr.substringBefore("#")
        val queryParams = mainPart.substringAfter("?", "")
        val credAndSocket = mainPart.substringBefore("?")
        val atIndex = credAndSocket.indexOf('@')
        val password = if (atIndex >= 0) decodeUrl(credAndSocket.substring(0, atIndex)) else ""
        val socketPart = if (atIndex >= 0) credAndSocket.substring(atIndex + 1) else credAndSocket
        val socket = splitHostPort(socketPart, 443) ?: return null
        val params = queryMap(queryParams)
        return VpnServer(
            name = desc,
            address = socket.first,
            port = socket.second,
            uuid = password,
            protocol = "HYSTERIA2",
            security = "tls",
            sni = params["sni"] ?: params["peer"] ?: params["servername"] ?: "",
            alpn = params["alpn"] ?: "h3",
            obfsType = params["obfs"] ?: params["obfstype"] ?: "",
            obfsPassword = params["obfs-password"] ?: params["obfspassword"] ?: params["obfs_password"] ?: "",
            congestion = params["congestion"] ?: params["congestioncontrol"] ?: "",
            upMbps = params["upmbps"]?.toIntOrNull() ?: params["up"]?.toIntOrNull() ?: 0,
            downMbps = params["downmbps"]?.toIntOrNull() ?: params["down"]?.toIntOrNull() ?: 0,
            allowInsecure = params.isInsecure(),
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseTuic(link: String, groupName: String): VpnServer? {
        val uriStr = link.substringAfter("://")
        val rawDesc = uriStr.substringAfter("#", "TUIC Server")
        val desc = decodeUrl(rawDesc).ifBlank { "TUIC Server" }
        val mainPart = uriStr.substringBefore("#")
        val queryParams = mainPart.substringAfter("?", "")
        val credAndSocket = mainPart.substringBefore("?")
        val atIndex = credAndSocket.indexOf('@')
        if (atIndex == -1) return null
        val auth = decodeUrl(credAndSocket.substring(0, atIndex))
        val socket = splitHostPort(credAndSocket.substring(atIndex + 1), 443) ?: return null
        val params = queryMap(queryParams)
        return VpnServer(
            name = desc,
            address = socket.first,
            port = socket.second,
            uuid = auth,
            protocol = "TUIC",
            security = "tls",
            sni = params["sni"] ?: params["servername"] ?: "",
            alpn = params["alpn"] ?: "h3",
            congestion = params["congestion_control"] ?: params["congestion"] ?: "bbr",
            udpRelayMode = params["udp_relay_mode"] ?: "native",
            allowInsecure = params.isInsecure(),
            groupName = groupName,
            originalLink = link
        )
    }

    private fun parseSocks(link: String, groupName: String): VpnServer? {
        val uriStr = link.substringAfter("://")
        val rawDesc = uriStr.substringAfter("#", "SOCKS Server")
        val desc = decodeUrl(rawDesc).ifBlank { "SOCKS Server" }
        val mainPart = uriStr.substringBefore("#").substringBefore("?")
        val decoded = decodeBase64Safe(mainPart).ifBlank { mainPart }
        val atIndex = decoded.indexOf('@')
        val auth = if (atIndex >= 0) decodeUrl(decoded.substring(0, atIndex)) else ""
        val socketPart = if (atIndex >= 0) decoded.substring(atIndex + 1) else decoded
        val socket = splitHostPort(socketPart, 1080) ?: return null
        return VpnServer(
            name = desc,
            address = socket.first,
            port = socket.second,
            uuid = auth,
            protocol = "SOCKS",
            security = "none",
            groupName = groupName,
            originalLink = link
        )
    }

    private fun defaultPortFor(type: String): Int = when (type.uppercase(Locale.US)) {
        "SHADOWSOCKS", "SS" -> 8388
        "SOCKS" -> 1080
        else -> 443
    }

    private fun String?.asBool(): Boolean = this?.equals("true", true) == true || this == "1" || this?.equals("yes", true) == true

    private fun Map<String, String>.isInsecure(): Boolean =
        this["allowinsecure"].asBool() || this["allow_insecure"].asBool() || this["insecure"].asBool() || this["skip_cert_verify"].asBool()

    private fun List<VpnServer>.distinctServers(): List<VpnServer> =
        distinctBy { it.originalLink.ifBlank { "${it.protocol}-${it.address}-${it.port}-${it.uuid}-${it.security}-${it.networkType}" } }
}
