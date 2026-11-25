package com.eyeguardian.app
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager

class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "تحذير: إيقاف الحماية يتطلب طلب مخرج الطوارئ (انتظار 24 ساعة)."
    override fun onDisabled(context: Context, intent: Intent) {
        Notifier.alert(context, "تم تعطيل صلاحية المسؤول", "تم إيقاف حماية حارس البصر!")
    }
}

object Protection {
    private val RESTRICTIONS = listOf(UserManager.DISALLOW_FACTORY_RESET, UserManager.DISALLOW_SAFE_BOOT,
        UserManager.DISALLOW_UNINSTALL_APPS)
    fun admin(c: Context) = ComponentName(c, AdminReceiver::class.java)
    fun dpm(c: Context) = c.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    fun isAdmin(c: Context) = dpm(c).isAdminActive(admin(c))
    fun isOwner(c: Context) = dpm(c).isDeviceOwnerApp(c.packageName)

    fun lockNow(c: Context) { if (isAdmin(c)) dpm(c).lockNow() }

    /** منع الحذف + منع إعادة الضبط + منع الوضع الآمن (Device Owner فقط). */
    fun enforce(c: Context) {
        if (!isOwner(c)) return
        val d = dpm(c); val a = admin(c)
        d.setUninstallBlocked(a, c.packageName, true)
        RESTRICTIONS.forEach { d.addUserRestriction(a, it) }
        d.setLockTaskPackages(a, arrayOf(c.packageName, "com.android.dialer", "com.google.android.dialer", "com.android.server.telecom"))
    }

    /** مخرج الطوارئ بعد انتهاء الـ24 ساعة. */
    fun release(c: Context) {
        val d = dpm(c); val a = admin(c)
        if (isOwner(c)) {
            d.setUninstallBlocked(a, c.packageName, false)
            RESTRICTIONS.forEach { d.clearUserRestriction(a, it) }
            d.clearDeviceOwnerApp(c.packageName)
        } else if (isAdmin(c)) d.removeActiveAdmin(a)
    }
}
