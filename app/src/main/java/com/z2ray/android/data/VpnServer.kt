package com.z2ray.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "vpn_servers")
data class VpnServer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val port: Int,
    val uuid: String, // password, uuid, username, or raw auth material depending on protocol
    val protocol: String, // VLESS, VMESS, TROJAN, SHADOWSOCKS, HYSTERIA2, TUIC, SOCKS, CUSTOM_JSON
    val security: String = "none", // tls, reality, none, or shadowsocks method
    val sni: String = "",
    val path: String = "",
    val host: String = "",
    val networkType: String = "tcp", // tcp, ws, grpc, h2/http, httpupgrade, xhttp/splithttp, quic, kcp
    val flow: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val fingerprint: String = "chrome",
    val spiderX: String = "",
    val alpn: String = "",
    val allowInsecure: Boolean = false,

    // Extended protocol/transport fields. They default to empty so older rows remain easy to migrate.
    val encryption: String = "",
    val alterId: Int = 0,
    val packetEncoding: String = "",
    val headerType: String = "",
    val serviceName: String = "",
    val authority: String = "",
    val mode: String = "",
    val quicSecurity: String = "",
    val quicKey: String = "",
    val seed: String = "",
    val pinnedPeerCertSha256: String = "",
    val verifyPeerCertByName: String = "",
    val echConfigList: String = "",
    val obfsType: String = "",
    val obfsPassword: String = "",
    val congestion: String = "",
    val upMbps: Int = 0,
    val downMbps: Int = 0,
    val udpRelayMode: String = "",
    val rawJson: String = "",

    val latency: Int = 0, // Ping in ms (-1 if error, 0 if not tested)
    val isSelected: Boolean = false,
    val groupName: String = "Custom Configs",
    val originalLink: String = "",
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
