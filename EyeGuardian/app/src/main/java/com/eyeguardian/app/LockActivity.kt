package com.eyeguardian.app
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.view.*
import android.widget.*
import com.eyeguardian.app.Prefs.lockUntil

class LockActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var tv: TextView
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setShowWhenLocked(true); setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#B71C1C")); setPadding(48, 48, 48, 48)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        l.addView(TextView(this).apply { text = "غض بصرك\nتذكّر أن الله يراك"; textSize = 34f
            setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        tv = TextView(this).apply { textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, 48, 0, 48) }
        l.addView(tv)
        l.addView(Button(this).apply { text = "📞 مكالمة طوارئ"
            setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL).setData(android.net.Uri.parse("tel:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } })
        setContentView(l)
        if (Protection.isOwner(this)) try { startLockTask() } catch (_: Exception) {}
        tick()
    }
    override fun onResume() { super.onResume(); visible = true }
    override fun onPause() { visible = false; super.onPause() }
    private fun tick() {
        val left = (lockUntil - System.currentTimeMillis()) / 1000
        if (left <= 0) { try { stopLockTask() } catch (_: Exception) {}; finish(); return }
        tv.text = "الوقت المتبقي\n%d:%02d:%02d".format(left / 3600, left % 3600 / 60, left % 60)
        h.postDelayed({ tick() }, 500)
    }
    @Deprecated("Deprecated in Java") override fun onBackPressed() {}
    override fun onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy() }
    companion object {
        @Volatile var visible = false
        fun start(c: Context) = c.startActivity(Intent(c, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }
}
