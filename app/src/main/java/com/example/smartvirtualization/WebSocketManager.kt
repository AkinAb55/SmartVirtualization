package com.example.smartvirtualization.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketManager private constructor() {
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    companion object {
        private const val TAG = "WebSocketManager"
        private var instance: WebSocketManager? = null

        @Synchronized
        fun getInstance(): WebSocketManager {
            if (instance == null) {
                instance = WebSocketManager()
            }
            return instance!!
        }
    }

    suspend fun startConnection(
        serverUrl: String,
        onConnected: () -> Unit,
        onMessageReceived: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            if (isConnected) {
                onFailure("Already connected")
                return
            }

            val request = Request.Builder()
                .url(serverUrl)
                .build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    isConnected = true
                    Log.d(TAG, "WebSocket connected")
                    onConnected()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    Log.d(TAG, "Message received: ${text.take(50)}...")
                    onMessageReceived(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosing(webSocket, code, reason)
                    webSocket.close(1000, null)
                    Log.d(TAG, "WebSocket closing: $reason")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    isConnected = false
                    Log.e(TAG, "WebSocket error: ${t.message}")
                    onFailure("Connection error: ${t.message}")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosed(webSocket, code, reason)
                    isConnected = false
                    Log.d(TAG, "WebSocket closed")
                }
            }

            webSocket = client.newWebSocket(request, listener)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting connection: ${e.message}")
            onFailure("Failed to start connection: ${e.message}")
        }
    }

    suspend fun sendMessage(message: String) {
        try {
            if (webSocket != null && isConnected) {
                webSocket?.send(message)
                Log.d(TAG, "Message sent: ${message.take(50)}...")
            } else {
                Log.w(TAG, "WebSocket not connected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
        }
    }

    fun closeConnection() {
        try {
            webSocket?.close(1000, "Client closing")
            webSocket = null
            isConnected = false
            Log.d(TAG, "Connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        }
    }

    fun isConnected(): Boolean = isConnected
}
