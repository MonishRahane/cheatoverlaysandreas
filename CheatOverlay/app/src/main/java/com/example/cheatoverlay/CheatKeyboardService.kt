package com.example.cheatoverlay

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/** A normal on-screen keyboard that sends real key events to the focused app (the game). */
class CheatKeyboardService : InputMethodService() {

    private val ui = Handler(Looper.getMainLooper())
    private lateinit var status: TextView

    private val presets = listOf(
        "Health/Armor/Money" to "HESOYAM",
        "Weapons 1" to "LXGIWYL",
        "Weapons 2" to "KJKSZPJ",
        "Weapons 3" to "UZUMYMW",
        "Wanted down" to "ASNAEB",
        "Wanted up" to "OSRBLHH",
        "Jetpack" to "YECGAA",
        "Rhino tank" to "AIWPRTON",
        "Hunter" to "OHDUDE"
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // Show the keyboard even if a hardware keyboard is connected.
    override fun onEvaluateInputViewShown(): Boolean { super.onEvaluateInputViewShown(); return true }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FF222222"))
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        // Header: status + switch keyboard + hide
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        status = TextView(this).apply {
            text = "Cheat Keyboard"; textSize = 14f; setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(6), 0, 0, 0)
        }
        header.addView(status)
        header.addView(round("⌨", "#FF37474F") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        header.addView(round("✕", "#FFC62828") { requestHideSelf(0) })
        root.addView(header)

        // One-tap cheats
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        presets.forEach { (name, code) ->
            presetRow.addView(Button(this).apply {
                text = name; textSize = 11f; isAllCaps = false
                setOnClickListener { sendCode(code) }
            })
        }
        root.addView(HorizontalScrollView(this).apply { addView(presetRow) })

        // QWERTY letters
        listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM").forEach { row ->
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            row.forEach { c ->
                r.addView(Button(this).apply {
                    text = c.toString(); textSize = 15f
                    minWidth = 0; minimumWidth = 0; setPadding(0, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
                        .apply { setMargins(dp(1), dp(1), dp(1), dp(1)) }
                    setOnClickListener { pressKey(KeyEvent.KEYCODE_A + (c - 'A')) }
                })
            }
            root.addView(r)
        }

        // Extra keys
        val extra = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun extraBtn(label: String, code: Int) = extra.addView(Button(this).apply {
            text = label; isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
            setOnClickListener { pressKey(code) }
        })
        extraBtn("Del", KeyEvent.KEYCODE_DEL)
        extraBtn("Enter", KeyEvent.KEYCODE_ENTER)
        extraBtn("Back", KeyEvent.KEYCODE_BACK)
        root.addView(extra)

        return root
    }

    private fun pressKey(code: Int): Boolean {
        val ic = currentInputConnection
        if (ic == null) { status.text = "Game isn't accepting keys here"; return false }
        val t = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
        return true
    }

    private fun sendCode(code: String) {
        status.text = "Sending $code…"
        code.uppercase().filter { it in 'A'..'Z' }.forEachIndexed { i, c ->
            ui.postDelayed({
                val ok = pressKey(KeyEvent.KEYCODE_A + (c - 'A'))
                if (ok && i == code.length - 1) status.text = "Sent $code"
            }, i * 60L)
        }
    }

    private fun round(label: String, color: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(color)) }
        layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { leftMargin = dp(6) }
        setOnClickListener { onClick() }
    }
}
