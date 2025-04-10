package com.charan.clipboardsynker

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class Network(
    private val myPort: Int,
    private val targetIP: String,
    private val targetPort: Int,
    private val onMessageReceived: (String) -> Unit
) {
    private var socket: DatagramSocket? = null

    init {
        thread {
            try {
                if (myPort < 1024) {
                    throw IllegalArgumentException("Port must be 1024 or higher"
                }
                socket = DatagramSocket(myPort)
                startListening()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Volatile
    private var isRunning = true

    private fun startListening() {
        thread {
            try {
                val buffer = ByteArray(1024)
//                Log.d("Network", "📡 Listening for incoming messages on port $myPort...")
                while (true) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val receivedText = String(packet.data, 0, packet.length)
//                    Log.d("Network", "📥 Received message: \"$receivedText\" from ${packet.address}:${packet.port}")
                    onMessageReceived(receivedText)
                }
            } catch (e: Exception) {
//                Log.e("Network", "❌ Failed while listening: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(message: String) {
        thread {
            try {
                val address = InetAddress.getByName(targetIP)
                val data = message.toByteArray()
                val packet = DatagramPacket(data, data.size, address, targetPort)
                socket?.send(packet)
//                Log.d("Network", "✅ Sent message: \"$message\" to $targetIP:$targetPort")
            } catch (e: Exception) {
//                Log.e("Network", "❌ Failed to send message: ${e.message}")
                e.printStackTrace()
            }
        }
    }


    fun close() {
        isRunning = false

        socket?.close()
    }
}
