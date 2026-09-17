package com.assistant.limorekeeper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnGrantNotifAccess: Button
    private lateinit var btnDisableBatteryOpt: Button
    private lateinit var btnOpenLimore: Button
    private lateinit var btnStopAlarm: Button

    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnGrantNotifAccess = findViewById(R.id.btnGrantNotifAccess)
        btnDisableBatteryOpt = findViewById(R.id.btnDisableBatteryOpt)
        btnOpenLimore = findViewById(R.id.btnOpenLimore)
        btnStopAlarm = findViewById(R.id.btnStopAlarm)

        updateStatus()

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

        btnOpenLimore.setOnClickListener {
            launchLimoreApp()
        }

        btnStopAlarm.setOnClickListener {
            LimoreNotificationListener.stopAlarm()
            Toast.makeText(this, "Alarm & getar dimatikan!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasNotifAccess = isNotificationServiceEnabled()
        if (hasNotifAccess) {
            btnGrantNotifAccess.text = "✅ Izin Notifikasi Aktif"
            btnGrantNotifAccess.isEnabled = false
        } else {
            btnGrantNotifAccess.text = "⚠️ Izinkan Akses Notifikasi"
            btnGrantNotifAccess.isEnabled = true
        }

        if (isServiceRunning) {
            tvStatus.text = "Status: 🟢 Pemantau Antrean AKTIF"
            btnToggleService.text = "Hentikan Pemantau"
        } else {
            tvStatus.text = "Status: ⚪ Pemantau Nonaktif"
            btnToggleService.text = "Mulai Pantau Antrean"
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
