package com.tedo.ios27launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.tedo.ios27launcher.badges.BadgeStore
import com.tedo.ios27launcher.badges.NotifItem
import com.tedo.ios27launcher.data.AppInfo
import com.tedo.ios27launcher.data.AppRepository

/** Waktu relatif ringkas ala iOS: "baru saja", "3 mnt", "1 jam", "2 hr". */
fun relTime(t: Long): String {
    val d = (System.currentTimeMillis() - t).coerceAtLeast(0)
    val m = d / 60_000
    return when {
        m < 1 -> "baru saja"
        m < 60 -> "$m mnt"
        m < 60 * 24 -> "${m / 60} jam"
        else -> "${m / (60 * 24)} hr"
    }
}

/**
 * Notification Center milik launcher sendiri (gaya iOS): membaca notifikasi asli lewat
 * Akses Notifikasi, mengelompokkan per aplikasi, bisa membuka app dan menghapus notifikasi
 * langsung dari sini — panel bawaan XOS tidak perlu dibuka lagi dari layar utama.
 */
@Composable
fun NotificationCenterScreen(apps: List<AppInfo>, glass: Float, mode: IconMode, tint: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    val items = BadgeStore.notifications
    val enabled = remember { BadgeStore.isEnabled(context) }
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val groups = remember(items) { items.groupBy { it.pkg } }

    SecondaryBg {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("‹ Kembali", color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { onBack() })
                if (items.any { !it.ongoing }) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(0.18f))
                        .clickable { BadgeStore.dismissAll() }.padding(horizontal = 13.dp, vertical = 6.dp)) {
                        Text("Hapus Semua", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Notification Center", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))

            if (!enabled) {
                NcPanel(glass) {
                    Text("Izin Akses Notifikasi belum aktif", color = Color.White,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Supaya launcher bisa menampilkan & mengatur notifikasi di sini (dan lencana angka di ikon), " +
                         "aktifkan izinnya sekali: ketuk tombol di bawah, cari \"iOS 27 Launcher\", lalu izinkan.",
                        color = Color.White.copy(0.85f), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White)
                        .clickable {
                            try { context.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
                        }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Aktifkan Izin", color = Color(0xFF1C1C1E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (items.isEmpty()) {
                NcPanel(glass) {
                    Text("Tidak ada notifikasi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Semua bersih. Notifikasi baru akan muncul di sini.",
                        color = Color.White.copy(0.8f), fontSize = 12.sp)
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    groups.forEach { (pkg, list) ->
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val app = byPkg[pkg]
                                if (app != null) AppGlyph(app, 20, mode, tint) { }
                                Spacer(Modifier.width(7.dp))
                                Text(app?.label ?: pkg, color = Color.White.copy(0.75f), fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                        items(list, key = { it.key }) { n -> NotifCard(n, glass, pkg, byPkg) }
                    }
                    item {
                        Text("Notifikasi bertanda • sedang berjalan (musik/panggilan) tidak bisa dihapus dari sini.",
                            color = Color.White.copy(0.55f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifCard(n: NotifItem, glass: Float, pkg: String, byPkg: Map<String, AppInfo>) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(0.13f + glass * 0.22f))
        .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(20.dp))
        .clickable {
            // Audit v0.12: buka NOTIFIKASINYA langsung bila intent aslinya ada.
            val pi = BadgeStore.intents[n.key]
            val opened = try { pi?.send(); pi != null } catch (e: Exception) { false }
            if (!opened) AppRepository.launch(context, pkg)
        }.padding(12.dp)) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(n.title.ifBlank { byPkg[pkg]?.label ?: pkg }, color = Color.White,
                    fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    modifier = Modifier.weight(1f))
                Text(relTime(n.time) + if (n.ongoing) " •" else "", color = Color.White.copy(0.6f), fontSize = 10.5.sp)
            }
            if (n.text.isNotBlank()) Text(n.text, color = Color.White.copy(0.88f), fontSize = 12.5.sp, maxLines = 3)
        }
        if (!n.ongoing) {
            Box(Modifier.padding(start = 8.dp).size(22.dp).clip(CircleShape)
                .background(Color.White.copy(0.18f)).clickable { BadgeStore.dismiss(n.key) },
                contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun NcPanel(glass: Float, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Color.White.copy(0.13f + glass * 0.22f))
        .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(24.dp)).padding(16.dp),
        content = content)
}
