package com.charan.clipboardsynker

import android.app.*
import android.content.*
import android.os.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat

class MainActivity : ComponentActivity() {
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var viewText: TextView
    private var network: Network? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startClipboardSyncService()

        viewText = findViewById(R.id.viewText)
        val yourPortEditText = findViewById<EditText>(R.id.yourPortEditText)
        val targetIpEditText = findViewById<EditText>(R.id.targetIpEditText)
        val targetPortEditText = findViewById<EditText>(R.id.targetPortEditText)
        val startConnectionButton = findViewById<Button>(R.id.startConnectionButton)
        val editText = findViewById<EditText>(R.id.myEditText)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val receivedTextView = findViewById<TextView>(R.id.viewText)

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener {
            runOnUiThread { updateTextView() }  // Ensuring UI update on the main thread
        }
        updateTextView()

        startConnectionButton.setOnClickListener {
            val myPort = yourPortEditText.text.toString().toIntOrNull()
            val targetIP = targetIpEditText.text.toString()
            val targetPort = targetPortEditText.text.toString().toIntOrNull()

            if (myPort == null || targetPort == null || targetIP.isEmpty()) {
                receivedTextView.text = "❌ Please enter valid port numbers and IP address"
                return@setOnClickListener
            }
            if (myPort < 1024 || targetPort < 1024) {
                receivedTextView.text = "⚠️ Port must be 1024 or higher"
                return@setOnClickListener
            }

            network = Network(myPort, targetIP, targetPort) { receivedMessage ->
                runOnUiThread {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied Text", receivedMessage))
                    viewText.text = receivedMessage // Update UI with received message
                }
            }

            receivedTextView.text = "✅ Connection Started on Port $myPort"
            sendButton.visibility = Button.VISIBLE
        }

        sendButton.setOnClickListener {
            val message = editText.text.toString()
            if (message.isNotEmpty()) {
                network?.sendMessage(message)
            }
        }
    }

    private fun updateTextView() {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val copiedText = clip.getItemAt(0).text.toString()
            viewText.text = copiedText // Update the TextView with the clipboard content
            network?.sendMessage(copiedText) // Send clipboard text over the network
        }
    }

    private fun startClipboardSyncService() {
        val channelId = "ClipboardSyncChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Clipboard Sync Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Clipboard Sync Running")
            .setContentText("Your clipboard sync is active in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        val serviceIntent = Intent(this, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    class ForegroundService : Service() {
        override fun onCreate() {
            super.onCreate()
            startForegroundService()
        }

        private fun startForegroundService() {
            val channelId = "ClipboardSyncChannel"
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Clipboard Sync Running")
                .setContentText("Your clipboard sync is active in the background")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()

            startForeground(1, notification)
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            return START_STICKY
        }

        override fun onBind(intent: Intent?): IBinder? {
            return null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        network?.close()
    }
}
