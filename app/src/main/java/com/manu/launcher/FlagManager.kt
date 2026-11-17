package com.manu.launcher

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.File

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

    private fun runShizukuCmd(cmd: String): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as ShizukuRemoteProcess
    }

    fun writeFlags(json: String, cacheDir: File, log: (String) -> Unit = {}): Boolean {
        val local = File(cacheDir, "ClientAppSettings.json")
        return try {
            local.writeText(json)
            val cmd = "cp '${local.absolutePath}' $TARGET && chmod 644 $TARGET && echo OK"
            val proc = when {
                hasRoot() -> Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                shizukuReady() -> runShizukuCmd(cmd)
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
                shizukuReady() -> runShizukuCmd(cmd)
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

    fun readFlags(): String {
        return try {
            val f = File(TARGET)
            if (f.exists()) f.readText() else ""
        } catch (_: Exception) {
            ""
        }
    }
}
