package com.eyeguardian.app
import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("eg", Context.MODE_PRIVATE)
    fun today() = System.currentTimeMillis() / 86_400_000L

    fun violations(c: Context): Int {
        val s = sp(c); if (s.getLong("day", 0) != today()) { s.edit().putLong("day", today()).putInt("v", 0).apply() }
        return s.getInt("v", 0)
    }
    fun addViolation(c: Context): Int {
        val n = violations(c) + 1
        val log = (listOf("${java.text.SimpleDateFormat("MM/dd HH:mm").format(java.util.Date())}  مخالفة رقم $n") +
            (sp(c).getString("log", "") ?: "").split("\n").filter { it.isNotBlank() }).take(50)
        sp(c).edit().putInt("v", n).putString("log", log.joinToString("\n")).apply()
        return n
    }
    fun log(c: Context) = (sp(c).getString("log", "") ?: "").split("\n").filter { it.isNotBlank() }

    var Context.lockUntil: Long
        get() = sp(this).getLong("lockUntil", 0)
        set(v) { sp(this).edit().putLong("lockUntil", v).apply() }

    /** 1 = عادي، 2 = صارم، 3 = شديد الصرامة */
    fun strictness(c: Context) = sp(c).getInt("strict", 3)
    fun setStrictness(c: Context, v: Int) = sp(c).edit().putInt("strict", v).apply()

    fun disableRequestedAt(c: Context) = sp(c).getLong("disableReq", 0)
    fun requestDisable(c: Context) = sp(c).edit().putLong("disableReq", System.currentTimeMillis()).apply()
    fun cancelDisable(c: Context) = sp(c).edit().putLong("disableReq", 0).apply()
    const val DISABLE_DELAY_MS = 24L * 3600 * 1000
    fun canDisableNow(c: Context): Boolean {
        val t = disableRequestedAt(c); return t > 0 && System.currentTimeMillis() - t >= DISABLE_DELAY_MS
    }
}
