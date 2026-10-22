package com.tedo.ios27launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap?,          // sudah di-downscale & di-cache (performa v0.4)
    val enabled: Boolean = true      // false = dinonaktifkan sistem (beku beneran)
)

object AppRepository {
    private val iconCache = LruCache<String, ImageBitmap>(300)
    @Volatile private var cachedApps: List<AppInfo>? = null
    @Volatile private var lastLoadMs: Long = 0
    @Volatile private var invalidated: Boolean = true

    /** Daftar app di-cache di memori: pemuatan ulang sesudah dari Info Aplikasi jadi instan.
     *  Pakai invalidate() bila perlu muat ulang paksa. */
    fun loadLaunchableApps(context: Context, force: Boolean = false): List<AppInfo> {
        // Audit v0.12: dulu SETIAP onResume memindai ulang semua paket (berat & boros).
        // Kini muat paksa diabaikan bila <120 dtk sejak muat terakhir, kecuali invalidate().
        cachedApps?.let {
            if (!force) return it
            if (!invalidated && System.currentTimeMillis() - lastLoadMs < 120_000) return it
        }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val result = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                val enabled = try {
                    pm.getApplicationInfo(pkg, 0).enabled &&
                        pm.getApplicationEnabledSetting(pkg) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                        pm.getApplicationEnabledSetting(pkg) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                } catch (e: Exception) { true }
                val icon = iconCache.get(pkg) ?: try {
                    ri.loadIcon(pm)?.toBitmap(128, 128)?.asImageBitmap()?.also { iconCache.put(pkg, it) }
                } catch (e: Exception) { null }
                AppInfo(ri.loadLabel(pm).toString(), pkg, icon, enabled)
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
        cachedApps = result
        lastLoadMs = System.currentTimeMillis()
        invalidated = false
        return result
    }

    fun invalidate() { invalidated = true }

    fun launch(context: Context, packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    /** Halaman Info Aplikasi di Pengaturan — tempat tombol Nonaktifkan/Aktifkan berada,
     *  saklar yang sama persis dengan yang dipakai XOS Freezer. */
    fun openAppInfo(context: Context, packageName: String) {
        val i = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}
