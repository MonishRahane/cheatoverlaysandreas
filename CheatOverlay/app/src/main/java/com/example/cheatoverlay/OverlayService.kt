package com.example.cheatoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var bubble: TextView
    private lateinit var panel: LinearLayout
    private lateinit var display: TextView
    private val buffer = StringBuilder()
    private var panelShown = false
    private val ui = Handler(Looper.getMainLooper())

    private val presets = listOf(
        "Health/Armor/Money" to "HESOYAM",
        "Weapons 1" to "LXGIWYL",
        "Weapons 2" to "KJKSZPJ",
        "Weapons 3" to "UZUMYMW",
        "Wanted level down" to "ASNAEB",
        "Wanted level up" to "OSRBLHH",
        "Jetpack" to "YECGAA",
        "Rhino tank" to "AIWPRTON",
        "Hunter" to "OHDUDE"
    )

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        buildBubble()
        buildPanel()
    }

    override fun onDestroy() {
        runCatching { wm.removeView(bubble) }
        runCatching { if (panelShown) wm.removeView(panel) }
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun overlayParams(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun buildBubble() {
        bubble = TextView(this).apply {
            text = "⌨"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1565C0"))
            }
        }
        val p = overlayParams(dp(52), dp(52)).apply { x = 0; y = dp(120) }

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - sx).toInt(); val dy = (e.rawY - sy).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                    p.x = ox + dx; p.y = oy + dy
                    wm.updateViewLayout(bubble, p)
                }
                MotionEvent.ACTION_UP -> if (!moved) togglePanel(p)
            }
            true
        }
        wm.addView(bubble, p)
    }

    private fun buildPanel() {
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#EE222222"))
            }
        }

        display = TextView(this).apply {
            textSize = 18f; setTextColor(Color.WHITE); text = "—"
            setPadding(dp(4), dp(2), dp(4), dp(6))
        }
        panel.addView(display)

        // Preset row (one tap sends the full cheat)
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        presets.forEach { (name, code) ->
            presetRow.addView(Button(this).apply {
                text = name; textSize = 11f; isAllCaps = false
                setOnClickListener { send(code) }
            })
        }
        panel.addView(HorizontalScrollView(this).apply { addView(presetRow) })

        // A-Z keyboard
        val grid = GridLayout(this).apply { columnCount = 7 }
        for (c in 'A'..'Z') {
            grid.addView(Button(this).apply {
                text = c.toString(); minWidth = 0; minimumWidth = 0
                setPadding(0, 0, 0, 0)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dp(40); height = dp(40)
                }
                setOnClickListener { buffer.append(c); updateDisplay() }
            })
        }
        panel.addView(grid)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Del"; setOnClickListener {
                if (buffer.isNotEmpty()) buffer.deleteCharAt(buffer.length - 1); updateDisplay()
            }
        })
        actions.addView(Button(this).apply {
            text = "Clear"; setOnClickListener { buffer.clear(); updateDisplay() }
        })
        actions.addView(Button(this).apply {
            text = "SEND"; setOnClickListener {
                if (buffer.isNotEmpty()) { send(buffer.toString()); buffer.clear(); updateDisplay() }
            }
        })
        panel.addView(actions)
    }

    private fun togglePanel(bubbleParams: WindowManager.LayoutParams) {
        if (panelShown) {
            wm.removeView(panel); panelShown = false
        } else {
            val p = overlayParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT).apply {
                x = bubbleParams.x + dp(58); y = bubbleParams.y
            }
            wm.addView(panel, p); panelShown = true
        }
    }

    private fun updateDisplay() { display.text = if (buffer.isEmpty()) "—" else buffer.toString() }

    private fun send(code: String) {
        if (!KeySender.shizukuReady()) { display.text = "Shizuku not ready"; return }
        display.text = "Sending $code…"
        KeySender.sendCheat(code) { ok ->
            ui.post { display.text = if (ok) "Sent $code" else "Failed — check Shizuku" }
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("overlay", "Overlay", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, "overlay")
            .setContentTitle("Cheat Overlay running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}
