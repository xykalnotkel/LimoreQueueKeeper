package com.assistant.limorekeeper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    // Tab Containers
    private lateinit var tabDashboard: View
    private lateinit var tabAntiKill: View
    private lateinit var tabSettings: View
    private lateinit var bottomNav: BottomNavigationView

    // Dashboard Views
    private lateinit var tvStatus: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnOpenLimore: Button
    private lateinit var btnStopAlarm: Button
    private lateinit var btnGrantNotifAccess: Button
    private lateinit var btnDisableBatteryOpt: Button

    // Anti-Kill Views
    private lateinit var tvShizukuStatus: TextView
    private lateinit var btnShizukuOptimize: Button
    private lateinit var btnToggleFloatingBubble: Button
    private lateinit var btnLaunchFreeform: Button

    // Settings Views
    private lateinit var switchAutoLaunch: SwitchCompat
    private lateinit var switchSoundAlarm: SwitchCompat
    private lateinit var switchVibrate: SwitchCompat
    private lateinit var btnTestAlarm: Button

    private lateinit var prefs: SharedPreferences
    private var isServiceRunning = false
    private var isFloatingBubbleRunning = false
    private var testRingtone: Ringtone? = null
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

        prefs = getSharedPreferences("limore_settings", Context.MODE_PRIVATE)

        initViews()
        setupBottomNav()
        setupDashboardActions()
        setupAntiKillActions()
        setupSettingsActions()

        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        updateStatus()
    }

    private fun initViews() {
        tabDashboard = findViewById(R.id.tabDashboard)
        tabAntiKill = findViewById(R.id.tabAntiKill)
        tabSettings = findViewById(R.id.tabSettings)
        bottomNav = findViewById(R.id.bottomNav)

        // Tab 1
        tvStatus = findViewById(R.id.tvStatus)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnOpenLimore = findViewById(R.id.btnOpenLimore)
        btnStopAlarm = findViewById(R.id.btnStopAlarm)
        btnGrantNotifAccess = findViewById(R.id.btnGrantNotifAccess)
        btnDisableBatteryOpt = findViewById(R.id.btnDisableBatteryOpt)

        // Tab 2
        tvShizukuStatus = findViewById(R.id.tvShizukuStatus)
        btnShizukuOptimize = findViewById(R.id.btnShizukuOptimize)
        btnToggleFloatingBubble = findViewById(R.id.btnToggleFloatingBubble)
        btnLaunchFreeform = findViewById(R.id.btnLaunchFreeform)

        // Tab 3
        switchAutoLaunch = findViewById(R.id.switchAutoLaunch)
        switchSoundAlarm = findViewById(R.id.switchSoundAlarm)
        switchVibrate = findViewById(R.id.switchVibrate)
        btnTestAlarm = findViewById(R.id.btnTestAlarm)

        // Load Preferences
        switchAutoLaunch.isChecked = prefs.getBoolean("pref_auto_launch", true)
        switchSoundAlarm.isChecked = prefs.getBoolean("pref_sound", true)
        switchVibrate.isChecked = prefs.getBoolean("pref_vibrate", true)
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    tabDashboard.visibility = View.VISIBLE
                    tabAntiKill.visibility = View.GONE
                    tabSettings.visibility = View.GONE
                    true
                }
                R.id.nav_antikill -> {
                    tabDashboard.visibility = View.GONE
                    tabAntiKill.visibility = View.VISIBLE
                    tabSettings.visibility = View.GONE
                    updateStatus()
                    true
                }
                R.id.nav_settings -> {
                    tabDashboard.visibility = View.GONE
                    tabAntiKill.visibility = View.GONE
                    tabSettings.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDashboardActions() {
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
            stopTestRingtone()
            Toast.makeText(this, "Alarm & getar dimatikan!", Toast.LENGTH_SHORT).show()
        }

        btnGrantNotifAccess.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        btnDisableBatteryOpt.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }
    }

    private fun setupAntiKillActions() {
        btnShizukuOptimize.setOnClickListener {
            handleShizukuOptimizationClick()
        }

        btnToggleFloatingBubble.setOnClickListener {
            toggleFloatingBubble()
        }

        btnLaunchFreeform.setOnClickListener {
            launchLimoreInFreeform()
        }
    }

    private fun setupSettingsActions() {
        switchAutoLaunch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_auto_launch", isChecked).apply()
        }
        switchSoundAlarm.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_sound", isChecked).apply()
        }
        switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_vibrate", isChecked).apply()
        }

        btnTestAlarm.setOnClickListener {
            triggerTestAlarm()
        }
    }

    private fun triggerTestAlarm() {
        try {
            stopTestRingtone()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            testRingtone = RingtoneManager.getRingtone(this, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                testRingtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            testRingtone?.play()
            Toast.makeText(this, "🔔 Membunyikan nada uji coba (3 detik)...", Toast.LENGTH_SHORT).show()
            mainHandler.postDelayed({
                stopTestRingtone()
            }, 3000)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menguji suara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopTestRingtone() {
        try {
            if (testRingtone?.isPlaying == true) {
                testRingtone?.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleFloatingBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Izin Menampilkan di Atas Aplikasi")
                .setMessage("Untuk menampilkan Bubble Melayang, Anda perlu mengaktifkan izin 'Tampilkan di atas aplikasi lain'.")
                .setPositiveButton("Buka Pengaturan") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
                .setNegativeButton("Batal", null)
                .show()
            return
        }

        val serviceIntent = Intent(this, FloatingBubbleService::class.java)
        if (isFloatingBubbleRunning) {
            stopService(serviceIntent)
            isFloatingBubbleRunning = false
            btnToggleFloatingBubble.text = "🌐 Tampilkan Bubble Melayang"
            Toast.makeText(this, "Bubble ditutup.", Toast.LENGTH_SHORT).show()
        } else {
            startService(serviceIntent)
            isFloatingBubbleRunning = true
            btnToggleFloatingBubble.text = "✖ Sembunyikan Bubble Melayang"
            Toast.makeText(this, "Bubble melayang aktif!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchLimoreInFreeform() {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Thread {
                    val cmd = arrayOf("sh", "-c", "am start -n com.lingwoyun.limore/.MainActivity --windowingMode 5 || am start -n com.lingwoyun.limore/com.lingwoyun.limore.MainActivity")
                    runShellViaShizuku(cmd)
                }.start()
                Toast.makeText(this, "Membuka Limore dalam mode Freeform Window...", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            // fallback
        }
        launchLimoreApp()
    }

    private fun handleShizukuOptimizationClick() {
        try {
            if (!Shizuku.pingBinder()) {
                AlertDialog.Builder(this)
                    .setTitle("Shizuku Belum Aktif")
                    .setMessage("Aplikasi Shizuku belum aktif di HP Anda.\n\nBuka aplikasi Shizuku, lalu jalankan via Wireless Debugging (tanpa PC).")
                    .setPositiveButton("Buka Shizuku") { _, _ ->
                        val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                        if (intent != null) startActivity(intent)
                        else Toast.makeText(this, "Aplikasi Shizuku belum terpasang!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Error Shizuku: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun runAntiKillCommandsViaShizuku() {
        btnShizukuOptimize.isEnabled = false
        btnShizukuOptimize.text = "⏳ Sedang Menerapkan Anti-Kill..."

        Thread {
            val commands = listOf(
                "cmd appops set com.lingwoyun.limore RUN_IN_BACKGROUND allow",
                "dumpsys deviceidle whitelist +com.lingwoyun.limore",
                "/system/bin/device_config put activity_manager max_phantom_processes 2147483647",
                "settings put global settings_enable_monitor_phantom_procs false",
                "settings put global enable_freeform_support 1",
                "settings put global force_resizable_activities 1",
                "cmd appops set com.assistant.limorekeeper RUN_IN_BACKGROUND allow",
                "dumpsys deviceidle whitelist +com.assistant.limorekeeper"
            )

            val logs = StringBuilder()
            for (cmd in commands) {
                val (_, error) = runShellViaShizuku(arrayOf("sh", "-c", cmd))
                if (error.isNotEmpty()) {
                    logs.append("⚠️ $cmd\n")
                } else {
                    logs.append("✅ $cmd\n")
                }
            }

            mainHandler.post {
                btnShizukuOptimize.isEnabled = true
                btnShizukuOptimize.text = "⚡ 1-Click Terapkan Anti-Kill"
                AlertDialog.Builder(this)
                    .setTitle("🎉 Proteksi Anti-Kill Diterapkan!")
                    .setMessage("Pengaturan kernel & sistem Android Go berhasil dikonfigurasi:\n\n$logs\nLimore kini memiliki izin penuh di latar belakang dan mode Freeform aktif!")
                    .setPositiveButton("Sip!", null)
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

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        stopTestRingtone()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasNotifAccess = isNotificationServiceEnabled()
        if (hasNotifAccess) {
            btnGrantNotifAccess.text = "✅ Izin Notifikasi: Aktif"
            btnGrantNotifAccess.isEnabled = false
        } else {
            btnGrantNotifAccess.text = "⚠️ Izinkan Akses Notifikasi"
            btnGrantNotifAccess.isEnabled = true
        }

        if (isServiceRunning) {
            tvStatus.text = "🟢 Pemantau Aktif (CPU Awake)"
            btnToggleService.text = "Hentikan Pemantau Antrean"
        } else {
            tvStatus.text = "⚪ Layanan Pemantau Nonaktif"
            btnToggleService.text = "Mulai Pantau Antrean"
        }

        try {
            if (Shizuku.pingBinder()) {
                val isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                if (isGranted) {
                    tvShizukuStatus.text = "Shizuku: 🟢 Terhubung & Siap"
                } else {
                    tvShizukuStatus.text = "Shizuku: 🟡 Berjalan (Perlu Izin)"
                }
            } else {
                tvShizukuStatus.text = "Shizuku: 🔴 Belum Aktif"
            }
        } catch (e: Exception) {
            tvShizukuStatus.text = "Shizuku: ⚪ Tidak Terdeteksi"
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
        Toast.makeText(this, "Pemantau antrean aktif di background!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Aplikasi Limore belum terpasang di HP ini!", Toast.LENGTH_LONG).show()
        }
    }
}
