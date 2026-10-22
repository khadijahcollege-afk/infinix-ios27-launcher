package com.tedo.ios27launcher.ui

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.tedo.ios27launcher.IosMeshWallpaper
import com.tedo.ios27launcher.badges.BadgeStore
import com.tedo.ios27launcher.data.AppInfo

/** Latar bersama layar sekunder: mesh iOS + lapisan gelap tipis (menyatu dengan home). */
@Composable
fun SecondaryBg(content: @Composable BoxScope.() -> Unit) {
    // v0.13: background (wallpaper/mesh + lapisan gelap) kini digambar LauncherRoot secara
    // full-screen sampai ke belakang status bar; layar sekunder cukup transparan di atasnya.
    Box(Modifier.fillMaxSize()) { content() }
}

/** "Search or Ask" ala iOS 27 (dengan Siri AI): gelembung kaca hitam berisi pencarian app. */
@Composable
fun SearchScreen(apps: List<AppInfo>, mode: IconMode, tint: Color, onBack: () -> Unit, onLaunch: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = apps.filter { it.label.contains(q, ignoreCase = true) }
    SecondaryBg {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Text("‹ Kembali", color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(0.20f), RoundedCornerShape(28.dp)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SearchGlyph(16, Color.White.copy(0.9f))
                    Spacer(Modifier.width(8.dp))
                    Text("Search or Ask", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = q, onValueChange = { q = it },
                    placeholder = { Text("Cari aplikasi…", color = Color.White.copy(0.55f)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(0.12f), unfocusedContainerColor = Color.White.copy(0.12f),
                        focusedBorderColor = Color.White.copy(0.3f), unfocusedBorderColor = Color.White.copy(0.2f)))
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(shown) { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppGlyph(app, 56, mode, tint) { onLaunch(app.packageName) }
                        Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** App Library ala iOS: semua app dikelompokkan per huruf awal. */
@Composable
fun LibraryScreen(apps: List<AppInfo>, mode: IconMode, tint: Color, onBack: () -> Unit, onLaunch: (String) -> Unit) {
    val groups = apps.groupBy { it.label.firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap()
    SecondaryBg {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Text("‹ Kembali", color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.height(8.dp))
            Text("App Library", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.weight(1f)) {
                groups.forEach { (letter, group) ->
                    item { Text(letter.toString(), color = Color.White.copy(0.7f), fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 6.dp)) }
                    items(group.chunked(4)) { rowApps ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            rowApps.forEach { app ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                                    AppGlyph(app, 56, mode, tint) { onLaunch(app.packageName) }
                                    Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

/**
 * Control Center kaca ala iOS 27 + pusat "Appearance" launcher:
 * mode ikon 4 resmi (Default/Dark/Clear/Satu Warna), Liquid Glass, status bar gaya iOS,
 * Dynamic Island, lencana notifikasi, dan pintasan Pengaturan (Suara/Wallpaper/Baterai/Aksesibilitas).
 */
@Composable
fun ControlCenterScreen(
    glass: Float, onGlassChange: (Float) -> Unit,
    mode: IconMode, onModeChange: (IconMode) -> Unit,
    tint: Color, onTintChange: (Color) -> Unit,
    showLabels: Boolean, onLabelsChange: (Boolean) -> Unit,
    wpSystem: Boolean, onWpChange: (Boolean) -> Unit,
    fakeBar: Boolean, onFakeBarChange: (Boolean) -> Unit,
    islandMode: String, onIslandModeChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var torch by remember { mutableStateOf(false) }
    fun openSettings(action: String) {
        try { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (e: Exception) {
            try { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
        }
    }
    fun toggleTorch(on: Boolean) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
            if (id != null) { cm.setTorchMode(id, on); torch = on }
        } catch (e: Exception) { }
    }
    SecondaryBg {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Control Center", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(0.18f))
                    .clickable { onBack() }.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text("Selesai", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(0.14f + glass * 0.25f))
                        .border(1.dp, Color.White.copy(0.22f), RoundedCornerShape(26.dp)).padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        CcButton(CcKind.PLANE, "Pesawat", tint) { openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS) }
                        CcButton(CcKind.CELL, "Data", tint) { openSettings(Settings.ACTION_DATA_ROAMING_SETTINGS) }
                        CcButton(CcKind.WIFI, "Wi-Fi", tint) { openSettings(Settings.ACTION_WIFI_SETTINGS) }
                        CcButton(CcKind.BT, "Bluetooth", tint) { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CcCard(Modifier.weight(1f), glass) {
                            CcButton(CcKind.TORCH, "Senter", tint, active = torch) { toggleTorch(!torch) }
                        }
                        CcCard(Modifier.weight(1f), glass) {
                            CcButton(CcKind.MOON, "Jangan Ganggu", tint) { openSettings("android.settings.ZEN_MODE_SETTINGS") }
                        }
                        CcCard(Modifier.weight(1f), glass) {
                            CcButton(CcKind.ROTLOCK, "Kunci Rotasi", tint) { openSettings(Settings.ACTION_DISPLAY_SETTINGS) }
                        }
                        CcCard(Modifier.weight(1f), glass) {
                            CcButton(CcKind.SUN, "Terang", tint) { openSettings(Settings.ACTION_DISPLAY_SETTINGS) }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CcCard(Modifier.weight(1f), glass) {
                            CcButton(CcKind.SOUND, "Suara", tint) { openSettings(Settings.ACTION_SOUND_SETTINGS) }
                        }
                        CcCard(Modifier.weight(1f), glass) {
                            CcButton(CcKind.PIC, "Wallpaper", tint) {
                                try { context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                catch (e: Exception) { openSettings(Settings.ACTION_DISPLAY_SETTINGS) }
                            }
                        }
                        CcCard(Modifier.weight(1f), glass) {
                            CcButton(CcKind.BATT, "Baterai", tint) { openSettings("android.intent.action.POWER_USAGE_SUMMARY") }
                        }
                        CcCard(Modifier.weight(1f), glass) {
                            CcButton(CcKind.A11Y, "Aksesibilitas", tint) { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
                        }
                    }
                }
                item {
                    GlassPanel(glass) {
                        Text("Liquid Glass — iOS 27", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Tingkat kaca Dock, widget & panel (di iOS 27: Settings › Appearance).",
                            color = Color.White.copy(0.8f), fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ultra Clear", color = Color.White.copy(0.85f), fontSize = 12.sp)
                            Text("Fully Tinted", color = Color.White.copy(0.85f), fontSize = 12.sp)
                        }
                        Slider(value = glass, onValueChange = onGlassChange,
                            colors = SliderDefaults.colors(thumbColor = Color.White,
                                activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.35f)))
                    }
                }
                item {
                    GlassPanel(glass) {
                        Text("Ikon Aplikasi — 4 Mode iOS 27", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Sama seperti Edit › Customize di iOS: Default berwarna, Dark latar gelap, " +
                             "Clear kaca transparan glyph putih, Satu Warna (Tinted).",
                            color = Color.White.copy(0.8f), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CcPill("Default", mode == IconMode.DEFAULT) { onModeChange(IconMode.DEFAULT) }
                            CcPill("Dark", mode == IconMode.DARK) { onModeChange(IconMode.DARK) }
                            CcPill("Clear", mode == IconMode.CLEAR) { onModeChange(IconMode.CLEAR) }
                            CcPill("Satu Warna", mode == IconMode.TINTED) { onModeChange(IconMode.TINTED) }
                        }
                        if (mode == IconMode.TINTED) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                TINT_PRESETS.forEach { (c, name) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.size(34.dp).clip(CircleShape).background(c)
                                            .border(if (c == tint) 2.5.dp else 0.dp, Color.White, CircleShape)
                                            .clickable { onTintChange(c) })
                                        Text(name, color = Color.White.copy(0.85f), fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    GlassPanel(glass) {
                        Text("Tampilan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Nama aplikasi di bawah ikon", color = Color.White.copy(0.85f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CcPill("Tampil", showLabels) { onLabelsChange(true) }
                            CcPill("Sembunyi", !showLabels) { onLabelsChange(false) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Wallpaper layar utama", color = Color.White.copy(0.85f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CcPill("Wallpaper HP", wpSystem) { onWpChange(true) }
                            CcPill("Mesh iOS", !wpSystem) { onWpChange(false) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Status bar atas", color = Color.White.copy(0.85f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CcPill("Gaya iOS", fakeBar) { onFakeBarChange(true) }
                            CcPill("Bawaan XOS", !fakeBar) { onFakeBarChange(false) }
                        }
                        Text("Gaya iOS: bilah XOS disembunyikan, launcher menggambar jam, sinyal & baterai sendiri. " +
                             "Geser dari kiri atas = Notifikasi, kanan atas = Control Center (ala iOS 27).",
                            color = Color.White.copy(0.7f), fontSize = 10.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Dynamic Island (kini melayang di bawah status bar — tidak menutupi kamera)",
                            color = Color.White.copy(0.85f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CcPill("Popup", islandMode == "POPUP") { onIslandModeChange("POPUP") }
                            CcPill("Selalu", islandMode == "ALWAYS") { onIslandModeChange("ALWAYS") }
                            CcPill("Mati", islandMode == "OFF") { onIslandModeChange("OFF") }
                        }
                        Text("Popup = muncul sebentar saat ada notifikasi baru, lalu hilang. Selalu = pil ramping selalu tampil.",
                            color = Color.White.copy(0.7f), fontSize = 10.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Lencana Notifikasi (angka merah di ikon, seperti iOS)",
                            color = Color.White.copy(0.85f), fontSize = 12.sp)
                        val badgeOn = remember { BadgeStore.isEnabled(context) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            CcPill(if (badgeOn) "Sudah Aktif ✓" else "Aktifkan…", badgeOn) {
                                try { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
                            }
                            if (!badgeOn) Text("Cari \"iOS 27 Launcher\" lalu izinkan",
                                color = Color.White.copy(0.7f), fontSize = 10.sp)
                        }
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                        .background(Color.White).clickable { onOpenSettings() }
                        .padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                        Text("Pengaturan Launcher ›", color = Color(0xFF1C1C1E),
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                item {
                    Text("Catatan: kunci layar, Siri AI & isi aplikasi adalah wilayah sistem iOS — " +
                         "di Android itu wilayah XOS/app masing-masing, tidak bisa digantikan launcher.",
                        color = Color.White.copy(0.65f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun GlassPanel(glass: Float, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Color.White.copy(0.14f + glass * 0.25f))
        .border(1.dp, Color.White.copy(0.22f), RoundedCornerShape(26.dp)).padding(16.dp),
        content = content)
}

@Composable
private fun CcPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50))
        .background(if (selected) Color.White else Color.White.copy(0.16f))
        .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 7.dp)) {
        Text(label, color = if (selected) Color(0xFF1C1C1E) else Color.White,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CcCard(modifier: Modifier = Modifier, glass: Float, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(RoundedCornerShape(26.dp))
        .background(Color.White.copy(0.14f + glass * 0.25f))
        .border(1.dp, Color.White.copy(0.22f), RoundedCornerShape(26.dp))
        .padding(vertical = 10.dp), contentAlignment = Alignment.Center, content = content)
}

@Composable
private fun CcButton(kind: CcKind, label: String, color: Color, active: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(66.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(color)
            .then(if (active) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier)
            .clickable { onClick() },
            contentAlignment = Alignment.Center) {
            CcGlyph(kind, 25)
        }
        Text(label, color = Color.White.copy(0.9f), fontSize = 10.sp, maxLines = 2,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}
