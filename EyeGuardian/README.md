# حارس البصر – خطوات البناء والتثبيت
1. افتح المجلد في Android Studio (Koala أو أحدث) ← Sync ← Build > Build APK(s).
   الملف: app/build/outputs/apk/debug/app-debug.apk
2. ثبّت الـAPK. افتح التطبيق: فعّل (1) صلاحية المسؤول، (2) العرض فوق التطبيقات، (3) المراقبة.
3. لمنع الحذف فعليًا (Device Owner): امسح كل حسابات Google من التليفون مؤقتًا، فعّل USB debugging، ثم:
   adb shell dpm set-device-owner com.eyeguardian.app/.AdminReceiver
   بعدها التطبيق يمنع الحذف وإعادة الضبط والوضع الآمن. ارجع أضف حساباتك.
4. مخرج الطوارئ: داخل التطبيق ← طلب إيقاف ← بعد 24 ساعة يظهر زر الإيقاف.
