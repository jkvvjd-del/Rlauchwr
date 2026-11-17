package com.manu.launcher

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.File

/**
 * Real write of ClientAppSettings.json into /data/local/tmp/
 * Supports Root (su) and Shizuku.
 */
object FlagManager {

    private const val TARGET = "/data/local/tmp/ClientAppSettings.json"

    fun hasRoot(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        p.waitFor() == 0
    } catch (_: Exception) {
        false
    }

    fun shizukuReady(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    fun accessMode(): String = when {
        hasRoot() -> "Root"
        shizukuReady() -> "Shizuku"
        else -> "нет (нужен Root или Shizuku)"
    }

    /** Request Shizuku permission if not granted yet */
    fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
            ) {
                Shizuku.requestPermission(0)
            }
        } catch (_: Exception) {}
    }

    /**
     * Write json via elevated process.
     * IMPORTANT: pass externalCacheDir ?: cacheDir so Shizuku can read the file.
     */
    fun writeFlags(json: String, cacheDir: File, log: (String) -> Unit = {}): Boolean {
        val local = File(cacheDir, "ClientAppSettings.json")
        return try {
            local.writeText(json)
            val cmd = "cp '${local.absolutePath}' $TARGET && chmod 644 $TARGET && echo OK"
            val proc = when {
                hasRoot() -> Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                shizukuReady() -> Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                else -> {
                    log("нет Root и нет Shizuku")
                    return false
                }
            }
            val out = proc.inputStream.bufferedReader().readText().trim()
            val err = proc.errorStream.bufferedReader().readText().trim()
            val code = proc.waitFor()
            log("exit=$code out=$out err=$err")
            code == 0
        } catch (e: Exception) {
            log(e.message ?: "error")
            false
        }
    }

    fun clearFlags(log: (String) -> Unit = {}): Boolean {
        val cmd = "rm -f $TARGET && echo OK"
        return try {
            val proc = when {
                hasRoot() -> Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                shizukuReady() -> Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                else -> return false
            }
            val code = proc.waitFor()
            log("clear exit=$code")
            code == 0
        } catch (e: Exception) {
            log(e.message ?: "error")
            false
        }
    }

    fun readFlags(): String = try {
        File(TARGET).takeIf { it.exists() }?.readText() ?: ""
    } catch (_: Exception) {
        ""
    }
}
