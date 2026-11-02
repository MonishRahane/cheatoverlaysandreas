package com.example.cheatoverlay

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.inputmethod.InputMethodManager
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var status: TextView

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        status = TextView(this).apply { textSize = 16f }
        root.addView(TextView(this).apply { text = "Cheat Overlay"; textSize = 24f })
        root.addView(status)

        fun btn(label: String, action: () -> Unit) = Button(this).apply {
            text = label; setOnClickListener { action() }
            root.addView(this)
        }

        btn("1. Allow display over other apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
        btn("2. Check key access (root / Shizuku)") { refresh() }
        btn("3. Start overlay") {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            startForegroundService(Intent(this, OverlayService::class.java))
        }
        btn("Stop overlay") { stopService(Intent(this, OverlayService::class.java)) }
        btn("KEYBOARD MODE 1: Enable Cheat Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        btn("KEYBOARD MODE 2: Switch to Cheat Keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        btn("Grant Shizuku permission (optional, only if no root)") {
            try {
                if (!Shizuku.pingBinder()) {
                    status.text = "Shizuku is not running."
                } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(100)
                }
            } catch (e: Throwable) { status.text = "Shizuku not installed." }
        }

        setContentView(root)
        runCatching { Shizuku.addRequestPermissionResultListener(permListener) }
    }

    override fun onResume() { super.onResume(); refresh() }

    override fun onDestroy() {
        runCatching { Shizuku.removeRequestPermissionResultListener(permListener) }
        super.onDestroy()
    }

    // Back button simply closes this screen; the overlay keeps running until you press ✕.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { finish() }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        status.text = "Overlay permission: ${if (overlay) "OK" else "missing"}\nKey access: checking…"
        Thread {
            val m = KeySender.detect(force = true)
            val text = when (m) {
                KeySender.Mode.ROOT -> "Root"
                KeySender.Mode.SHIZUKU -> "Shizuku"
                KeySender.Mode.NONE -> "NOT available (needs root or Shizuku)"
            }
            runOnUiThread {
                status.text = "Overlay permission: ${if (overlay) "OK" else "missing"}\nKey access: $text"
            }
        }.start()
    }
}
