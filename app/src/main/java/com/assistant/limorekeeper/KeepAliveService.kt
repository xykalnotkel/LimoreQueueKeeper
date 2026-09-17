package com.assistant.limorekeeper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import rikka.shizuku.Shizuku

class KeepAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val CHANNEL_ID = "limore_keepalive_service"
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val oomLockRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            protectLimoreProcessInKernel()
            handler.postDelayed(this, 12000) // Jalankan setiap 12 detik
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        acquireLocks()
        startAsForeground()
        handler.post(oomLockRunnable)
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "LimoreKeeper::KeepAliveWakelock"
            ).apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L) // Tahan CPU aktif maks 6 jam saat antre
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "LimoreKeeper::WifiLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun protectLimoreProcessInKernel() {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Thread {
                    val clazz = Class.forName("rikka.shizuku.Shizuku")
                    val method = clazz.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    method.isAccessible = true
                    // Kunci oom_score_adj Limore ke -900 (level System App, LMK dilarang mematikan)
                    val cmd = arrayOf(
                        "sh", "-c",
                        "PID=\$(pidof com.lingwoyun.limore); if [ -n \"\$PID\" ]; then echo -900 > /proc/\$PID/oom_score_adj 2>/dev/null; fi"
                    )
                    val proc = method.invoke(null, cmd, null, null) as java.lang.Process
                    proc.waitFor()
                }.start()
            }
        } catch (e: Exception) {
            // Abaikan jika shizuku belum aktif
        }
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Limore Ultra Protection Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga koneksi antrean & melindungi proses Limore dari LMK"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Limore Ultra Protection Aktif")
            .setContentText("Koneksi antrean & Proteksi Kernel LMK sedang berjalan...")
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            isRunning = false
            handler.removeCallbacks(oomLockRunnable)
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(oomLockRunnable)
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
