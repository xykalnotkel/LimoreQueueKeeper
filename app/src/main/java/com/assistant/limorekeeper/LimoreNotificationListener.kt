package com.assistant.limorekeeper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class LimoreNotificationListener : NotificationListenerService() {

    companion object {
        private var ringtone: Ringtone? = null
        private var vibrator: Vibrator? = null

        fun stopAlarm() {
            try {
                ringtone?.stop()
                vibrator?.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName ?: ""
        // Deteksi paket Limore Cloud Game
        if (pkgName.contains("limore") || pkgName == "com.lingwoyun.limore") {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val fullContent = "$title $text".lowercase()

            // Jika ada indikasi antrean siap / giliran main / notif penting
            // Atau tangkap semua notifikasi dari Limore jika antrean selesai
            triggerAlarm(title, text)
        }
    }

    private fun triggerAlarm(title: String, message: String) {
        val context = applicationContext

        // 1. Bunyikan Alarm Keras
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Getar Berulang
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 300, 800, 300, 1200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = loop
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Tampilkan Notifikasi Darurat & Tombol Buka Limore
        val channelId = "limore_alert_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Limore Queue Ready Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm ketika antrean Limore sudah selesai"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent untuk langsung membuka Limore saat diklik
        val launchIntent = packageManager.getLaunchIntentForPackage("com.lingwoyun.limore")
            ?: Intent(this, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🚨 ANTREAN LIMORE SIAP!")
            .setContentText(if (message.isNotEmpty()) message else "Giliran main kamu sudah tiba! Segera masuk!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(9999, notification)

        // Otomatis luncurkan Limore ke layar depan agar tidak kehabisan waktu timeout
        try {
            startActivity(launchIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
