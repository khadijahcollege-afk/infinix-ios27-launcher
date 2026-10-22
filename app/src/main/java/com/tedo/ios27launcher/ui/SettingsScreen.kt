package com.tedo.ios27launcher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.tedo.ios27launcher.badges.BadgeStore

/**
 * Pengaturan Launcher — semua pengaturan launcher diatur dari sini (seperti Desktop Settings
 * di XOS / Settings di iOS): Tampilan, Notifikasi, Gestur, Freezer, dan Tentang.
 */
@Composable
fun SettingsScreen(
    glass: Float, onGlassChange: (Float) -> Unit,
    mode: IconMode, onModeChange: (IconMode) -> Unit,
    tint: Color, onTintChange: (Color) -> Unit,
    showLabels: Boolean, onLabelsChange: (Boolean) -> Unit,
    wpSystem: Boolean, onWpChange: (Boolean) -> Unit,
    fakeBar: Boolean, onFakeBarChange: (Boolean) -> Unit,
    islandMode: String, onIslandModeChange: (String) -> Unit,
    badgesOn: Boolean, onBadgesChange: (Boolean) -> Unit,
    notifPanel: String, onNotifPanelChange: (String) -> Unit,
    swipeDown: String, onSwipeDownChange: (String) -> Unit,
    gestures: Map<String, String>, onGestureChange: (String, String) -> Unit,
    onWallpaperChanged: () -> Unit,
    frozenCount: Int, onOpenFreezer: () -> Unit, onOpenNotifCenter: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listenerOn = BadgeStore.isEnabled(context)
    SecondaryBg {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Text("‹ Kembali", color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.height(8.dp))
            Text("Pengaturan Launcher", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                item { SectionTitle("LAUNCHER BAWAAN") }
                item {
                    SetPanel(glass) {
                        SetRow("Jadikan launcher bawaan (default)", "Supaya tombol Home selalu membuka launcher ini, bukan XOS.") {
                            try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
                        }
                    }
                }

                item { SectionTitle("TAMPILAN") }
                item {
                    SetPanel(glass) {
                        Text("Mode Ikon — iOS 27", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SetPill("Default", mode == IconMode.DEFAULT) { onModeChange(IconMode.DEFAULT) }
                            SetPill("Dark", mode == IconMode.DARK) { onModeChange(IconMode.DARK) }
                            SetPill("Clear", mode == IconMode.CLEAR) { onModeChange(IconMode.CLEAR) }
                            SetPill("Satu Warna", mode == IconMode.TINTED) { onModeChange(IconMode.TINTED) }
                        }
                        if (mode == IconMode.TINTED) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TINT_PRESETS.forEach { (c, _) ->
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(c)
                                        .border(if (c == tint) 2.5.dp else 0.dp, Color.White, CircleShape)
                                        .clickable { onTintChange(c) })
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Liquid Glass", color = Color.White.copy(0.9f), fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ultra Clear", color = Color.White.copy(0.7f), fontSize = 10.5.sp)
                            Text("Fully Tinted", color = Color.White.copy(0.7f), fontSize = 10.5.sp)
                        }
                        Slider(value = glass, onValueChange = onGlassChange,
                            colors = SliderDefaults.colors(thumbColor = Color.White,
                                activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.35f)))
                        SetToggleRow("Nama aplikasi di bawah ikon", showLabels, onLabelsChange)
                        SetToggleRow("Wallpaper HP (mati = mesh iOS)", wpSystem, onWpChange)
                        val pickWp = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()) { uri ->
                            if (uri != null) try {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    val bmp = android.graphics.BitmapFactory.decodeStream(input)
                                    if (bmp != null) {
                                        val sc = if (bmp.width > 1080) 1080f / bmp.width else 1f
                                        val outBmp = if (sc < 1f) android.graphics.Bitmap.createScaledBitmap(
                                            bmp, (bmp.width * sc).toInt(), (bmp.height * sc).toInt(), true) else bmp
                                        java.io.File(context.filesDir, "wallpaper.jpg").outputStream().use { os ->
                                            outBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, os) }
                                        onWallpaperChanged()
                                    }
                                }
                            } catch (e: Exception) { }
                        }
                        SetRow("Pilih wallpaper dari galeri",
                            "JALUR PASTI TAMPIL: pilih foto wallpaper-mu sendiri (mis. foto yang sekarang terpasang di XOS). " +
                            "Fotonya tersimpan di dalam launcher, jadi tidak bergantung cara XOS menyimpan wallpaper sistem.") {
                            pickWp.launch("image/*")
                        }
                        SetRow("Hapus wallpaper galeri", "Kembali memakai wallpaper sistem HP / mesh iOS.") {
                            try { java.io.File(context.filesDir, "wallpaper.jpg").delete() } catch (e: Exception) { }
                            onWallpaperChanged()
                        }
                        SetToggleRow("Status bar gaya iOS (gambar sendiri)", fakeBar, onFakeBarChange)
                        Text("Dynamic Island — melayang di bawah status bar (tidak menutupi kamera depan)",
                            color = Color.White.copy(0.92f), fontSize = 12.5.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SetPill("Popup", islandMode == "POPUP") { onIslandModeChange("POPUP") }
                            SetPill("Selalu", islandMode == "ALWAYS") { onIslandModeChange("ALWAYS") }
                            SetPill("Mati", islandMode == "OFF") { onIslandModeChange("OFF") }
                        }
                        Text("Popup: muncul ±4 detik saat notifikasi baru masuk (ikon app + judul + jumlah), lalu hilang sendiri.",
                            color = Color.White.copy(0.65f), fontSize = 10.5.sp)
                    }
                }

                item { SectionTitle("NOTIFIKASI") }
                item {
                    SetPanel(glass) {
                        Text("Panel yang dibuka gestur notifikasi", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SetPill("Launcher (iOS)", notifPanel == "LAUNCHER") { onNotifPanelChange("LAUNCHER") }
                            SetPill("Bawaan XOS", notifPanel == "XOS") { onNotifPanelChange("XOS") }
                        }
                        Text("Pilihan \"Launcher\" membuka Notification Center milik launcher ini — notifikasimu diatur di sini, " +
                             "bukan panel XOS. Catatan jujur: dari dalam aplikasi lain, geser dari tepi atas tetap membuka panel XOS " +
                             "karena itu wilayah sistem Android; tidak ada launcher yang bisa memblokirnya.",
                            color = Color.White.copy(0.7f), fontSize = 10.5.sp)
                        Spacer(Modifier.height(10.dp))
                        SetRow("Buka Notification Center", "Lihat, buka, dan hapus notifikasi dari launcher.") { onOpenNotifCenter() }
                        val canOverlay = Settings.canDrawOverlays(context)
                        SetRow(
                            if (canOverlay) "Dynamic Island di luar launcher: IZIN AKTIF ✓" else "Dynamic Island di luar launcher: BELUM ADA IZIN",
                            "Penting: supaya popup Island muncul juga di atas aplikasi lain (WhatsApp, game, browser). " +
                            "Android mewajibkan izin 'Tampilkan di atas aplikasi lain' — aktifkan sekali lewat baris ini, " +
                            "cari 'iOS 27 Launcher' lalu izinkan.") {
                            try { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            catch (e: Exception) { }
                        }
                        SetToggleRow("Lencana angka di ikon", badgesOn, onBadgesChange)
                        Spacer(Modifier.height(6.dp))
                        SetRow("Matikan popup banner bawaan XOS",
                            "Launcher tidak bisa mematikan popup sistem secara langsung (wilayah Android). " +
                            "Tombol ini membuka Pengaturan Notifikasi Android — di sana matikan banner/pop-up per aplikasi; " +
                            "setelah itu Dynamic Island launcher ini yang jadi penanda notifikasimu.") {
                            try { context.startActivity(Intent("android.settings.NOTIFICATION_SETTINGS")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
                        }
                        Spacer(Modifier.height(6.dp))
                        SetRow(
                            if (listenerOn) "Akses Notifikasi: AKTIF ✓" else "Akses Notifikasi: BELUM AKTIF",
                            "Wajib aktif untuk Notification Center, lencana, dan Dynamic Island."
                        ) {
                            try { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
                        }
                    }
                }

                item { SectionTitle("GESTUR") }
                item {
                    SetPanel(glass) {
                        Text("Atur sendiri tiap gestur — gaya pengaturan launcher referensimu, disesuaikan untuk iOS 27.",
                            color = Color.White.copy(0.75f), fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        GestureRowSet("Geser ke atas (1 jari)", gestures["SWIPE_UP"] ?: "LIBRARY",
                            listOf("LIBRARY" to "App Library", "SEARCH" to "Search", "NONE" to "Tidak ada")) { onGestureChange("SWIPE_UP", it) }
                        GestureRowSet("Geser ke bawah (1 jari)", swipeDown,
                            listOf("NOTIF" to "Notifikasi", "SEARCH" to "Search", "XOS" to "Panel XOS", "NONE" to "Tidak ada")) { onSwipeDownChange(it) }
                        GestureRowSet("Geser ke atas (2 jari)", gestures["TWO_UP"] ?: "FREEZER",
                            listOf("FREEZER" to "Freezer", "LIBRARY" to "App Library", "NONE" to "Tidak ada")) { onGestureChange("TWO_UP", it) }
                        GestureRowSet("Geser ke bawah (2 jari)", gestures["TWO_DOWN"] ?: "VOICE",
                            listOf("VOICE" to "Pencarian Suara", "SEARCH" to "Search", "NONE" to "Tidak ada")) { onGestureChange("TWO_DOWN", it) }
                        GestureRowSet("Cubit masuk (2 jari)", gestures["PINCH_IN"] ?: "EDIT",
                            listOf("EDIT" to "Mode Edit", "SETTINGS" to "Pengaturan", "NONE" to "Tidak ada")) { onGestureChange("PINCH_IN", it) }
                        GestureRowSet("Cubit keluar (2 jari)", gestures["PINCH_OUT"] ?: "NONE",
                            listOf("NONE" to "Tidak ada", "LIBRARY" to "App Library", "SETTINGS" to "Pengaturan")) { onGestureChange("PINCH_OUT", it) }
                        GestureRowSet("Putar searah jarum jam (2 jari)", gestures["ROT_CW"] ?: "NONE",
                            listOf("NONE" to "Tidak ada", "CONTROL" to "Control Center", "FREEZER" to "Freezer")) { onGestureChange("ROT_CW", it) }
                        GestureRowSet("Putar berlawanan jarum jam (2 jari)", gestures["ROT_CCW"] ?: "NONE",
                            listOf("NONE" to "Tidak ada", "CONTROL" to "Control Center", "FREEZER" to "Freezer")) { onGestureChange("ROT_CCW", it) }
                        Spacer(Modifier.height(4.dp))
                        Text("Tetap (gaya iOS): tepi kiri atas status bar = Notifikasi • tepi kanan atas = Control Center • " +
                             "geser melewati halaman terakhir = App Library • tekan-tahan ikon = jiggle + Customize.",
                            color = Color.White.copy(0.7f), fontSize = 10.5.sp)
                    }
                }

                item { SectionTitle("FREEZER") }
                item {
                    SetPanel(glass) {
                        SetRow("Buka Freezer", "$frozenCount app dibekukan dari launcher ini + jalan pintas Freezer bawaan Infinix.") { onOpenFreezer() }
                    }
                }

                item { SectionTitle("TENTANG") }
                item {
                    SetPanel(glass) {
                        Text("iOS 27 Launcher untuk Infinix Note 12", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Versi 0.14.0 • com.tedo.ios27launcher\nDibuat khusus untuk Tedo. Tanpa Shizuku, tanpa root.",
                            color = Color.White.copy(0.75f), fontSize = 11.sp)
                    }
                }
                item { Spacer(Modifier.height(6.dp)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = Color.White.copy(0.6f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun SetPanel(glass: Float, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Color.White.copy(0.13f + glass * 0.22f))
        .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(24.dp)).padding(16.dp),
        content = content)
}

@Composable
private fun SetPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50))
        .background(if (selected) Color.White else Color.White.copy(0.16f))
        .clickable { onClick() }.padding(horizontal = 13.dp, vertical = 7.dp)) {
        Text(label, color = if (selected) Color(0xFF1C1C1E) else Color.White,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SetRow(title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = Color.White.copy(0.65f), fontSize = 10.5.sp)
        }
        Text("›", color = Color.White.copy(0.6f), fontSize = 20.sp)
    }
}

@Composable
private fun SetToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(0.92f), fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SetPill("Aktif", value) { onChange(true) }
            SetPill("Mati", !value) { onChange(false) }
        }
    }
}

/** Baris pengaturan satu gestur: nama + pilihan aksinya (gaya daftar pengaturan referensi). */
@Composable
fun GestureRowSet(title: String, current: String,
                  options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        Text(options.firstOrNull { it.first == current }?.second ?: current,
            color = Color.White.copy(0.65f), fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())) {
            options.forEach { (value, label) -> SetPill(label, current == value) { onPick(value) } }
        }
    }
}
