package com.tedo.ios27launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.tedo.ios27launcher.data.AppInfo
import com.tedo.ios27launcher.freezer.FreezerManager

/**
 * Freezer v0.11 — desain ulang gaya iOS: header ikon biru es, lalu daftar grup ala Settings iOS
 * (kartu membulat berisi baris-baris app dengan garis pemisah tipis).
 *
 * Dua jalur tetap gamblang:
 * A) FREEZER BAWAAN INFINIX — baris pertama; di sinilah app beku XOS milik user tersimpan
 *    (daftar privat sistem, tidak bisa ditampilkan launcher lain). Ketuk untuk membukanya.
 * B) DIBEKUKAN DARI LAUNCHER INI — app yang dibekukan lewat launcher ini, dengan tombol
 *    "Cairkan". Di bawahnya daftar semua app aktif dengan tombol "Bekukan" (menyembunyikan
 *    app lalu membuka Info Aplikasi untuk menekan "Nonaktifkan" — saklar sistem yang sama
 *    dengan yang dipakai XOS).
 */
@Composable
fun FreezerScreen(
    frozenApps: List<AppInfo>, allApps: List<AppInfo>, freezer: FreezerManager,
    glass: Float, mode: IconMode, tint: Color, onBack: () -> Unit, onChanged: () -> Unit
) {
    val hidden = freezer.hiddenPackages()
    val pickList = allApps.filter { it.enabled && it.packageName !in hidden }
    val card = Color.White.copy(alpha = 0.11f + glass * 0.20f)
    val cardBorder = Color.White.copy(alpha = 0.16f)
    val hair = Color.White.copy(alpha = 0.09f)

    SecondaryBg {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Text("‹ Kembali", color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.height(14.dp))
            // Header ala iOS: ikon biru es + judul + ringkasan
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF2F80ED)))),
                    contentAlignment = Alignment.Center) { SnowGlyph(30) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Freezer", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("${frozenApps.size} app dibekukan dari launcher ini",
                        color = Color.White.copy(0.75f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(18.dp))

            LazyColumn(Modifier.weight(1f)) {
                // A) Freezer bawaan
                item { FzSection("FREEZER BAWAAN INFINIX") }
                item {
                    FzCard(card, cardBorder) {
                        Row(Modifier.fillMaxWidth().clickable { freezer.openBuiltinFreezer() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF2F80ED)))),
                                contentAlignment = Alignment.Center) { SnowGlyph(21) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Freezer Bawaan Infinix", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("App beku XOS-mu ada di sini — daftarnya privat di sistem, jadi dibuka di tempat aslinya.",
                                    color = Color.White.copy(0.7f), fontSize = 11.sp)
                            }
                            Text("›", color = Color.White.copy(0.55f), fontSize = 22.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }

                // B) Beku dari launcher ini
                item { FzSection("DIBEKUKAN DARI LAUNCHER INI (${frozenApps.size})") }
                item {
                    FzCard(card, cardBorder) {
                        if (frozenApps.isEmpty()) {
                            Text("Belum ada. Bekukan app pertamamu dari daftar di bawah.",
                                color = Color.White.copy(0.8f), fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp))
                        } else {
                            frozenApps.forEachIndexed { i, app ->
                                if (i > 0) Box(Modifier.fillMaxWidth().height(0.7.dp).background(hair))
                                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    AppGlyph(app, 40, mode, tint) { }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(app.label, color = Color.White, fontSize = 14.sp, maxLines = 1)
                                        Text(if (app.enabled) "Tersembunyi dari home" else "Nonaktif (beku)",
                                            color = Color(0xFF7CC4FF), fontSize = 10.5.sp)
                                    }
                                    FzPill("Cairkan", Color.White, Color(0xFF1C1C1E)) {
                                        freezer.unfreeze(app.enabled, app.packageName); onChanged()
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }

                // C) Bekukan app lain
                item { FzSection("BEKUKAN APLIKASI") }
                item {
                    Text("Ketuk \"Bekukan\": app disembunyikan, lalu kamu diarahkan menekan tombol " +
                         "\"Nonaktifkan\" di Info Aplikasi (saklar yang sama dengan Freezer XOS).",
                        color = Color.White.copy(0.65f), fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                }
                item {
                    FzCard(card, cardBorder) {
                        Column {
                            pickList.forEachIndexed { i, app ->
                                if (i > 0) Box(Modifier.fillMaxWidth().height(0.7.dp).background(hair))
                                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    AppGlyph(app, 40, mode, tint) { }
                                    Spacer(Modifier.width(12.dp))
                                    Text(app.label, color = Color.White, fontSize = 14.sp, maxLines = 1,
                                        modifier = Modifier.weight(1f))
                                    FzPill("Bekukan", Color(0xFF2E7FD9), Color.White) {
                                        freezer.freeze(app.packageName); onChanged()
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}

@Composable
private fun FzSection(t: String) {
    Text(t, color = Color.White.copy(0.6f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
}

@Composable
private fun FzCard(bg: Color, border: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg)
        .border(1.dp, border, RoundedCornerShape(20.dp)), content = content)
}

@Composable
private fun FzPill(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).clickable { onClick() }
        .padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
