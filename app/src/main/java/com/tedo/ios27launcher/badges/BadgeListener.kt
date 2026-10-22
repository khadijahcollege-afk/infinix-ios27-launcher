package com.tedo.ios27launcher.badges

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color as AColor
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Satu notifikasi asli yang ditampilkan di Notification Center milik launcher. */
data class NotifItem(
    val key: String,
    val pkg: String,
    val title: String,
    val text: String,
    val time: Long,
    val ongoing: Boolean
)

/** Penyimpan data notifikasi yang dibaca UI: lencana angka + daftar Notification Center. */
object BadgeStore {
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var notifications by mutableStateOf<List<NotifItem>>(emptyList())
        private set
    @Volatile var service: BadgeListener? = null
    /** Intent asli tiap notifikasi, agar ketukan di Notification Center membuka
     *  notifikasinya langsung (bukan sekadar membuka app-nya). */
    @Volatile var intents: Map<String, PendingIntent> = emptyMap()
    /** True saat MainActivity launcher sedang tampil — overlay Island tidak perlu dobel. */
    @Volatile var launcherForeground: Boolean = false

    fun update(m: Map<String, Int>, items: List<NotifItem>) { counts = m; notifications = items }
    fun dismiss(key: String) { try { service?.cancelNotification(key) } catch (e: Exception) { } }
    fun dismissAll() { try { service?.cancelAllNotifications() } catch (e: Exception) { } }

    /** Sudah diberi izin Akses Notifikasi oleh user? */
    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(context.packageName)
    }
}

/**
 * Membaca notifikasi asli sistem untuk launcher: lencana angka ala iOS + Notification Center
 * milik launcher + DYNAMIC ISLAND MELAYANG yang tembus ke luar launcher (di atas app lain)
 * lewat izin "Tampilkan di atas aplikasi lain" (SYSTEM_ALERT_WINDOW).
 * Butuh izin "Akses Notifikasi" dari Pengaturan Android.
 */
class BadgeListener : NotificationListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var prevKeys: Set<String>? = null
    private val hideRunnable = Runnable { removeOverlay() }

    override fun onListenerConnected() { BadgeStore.service = this; refresh() }
    override fun onListenerDisconnected() { BadgeStore.service = null; mainHandler.post { removeOverlay() } }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun islandMode(): String =
        getSharedPreferences("glass", 0).getString("islandMode", "POPUP") ?: "POPUP"

    private fun refresh() {
        try {
            val active = activeNotifications?.filter { it.packageName != packageName } ?: emptyList()
            val counts = active
                .filter { (it.notification?.flags ?: 0) and Notification.FLAG_ONGOING_EVENT == 0 }
                .groupBy { it.packageName }
                .mapValues { it.value.size }
            val items = active.map { sbn ->
                val n = sbn.notification
                val ex = n?.extras
                val title = ex?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: ex?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString() ?: ""
                val text = ex?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    ?: ex?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                NotifItem(
                    key = sbn.key, pkg = sbn.packageName,
                    title = title, text = text,
                    time = sbn.postTime,
                    ongoing = (n?.flags ?: 0) and Notification.FLAG_ONGOING_EVENT != 0
                )
            }.sortedByDescending { it.time }
            BadgeStore.intents = active.mapNotNull { sbn ->
                sbn.notification?.contentIntent?.let { sbn.key to it } }.toMap()
            BadgeStore.update(counts, items)
            handleIsland(active.map { it.key }.toSet(), items, counts.values.sum())
        } catch (e: Exception) { /* listener belum terhubung penuh */ }
    }

    /** Logika Island melayang di LUAR launcher (di dalam launcher ditangani IslandPopup). */
    private fun handleIsland(keys: Set<String>, items: List<NotifItem>, total: Int) {
        val prev = prevKeys
        prevKeys = keys
        if (BadgeStore.launcherForeground) return
        if (!Settings.canDrawOverlays(this)) return
        val mode = islandMode()
        if (mode == "OFF") { mainHandler.post { removeOverlay() }; return }
        if (mode == "ALWAYS") {
            // Pil ramping selalu tampil; diperbarui setiap perubahan notifikasi.
            mainHandler.post { showOverlay(items.firstOrNull(), total, slim = true) }
            return
        }
        // POPUP: hanya bila ada kunci notifikasi BARU (dan bukan ledakan awal saat listener tersambung)
        if (prev == null) return
        val newKeys = keys - prev
        if (newKeys.isEmpty()) return
        val latest = items.firstOrNull { it.key in newKeys && !it.ongoing }
            ?: items.firstOrNull { it.key in newKeys } ?: return
        mainHandler.post {
            showOverlay(latest, total, slim = false)
            mainHandler.removeCallbacks(hideRunnable)
            mainHandler.postDelayed(hideRunnable, 4200)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** Gambar pil Island dengan View bawaan Android (ringan, tanpa Compose di service). */
    private fun showOverlay(latest: NotifItem?, total: Int, slim: Boolean) {
        removeOverlay()
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val pill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(7), dp(12), dp(7))
                background = GradientDrawable().apply {
                    setColor(AColor.BLACK); cornerRadius = dp(26).toFloat()
                    setStroke(dp(1), AColor.argb(36, 255, 255, 255))
                }
            }
            if (!slim && latest != null) {
                try {
                    val iv = ImageView(this)
                    iv.setImageDrawable(packageManager.getApplicationIcon(latest.pkg))
                    iv.layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
                    pill.addView(iv)
                } catch (e: Exception) { }
                val col = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(9), 0, 0, 0)
                }
                val appLabel = try { packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(latest.pkg, 0)).toString() } catch (e: Exception) { latest.pkg }
                col.addView(TextView(this).apply {
                    text = appLabel; setTextColor(AColor.rgb(166, 166, 166)); textSize = 9.5f
                    maxLines = 1; ellipsize = TextUtils.TruncateAt.END; maxWidth = dp(210)
                })
                col.addView(TextView(this).apply {
                    text = latest.title.ifBlank { latest.text }
                    setTextColor(AColor.WHITE); textSize = 12f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    maxLines = 1; ellipsize = TextUtils.TruncateAt.END; maxWidth = dp(210)
                })
                pill.addView(col)
                if (total > 0) {
                    val badge = TextView(this).apply {
                        text = total.toString(); setTextColor(AColor.WHITE); textSize = 10.5f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER
                        setPadding(dp(6), dp(2), dp(6), dp(2))
                        background = GradientDrawable().apply {
                            setColor(AColor.rgb(255, 59, 48)); cornerRadius = dp(11).toFloat() }
                    }
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.leftMargin = dp(9)
                    pill.addView(badge, lp)
                }
            } else {
                if (total > 0) {
                    val dot = View(this).apply {
                        background = GradientDrawable().apply {
                            setColor(AColor.rgb(255, 59, 48)); cornerRadius = dp(3).toFloat() }
                    }
                    pill.addView(dot, LinearLayout.LayoutParams(dp(6), dp(6)))
                    val tv = TextView(this).apply {
                        text = "$total notifikasi"; setTextColor(AColor.WHITE); textSize = 10.5f
                        setPadding(dp(6), 0, 0, 0)
                    }
                    pill.addView(tv)
                } else {
                    pill.minimumWidth = dp(104)
                }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp(38)
            }
            wm.addView(pill, params)
            overlayView = pill
        } catch (e: Exception) { /* izin overlay dicabut / jendela gagal */ }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (e: Exception) { }
            overlayView = null
        }
    }
}
