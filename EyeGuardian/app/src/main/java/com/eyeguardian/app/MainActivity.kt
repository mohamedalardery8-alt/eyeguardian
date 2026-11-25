package com.eyeguardian.app
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)
    private val projLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK && r.data != null) {
            startForegroundService(Intent(this, GuardService::class.java).putExtra("code", r.resultCode).putExtra("data", r.data))
        }
        tick++
    }
    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onResume() { super.onResume(); tick++; Protection.enforce(this) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch("android.permission.POST_NOTIFICATIONS")
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize()) { Screen() } }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable fun Screen() {
        val t = tick
        var strict by remember { mutableIntStateOf(Prefs.strictness(this)) }
        val admin = Protection.isAdmin(this); val owner = Protection.isOwner(this)
        val overlay = Settings.canDrawOverlays(this)
        val req = Prefs.disableRequestedAt(this)
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🛡️ حارس البصر", style = MaterialTheme.typography.headlineMedium)
            Card { Column(Modifier.padding(14.dp)) {
                Text("حالة الحماية", style = MaterialTheme.typography.titleMedium)
                Text("مخالفات اليوم: ${Prefs.violations(this@MainActivity)}")
                Text("صلاحية المسؤول (قفل الهاتف): ${if (admin) "✅" else "❌"}")
                Text("Device Owner (منع الحذف): ${if (owner) "✅" else "❌"}")
                Text("العرض فوق التطبيقات: ${if (overlay) "✅" else "❌"}")
            } }
            if (!admin) Button({ startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, Protection.admin(this@MainActivity))) }) { Text("1) تفعيل صلاحية المسؤول") }
            if (!overlay) Button({ startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }) { Text("2) السماح بالعرض فوق التطبيقات") }
            Button({ projLauncher.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()) }) { Text("3) تشغيل المراقبة") }
            Text("مستوى الصرامة: " + listOf("عادي", "صارم", "شديد الصرامة")[strict - 1])
            Slider(strict.toFloat(), { strict = Math.round(it); Prefs.setStrictness(this@MainActivity, strict) }, valueRange = 1f..3f, steps = 1)
            Text("سجل المخالفات", style = MaterialTheme.typography.titleMedium)
            Prefs.log(this@MainActivity).take(10).forEach { Text(it) }
            HorizontalDivider()
            Text("مخرج الطوارئ", style = MaterialTheme.typography.titleMedium)
            if (getSharedPreferences("eg", MODE_PRIVATE).getLong("lockUntil", 0) > System.currentTimeMillis()) Text("غير متاح أثناء العقوبة")
            else if (req == 0L) Button({ Prefs.requestDisable(this@MainActivity); tick++ }) { Text("طلب إيقاف الحماية (ينتظر 24 ساعة)") }
            else if (Prefs.canDisableNow(this@MainActivity)) Button({ Prefs.cancelDisable(this@MainActivity)
                stopService(Intent(this@MainActivity, GuardService::class.java)); Protection.release(this@MainActivity); tick++ }) { Text("إيقاف الحماية الآن") }
            else {
                val hrs = (Prefs.DISABLE_DELAY_MS - (System.currentTimeMillis() - req)) / 3_600_000
                Text("متبقي حوالي $hrs ساعة"); OutlinedButton({ Prefs.cancelDisable(this@MainActivity); tick++ }) { Text("إلغاء الطلب") }
            }
        }
    }
}
