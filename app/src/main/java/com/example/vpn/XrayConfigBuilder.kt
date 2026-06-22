package com.example.vpn

import com.example.data.VpnServer
import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {

    fun build(server: VpnServer, dnsServer: String, routingMode: String): String {
        val root = JSONObject()

        root.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        root.put("dns", JSONObject().apply {
            put("servers", JSONArray().put(dnsServer).put("1.1.1.1"))
        })

        root.put("inbounds", JSONArray().put(tunInbound(dnsServer)))
        root.put("outbounds", JSONArray().put(proxyOutbound(server)).put(directOutbound()).put(blockOutbound()))
        root.put("routing", routing(routingMode))
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

    private fun proxyOutbound(server: VpnServer): JSONObject {
        return when (server.protocol.uppercase()) {
            "VMESS" -> vmessOutbound(server)
            "TROJAN" -> trojanOutbound(server)
            "SHADOWSOCKS", "SS" -> shadowsocksOutbound(server)
            else -> vlessOutbound(server)
        }
    }

    private fun vlessOutbound(server: VpnServer): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "vless")
        put("settings", JSONObject().apply {
            put("vnext", JSONArray().put(JSONObject().apply {
                put("address", server.address)
                put("port", server.port)
                put("users", JSONArray().put(JSONObject().apply {
                    put("id", server.uuid)
                    put("encryption", "none")
                    if (server.flow.isNotBlank()) put("flow", server.flow)
                }))
            }))
        })
        put("streamSettings", streamSettings(server))
        put("mux", muxSettings())
    }

    private fun vmessOutbound(server: VpnServer): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "vmess")
        put("settings", JSONObject().apply {
            put("vnext", JSONArray().put(JSONObject().apply {
                put("address", server.address)
                put("port", server.port)
                put("users", JSONArray().put(JSONObject().apply {
                    put("id", server.uuid)
                    put("alterId", 0)
                    put("security", "auto")
                }))
            }))
        })
        put("streamSettings", streamSettings(server))
        put("mux", muxSettings())
    }

    private fun trojanOutbound(server: VpnServer): JSONObject = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "trojan")
        put("settings", JSONObject().apply {
            put("servers", JSONArray().put(JSONObject().apply {
                put("address", server.address)
                put("port", server.port)
                put("password", server.uuid)
            }))
        })
        put("streamSettings", streamSettings(server))
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
            }))
        })
    }


    private fun muxSettings(): JSONObject = JSONObject().apply {
        // Disabled by default; modern VLESS/Reality/TLS nodes usually perform better without forced mux.
        put("enabled", false)
        put("concurrency", 8)
    }

    private fun streamSettings(server: VpnServer): JSONObject = JSONObject().apply {
        val network = server.networkType.ifBlank { "tcp" }.lowercase()
        val security = server.security.ifBlank { "none" }.lowercase()

        put("network", network)
        put("security", if (security == "tls" || security == "reality") security else "none")

        if (security == "tls") {
            put("tlsSettings", JSONObject().apply {
                put("serverName", server.sni.ifBlank { server.host })
                put("allowInsecure", server.allowInsecure)
                if (server.alpn.isNotBlank()) put("alpn", JSONArray(server.alpn.split(",").map { it.trim() }.filter { it.isNotEmpty() }))
                if (server.fingerprint.isNotBlank()) put("fingerprint", server.fingerprint)
            })
        }

        if (security == "reality") {
            put("realitySettings", JSONObject().apply {
                put("serverName", server.sni.ifBlank { server.host })
                put("fingerprint", server.fingerprint.ifBlank { "chrome" })
                if (server.publicKey.isNotBlank()) put("publicKey", server.publicKey)
                if (server.shortId.isNotBlank()) put("shortId", server.shortId)
                if (server.spiderX.isNotBlank()) put("spiderX", server.spiderX)
            })
        }

        if (network == "ws") {
            put("wsSettings", JSONObject().apply {
                put("path", server.path.ifBlank { "/" })
                if (server.host.isNotBlank()) {
                    put("headers", JSONObject().apply { put("Host", server.host) })
                }
            })
        }

        if (network == "grpc") {
            put("grpcSettings", JSONObject().apply {
                put("serviceName", server.path.trim('/'))
            })
        }
    }

    private fun routing(routingMode: String): JSONObject = JSONObject().apply {
        put("domainStrategy", "IPIfNonMatch")
        val rules = JSONArray()

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
                put("ip", JSONArray()
                    .put("10.0.0.0/8")
                    .put("172.16.0.0/12")
                    .put("192.168.0.0/16")
                    .put("127.0.0.0/8")
                    .put("169.254.0.0/16")
                    .put("fc00::/7")
                    .put("fe80::/10")
                    .put("geoip:ir")
                    .put("geoip:private"))
                put("outboundTag", "direct")
            })
        }

        if (routingMode == "Direct") {
            rules.put(JSONObject().apply {
                put("type", "field")
                put("network", "tcp,udp")
                put("outboundTag", "direct")
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
}
