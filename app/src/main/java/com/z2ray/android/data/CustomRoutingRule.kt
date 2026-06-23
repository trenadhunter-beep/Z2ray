package com.z2ray.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.UUID

enum class RoutingRuleType {
    DOMAIN,
    IP,
    PROTOCOL,
    NETWORK
}

enum class RoutingOutboundTag(val tag: String) {
    PROXY("proxy"),
    DIRECT("direct"),
    BLOCK("block")
}

data class CustomRoutingRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Custom rule",
    val type: RoutingRuleType = RoutingRuleType.DOMAIN,
    val values: List<String> = emptyList(),
    val outboundTag: String = RoutingOutboundTag.DIRECT.tag,
    val enabled: Boolean = true
) : Serializable {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("values", JSONArray().also { arr -> values.forEach { arr.put(it) } })
        put("outboundTag", outboundTag)
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(obj: JSONObject): CustomRoutingRule = CustomRoutingRule(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name", "Custom rule"),
            type = runCatching { RoutingRuleType.valueOf(obj.optString("type", "DOMAIN")) }.getOrDefault(RoutingRuleType.DOMAIN),
            values = obj.optJSONArray("values")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { value -> value.isNotBlank() } }
            } ?: emptyList(),
            outboundTag = obj.optString("outboundTag", RoutingOutboundTag.DIRECT.tag),
            enabled = obj.optBoolean("enabled", true)
        )

        fun listToJson(rules: List<CustomRoutingRule>): String = JSONArray().also { arr ->
            rules.forEach { arr.put(it.toJson()) }
        }.toString()

        fun listFromJson(json: String): List<CustomRoutingRule> = runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { index ->
                arr.optJSONObject(index)?.let { fromJson(it) }
            }
        }.getOrDefault(emptyList())

        fun fromImportText(text: String): List<CustomRoutingRule> {
            val trimmed = text.trim()
            if (trimmed.isBlank()) return emptyList()
            return if (trimmed.startsWith("[")) {
                listFromJson(trimmed)
            } else {
                trimmed.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .mapNotNull { line ->
                        val parts = line.split(",").map { it.trim() }
                        if (parts.size < 3) return@mapNotNull null
                        val type = runCatching { RoutingRuleType.valueOf(parts[0].uppercase()) }.getOrNull() ?: return@mapNotNull null
                        val outbound = parts[1].lowercase().takeIf { it in setOf("proxy", "direct", "block") } ?: return@mapNotNull null
                        CustomRoutingRule(
                            name = "${type.name.lowercase()} → $outbound",
                            type = type,
                            outboundTag = outbound,
                            values = parts.drop(2).flatMap { it.split("|", " ") }.map { it.trim() }.filter { it.isNotBlank() }
                        )
                    }
            }
        }
    }
}
