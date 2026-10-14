package com.example.jarvis

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.abs

/**
 * Optional floating orb. Android requires the user to grant overlay permission.
 * It only opens JARVIX or closes itself; it does not capture screen content.
 */
class FloatingBubbleService : Service() {
 companion object {
  const val ACTION_SET_MOOD = "com.example.jarvis.SET_FLOATING_MOOD"
  const val EXTRA_MOOD = "mood"
  @Volatile var isRunning: Boolean = false
   private set
 }
 private lateinit var windowManager: WindowManager
 private var orb: LivingBubbleView? = null
 private lateinit var params: WindowManager.LayoutParams
 private var initialX = 0
 private var initialY = 0
 private var initialTouchX = 0f
 private var initialTouchY = 0f
 private var dragged = false

 override fun onCreate() {
  super.onCreate()
  windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
 }

 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  if (intent?.action == ACTION_SET_MOOD) {
   val name = intent.getStringExtra(EXTRA_MOOD)
   val mood = runCatching { LivingBubbleView.Mood.valueOf(name ?: "IDLE") }.getOrDefault(LivingBubbleView.Mood.IDLE)
   orb?.setMood(mood)
   return START_STICKY
  }
  if (!android.provider.Settings.canDrawOverlays(this)) {
   Toast.makeText(this, "Autorise la bulle dans les paramètres Android.", Toast.LENGTH_LONG).show()
   stopSelf()
   return START_NOT_STICKY
  }
  if (orb == null) showOrb()
  return START_STICKY
 }

 private fun showOrb() {
  val view = LivingBubbleView(this)
  orb = view
  params = WindowManager.LayoutParams(
   dp(94), dp(94),
   WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
   PixelFormat.TRANSLUCENT
  ).apply {
   gravity = Gravity.TOP or Gravity.START
   x = dp(18)
   y = dp(180)
  }
  view.setOnTouchListener { v, event ->
   when (event.action) {
    MotionEvent.ACTION_DOWN -> {
     initialX = params.x
     initialY = params.y
     initialTouchX = event.rawX
     initialTouchY = event.rawY
     dragged = false
     true
    }
    MotionEvent.ACTION_MOVE -> {
     val dx = (event.rawX - initialTouchX).toInt()
     val dy = (event.rawY - initialTouchY).toInt()
     if (abs(dx) > dp(5) || abs(dy) > dp(5)) dragged = true
     params.x = initialX + dx
     params.y = initialY + dy
     runCatching { windowManager.updateViewLayout(v, params) }
     true
    }
    MotionEvent.ACTION_UP -> {
     if (!dragged) {
      if (event.eventTime - event.downTime > 650) {
       stopSelf()
      } else {
       val open = packageManager.getLaunchIntentForPackage(packageName)
       if (open != null) {
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(open)
       }
      }
     }
     true
    }
    else -> true
   }
  }
  windowManager.addView(view, params)
  isRunning = true
 }

 private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

 override fun onDestroy() {
  orb?.let { runCatching { windowManager.removeView(it) }; it.onDetachedFromWindow() }
  orb = null
  isRunning = false
  super.onDestroy()
 }

 override fun onBind(intent: Intent?): IBinder? = null
}
