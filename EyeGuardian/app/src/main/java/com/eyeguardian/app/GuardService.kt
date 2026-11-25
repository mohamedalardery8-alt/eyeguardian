package com.eyeguardian.app
import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

class GuardService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private lateinit var detector: FaceDetector
    private var busy = false; private var last = 0L; private var hits = 0; private var lastViolation = 0L
    private lateinit var thread: HandlerThread; private lateinit var handler: Handler

    override fun onBind(i: Intent?) = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        Notifier.channel(this)
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "eg_run").setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("حارس البصر").setContentText("🛡️ الحماية تعمل").setContentIntent(pi).setOngoing(true).build()
        startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        val code = i?.getIntExtra("code", 0) ?: 0
        val data = i?.getParcelableExtra<Intent>("data")
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        thread = HandlerThread("eg").also { it.start() }; handler = Handler(thread.looper)
        val strict = Prefs.strictness(this)
        detector = FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(when (strict) { 1 -> 0.10f; 2 -> 0.05f; else -> 0.025f }).build())
        val mpm = getSystemService(MediaProjectionManager::class.java)
        projection = mpm.getMediaProjection(code, data).apply {
            registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { Notifier.alert(this@GuardService, "توقفت المراقبة!", "اضغط لإعادة التفعيل"); stopSelf() }
            }, handler)
        }
        val dm = resources.displayMetrics; val sc = if (dm.widthPixels > 1080) 1080f / dm.widthPixels else 1f
        val w = (dm.widthPixels * sc).toInt(); val h = (dm.heightPixels * sc).toInt()
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        vd = projection!!.createVirtualDisplay("eg", w, h, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler)
        reader!!.setOnImageAvailableListener({ r -> analyze(r) }, handler)
        // أثناء العقوبة: أي تطبيق يُفتح تظهر فوقه الشاشة الحمراء فورًا
        val watch = object : Runnable { override fun run() {
            if (locked() && !LockActivity.visible) Handler(Looper.getMainLooper()).post { LockActivity.start(this@GuardService) }
            handler.postDelayed(this, 700) } }
        handler.post(watch)
        return START_NOT_STICKY
    }

    private fun locked() = getSharedPreferences("eg", MODE_PRIVATE).getLong("lockUntil", 0) > System.currentTimeMillis()

    private fun analyze(r: ImageReader) {
        val img = r.acquireLatestImage() ?: return
        val now = SystemClock.elapsedRealtime()
        if (busy || now - last < 250 || locked()) { img.close(); return }
        busy = true; last = now
        val p = img.planes[0]; val rowPad = p.rowStride - p.pixelStride * img.width
        val bmp = Bitmap.createBitmap(img.width + rowPad / p.pixelStride, img.height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(p.buffer); img.close()
        detector.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { faces -> onResult(Gender.anyWoman(bmp, faces)) }
            .addOnCompleteListener { bmp.recycle(); busy = false }
    }

    private fun onResult(found: Boolean) {
        hits = if (found) hits + 1 else 0
        val need = 1
        if (hits >= need && System.currentTimeMillis() - lastViolation > 3000) {
            lastViolation = System.currentTimeMillis(); hits = 0
            Handler(Looper.getMainLooper()).post { Penalty.onViolation(this) }
        }
    }
    override fun onDestroy() { vd?.release(); reader?.close(); projection?.stop(); super.onDestroy() }
}

/** نقطة ربط نموذج تمييز الجنس. الافتراضي: أي وجه = تنبيه. استبدله بنموذج TFLite لتقليل الإنذارات الخاطئة. */
object Gender {
    fun anyWoman(bmp: Bitmap, faces: List<Face>): Boolean = faces.isNotEmpty()
}
