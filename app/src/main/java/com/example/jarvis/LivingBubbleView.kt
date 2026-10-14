package com.example.jarvis

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Animated JARVIX orb. It reacts visually to interaction states and a cautious
 * text-based mood hint; it does not claim to measure acoustic emotion.
 */
class LivingBubbleView @JvmOverloads constructor(
 context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
 enum class Mood { IDLE, LISTENING, THINKING, SPEAKING, HAPPY, CALM, CONCERNED }

 private var mood = Mood.IDLE
 private var phase = 0f
 private var pulse = 0f
 private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
 private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
  duration = 2400L
  repeatCount = ValueAnimator.INFINITE
  interpolator = DecelerateInterpolator()
  addUpdateListener {
   phase = it.animatedValue as Float
   pulse = (0.5f + 0.5f * sin(phase * Math.PI * 2)).toFloat()
   invalidate()
  }
 }

 init {
  setLayerType(LAYER_TYPE_SOFTWARE, null)
  contentDescription = "Bulle vivante JARVIX"
  animator.start()
 }

 fun setMood(next: Mood) {
  mood = next
  contentDescription = when (next) {
   Mood.IDLE -> "JARVIX en attente"
   Mood.LISTENING -> "JARVIX écoute"
   Mood.THINKING -> "JARVIX réfléchit"
   Mood.SPEAKING -> "JARVIX parle"
   Mood.HAPPY -> "JARVIX réagit positivement"
   Mood.CALM -> "JARVIX est calme"
   Mood.CONCERNED -> "JARVIX réagit avec attention"
  }
  invalidate()
 }

 override fun onDraw(canvas: Canvas) {
  super.onDraw(canvas)
  val cx = width / 2f
  val cy = height / 2f
  val radius = min(width, height) * 0.29f
  val color = when (mood) {
   Mood.LISTENING -> Color.rgb(50, 210, 255)
   Mood.THINKING -> Color.rgb(145, 105, 255)
   Mood.SPEAKING -> Color.rgb(65, 235, 205)
   Mood.HAPPY -> Color.rgb(255, 190, 70)
   Mood.CALM -> Color.rgb(90, 175, 255)
   Mood.CONCERNED -> Color.rgb(255, 125, 110)
   Mood.IDLE -> Color.rgb(75, 160, 255)
  }
  val active = mood != Mood.IDLE
  val breathing = if (active) 0.10f * pulse else 0.035f * pulse

  // Ambient halo.
  paint.style = Paint.Style.FILL
  paint.shader = RadialGradient(cx, cy, radius * (2.15f + breathing), intArrayOf(
   withAlpha(color, if (active) 90 else 50), withAlpha(color, 0), Color.TRANSPARENT
  ), floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP)
  canvas.drawCircle(cx, cy, radius * 2.15f, paint)
  paint.shader = null

  // Three orbit rings.
  paint.style = Paint.Style.STROKE
  paint.strokeWidth = 2.0f
  paint.color = withAlpha(color, 70 + (pulse * 90).toInt())
  canvas.drawCircle(cx, cy, radius * (1.43f + breathing), paint)
  paint.strokeWidth = 1.3f
  paint.color = withAlpha(color, 95)
  canvas.save()
  canvas.rotate(phase * 360f, cx, cy)
  canvas.drawOval(cx - radius * 1.72f, cy - radius * 0.62f, cx + radius * 1.72f, cy + radius * 0.62f, paint)
  canvas.restore()
  canvas.save()
  canvas.rotate(-phase * 240f, cx, cy)
  canvas.drawOval(cx - radius * 1.52f, cy - radius * 0.48f, cx + radius * 1.52f, cy + radius * 0.48f, paint)
  canvas.restore()

  // Glossy, luminous core.
  paint.style = Paint.Style.FILL
  paint.shader = RadialGradient(
   cx - radius * 0.30f, cy - radius * 0.36f, radius * 1.55f,
   intArrayOf(Color.WHITE, color, darken(color), Color.rgb(4, 12, 34)),
   floatArrayOf(0f, 0.22f, 0.64f, 1f), Shader.TileMode.CLAMP
  )
  canvas.drawCircle(cx, cy, radius * (1f + breathing), paint)
  paint.shader = null

  // Tiny moving particles around the orb.
  paint.style = Paint.Style.FILL
  for (i in 0 until 7) {
   val angle = phase * Math.PI * 2 + i * (Math.PI * 2 / 7)
   val orbit = radius * (1.58f + (i % 3) * 0.16f)
   val x = cx + cos(angle).toFloat() * orbit
   val y = cy + sin(angle).toFloat() * orbit * 0.72f
   paint.color = withAlpha(color, 100 + (i * 20))
   canvas.drawCircle(x, y, 2.2f + (i % 3), paint)
  }
 }

 private fun withAlpha(color: Int, alpha: Int): Int =
  Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

 private fun darken(color: Int): Int =
  Color.rgb((Color.red(color) * 0.42f).toInt(), (Color.green(color) * 0.42f).toInt(), (Color.blue(color) * 0.42f).toInt())

 override fun onDetachedFromWindow() {
  animator.cancel()
  super.onDetachedFromWindow()
 }
}
