package com.z2ray.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class UrlTestResult(
    val ok: Boolean,
    val latencyMs: Int = -1,
    val httpCode: Int = 0,
    val bytesRead: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val message: String = ""
)

data class TlsHandshakeResult(
    val ok: Boolean,
    val latencyMs: Int = -1,
    val peerSha256: String = "",
    val protocol: String = "",
    val cipherSuite: String = "",
    val message: String = ""
)

class NetworkDiagnostics {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    suspend fun urlTest(url: String = DEFAULT_TEST_URL): UrlTestResult = withContext(Dispatchers.IO) {
        executeUrlTest(httpClient, url)
    }

    suspend fun urlTestThroughLocalSocks(url: String = DEFAULT_TEST_URL, port: Int = 10808): UrlTestResult = withContext(Dispatchers.IO) {
        val proxiedClient = httpClient.newBuilder()
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)))
            .build()
        executeUrlTest(proxiedClient, url)
    }

    private fun executeUrlTest(client: OkHttpClient, url: String): UrlTestResult {
        val start = System.currentTimeMillis()
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Z2ray-Connectivity-Test/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                val elapsed = (System.currentTimeMillis() - start).toInt()
                // generate_204 commonly returns 204 with an empty body. Any 2xx/3xx proves the active tunnel/system path works.
                UrlTestResult(
                    ok = response.code in 200..399,
                    latencyMs = elapsed,
                    httpCode = response.code,
                    message = "HTTP ${response.code} in ${elapsed}ms"
                )
            }
        }.getOrElse { error ->
            UrlTestResult(ok = false, latencyMs = -1, message = error.localizedMessage ?: error.javaClass.simpleName)
        }
    }

    suspend fun downloadSpeedTest(url: String = DEFAULT_DOWNLOAD_TEST_URL, maxBytes: Long = 2L * 1024L * 1024L): UrlTestResult = withContext(Dispatchers.IO) {
        executeDownloadTest(httpClient, url, maxBytes)
    }

    suspend fun downloadSpeedTestThroughLocalSocks(url: String = DEFAULT_DOWNLOAD_TEST_URL, maxBytes: Long = 2L * 1024L * 1024L, port: Int = 10808): UrlTestResult = withContext(Dispatchers.IO) {
        val proxiedClient = httpClient.newBuilder()
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)))
            .build()
        executeDownloadTest(proxiedClient, url, maxBytes)
    }

    private fun executeDownloadTest(client: OkHttpClient, url: String, maxBytes: Long): UrlTestResult {
        val start = System.currentTimeMillis()
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Z2ray-Speed-Test/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@runCatching UrlTestResult(false, httpCode = response.code, message = "HTTP ${response.code}")
                }
                val input = response.body?.byteStream() ?: return@runCatching UrlTestResult(false, httpCode = response.code, message = "Empty body")
                val buffer = ByteArray(16 * 1024)
                var total = 0L
                while (total < maxBytes) {
                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), maxBytes - total).toInt())
                    if (read <= 0) break
                    total += read
                }
                val elapsedMs = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                UrlTestResult(
                    ok = total > 0,
                    latencyMs = elapsedMs.toInt(),
                    httpCode = response.code,
                    bytesRead = total,
                    speedBytesPerSec = (total * 1000L) / elapsedMs,
                    message = "Downloaded $total bytes in ${elapsedMs}ms"
                )
            }
        }.getOrElse { error ->
            UrlTestResult(ok = false, message = error.localizedMessage ?: error.javaClass.simpleName)
        }
    }

    suspend fun udpProbe(server: VpnServer, payload: ByteArray = byteArrayOf(0), timeoutMs: Int = 2500): UrlTestResult = withContext(Dispatchers.IO) {
        if (server.port <= 0 || server.address.isBlank()) return@withContext UrlTestResult(false, message = "Invalid host/port")
        val start = System.currentTimeMillis()
        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                val packet = DatagramPacket(payload, payload.size, InetAddress.getByName(server.address), server.port)
                socket.send(packet)
                // UDP servers are not required to answer; successful send means UDP path creation worked locally.
                UrlTestResult(
                    ok = true,
                    latencyMs = (System.currentTimeMillis() - start).toInt(),
                    bytesRead = payload.size.toLong(),
                    message = "UDP datagram sent to ${server.address}:${server.port}"
                )
            }
        }.getOrElse { error ->
            UrlTestResult(false, message = error.localizedMessage ?: error.javaClass.simpleName)
        }
    }

    suspend fun tlsHandshake(server: VpnServer): TlsHandshakeResult = withContext(Dispatchers.IO) {
        val sni = server.sni.ifBlank { server.host.ifBlank { server.address } }
        val start = System.currentTimeMillis()
        runCatching {
            Socket().use { plain ->
                plain.connect(InetSocketAddress(server.address, server.port), 5000)
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                factory.createSocket(plain, sni, server.port, true).use { ssl ->
                    val socket = ssl as SSLSocket
                    socket.soTimeout = 8000
                    socket.startHandshake()
                    val session = socket.session
                    val cert = session.peerCertificates.firstOrNull() as? X509Certificate
                    val hash = cert?.encoded?.sha256Hex().orEmpty()
                    TlsHandshakeResult(
                        ok = true,
                        latencyMs = (System.currentTimeMillis() - start).toInt(),
                        peerSha256 = hash,
                        protocol = session.protocol.orEmpty(),
                        cipherSuite = session.cipherSuite.orEmpty(),
                        message = "TLS ${session.protocol} ${session.cipherSuite}"
                    )
                }
            }
        }.getOrElse { error ->
            TlsHandshakeResult(ok = false, message = error.localizedMessage ?: error.javaClass.simpleName)
        }
    }

    private fun ByteArray.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_TEST_URL = "https://www.google.com/generate_204"
        const val DEFAULT_DOWNLOAD_TEST_URL = "https://speed.cloudflare.com/__down?bytes=2097152"
    }
}
