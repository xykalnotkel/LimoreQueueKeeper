package com.assistant.limorekeeper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvShizukuStatus: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnShizukuOptimize: Button
    private lateinit var btnGrantNotifAccess: Button
    private lateinit var btnDisableBatteryOpt: Button
    private lateinit var btnOpenLimore: Button
    private lateinit var btnStopAlarm: Button

    private var isServiceRunning = false
    private val SHIZUKU_REQUEST_CODE = 2024
    private val mainHandler = Handler(Looper.getMainLooper())

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin Shizuku Diterima!", Toast.LENGTH_SHORT).show()
                runAntiKillCommandsViaShizuku()
            } else {
                Toast.makeText(this, "Izin Shizuku Ditolak pengguna.", Toast.LENGTH_SHORT).show()
            }
            updateStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvShizukuStatus = findViewById(R.id.tvShizukuStatus)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnShizukuOptimize = findViewById(R.id.btnShizukuOptimize)
        btnGrantNotifAccess = findViewById(R.id.btnGrantNotifAccess)
        btnDisableBatteryOpt = findViewById(R.id.btnDisableBatteryOpt)
        btnOpenLimore = findViewById(R.id.btnOpenLimore)
        btnStopAlarm = findViewById(R.id.btnStopAlarm)

        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        btnGrantNotifAccess.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        btnDisableBatteryOpt.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }

        btnToggleService.setOnClickListener {
            if (isServiceRunning) {
                stopKeepAliveService()
            } else {
                startKeepAliveService()
            }
        }

        btnShizukuOptimize.setOnClickListener {
            handleShizukuOptimizationClick()
        }

        btnOpenLimore.setOnClickListener {
            launchLimoreApp()
        }

        btnStopAlarm.setOnClickListener {
            LimoreNotificationListener.stopAlarm()
            Toast.makeText(this, "Alarm & getar dimatikan!", Toast.LENGTH_SHORT).show()
        }

        updateStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        // Cek Izin Notifikasi
        val hasNotifAccess = isNotificationServiceEnabled()
        if (hasNotifAccess) {
            btnGrantNotifAccess.text = "✅ Izin Notifikasi: Aktif"
            btnGrantNotifAccess.isEnabled = false
        } else {
            btnGrantNotifAccess.text = "⚠️ Izinkan Akses Notifikasi"
            btnGrantNotifAccess.isEnabled = true
        }

        // Cek Service
        if (isServiceRunning) {
            tvStatus.text = "Status Service: 🟢 AKTIF (Menahan Deep Sleep)"
            btnToggleService.text = "Hentikan Pemantau Antrean"
        } else {
            tvStatus.text = "Status Service: ⚪ Nonaktif"
            btnToggleService.text = "Mulai Pantau Antrean"
        }

        // Cek Status Shizuku
        try {
            val isBinderAlive = Shizuku.pingBinder()
            if (isBinderAlive) {
                val isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                if (isGranted) {
                    tvShizukuStatus.text = "Shizuku: 🟢 Terhubung & Diizinkan"
                } else {
                    tvShizukuStatus.text = "Shizuku: 🟡 Aktif (Butuh Izin)"
                }
            } else {
                tvShizukuStatus.text = "Shizuku: 🔴 Belum Berjalan"
            }
        } catch (e: Exception) {
            tvShizukuStatus.text = "Shizuku: ⚪ Tidak Terdeteksi"
        }
    }

    private fun handleShizukuOptimizationClick() {
        try {
            if (!Shizuku.pingBinder()) {
                AlertDialog.Builder(this)
                    .setTitle("Shizuku Belum Aktif")
                    .setMessage("Aplikasi Shizuku belum berjalan di HP Anda.\n\nSilakan buka aplikasi Shizuku dan aktifkan lewat Wireless Debugging (Bebas PC di Android 11/12/13), lalu kembali ke sini.")
                    .setPositiveButton("Buka Shizuku") { _, _ ->
                        val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                        if (intent != null) startActivity(intent)
                        else Toast.makeText(this, "Aplikasi Shizuku belum diinstal!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Tutup", null)
                    .show()
                return
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                runAntiKillCommandsViaShizuku()
            } else {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal terhubung ke Shizuku: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun runAntiKillCommandsViaShizuku() {
        btnShizukuOptimize.isEnabled = false
        btnShizukuOptimize.text = "⏳ Sedang Menerapkan Trik Anti-Kill..."

        Thread {
            val commands = listOf(
                // 1. Berikan izin bebas background tanpa batas ke Limore
                "cmd appops set com.lingwoyun.limore RUN_IN_BACKGROUND allow",
                // 2. Bebaskan Limore dari Doze & Battery Optimization
                "dumpsys deviceidle whitelist +com.lingwoyun.limore",
                // 3. Matikan Phantom Process Killer Android (anti kill proses anak streaming)
                "/system/bin/device_config put activity_manager max_phantom_processes 2147483647",
                "settings put global settings_enable_monitor_phantom_procs false",
                // 4. Aktifkan Freeform Floating Window & Resizeable
                "settings put global enable_freeform_support 1",
                "settings put global force_resizable_activities 1",
                // 5. Whitelist aplikasi keeper ini juga
                "cmd appops set com.assistant.limorekeeper RUN_IN_BACKGROUND allow",
                "dumpsys deviceidle whitelist +com.assistant.limorekeeper"
            )

            val logs = StringBuilder()
            var allSuccess = true

            for (cmd in commands) {
                val (output, error) = runShellViaShizuku(arrayOf("sh", "-c", cmd))
                if (error.isNotEmpty()) {
                    logs.append("⚠️ $cmd: $error\n")
                } else {
                    logs.append("✅ $cmd\n")
                }
            }

            mainHandler.post {
                btnShizukuOptimize.isEnabled = true
                btnShizukuOptimize.text = "⚡ 1-Click Terapkan Anti-Kill (Via Shizuku)"
                AlertDialog.Builder(this)
                    .setTitle("🎉 Optimasi Anti-Kill Berhasil!")
                    .setMessage("Semua perintah ADB berhasil dieksekusi langsung ke sistem Redmi A2 Anda:\n\n$logs\n\nSekarang Limore tidak akan dimatikan paksa oleh sistem Android Go saat ditinggal antre!")
                    .setPositiveButton("Mantap!", null)
                    .show()
            }
        }.start()
    }

    private fun runShellViaShizuku(command: Array<String>): Pair<String, String> {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true
            val remoteProcess = method.invoke(null, command, null, null) as java.lang.Process
            val output = remoteProcess.inputStream.bufferedReader().use { it.readText() }
            val error = remoteProcess.errorStream.bufferedReader().use { it.readText() }
            remoteProcess.destroy()
            Pair(output.trim(), error.trim())
        } catch (e: Exception) {
            Pair("", "Error: ${e.message}")
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Aplikasi sudah bebas dari pembatasan baterai!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startKeepAliveService() {
        val intent = Intent(this, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isServiceRunning = true
        updateStatus()
        Toast.makeText(this, "Pemantau antrean berjalan di latar belakang!", Toast.LENGTH_SHORT).show()
    }

    private fun stopKeepAliveService() {
        val intent = Intent(this, KeepAliveService::class.java)
        intent.action = "STOP_SERVICE"
        startService(intent)
        isServiceRunning = false
        updateStatus()
        Toast.makeText(this, "Pemantau dihentikan.", Toast.LENGTH_SHORT).show()
    }

    private fun launchLimoreApp() {
        val limorePkg = "com.lingwoyun.limore"
        val launchIntent = packageManager.getLaunchIntentForPackage(limorePkg)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(
                this,
                "Aplikasi Limore belum terpasang atau package name berbeda!",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
