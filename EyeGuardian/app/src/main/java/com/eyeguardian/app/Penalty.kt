package com.eyeguardian.app
import android.content.Context
import com.eyeguardian.app.Prefs.lockUntil

object Penalty {
    /** كل مخالفة = قفل ثابت لا يُلغى: ساعتين (المخالفة 1-2) أو 3 ساعات (3 فأكثر). */
    fun onViolation(c: Context) {
        val n = Prefs.addViolation(c)
        Notifier.siren(c)
        val hours = if (n >= 3 || Prefs.strictness(c) >= 3) 3L else 2L
        val end = System.currentTimeMillis() + hours * 3600_000
        if (c.lockUntil < end) c.lockUntil = end
        Protection.lockNow(c)
        LockActivity.start(c)
    }
}
