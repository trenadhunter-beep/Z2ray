package com.z2ray.android.vpn

import com.z2ray.android.data.CustomRoutingRule
import com.z2ray.android.data.RoutingRuleType
import com.z2ray.android.data.VpnServer
import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {
    const val LOCAL_SOCKS_PORT = 10808

    fun build(
        server: VpnServer,
        dnsServer: String,
        routingMode: String,
        customRules: List<CustomRoutingRule> = emptyList(),
        blockAds: Boolean = true,
        domainStrategy: String = "IPIfNonMatch",
        enableFragment: Boolean = false,
        fragmentSize: String = "10-20",
        fragmentInterval: String = "10-20",
        fragmentPackets: String = "tls",
        allowLan: Boolean = false
    ): String {
        if (server.protocol.equals("CUSTOM_JSON", ignoreCase = true) && server.rawJson.isNotBlank()) {
            return server.rawJson
        }

        val root = JSONObject()

        root.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        root.put("dns", JSONObject().apply {
            put("servers", JSONArray().put(dnsServer).put("https://1.1.1.1/dns-query").put("1.1.1.1"))
            put("queryStrategy", "UseIP")
        })

        root.put("inbounds", JSONArray().put(tunInbound(dnsServer)).put(localSocksInbound(allowLan)))
        root.put("outbounds", JSONArray().put(proxyOutbound(server, enableFragment, fragmentSize, fragmentInterval, fragmentPackets)).put(directOutbound()).put(blockOutbound()))
        root.put("routing", routing(routingMode, customRules, blockAds, domainStrategy))
        root.put("policy", JSONObject().apply {
            put("levels", JSONObject().apply {
                put("0", JSONObject().apply {
                    put("statsUserUplink", true)
                    put("statsUserDownlink", true)
                })
            })
            put("system", JSONObject().apply {
                put("statsOutboundUplink", true)
                put("statsOutboundDownlink", true)
            })
        })
        root.put("stats", JSONObject())

        return root.toString(2)
    }

    private fun localSocksInbound(allowLan: Boolean): JSONObject = JSONObject().apply {
        put("tag", "local-socks-in")
        put("listen", if (allowLan) "0.0.0.0" else "127.0.0.1")
        put("port", LOCAL_SOCKS_PORT)
        put("protocol", "socks")
        put("settings", JSONObject().apply {
            put("auth", "noauth")
            put("udp", true)
            put("userLevel", 0)
        })
        put("sniffing", JSONObject().apply {
            put("enabled", true)
            put("destOverride", JSONArray().put("http").put("tls").put("quic"))
            put("routeOnly", false)
        })
    }

    private fun tunInbound(dnsServer: String): JSONObject = JSONObject().apply {
        put("tag", "tun-in")
        put("protocol", "tun")
        put("settings", JSONObject().apply {
            put("name", "Z2rayTun")
            put("mtu", 1500)
            put("dns", JSONArray().put(dnsServer).put("1.1.1.1"))
            put("userLevel", 0)
        })
        put("sniffing", JSONObject().apply {
            put("enabled", true)
            put("destOverride", JSONArray().put("http").put("tls").put("quic"))
            put("routeOnly", false)
        })
    }

    private fun proxyOutbound(
        server: VpnServer,
        enableFragment: Boolean,
        fragmentSize: String,
        fragmentInterval: String,
        fragmentPackets: String
    ): JSONObject {
        return when (server.protocol.uppercase()) {
            "VMESS" -> vmessOutbound(server, enableFragment, fragmentSize, fragmentInterval, fragmentPackets)
            "TROJAN" -> trojanOutbound(server, enableFragment, fragmentSize, fragmentInterval, fragmentPackets)
            "SHADOWSOCKS", "SS" -> shadowsocksOutbound(server)
            "HYSTERIA2", "HY2", "HYSTERIA" -> hysteria2Outbound(server)
            "SOCKS", "SOCKS5" -> socksOutbound(server)
            "TUIC" -> unsupportedOutbound(server, "TUIC is not supported by Xray-core in this build. Use a raw sing-box JSON profile or another core.")
            else -> vlessOutbound(server, enableFragment, fragmentSize, fragmentInterval, fragmentPackets)
        }
    }

    private fun vlessOutbound(
        server: VpnServer,
        enableFragment: Boolean,
        fragmentSize: String,
        fragmentInterval: String,
        fragmentPackets: String
    ): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "vless")
        put("settings", JSONObject().apply {
            put("vnext", JSONArray().put(JSONObject().apply {
                put("address", server.address)
                put("port", server.port)
                put("users", JSONArray().put(JSONObject().apply {
                    put("id", server.uuid)
                    put("encryption", server.encryption.ifBlank { "none" })
                    val security = server.security.lowercase()
                    if (server.flow.isNotBlank() && (security == "tls" || security == "reality")) {
                        put("flow", server.flow)
                    }
                    if (server.packetEncoding.isNotBlank()) put("packetEncoding", server.packetEncoding)
                    put("level", 0)
                }))
            }))
        })
        put("streamSettings", streamSettings(server, enableFragment, fragmentSize, fragmentInterval, fragmentPackets))
        put("mux", muxSettings())
    }

    private fun vmessOutbound(
        server: VpnServer,
        enableFragment: Boolean,
        fragmentSize: String,
        fragmentInterval: String,
        fragmentPackets: String
    ): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "vmess")
        put("settings", JSONObject().apply {
            put("vnext", JSONArray().put(JSONObject().apply {
                put("address", server.address)
                put("port", server.port)
                put("users", JSONArray().put(JSONObject().apply {
                    put("id", server.uuid)
                    put("alterId", server.alterId)
                    put("security", server.encryption.ifBlank { "auto" })
                    put("level", 0)
                }))
            }))
        })
        put("streamSettings", streamSettings(server, enableFragment, fragmentSize, fragmentInterval, fragmentPackets))
        put("mux", muxSettings())
    }

    private fun trojanOutbound(
        server: VpnServer,
        enableFragment: Boolean,
        fragmentSize: String,
        fragmentInterval: String,
        fragmentPackets: String
    ): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "trojan")
        put("settings", JSONObject().apply {
            put("servers", JSONArray().put(JSONObject().apply {
                put("address", server.address)
                put("port", server.port)
                put("password", server.uuid)
                put("level", 0)
            }))
        })
        put("streamSettings", streamSettings(server, enableFragment, fragmentSize, fragmentInterval, fragmentPackets))
        put("mux", muxSettings())
    }

    private fun shadowsocksOutbound(server: VpnServer): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "shadowsocks")
        put("settings", JSONObject().apply {
            put("servers", JSONArray().put(JSONObject().apply {
                put("address", server.address)
                put("port", server.port)
                put("method", server.security.ifBlank { "aes-128-gcm" })
                put("password", server.uuid)
                put("level", 0)
            }))
        })
    }

    private fun hysteria2Outbound(server: VpnServer): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "hysteria")
        put("settings", JSONObject().apply {
            put("version", 2)
            put("address", server.address)
            put("port", server.port)
        })
        put("streamSettings", JSONObject().apply {
            put("network", "hysteria")
            put("security", "tls")
            put("tlsSettings", tlsSettings(server))
            put("hysteriaSettings", JSONObject().apply {
                put("version", 2)
                put("auth", server.uuid)
                put("udpIdleTimeout", 60)
                if (server.upMbps > 0) put("up", "${server.upMbps}mbps")
                if (server.downMbps > 0) put("down", "${server.downMbps}mbps")
                if (server.obfsPassword.isNotBlank()) {
                    put("obfs", JSONObject().apply {
                        put("type", server.obfsType.ifBlank { "salamander" })
                        put("password", server.obfsPassword)
                    })
                }
            })
        })
    }

    private fun socksOutbound(server: VpnServer): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "socks")
        put("settings", JSONObject().apply {
            put("servers", JSONArray().put(JSONObject().apply {
                put("address", server.address)
                put("port", server.port)
                val auth = server.uuid
                if (auth.contains(":")) {
                    put("users", JSONArray().put(JSONObject().apply {
                        put("user", auth.substringBefore(":"))
                        put("pass", auth.substringAfter(":"))
                    }))
                }
            }))
        })
    }

    private fun unsupportedOutbound(server: VpnServer, message: String): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "blackhole")
        put("settings", JSONObject().apply {
            put("response", JSONObject().apply {
                put("type", "http")
            })
        })
    }

    private fun muxSettings(): JSONObject = JSONObject().apply {
        // Disabled by default; modern VLESS/Reality/TLS nodes usually perform better without forced mux.
        put("enabled", false)
        put("concurrency", 8)
    }

    private fun streamSettings(
        server: VpnServer,
        enableFragment: Boolean,
        fragmentSize: String,
        fragmentInterval: String,
        fragmentPackets: String
    ): JSONObject = JSONObject().apply {
        val network = normalizeNetwork(server.networkType)
        val security = server.security.ifBlank { "none" }.lowercase()

        put("network", network)
        put("security", if (security == "tls" || security == "reality") security else "none")

        if (security == "tls") put("tlsSettings", tlsSettings(server))
        if (security == "reality") put("realitySettings", realitySettings(server))

        if (enableFragment) {
            put("sockopt", JSONObject().apply {
                put("fragment", JSONObject().apply {
                    put("packets", fragmentPackets.ifBlank { "tls" })
                    put("length", fragmentSize.ifBlank { "10-20" })
                    put("interval", fragmentInterval.ifBlank { "10-20" })
                })
            })
        }

        when (network) {
            "ws" -> put("wsSettings", wsSettings(server))
            "grpc" -> put("grpcSettings", grpcSettings(server))
            "http", "h2" -> put("httpSettings", httpSettings(server))
            "httpupgrade" -> put("httpupgradeSettings", httpUpgradeSettings(server))
            "xhttp" -> put("xhttpSettings", xhttpSettings(server))
            "splithttp" -> put("splithttpSettings", xhttpSettings(server))
            "kcp" -> put("kcpSettings", kcpSettings(server))
            "quic" -> put("quicSettings", quicSettings(server))
            "tcp" -> if (server.headerType.isNotBlank()) put("tcpSettings", tcpSettings(server))
        }
    }

    private fun tlsSettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("serverName", server.sni.ifBlank { server.host.ifBlank { server.address } })
        put("allowInsecure", server.allowInsecure)
        if (server.alpn.isNotBlank()) put("alpn", toStringArray(server.alpn))
        if (server.fingerprint.isNotBlank()) put("fingerprint", server.fingerprint)
        if (server.pinnedPeerCertSha256.isNotBlank()) put("pinnedPeerCertSha256", server.pinnedPeerCertSha256)
        if (server.verifyPeerCertByName.isNotBlank()) put("verifyPeerCertByName", server.verifyPeerCertByName)
        if (server.echConfigList.isNotBlank()) put("echConfigList", server.echConfigList)
    }

    private fun realitySettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("serverName", server.sni.ifBlank { server.host.ifBlank { server.address } })
        put("fingerprint", server.fingerprint.ifBlank { "chrome" })
        if (server.publicKey.isNotBlank()) put("publicKey", server.publicKey)
        if (server.shortId.isNotBlank()) put("shortId", server.shortId)
        if (server.spiderX.isNotBlank()) put("spiderX", server.spiderX)
    }

    private fun wsSettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("path", server.path.ifBlank { "/" })
        val host = server.host.ifBlank { server.authority }
        if (host.isNotBlank()) {
            put("headers", JSONObject().apply { put("Host", host) })
        }
    }

    private fun grpcSettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("serviceName", server.serviceName.ifBlank { server.path.trim('/') })
        if (server.authority.isNotBlank()) put("authority", server.authority)
        if (server.mode.isNotBlank()) put("multiMode", server.mode.equals("multi", ignoreCase = true))
    }

    private fun httpSettings(server: VpnServer): JSONObject = JSONObject().apply {
        val host = server.host.ifBlank { server.authority.ifBlank { server.sni } }
        if (host.isNotBlank()) put("host", JSONArray().put(host))
        if (server.path.isNotBlank()) put("path", server.path)
    }

    private fun httpUpgradeSettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("path", server.path.ifBlank { "/" })
        val host = server.host.ifBlank { server.authority }
        if (host.isNotBlank()) {
            put("headers", JSONObject().apply { put("Host", host) })
        }
    }

    private fun xhttpSettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("path", server.path.ifBlank { "/" })
        if (server.host.isNotBlank()) put("host", server.host)
        if (server.mode.isNotBlank()) put("mode", server.mode)
        if (server.authority.isNotBlank()) put("headers", JSONObject().apply { put("Host", server.authority) })
    }

    private fun kcpSettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("mtu", 1350)
        put("tti", 50)
        put("uplinkCapacity", if (server.upMbps > 0) server.upMbps else 12)
        put("downlinkCapacity", if (server.downMbps > 0) server.downMbps else 100)
        put("congestion", server.congestion.equals("true", ignoreCase = true))
        put("header", JSONObject().apply { put("type", server.headerType.ifBlank { "none" }) })
        if (server.seed.isNotBlank()) put("seed", server.seed)
    }

    private fun quicSettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("security", server.quicSecurity.ifBlank { "none" })
        put("key", server.quicKey)
        put("header", JSONObject().apply { put("type", server.headerType.ifBlank { "none" }) })
    }

    private fun tcpSettings(server: VpnServer): JSONObject = JSONObject().apply {
        put("header", JSONObject().apply {
            put("type", server.headerType.ifBlank { "none" })
            if (server.host.isNotBlank() || server.path.isNotBlank()) {
                put("request", JSONObject().apply {
                    if (server.path.isNotBlank()) put("path", JSONArray().put(server.path))
                    if (server.host.isNotBlank()) {
                        put("headers", JSONObject().apply {
                            put("Host", JSONArray().put(server.host))
                        })
                    }
                })
            }
        })
    }

    private fun routing(routingMode: String, customRules: List<CustomRoutingRule>, blockAds: Boolean, domainStrategy: String): JSONObject = JSONObject().apply {
        put("domainStrategy", domainStrategy.ifBlank { "IPIfNonMatch" })
        val rules = JSONArray()

        customRules.filter { it.enabled && it.values.isNotEmpty() }.forEach { rule ->
            rules.put(JSONObject().apply {
                put("type", "field")
                when (rule.type) {
                    RoutingRuleType.DOMAIN -> put("domain", JSONArray().also { arr -> rule.values.forEach { arr.put(it) } })
                    RoutingRuleType.IP -> put("ip", JSONArray().also { arr -> rule.values.forEach { arr.put(it) } })
                    RoutingRuleType.PROTOCOL -> put("protocol", JSONArray().also { arr -> rule.values.forEach { arr.put(it) } })
                    RoutingRuleType.NETWORK -> put("network", rule.values.joinToString(","))
                }
                put("outboundTag", rule.outboundTag)
            })
        }

        // Always keep private ranges direct to avoid breaking LAN/router access.
        rules.put(JSONObject().apply {
            put("type", "field")
            put("ip", JSONArray()
                .put("10.0.0.0/8")
                .put("172.16.0.0/12")
                .put("192.168.0.0/16")
                .put("127.0.0.0/8")
                .put("169.254.0.0/16")
                .put("fc00::/7")
                .put("fe80::/10")
                .put("geoip:private"))
            put("outboundTag", "direct")
        })

        if (routingMode == "Bypass Iran") {
            rules.put(JSONObject().apply {
                put("type", "field")
                put("domain", JSONArray()
                    .put("regexp:.*\\.ir$")
                    .put("geosite:ir")
                    .put("geosite:private"))
                put("outboundTag", "direct")
            })
            rules.put(JSONObject().apply {
                put("type", "field")
                put("ip", JSONArray().put("geoip:ir"))
                put("outboundTag", "direct")
            })
        }

        if (routingMode.startsWith("Direct")) {
            rules.put(JSONObject().apply {
                put("type", "field")
                put("network", "tcp,udp")
                put("outboundTag", "direct")
            })
        }

        // Basic ad/malware blocklist when geosite assets are available.
        if (blockAds) {
            rules.put(JSONObject().apply {
                put("type", "field")
                put("domain", JSONArray().put("geosite:category-ads-all"))
                put("outboundTag", "block")
            })
        }

        put("rules", rules)
    }

    private fun directOutbound(): JSONObject = JSONObject().apply {
        put("tag", "direct")
        put("protocol", "freedom")
    }

    private fun blockOutbound(): JSONObject = JSONObject().apply {
        put("tag", "block")
        put("protocol", "blackhole")
    }

    private fun normalizeNetwork(value: String): String = when (value.lowercase()) {
        "h2" -> "http"
        "http/2" -> "http"
        "httpupgrade", "http-upgrade" -> "httpupgrade"
        "split-http", "splithttp" -> "splithttp"
        "xhttp" -> "xhttp"
        "kcp", "mkcp" -> "kcp"
        "quic" -> "quic"
        "grpc" -> "grpc"
        "ws", "websocket" -> "ws"
        else -> "tcp"
    }

    private fun toStringArray(value: String): JSONArray = JSONArray().also { arr ->
        value.split(",", "|", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { arr.put(it) }
    }
}
