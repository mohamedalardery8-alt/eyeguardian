package com.eyeguardian.app
import android.content.*
import com.eyeguardian.app.Prefs.lockUntil

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Protection.enforce(c)
        // أندرويد يشترط موافقة المستخدم على MediaProjection بعد كل إعادة تشغيل
        Notifier.alert(c, "الحماية متوقفة بعد إعادة التشغيل", "اضغط لإعادة تفعيل المراقبة")
        if (c.lockUntil > System.currentTimeMillis()) LockActivity.start(c)
        else if (android.provider.Settings.canDrawOverlays(c))
            c.startActivity(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
