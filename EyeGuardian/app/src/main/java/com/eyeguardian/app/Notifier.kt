package com.eyeguardian.app
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*

object Notifier {
    private const val CH = "eg_alerts"
    fun channel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "تنبيهات", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel("eg_run", "الحماية تعمل", NotificationManager.IMPORTANCE_LOW))
    }
    fun alert(c: Context, title: String, text: String) {
        channel(c)
        val pi = PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, CH).setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title).setContentText(text).setContentIntent(pi).setAutoCancel(true).build()
        c.getSystemService(NotificationManager::class.java).notify((System.currentTimeMillis() % 100000).toInt(), n)
    }
    private var player: MediaPlayer? = null
    /** صوت إنذار مرتفع على قناة المنبه */
    fun siren(c: Context, ms: Long = 3000) {
        val am = c.getSystemService(AudioManager::class.java)
        am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            setDataSource(c, uri); prepare(); start()
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ player?.release(); player = null }, ms)
    }
}
