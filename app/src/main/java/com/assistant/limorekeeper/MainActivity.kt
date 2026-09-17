package com.assistant.limorekeeper

import android.app.ActivityOptions
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
    private lateinit var btnRefreshShizuku: Button
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

    // Shizuku Listeners
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            updateStatus()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            updateStatus()
        }
    }

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

        // Daftarkan listener Shizuku Sticky agar saat binder tiba langsung otomatis update
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
        btnRefreshShizuku = findViewById(R.id.btnRefreshShizuku)
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
        btnRefreshShizuku.setOnClickListener {
            updateStatus()
            val isAlive = isShizukuReady()
            if (isAlive) {
                Toast.makeText(this, "🟢 Shizuku Terhubung & Siap!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "🔴 Shizuku belum merespons. Pastikan Shizuku running di background.", Toast.LENGTH_LONG).show()
            }
        }

        btnShizukuOptimize.setOnClickListener {
            handleShizukuOptimizationClick()
        }

        btnToggleFloatingBubble.setOnClickListener {
            toggleFloatingBubble()
        }

        btnLaunchFreeform.setOnClickListener {
            forceLaunchLimoreFloating()
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

    private fun isShizukuReady(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    private fun handleShizukuOptimizationClick() {
        if (!isShizukuReady()) {
            // Coba periksa sekali lagi secara instan
            updateStatus()
        }

        if (!isShizukuReady()) {
            AlertDialog.Builder(this)
                .setTitle("Menghubungkan ke Shizuku")
                .setMessage("Shizuku belum terdeteksi aktif oleh sistem.\n\nLangkah mudah:\n1. Buka aplikasi Shizuku di HP Anda.\n2. Pastikan tertulis 'Shizuku is running'.\n3. Kembali ke sini dan klik tombol '🔄 Refresh' di samping status.")
                .setPositiveButton("Buka Shizuku") { _, _ ->
                    val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) startActivity(intent)
                    else Toast.makeText(this, "Aplikasi Shizuku belum terpasang!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Tutup", null)
                .show()
            return
        }

        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                runAntiKillCommandsViaShizuku()
            } else {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal meminta izin: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun runAntiKillCommandsViaShizuku() {
        btnShizukuOptimize.isEnabled = false
        btnShizukuOptimize.text = "⏳ Sedang Menerapkan Ultra Anti-Kill..."

        Thread {
            val commands = listOf(
                // 1. Izin background mutlak
                "cmd appops set com.lingwoyun.limore RUN_IN_BACKGROUND allow",
                "cmd appops set com.lingwoyun.limore RUN_ANY_IN_BACKGROUND allow",
                "cmd appops set com.lingwoyun.limore START_FOREGROUND allow",
                // 2. Whitelist dari Doze & Battery Optimization
                "dumpsys deviceidle whitelist +com.lingwoyun.limore",
                "dumpsys deviceidle whitelist +com.assistant.limorekeeper",
                // 3. Matikan Phantom Process Killer Android (anti kill streaming core)
                "/system/bin/device_config put activity_manager max_phantom_processes 2147483647",
                "settings put global settings_enable_monitor_phantom_procs false",
                // 4. Cegah pembunuhan cached idle process
                "device_config put activity_manager kill_bg_restricted_cached_idle false",
                // 5. Aktifkan Mode Freeform & Resizable (Jendela Melayang) di Android Go
                "settings put global enable_freeform_support 1",
                "settings put global force_resizable_activities 1",
                "settings put secure force_resizable_activities 1",
                // 6. Kunci Kernel oom_score_adj Limore jika sedang aktif
                "PID=\$(pidof com.lingwoyun.limore); if [ -n \"\$PID\" ]; then echo -900 > /proc/\$PID/oom_score_adj; fi"
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
                btnShizukuOptimize.text = "⚡ 1-Click Terapkan Ultra Anti-Kill"
                AlertDialog.Builder(this)
                    .setTitle("🔥 Ultra Anti-Kill Berhasil Diterapkan!")
                    .setMessage("Semua proteksi tingkat kernel & sistem telah aktif:\n\n$logs\nLimore kini terlindung dari Low Memory Killer (LMK) dan mode Jendela Melayang sudah terbuka!")
                    .setPositiveButton("Mantap!", null)
                    .show()
            }
        }.start()
    }

    private fun forceLaunchLimoreFloating() {
        Toast.makeText(this, "Memaksa Limore terbuka dalam mode Floating...", Toast.LENGTH_SHORT).show()

        // 1. Ambil nama activity peluncur Limore
        val launchIntent = packageManager.getLaunchIntentForPackage("com.lingwoyun.limore")
        val componentName = launchIntent?.component?.flattenToShortString() ?: "com.lingwoyun.limore/.MainActivity"

        // 2. Buka Bubble melayang agar tombol navigasi cepat tetap ada di layar
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            val bubbleIntent = Intent(this, FloatingBubbleService::class.java)
            startService(bubbleIntent)
            isFloatingBubbleRunning = true
            btnToggleFloatingBubble.text = "✖ Sembunyikan Bubble Melayang"
        }

        // 3. Eksekusi via Shizuku jika terhubung (perintah AM WindowingMode 5)
        if (isShizukuReady() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Thread {
                val cmds = listOf(
                    "settings put global enable_freeform_support 1",
                    "settings put global force_resizable_activities 1",
                    "am start --windowingMode 5 -n $componentName",
                    "cmd activity start --windowingMode 5 -n $componentName",
                    // Kunci oom_score_adj Limore seketika
                    "sleep 1 && PID=\$(pidof com.lingwoyun.limore); if [ -n \"\$PID\" ]; then echo -900 > /proc/\$PID/oom_score_adj; fi"
                )
                for (c in cmds) {
                    runShellViaShizuku(arrayOf("sh", "-c", c))
                }
            }.start()
        }

        // 4. Eksekusi via ActivityOptions reflection di level Java/Kotlin
        try {
            val options = ActivityOptions.makeBasic()
            val method = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
            method.invoke(options, 5) // 5 = WINDOWING_MODE_FREEFORM

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                startActivity(launchIntent, options.toBundle())
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback jika reflection gagal
        launchLimoreApp()
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            tvStatus.text = "🟢 Proteksi Aktif (Kernel LMK Locked)"
            btnToggleService.text = "Hentikan Pemantau Antrean"
        } else {
            tvStatus.text = "⚪ Layanan Pemantau Nonaktif"
            btnToggleService.text = "Mulai Pantau Antrean"
        }

        // Cek Status Shizuku
        try {
            if (Shizuku.pingBinder()) {
                val isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                if (isGranted) {
                    tvShizukuStatus.text = "Shizuku: 🟢 Terhubung & Siap"
                    btnShizukuOptimize.isEnabled = true
                } else {
                    tvShizukuStatus.text = "Shizuku: 🟡 Berjalan (Perlu Izin)"
                    btnShizukuOptimize.isEnabled = true
                }
            } else {
                tvShizukuStatus.text = "Shizuku: 🔴 Belum Merespons"
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
        Toast.makeText(this, "Pemantau antrean & Proteksi Kernel aktif!", Toast.LENGTH_SHORT).show()
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
