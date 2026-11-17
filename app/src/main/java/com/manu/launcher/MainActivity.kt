package com.manu.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var editor: EditText
    private lateinit var logView: TextView

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            updateStatus()
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                toast("Shizuku разрешён")
            } else {
                toast("Shizuku отказано")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Shizuku permission early
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
            FlagManager.requestShizukuPermission()
        } catch (_: Exception) {}

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D0D0F.toInt())
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "MANU Launcher"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)

        statusView = TextView(this).apply {
            setTextColor(0xFF00E5A0.toInt())
            textSize = 14f
            setPadding(0, 16, 0, 16)
        }
        root.addView(statusView)

        // Preset buttons
        val presetsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            "FPS 120" to Presets.fps120(),
            "Potato" to Presets.potato(),
            "Gray" to Presets.graySky(),
            "Low" to Presets.lowGfx()
        ).forEach { (name, json) ->
            val b = Button(this).apply {
                text = name
                setOnClickListener {
                    editor.setText(json)
                    applyFlags(json)
                }
            }
            presetsRow.addView(b, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(presetsRow)

        editor = EditText(this).apply {
            hint = "{ \"DFIntTaskSchedulerTargetFps\": 120 }"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF666666.toInt())
            setBackgroundColor(0xFF1E1E24.toInt())
            minLines = 8
            gravity = Gravity.TOP or Gravity.START
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setPadding(16, 16, 16, 16)
        }
        root.addView(editor, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = 16 })

        val btnApply = Button(this).apply {
            text = "APPLY FLAGS"
            setBackgroundColor(0xFF00E5A0.toInt())
            setTextColor(0xFF000000.toInt())
            setOnClickListener {
                FlagManager.requestShizukuPermission()
                applyFlags(editor.text.toString())
            }
        }
        root.addView(btnApply)

        val btnClear = Button(this).apply {
            text = "CLEAR FLAGS"
            setOnClickListener {
                val ok = FlagManager.clearFlags { logView.append("\n$it") }
                toast(if (ok) "Очищено" else "Не удалось")
                updateStatus()
            }
        }
        root.addView(btnClear)

        val btnLaunch = Button(this).apply {
            text = "▶ LAUNCH ROBLOX"
            setBackgroundColor(0xFF00AA00.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { launchRoblox() }
        }
        root.addView(btnLaunch)

        logView = TextView(this).apply {
            setTextColor(0xFF888888.toInt())
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        root.addView(ScrollView(this).apply {
            addView(logView)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 200
            )
        })

        setContentView(root)
        updateStatus()

        // Load existing flags if any
        val existing = FlagManager.readFlags()
        if (existing.isNotEmpty()) editor.setText(existing)
    }

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun applyFlags(json: String) {
        if (json.isBlank()) {
            toast("Пусто")
            return
        }
        // CRITICAL: externalCacheDir so Shizuku can read the file
        val dir = externalCacheDir ?: cacheDir
        val ok = FlagManager.writeFlags(json, dir) { msg ->
            runOnUiThread { logView.append("\n$msg") }
        }
        toast(if (ok) "Флаги применены" else "Нужен Root или Shizuku")
        updateStatus()
    }

    private fun updateStatus() {
        statusView.text = "Доступ: ${FlagManager.accessMode()}"
    }

    private fun launchRoblox() {
        val packages = listOf("com.roblox.client", "com.roblox.client.vnggames")
        for (pkg in packages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
        toast("Roblox не установлен")
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
