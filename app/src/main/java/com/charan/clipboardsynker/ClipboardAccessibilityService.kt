package com.charan.clipboardsynker

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ClipboardAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener {
            val clip = clipboardManager.primaryClip
            val text = clip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrEmpty()) {
//                Log.d("ClipboardService", "Copied: $text")
                MainActivity.latestClipboardText = text
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
