package com.charan.clipboardsynker

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
                    throw IllegalArgumentException("Port must be 1024 or higher")
                }
                socket = DatagramSocket(myPort)
                startListening()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startListening() {
        thread {
            try {
                val buffer = ByteArray(1024)
                while (true) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val receivedText = String(packet.data, 0, packet.length)
                    onMessageReceived(receivedText)
                }
            } catch (e: Exception) {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun close() {
        socket?.close()
    }
}
