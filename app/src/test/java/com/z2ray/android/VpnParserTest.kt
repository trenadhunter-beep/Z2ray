package com.z2ray.android

import com.z2ray.android.data.VpnParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VpnParserTest {

    @Test
    fun parsesVlessRealityExtendedFields() {
        val link = "vless://11111111-1111-1111-1111-111111111111@example.com:443?type=grpc&security=reality&sni=www.cloudflare.com&fp=chrome&pbk=pub&sid=abcd&serviceName=svc&flow=xtls-rprx-vision#Reality"
        val server = VpnParser.parseLine(link, "test")!!

        assertEquals("VLESS", server.protocol)
        assertEquals("reality", server.security)
        assertEquals("grpc", server.networkType)
        assertEquals("svc", server.path)
        assertEquals("pub", server.publicKey)
        assertEquals("abcd", server.shortId)
    }

    @Test
    fun parsesHysteria2ShareLink() {
        val link = "hy2://password@example.com:443?sni=example.com&obfs=salamander&obfs-password=secret&upmbps=50&downmbps=200#HY2"
        val server = VpnParser.parseLine(link, "test")!!

        assertEquals("HYSTERIA2", server.protocol)
        assertEquals("password", server.uuid)
        assertEquals("secret", server.obfsPassword)
        assertEquals(50, server.upMbps)
        assertEquals(200, server.downMbps)
    }

    @Test
    fun parsesSip008Json() {
        val json = """
            {
              "airport":"SS Group",
              "servers":[{"remarks":"SS1","server":"1.2.3.4","server_port":8388,"method":"2022-blake3-aes-128-gcm","password":"pass"}]
            }
        """.trimIndent()

        val servers = VpnParser.parseMany(json, "json")
        assertEquals(1, servers.size)
        assertEquals("SHADOWSOCKS", servers.first().protocol)
        assertEquals("2022-blake3-aes-128-gcm", servers.first().security)
    }

    @Test
    fun parsesBasicClashYaml() {
        val yaml = """
            proxies:
              - name: clash-vless
                type: vless
                server: example.com
                port: 443
                uuid: 11111111-1111-1111-1111-111111111111
                tls: true
                network: ws
                servername: cdn.example.com
                ws-opts:
                  path: /ws
                  headers:
                    Host: cdn.example.com
        """.trimIndent()

        val servers = VpnParser.parseMany(yaml, "clash")
        assertTrue(servers.isNotEmpty())
        val server = servers.first()
        assertEquals("VLESS", server.protocol)
        assertEquals("tls", server.security)
        assertEquals("ws", server.networkType)
        assertEquals("/ws", server.path)
    }
}
