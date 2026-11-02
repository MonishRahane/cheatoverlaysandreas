package com.example.cheatoverlay

import android.view.KeyEvent
import rikka.shizuku.Shizuku

/**
 * Sends key events to whatever app is in front.
 * Tries root first (su), then Shizuku if it happens to be running.
 */
object KeySender {

    enum class Mode { ROOT, SHIZUKU, NONE }

    @Volatile private var mode: Mode? = null

    /** Blocking (may show a root prompt) - call from a background thread. */
    fun detect(force: Boolean = false): Mode {
        if (!force) mode?.let { if (it != Mode.NONE) return it }
        val m = when {
            rootAvailable() -> Mode.ROOT
            shizukuReady() -> Mode.SHIZUKU
            else -> Mode.NONE
        }
        mode = m
        return m
    }

    private fun rootAvailable(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.contains("uid=0")
    } catch (e: Throwable) { false }

    fun shizukuReady(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) { false }

    // Shizuku 13+ made newProcess private, so call it via reflection.
    private fun run(cmd: String, m: Mode): Boolean = try {
        when (m) {
            Mode.ROOT ->
                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor() == 0
            Mode.SHIZUKU -> {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
                val p = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
                p.waitFor() == 0
            }
            Mode.NONE -> false
        }
    } catch (e: Throwable) { false }

    fun sendKeys(codes: List<Int>, onDone: (Boolean) -> Unit = {}) {
        Thread {
            val m = detect()
            val ok = codes.isNotEmpty() && m != Mode.NONE &&
                run("input keyevent " + codes.joinToString(" "), m)
            onDone(ok)
        }.start()
    }

    /** Sends letters as hardware key events, e.g. "HESOYAM". */
    fun sendCheat(code: String, onDone: (Boolean) -> Unit = {}) {
        val keys = code.uppercase()
            .filter { it in 'A'..'Z' }
            .map { KeyEvent.KEYCODE_A + (it - 'A') }
        sendKeys(keys, onDone)
    }

    /** Presses the Android Back key in the game (useful for game menus). */
    fun sendBack(onDone: (Boolean) -> Unit = {}) =
        sendKeys(listOf(KeyEvent.KEYCODE_BACK), onDone)
}
