package com.wol.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Utility to verify if the remote PC is reachable via TCP (e.g. over Tailscale).
 */
object TcpPingManager {

    /**
     * Checks if a TCP port on a target host is open and reachable.
     * Default timeout is 1500 ms as requested.
     */
    suspend fun checkHostOnline(
        host: String,
        port: Int,
        timeoutMs: Int = 1500
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        if (cleanHost.isBlank() || port !in 1..65535) {
            return@withContext false
        }

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(cleanHost, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
