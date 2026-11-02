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
        btn("2. Grant Shizuku permission") {
            try {
                if (!Shizuku.pingBinder()) {
                    status.text = "Shizuku is not running. Start it first (Wireless debugging)."
                } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(100)
                }
            } catch (e: Throwable) { status.text = "Shizuku not installed." }
        }
        btn("3. Start overlay") {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            startForegroundService(Intent(this, OverlayService::class.java))
        }
        btn("Stop overlay") { stopService(Intent(this, OverlayService::class.java)) }

        setContentView(root)
        Shizuku.addRequestPermissionResultListener(permListener)
    }

    override fun onResume() { super.onResume(); refresh() }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        val shizuku = KeySender.shizukuReady()
        status.text = "Overlay permission: ${if (overlay) "OK" else "missing"}\n" +
            "Shizuku: ${if (shizuku) "ready" else "not ready"}\n"
    }
}
