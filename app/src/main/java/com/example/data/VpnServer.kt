package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "vpn_servers")
data class VpnServer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val port: Int,
    val uuid: String, // password or uuid
    val protocol: String, // VLESS, VMESS, TROJAN, SHADOWSOCKS
    val security: String = "none", // tls, reality, none, or shadowsocks method
    val sni: String = "",
    val path: String = "",
    val host: String = "",
    val networkType: String = "tcp", // ws, grpc, tcp
    val flow: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val fingerprint: String = "chrome",
    val spiderX: String = "",
    val alpn: String = "",
    val allowInsecure: Boolean = false,
    val latency: Int = 0, // Ping in ms (-1 if error, 0 if not tested)
    val isSelected: Boolean = false,
    val groupName: String = "Custom Configs",
    val originalLink: String = "",
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
