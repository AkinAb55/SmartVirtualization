package com.example.smartvirtualization.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockWebServer
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class WebSocketServer private constructor() {
    private var server: MockWebServer? = null
    private var clientSocket: WebSocket? = null
    private var isRunning = false

    companion object {
        private const val TAG = "WebSocketServer"
        private var instance: WebSocketServer? = null
        const val DEFAULT_PORT = 8080

        @Synchronized
        fun getInstance(): WebSocketServer {
            if (instance == null) {
                instance = WebSocketServer()
            }
            return instance!!
        }
    }

    fun start(
        port: Int = DEFAULT_PORT,
        onClientConnected: () -> Unit,
        onMessageReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRunning) return

        try {
            server = MockWebServer()
            server?.start(InetAddress.getLocalHost(), port)

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    clientSocket = webSocket
                    Log.d(TAG, "Client connected")
                    onClientConnected()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    onMessageReceived(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosing(webSocket, code, reason)
                    Log.d(TAG, "Client disconnecting: $reason")
                    webSocket.close(1000, null)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    Log.e(TAG, "WebSocket error: ${t.message}")
                    onError("Connection error: ${t.message}")
                }
            }

            // Блок с setIdleCallback удален, так как он не поддерживается актуальными версиями библиотеки

            isRunning = true
            Log.d(TAG, "Server started on port $port")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting server: ${e.message}")
            onError("Failed to start server: ${e.message}")
        }
    }

    fun sendMessage(message: String) {
        try {
            clientSocket?.send(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
        }
    }

    fun stop() {
        try {
            clientSocket?.close(1000, "Server shutting down")
            clientSocket = null
            server?.shutdown()
            server = null
            isRunning = false
            Log.d(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server: ${e.message}")
        }
    }

    fun isServerRunning(): Boolean = isRunning

    fun getServerAddress(): String? {
        return try {
            server?.let {
                "ws://${InetAddress.getLocalHost().hostAddress}:${it.port}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting server address: ${e.message}")
            null
        }
    }
}
