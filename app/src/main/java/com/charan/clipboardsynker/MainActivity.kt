package com.charan.clipboardsynker

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import android.content.ClipData
import android.content.ClipboardManager

class MainActivity : ComponentActivity() {
    private lateinit var clipboardManager: ClipboardManager
    private var network: Network? = null
    private var suppressClipboardUpdate = false

    companion object {
        var latestClipboardText: String? = null // Shared by service
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewText = findViewById<TextView>(R.id.viewText)
        val yourPortEditText = findViewById<EditText>(R.id.yourPortEditText)
        val targetIpEditText = findViewById<EditText>(R.id.targetIpEditText)
        val targetPortEditText = findViewById<EditText>(R.id.targetPortEditText)
        val startConnectionButton = findViewById<Button>(R.id.startConnectionButton)
        val editText = findViewById<EditText>(R.id.myEditText)
        val sendButton = findViewById<Button>(R.id.sendButton)

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        startConnectionButton.setOnClickListener {
            val myPort = yourPortEditText.text.toString().toIntOrNull()
            val targetIP = targetIpEditText.text.toString()
            val targetPort = targetPortEditText.text.toString().toIntOrNull()

            if (myPort == null || targetPort == null || targetIP.isEmpty()) {
                viewText.text = "❌ Invalid connection details"
                return@setOnClickListener
            }

            network = Network(myPort, targetIP, targetPort) { receivedMessage ->
                runOnUiThread {
                    suppressClipboardUpdate = true
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied Text", receivedMessage))
                    viewText.text = receivedMessage
                    Handler(Looper.getMainLooper()).postDelayed({
                        suppressClipboardUpdate = false
                    }, 500)
                }
            }
            viewText.text = "✅ Connection Started"
            sendButton.visibility = View.VISIBLE
        }

        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivity(intent)

        sendButton.setOnClickListener {
            val message = editText.text.toString()
            if (message.isNotEmpty()) {
                network?.sendMessage(message)
            }
        }

        clipboardManager.addPrimaryClipChangedListener {
            if (suppressClipboardUpdate) return@addPrimaryClipChangedListener
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val text = item.text.toString()
                editText.setText(text)
                network?.sendMessage(text)
            }
        }

        showFloatingBubble()

        // Start accessibility service
        val intent1 = Intent(this, ClipboardAccessibilityService::class.java)
        startService(intent1)
    }

    private fun showFloatingBubble() {
        val bubble = ImageView(this)
        bubble.setImageResource(android.R.drawable.presence_online)

        val layoutParams = WindowManager.LayoutParams(
            150, 150,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 300

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(bubble, layoutParams)

        // Touch logic with click detection
        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var touchStartTime: Long = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        touchStartTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        val clickDuration = System.currentTimeMillis() - touchStartTime

                        // Detect a tap (not a drag)
                        if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10 && clickDuration < 300) {
                            Log.d("Bubble", "🟢 Bubble clicked!")
                            val text = latestClipboardText
                            if (!text.isNullOrEmpty()) {
                                network?.sendMessage(text)
                                Toast.makeText(applicationContext, "📤 Sent: $text", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(applicationContext, "⚠️ Clipboard is empty or inaccessible", Toast.LENGTH_SHORT).show()
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(bubble, layoutParams)
                        return true
                    }
                }
                return false
            }
        })
    }

}
