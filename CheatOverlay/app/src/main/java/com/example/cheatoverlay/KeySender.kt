package com.example.cheatoverlay

import android.view.KeyEvent
import rikka.shizuku.Shizuku

object KeySender {

    fun shizukuReady(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) { false }

    // Shizuku 13+ made newProcess private, so call it via reflection.
    private fun run(cmd: String) {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        m.isAccessible = true
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        p.waitFor()
    }

    /** Sends letters as hardware key events, e.g. "HESOYAM". */
    fun sendCheat(code: String, onDone: (Boolean) -> Unit = {}) {
        Thread {
            val ok = try {
                val keys = code.uppercase()
                    .filter { it in 'A'..'Z' }
                    .map { KeyEvent.KEYCODE_A + (it - 'A') }
                    .joinToString(" ")
                if (keys.isNotEmpty()) run("input keyevent $keys")
                true
            } catch (e: Throwable) { false }
            onDone(ok)
        }.start()
    }
}
