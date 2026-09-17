package com.assistant.limorekeeper

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_bubble, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        windowManager?.addView(floatingView, params)
        setupBubbleControls()
    }

    private fun setupBubbleControls() {
        val root = floatingView ?: return
        val bubbleIcon = root.findViewById<ImageView>(R.id.imgBubbleIcon)
        val menuLayout = root.findViewById<LinearLayout>(R.id.layoutBubbleMenu)
        val btnLaunchLimore = root.findViewById<View>(R.id.btnBubbleLaunchLimore)
        val btnStopAlarm = root.findViewById<View>(R.id.btnBubbleStopAlarm)
        val btnClose = root.findViewById<View>(R.id.btnBubbleClose)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        bubbleIcon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params?.x = initialX + dx
                    params?.y = initialY + dy
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        // Toggle mini menu
                        if (menuLayout.visibility == View.VISIBLE) {
                            menuLayout.visibility = View.GONE
                        } else {
                            menuLayout.visibility = View.VISIBLE
                        }
                    }
                    true
                }
                else -> false
            }
        }

        btnLaunchLimore.setOnClickListener {
            menuLayout.visibility = View.GONE
            launchLimoreFreeformOrStandard()
        }

        btnStopAlarm.setOnClickListener {
            LimoreNotificationListener.stopAlarm()
            Toast.makeText(this, "Alarm dimatikan!", Toast.LENGTH_SHORT).show()
            menuLayout.visibility = View.GONE
        }

        btnClose.setOnClickListener {
            stopSelf()
        }
    }

    private fun launchLimoreFreeformOrStandard() {
        // Coba buka dengan Freeform via Shizuku jika terhubung
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Thread {
                    val clazz = Class.forName("rikka.shizuku.Shizuku")
                    val method = clazz.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    method.isAccessible = true
                    // Luncurkan Limore dengan flag freeform window
                    val cmd = arrayOf("sh", "-c", "am start -n com.lingwoyun.limore/.MainActivity --windowingMode 5 || am start -n com.lingwoyun.limore/com.lingwoyun.limore.MainActivity")
                    val proc = method.invoke(null, cmd, null, null) as java.lang.Process
                    proc.waitFor()
                }.start()
                Toast.makeText(this, "Meluncurkan Limore (Freeform)...", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            // fallback
        }

        val launchIntent = packageManager.getLaunchIntentForPackage("com.lingwoyun.limore")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Aplikasi Limore tidak ditemukan!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
