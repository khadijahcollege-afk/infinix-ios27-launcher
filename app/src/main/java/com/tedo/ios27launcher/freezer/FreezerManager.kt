package com.tedo.ios27launcher.freezer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.tedo.ios27launcher.data.AppRepository

/**
 * FREEZER v0.2 — TANPA Shizuku (permintaan Tedo), meniru cara XOS Freezer yang asli.
 *
 * Hasil riset cara kerja XOS Freezer:
 * Freezer bawaan Infinix sebenarnya hanyalah fitur "nonaktifkan aplikasi" bawaan Android.
 * Bedanya, XOS Launcher/XManager adalah app SISTEM bertanda tangan Transsion, jadi mereka
 * boleh menekan saklar disable itu sendiri. App pihak ketiga tidak diberi hak itu oleh Android
 * (semua app freezer di Play Store pun butuh root / device-owner / Shizuku untuk itu).
 *
 * Jadi launcher ini memakai 2 jalan resmi tanpa app tambahan apa pun:
 * 1. Pintasan ke Freezer BAWAAN Infinix — Freezer aslinya tetap ada di sistem HP walau
 *    launcher default diganti. Tombolnya membuka XManager / XOS Launcher tempat Freezer tinggal.
 * 2. Freezer internal launcher ini: app disembunyikan dari home + 1 ketuk membuka halaman
 *    Info Aplikasi, tempat user menekan "Nonaktifkan" — saklar sistem yang sama persis.
 *    Status beku dibaca dari sistem (ApplicationInfo.enabled), jadi app yang dibekukan
 *    oleh XOS Freezer pun otomatis muncul di daftar beku launcher ini, dan sebaliknya.
 */
class FreezerManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("freezer", Context.MODE_PRIVATE)

    /** App yang disembunyikan manual lewat launcher ini (belum tentu dinonaktifkan sistem). */
    fun hiddenPackages(): Set<String> = prefs.getStringSet("hidden", emptySet()) ?: emptySet()

    fun hide(pkg: String) =
        prefs.edit().putStringSet("hidden", hiddenPackages() + pkg).apply()

    fun unhide(pkg: String) =
        prefs.edit().putStringSet("hidden", hiddenPackages() - pkg).apply()

    /** Bekukan: sembunyikan dari home, lalu antar user ke saklar Nonaktifkan milik sistem. */
    fun freeze(pkg: String) {
        hide(pkg)
        Toast.makeText(context,
            "Terakhir: tekan tombol Nonaktifkan di halaman ini agar beku beneran seperti XOS",
            Toast.LENGTH_LONG).show()
        AppRepository.openAppInfo(context, pkg)
    }

    /** Cairkan: tampilkan lagi; bila app masih dinonaktifkan sistem, antar ke halaman
     *  Info Aplikasi untuk menekan Aktifkan. */
    fun unfreeze(appEnabled: Boolean, pkg: String) {
        unhide(pkg)
        if (!appEnabled) {
            Toast.makeText(context,
                "Tekan tombol Aktifkan di halaman ini untuk mencairkan",
                Toast.LENGTH_LONG).show()
            AppRepository.openAppInfo(context, pkg)
        }
    }

    /** Buka Freezer bawaan Infinix (di dalam XManager / XOS Launcher). */
    fun openBuiltinFreezer(): Boolean {
        val pm = context.packageManager
        // Kandidat activity Freezer di beberapa versi XOS (dicoba satu-satu, aman bila gagal).
        val components = listOf(
            "com.transsion.XOSLauncher" to "com.transsion.XOSLauncher.freezer.FreezerActivity",
            "com.transsion.XOSLauncher" to "com.transsion.launcher3.freezer.FreezerActivity",
            "com.transsion.xmanager" to "com.transsion.xmanager.appmanager.FreezerActivity"
        )
        for ((pkg, cls) in components) {
            try {
                context.startActivity(Intent(Intent.ACTION_MAIN)
                    .setComponent(ComponentName(pkg, cls))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (e: Exception) { /* coba kandidat berikutnya */ }
        }
        // Fallback: buka app sistem tempat Freezer tinggal.
        for (pkg in listOf("com.transsion.xmanager", "com.transsion.phonemaster", "com.transsion.XOSLauncher")) {
            try {
                pm.getLaunchIntentForPackage(pkg)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                    Toast.makeText(context, "Cari ikon Freezer di dalamnya ya", Toast.LENGTH_SHORT).show()
                    return true
                }
            } catch (e: Exception) { /* lanjut */ }
        }
        Toast.makeText(context, "Freezer bawaan tidak ditemukan di HP ini", Toast.LENGTH_SHORT).show()
        return false
    }
}
