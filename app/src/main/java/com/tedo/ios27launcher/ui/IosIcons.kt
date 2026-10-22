package com.tedo.ios27launcher.ui

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tedo.ios27launcher.data.AppInfo
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/** Jenis ikon gaya iOS yang digambar sendiri (bukan ikon bawaan app Android). */
enum class IosKind { PHONE, MESSAGES, SAFARI, MAIL, PHOTOS, CAMERA, CLOCK, CALENDAR, MAPS, SETTINGS, CALC, MUSIC, NOTES, WEATHER,
    WHATSAPP, INSTAGRAM, FACEBOOK, YOUTUBE, TELEGRAM, TIKTOK, APPSTORE, COMPASS, FILES,
    FACETIME, WALLET, HEALTH, REMINDERS, PODCASTS,
    DISCORD, SPOTIFY, XAPP, NETFLIX, SHOPEE, BCA }

/** Petakan app -> ikon iOS berdasarkan package & label (Indonesia/Inggris). */
fun iosKindFor(app: AppInfo): IosKind? {
    val p = app.packageName.lowercase(); val l = app.label.lowercase()
    return when {
        "tachyon" in p || "meetings" in p || l == "meet" || l == "duo" || "facetime" in l -> IosKind.FACETIME
        "wallet" in p || "dompet" in l || l == "ovo" || l == "dana" || l == "gopay" || p == "id.dana" || "ovo" in p -> IosKind.WALLET
        "health" in p || "kesehatan" in l || "fitness" in p || l == "sehat" -> IosKind.HEALTH
        "reminder" in p || "pengingat" in l || "reminder" in l || "todo" in p || l == "tasks" || "tasks" in p -> IosKind.REMINDERS
        "podcast" in p || "podcast" in l -> IosKind.PODCASTS
        "discord" in p -> IosKind.DISCORD
        "spotify" in p -> IosKind.SPOTIFY
        p == "com.twitter.android" || p == "com.x.android" || l == "x" -> IosKind.XAPP
        "netflix" in p -> IosKind.NETFLIX
        "shopee" in p -> IosKind.SHOPEE
        "mybca" in p || "bca" in p -> IosKind.BCA
        "whatsapp" in p -> IosKind.WHATSAPP
        "instagram" in p -> IosKind.INSTAGRAM
        "facebook" in p || "katana" in p || l == "facebook" || l == "lite" && "facebook" in p -> IosKind.FACEBOOK
        "youtube" in p || "ytmusic" in p -> IosKind.YOUTUBE
        "telegram" in p -> IosKind.TELEGRAM
        "tiktok" in p || "musically" in p || "zhiliaoapp" in p -> IosKind.TIKTOK
        "spotify" in p -> IosKind.MUSIC
        "messenger" in p -> IosKind.MESSAGES
        "vending" in p || l == "play store" || l == "google play" -> IosKind.APPSTORE
        "dialer" in p || "telepon" in l || l == "phone" || "incallui" in p -> IosKind.PHONE
        "messaging" in p || "mms" in p || "pesan" in l || l == "messages" || l == "messaging" -> IosKind.MESSAGES
        p == "com.android.chrome" || "browser" in p || l == "safari" -> IosKind.SAFARI
        "gmail" in p || "android.gm" in p || "gmail" in l || "email" in p || l == "mail" -> IosKind.MAIL
        "gallery" in p || "photos" in p || "galeri" in l || "foto" in l -> IosKind.PHOTOS
        "camera" in p || "kamera" in l -> IosKind.CAMERA
        "compass" in p || "kompas" in l || l == "compass" -> IosKind.COMPASS
        "clock" in p || "deskclock" in p || l == "jam" || "alarm" in l || l == "clock" -> IosKind.CLOCK
        "calendar" in p || "kalender" in l -> IosKind.CALENDAR
        "maps" in p || l == "maps" || "peta" in l -> IosKind.MAPS
        p == "com.android.settings" || "pengaturan" in l || l == "settings" -> IosKind.SETTINGS
        "calculator" in p || "kalkulator" in l -> IosKind.CALC
        "filemanager" in p || "file manager" in l || "documentsui" in p || l == "file saya" -> IosKind.FILES
        "music" in p || "musik" in l -> IosKind.MUSIC
        "notes" in p || "catatan" in l || "note" in l -> IosKind.NOTES
        "weather" in p || "cuaca" in l -> IosKind.WEATHER
        else -> null
    }
}

/** Empat mode tampilan ikon resmi iOS 27 (Edit › Customize di Home Screen iOS). */
enum class IconMode { DEFAULT, DARK, CLEAR, TINTED }

val TINT_PRESETS: List<Pair<Color, String>> = listOf(
    Color(0xFF4A90D9) to "Biru", Color(0xFF8E6BD8) to "Ungu", Color(0xFF9AA0A6) to "Abu",
    Color(0xFF3FA46A) to "Hijau", Color(0xFFE77CA8) to "Pink")

/**
 * Efek mode ikon iOS 27 via GPU RenderEffect (nyaris tanpa biaya performa):
 * - DARK: karya seni sama, digelapkan (latar gelap ala mode Dark)
 * - CLEAR: siluet putih dari luminansi ikon di atas panel kaca transparan (panel digambar pemanggil)
 * - TINTED: satu warna vivid — latar jadi warna tint, bagian terang (glyph) jadi nyaris putih,
 *   sesuai format Tinted iOS 27 (bukan lagi hitam+glyph berwarna seperti iOS 18)
 */
fun Modifier.iconEffect(mode: IconMode, tint: Color): Modifier = composed {
    // Audit v0.12: efek dibuat SEKALI per (mode, warna) dan dipakai ulang — dulu dibuat
    // ulang pada setiap rekomposisi ikon (boros GPU/CPU saat menggeser halaman).
    val effect = remember(mode, tint) {
        if (Build.VERSION.SDK_INT < 31 || mode == IconMode.DEFAULT) null
        else buildIconEffect(mode, tint)
    }
    if (effect == null) this else this.graphicsLayer { renderEffect = effect }
}

/** Bangun RenderEffect untuk mode ikon (dipakai iconEffect; hasilnya di-cache remember). */
private fun buildIconEffect(mode: IconMode, tint: Color): RenderEffect {
    val r = android.graphics.Color.red(tint.toArgb()) / 255f
    val g = android.graphics.Color.green(tint.toArgb()) / 255f
    val b = android.graphics.Color.blue(tint.toArgb()) / 255f
    val arr = when (mode) {
        IconMode.DARK -> floatArrayOf(
            0.547f, 0.121f, 0.078f, 0f, -12f,
            0.055f, 0.593f, 0.043f, 0f, -12f,
            0.043f, 0.129f, 0.534f, 0f, -12f,
            0f, 0f, 0f, 1f, 0f)
        IconMode.CLEAR -> floatArrayOf(
            0f, 0f, 0f, 0f, 255f,
            0f, 0f, 0f, 0f, 255f,
            0f, 0f, 0f, 0f, 255f,
            0.096f, 0.322f, 0.032f, 0.35f, 0f)
        else -> { // TINTED vivid ala iOS 27
            val lo = floatArrayOf(r * 0.55f, g * 0.55f, b * 0.55f)
            val hi = floatArrayOf(r + (1f - r) * 0.88f, g + (1f - g) * 0.88f, b + (1f - b) * 0.88f)
            floatArrayOf(
                (hi[0] - lo[0]) * 0.299f, (hi[0] - lo[0]) * 0.587f, (hi[0] - lo[0]) * 0.114f, 0f, lo[0] * 255f,
                (hi[1] - lo[1]) * 0.299f, (hi[1] - lo[1]) * 0.587f, (hi[1] - lo[1]) * 0.114f, 0f, lo[1] * 255f,
                (hi[2] - lo[2]) * 0.299f, (hi[2] - lo[2]) * 0.587f, (hi[2] - lo[2]) * 0.114f, 0f, lo[2] * 255f,
                0f, 0f, 0f, 1f, 0f)
        }
    }
    val filter = android.graphics.ColorMatrixColorFilter(arr)
    return android.graphics.RenderEffect.createColorFilterEffect(filter).asComposeRenderEffect()
}

/** Ikon app seragam untuk semua layar: glyph iOS bila dikenal, ikon asli bila tidak. */
@Composable
fun AppGlyph(app: AppInfo, sizeDp: Int, mode: IconMode, tint: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape((sizeDp * 0.29f).dp)
    val pane = if (mode == IconMode.CLEAR)
        Modifier.background(Color.White.copy(0.13f)).border(1.dp, Color.White.copy(0.28f), shape)
    else Modifier
    val kind = iosKindFor(app)
    Box(Modifier.size(sizeDp.dp).clip(shape).then(pane).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Box(Modifier.iconEffect(mode, tint)) {
            if (kind != null) IosIcon(kind, sizeDp)
            else if (app.icon != null) Image(app.icon, null, Modifier.size(sizeDp.dp), contentScale = ContentScale.Crop)
            else Box(Modifier.size(sizeDp.dp).background(Color.White.copy(0.15f)),
                contentAlignment = Alignment.Center) { Text("◻", color = Color.White) }
        }
    }
}

private fun DrawScope.roundBg(colors: List<Color>, r: Float) =
    drawRoundRect(Brush.verticalGradient(colors), cornerRadius = CornerRadius(r, r), size = size)

/** Kaca pembesar satu warna untuk tombol Search (persis referensi: tanpa emoji warna-warni). */
@Composable
fun SearchGlyph(sizeDp: Int, color: Color) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val sw = size.minDimension * 0.13f
        drawCircle(color, size.minDimension * 0.30f,
            Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(sw))
        drawLine(color, Offset(size.width * 0.63f, size.height * 0.63f),
            Offset(size.width * 0.90f, size.height * 0.90f), sw, StrokeCap.Round)
    }
}

/** Ikon iOS digambar dengan Canvas: Telepon, Pesan, Safari, Mail, Foto, Kamera, Jam hidup,
 *  Kalender tanggal asli, WhatsApp, Instagram, YouTube, Telegram, TikTok, App Store, dll. */
@Composable
fun IosIcon(kind: IosKind, sizeDp: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp * 0.29f).dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val r = w * 0.29f
            val cx = w / 2; val cy = h / 2
            when (kind) {
                IosKind.PHONE -> { roundBg(listOf(Color(0xFF67D45C), Color(0xFF28B81F)), r)
                    drawArc(Color.White, -50f, 200f, false, Offset(w*0.18f, h*0.30f), Size(w*0.64f, w*0.44f), style = Stroke(w*0.16f))
                    drawCircle(Color.White, w*0.085f, Offset(w*0.235f, h*0.44f)); drawCircle(Color.White, w*0.085f, Offset(w*0.765f, h*0.44f)) }
                IosKind.MESSAGES -> { roundBg(listOf(Color(0xFF65D95E), Color(0xFF1EC91B)), r)
                    drawRoundRect(Color.White, Offset(w*0.16f, h*0.22f), Size(w*0.68f, h*0.46f), CornerRadius(w*0.22f, w*0.22f))
                    drawPath(Path().apply { moveTo(w*0.30f, h*0.64f); lineTo(w*0.30f, h*0.84f); lineTo(w*0.52f, h*0.66f); close() }, Color.White) }
                IosKind.WHATSAPP -> { roundBg(listOf(Color(0xFF5BD066), Color(0xFF1FA855)), r)
                    drawCircle(Color.White, w*0.335f, Offset(cx, cy - h*0.03f))
                    drawPath(Path().apply { moveTo(w*0.30f, h*0.60f); lineTo(w*0.24f, h*0.82f); lineTo(w*0.47f, h*0.68f); close() }, Color.White)
                    drawArc(Color(0xFF25B34B), -50f, 200f, false, Offset(w*0.33f, h*0.335f), Size(w*0.34f, w*0.235f), style = Stroke(w*0.085f))
                    drawCircle(Color(0xFF25B34B), w*0.045f, Offset(w*0.375f, h*0.415f)); drawCircle(Color(0xFF25B34B), w*0.045f, Offset(w*0.625f, h*0.415f)) }
                IosKind.INSTAGRAM -> { drawRoundRect(Brush.linearGradient(listOf(Color(0xFF7B3FE4), Color(0xFFE1306C), Color(0xFFF6A03C)),
                        Offset(0f, h), Offset(w, 0f)), cornerRadius = CornerRadius(r, r), size = size)
                    drawRoundRect(Color.White, Offset(w*0.20f, h*0.20f), Size(w*0.60f, h*0.60f), CornerRadius(w*0.17f, w*0.17f), style = Stroke(w*0.055f))
                    drawCircle(Color.White, w*0.155f, Offset(cx, cy), style = Stroke(w*0.055f))
                    drawCircle(Color.White, w*0.045f, Offset(w*0.685f, h*0.315f)) }
                IosKind.FACEBOOK -> { roundBg(listOf(Color(0xFF2E7CF6), Color(0xFF0D5FCC)), r)
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE; textSize = w * 0.98f
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                            textAlign = android.graphics.Paint.Align.CENTER }
                        drawText("f", cx + w*0.05f, cy + w*0.36f, paint) } }
                IosKind.YOUTUBE -> { roundBg(listOf(Color(0xFFFF3B30), Color(0xFFE01E14)), r)
                    drawRoundRect(Color.White, Offset(w*0.14f, h*0.24f), Size(w*0.72f, h*0.52f), CornerRadius(w*0.14f, w*0.14f))
                    drawPath(Path().apply { moveTo(w*0.43f, h*0.36f); lineTo(w*0.43f, h*0.64f); lineTo(w*0.63f, h*0.50f); close() }, Color(0xFFFF3B30)) }
                IosKind.TELEGRAM -> { roundBg(listOf(Color(0xFF4FB3E8), Color(0xFF1E96D6)), r)
                    drawPath(Path().apply { moveTo(w*0.18f, h*0.50f); lineTo(w*0.84f, h*0.22f); lineTo(w*0.66f, h*0.78f);
                        lineTo(w*0.50f, h*0.62f); close() }, Color.White)
                    drawLine(Color(0xFF1E96D6), Offset(w*0.50f, h*0.62f), Offset(w*0.78f, h*0.27f), w*0.03f) }
                IosKind.TIKTOK -> { roundBg(listOf(Color(0xFF202024), Color.Black), r)
                    listOf(Color(0xFF25F4EE) to -0.022f, Color(0xFFFE2C55) to 0.022f, Color.White to 0f).forEach { (c, o) ->
                        drawLine(c, Offset(w*(0.56f+o), h*0.20f), Offset(w*(0.56f+o), h*0.60f), w*0.075f)
                        drawLine(c, Offset(w*(0.56f+o), h*0.20f), Offset(w*(0.80f+o), h*0.30f), w*0.075f)
                        drawOval(c, Offset(w*(0.28f+o), h*0.56f), Size(w*0.30f, h*0.21f)) } }
                IosKind.APPSTORE -> { roundBg(listOf(Color(0xFF3FB6F2), Color(0xFF1657D9)), r)
                    drawLine(Color.White, Offset(w*0.27f, h*0.76f), Offset(cx, h*0.24f), w*0.075f, StrokeCap.Round)
                    drawLine(Color.White, Offset(cx, h*0.24f), Offset(w*0.73f, h*0.76f), w*0.075f, StrokeCap.Round)
                    drawLine(Color.White, Offset(w*0.365f, h*0.575f), Offset(w*0.635f, h*0.575f), w*0.075f, StrokeCap.Round) }
                IosKind.COMPASS -> { roundBg(listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E)), r)
                    drawPath(Path().apply { moveTo(cx, cy); lineTo(w*0.68f, h*0.30f); lineTo(w*0.56f, h*0.56f); close() }, Color(0xFFFF3B30))
                    drawPath(Path().apply { moveTo(cx, cy); lineTo(w*0.32f, h*0.70f); lineTo(w*0.44f, h*0.44f); close() }, Color.White)
                    drawCircle(Color(0xFF1C1C1E), w*0.045f, Offset(cx, cy)) }
                IosKind.SAFARI -> { roundBg(listOf(Color.White, Color(0xFFEDEDEF)), r)
                    drawCircle(Color(0xFFE3E5E8), w*0.40f, Offset(cx, cy))
                    drawPath(Path().apply { moveTo(cx, cy); lineTo(w*0.66f, h*0.30f); lineTo(w*0.60f, h*0.60f); close() }, Color(0xFFFF3B30))
                    drawPath(Path().apply { moveTo(cx, cy); lineTo(w*0.34f, h*0.70f); lineTo(w*0.40f, h*0.40f); close() }, Color.White)
                    drawCircle(Color(0xFF333333), w*0.045f, Offset(cx, cy)) }
                IosKind.MAIL -> { roundBg(listOf(Color(0xFF4E9BF5), Color(0xFF1663E8)), r)
                    drawRoundRect(Color.White, Offset(w*0.15f, h*0.28f), Size(w*0.70f, h*0.46f), CornerRadius(w*0.06f, w*0.06f))
                    drawLine(Color(0xFF1663E8), Offset(w*0.17f, h*0.31f), Offset(cx, h*0.58f), w*0.05f)
                    drawLine(Color(0xFF1663E8), Offset(w*0.83f, h*0.31f), Offset(cx, h*0.58f), w*0.05f) }
                IosKind.PHOTOS -> { roundBg(listOf(Color.White, Color(0xFFF2F2F4)), r)
                    val petal = listOf(Color(0xFFF6C445), Color(0xFFF08A3C), Color(0xFFE85D75), Color(0xFFB45BC9),
                        Color(0xFF5B8DEF), Color(0xFF4FB0C6), Color(0xFF6FCF97), Color(0xFFA8C256))
                    petal.forEachIndexed { i, c -> rotate(i * 45f, Offset(cx, cy)) {
                        drawOval(c.copy(alpha = 0.75f), Offset(cx - w*0.075f, h*0.14f), Size(w*0.15f, h*0.36f)) } } }
                IosKind.CAMERA -> { roundBg(listOf(Color(0xFFF4F4F6), Color(0xFFD9D9DE)), r)
                    drawRoundRect(Color(0xFF48484A), Offset(w*0.10f, h*0.30f), Size(w*0.80f, h*0.44f), CornerRadius(w*0.09f, w*0.09f))
                    drawRoundRect(Color(0xFF48484A), Offset(w*0.36f, h*0.20f), Size(w*0.28f, h*0.14f), CornerRadius(w*0.05f, w*0.05f))
                    drawCircle(Color(0xFF1C1C1E), w*0.155f, Offset(cx, h*0.52f))
                    drawCircle(Color(0xFF3A6EA5), w*0.085f, Offset(cx, h*0.52f)) }
                IosKind.CLOCK -> { roundBg(listOf(Color(0xFF1C1C1E), Color.Black), r)
                    drawCircle(Color.White, w*0.40f, Offset(cx, cy))
                    val cal = Calendar.getInstance()
                    val ha = (cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f) / 12f * 360f - 90f
                    val ma = cal.get(Calendar.MINUTE) / 60f * 360f - 90f
                    drawLine(Color(0xFF1C1C1E), Offset(cx, cy),
                        Offset(cx + (cos(Math.toRadians(ha.toDouble())) * w * 0.22f).toFloat(), cy + (sin(Math.toRadians(ha.toDouble())) * w * 0.22f).toFloat()), w*0.055f)
                    drawLine(Color(0xFF1C1C1E), Offset(cx, cy),
                        Offset(cx + (cos(Math.toRadians(ma.toDouble())) * w * 0.32f).toFloat(), cy + (sin(Math.toRadians(ma.toDouble())) * w * 0.32f).toFloat()), w*0.04f)
                    val sa = (cal.get(Calendar.SECOND) / 60f * 360f - 90f).toDouble()
                    drawLine(Color(0xFFFF9500), Offset(cx, cy),
                        Offset(cx + (cos(Math.toRadians(sa)) * w * 0.34f).toFloat(), cy + (sin(Math.toRadians(sa)) * w * 0.34f).toFloat()), w*0.02f) }
                IosKind.CALENDAR -> { roundBg(listOf(Color.White, Color.White), r) }
                IosKind.MAPS -> { roundBg(listOf(Color(0xFFA8E063), Color(0xFF56AB2F)), r)
                    drawPath(Path().apply { moveTo(w*0.2f, h*0.75f); lineTo(w*0.45f, h*0.25f); lineTo(w*0.55f, h*0.25f); lineTo(w*0.8f, h*0.75f); close() }, Color(0xFFF7F7F7))
                    drawCircle(Color(0xFFFF3B30), w*0.13f, Offset(cx, h*0.40f)); drawCircle(Color.White, w*0.055f, Offset(cx, h*0.40f)) }
                IosKind.SETTINGS -> { roundBg(listOf(Color(0xFFD1D1D6), Color(0xFFAEAEB2)), r)
                    repeat(8) { i -> rotate(i * 45f, Offset(cx, cy)) {
                        drawRoundRect(Color(0xFF636366), Offset(cx - w*0.055f, h*0.08f), Size(w*0.11f, h*0.20f), CornerRadius(w*0.04f, w*0.04f)) } }
                    drawCircle(Color(0xFF8E8E93), w*0.26f, Offset(cx, cy)); drawCircle(Color(0xFFD1D1D6), w*0.13f, Offset(cx, cy)) }
                IosKind.CALC -> { roundBg(listOf(Color(0xFF48484A), Color(0xFF2C2C2E)), r)
                    val s = w * 0.17f; val gap = w * 0.075f; val x0 = w * 0.16f; val y0 = h * 0.18f
                    for (row in 0..2) for (col in 0..2) {
                        val c = if (row == 2 && col == 2) Color(0xFFFF9500) else Color(0xFFE5E5EA)
                        drawRoundRect(c, Offset(x0 + col * (s + gap), y0 + row * (s + gap)), Size(s, s), CornerRadius(s * 0.35f, s * 0.35f)) } }
                IosKind.MUSIC -> { roundBg(listOf(Color(0xFFFC5C7D), Color(0xFFEE2D55)), r)
                    drawLine(Color.White, Offset(w*0.42f, h*0.70f), Offset(w*0.42f, h*0.28f), w*0.055f)
                    drawLine(Color.White, Offset(w*0.70f, h*0.64f), Offset(w*0.70f, h*0.22f), w*0.055f)
                    drawLine(Color.White, Offset(w*0.42f, h*0.28f), Offset(w*0.70f, h*0.22f), w*0.055f)
                    drawCircle(Color.White, w*0.10f, Offset(w*0.35f, h*0.70f)); drawCircle(Color.White, w*0.10f, Offset(w*0.63f, h*0.64f)) }
                IosKind.NOTES -> { roundBg(listOf(Color.White, Color.White), r)
                    drawRect(Color(0xFFF9D949), Offset(0f, 0f), Size(w, h*0.30f))
                    repeat(3) { i -> drawLine(Color(0xFFC7C7CC), Offset(w*0.16f, h*(0.48f + i*0.15f)), Offset(w*0.84f, h*(0.48f + i*0.15f)), w*0.035f) } }
                IosKind.WEATHER -> { roundBg(listOf(Color(0xFF4A90D9), Color(0xFF2F6FBF)), r)
                    drawCircle(Color(0xFFFFCC02), w*0.14f, Offset(w*0.36f, h*0.36f))
                    drawCircle(Color.White, w*0.15f, Offset(w*0.55f, h*0.62f)); drawCircle(Color.White, w*0.12f, Offset(w*0.36f, h*0.66f)); drawCircle(Color.White, w*0.12f, Offset(w*0.72f, h*0.66f)) }
                IosKind.FACETIME -> { roundBg(listOf(Color(0xFF67D45C), Color(0xFF28B81F)), r)
                    drawRoundRect(Color.White, Offset(w*0.13f, h*0.31f), Size(w*0.50f, h*0.38f), CornerRadius(w*0.10f, w*0.10f))
                    drawPath(Path().apply { moveTo(w*0.66f, h*0.44f); lineTo(w*0.88f, h*0.30f); lineTo(w*0.88f, h*0.70f); lineTo(w*0.66f, h*0.56f); close() }, Color.White) }
                IosKind.WALLET -> { roundBg(listOf(Color(0xFF2C2C2E), Color.Black), r)
                    drawRoundRect(Color(0xFFF5A8C0), Offset(w*0.18f, h*0.16f), Size(w*0.64f, h*0.16f), CornerRadius(w*0.06f, w*0.06f))
                    drawRoundRect(Color(0xFFF6A03C), Offset(w*0.18f, h*0.26f), Size(w*0.64f, h*0.16f), CornerRadius(w*0.06f, w*0.06f))
                    drawRoundRect(Color(0xFF4E9BF5), Offset(w*0.18f, h*0.36f), Size(w*0.64f, h*0.16f), CornerRadius(w*0.06f, w*0.06f))
                    drawRoundRect(Color(0xFF48484A), Offset(w*0.18f, h*0.46f), Size(w*0.64f, h*0.38f), CornerRadius(w*0.06f, w*0.06f)) }
                IosKind.HEALTH -> { roundBg(listOf(Color.White, Color(0xFFF2F2F4)), r)
                    drawCircle(Color(0xFFFA2D48), w*0.165f, Offset(w*0.365f, h*0.38f))
                    drawCircle(Color(0xFFFA2D48), w*0.165f, Offset(w*0.635f, h*0.38f))
                    drawPath(Path().apply { moveTo(w*0.215f, h*0.47f); lineTo(w*0.785f, h*0.47f); lineTo(cx, h*0.84f); close() }, Color(0xFFFA2D48)) }
                IosKind.REMINDERS -> { roundBg(listOf(Color.White, Color(0xFFF2F2F4)), r)
                    val dotC = listOf(Color(0xFFFF9500), Color(0xFF0A84FF), Color(0xFFFF3B30))
                    dotC.forEachIndexed { i, c ->
                        val y = h * (0.28f + i * 0.22f)
                        drawCircle(c, w*0.065f, Offset(w*0.24f, y))
                        drawLine(Color(0xFFC7C7CC), Offset(w*0.38f, y), Offset(w*0.82f, y), w*0.045f) } }
                IosKind.PODCASTS -> { roundBg(listOf(Color(0xFFB06BD6), Color(0xFF7D2AE8)), r)
                    drawCircle(Color.White, w*0.075f, Offset(cx, h*0.34f))
                    drawRoundRect(Color.White, Offset(cx - w*0.045f, h*0.46f), Size(w*0.09f, h*0.30f), CornerRadius(w*0.045f, w*0.045f))
                    drawArc(Color.White, -160f, 140f, false, Offset(cx - w*0.17f, h*0.17f), Size(w*0.34f, w*0.34f), style = Stroke(w*0.05f))
                    drawArc(Color.White, 20f, 140f, false, Offset(cx - w*0.17f, h*0.17f), Size(w*0.34f, w*0.34f), style = Stroke(w*0.05f)) }
                IosKind.FILES -> { roundBg(listOf(Color.White, Color(0xFFF2F2F4)), r)
                    drawRoundRect(Color(0xFFF7B733), Offset(w*0.14f, h*0.26f), Size(w*0.34f, h*0.12f), CornerRadius(w*0.05f, w*0.05f))
                    drawRoundRect(Color(0xFFFFC94D), Offset(w*0.12f, h*0.34f), Size(w*0.76f, h*0.44f), CornerRadius(w*0.07f, w*0.07f))
                    drawRoundRect(Color(0xFFFFD97A), Offset(w*0.12f, h*0.34f), Size(w*0.76f, h*0.12f), CornerRadius(w*0.07f, w*0.07f)) }
                IosKind.DISCORD -> { roundBg(listOf(Color(0xFF5865F2), Color(0xFF4752C4)), r)
                    drawRoundRect(Color.White, Offset(w*0.16f, h*0.30f), Size(w*0.68f, h*0.42f), CornerRadius(w*0.20f, w*0.20f))
                    drawCircle(Color(0xFF5865F2), w*0.055f, Offset(w*0.38f, h*0.50f))
                    drawCircle(Color(0xFF5865F2), w*0.055f, Offset(w*0.62f, h*0.50f))
                    drawArc(Color(0xFF5865F2), 20f, 140f, false, Offset(w*0.36f, h*0.44f), Size(w*0.28f, w*0.24f), style = Stroke(w*0.045f))
                    drawLine(Color.White, Offset(w*0.26f, h*0.30f), Offset(w*0.32f, h*0.22f), w*0.05f, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(w*0.74f, h*0.30f), Offset(w*0.68f, h*0.22f), w*0.05f, cap = StrokeCap.Round) }
                IosKind.SPOTIFY -> { roundBg(listOf(Color(0xFF1DB954), Color(0xFF1AA34A)), r)
                    listOf(0.30f, 0.46f, 0.62f).forEachIndexed { i, y ->
                        drawArc(Color.Black, -35f, 105f, false,
                            Offset(w*(0.24f + i*0.045f), h*(y - 0.13f)), Size(w*(0.52f - i*0.09f), h*0.34f), style = Stroke(w*0.055f, cap = StrokeCap.Round)) } }
                IosKind.XAPP -> { roundBg(listOf(Color.Black, Color(0xFF1A1A1A)), r)
                    drawLine(Color.White, Offset(w*0.28f, h*0.26f), Offset(w*0.72f, h*0.74f), w*0.085f, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(w*0.72f, h*0.26f), Offset(w*0.28f, h*0.74f), w*0.085f, cap = StrokeCap.Round) }
                IosKind.NETFLIX -> { roundBg(listOf(Color(0xFF141414), Color.Black), r)
                    drawRect(Color(0xFFB00610), Offset(w*0.26f, h*0.18f), Size(w*0.13f, h*0.64f))
                    drawRect(Color(0xFFB00610), Offset(w*0.61f, h*0.18f), Size(w*0.13f, h*0.64f))
                    val diag = Path().apply { moveTo(w*0.39f, h*0.18f); lineTo(w*0.52f, h*0.18f); lineTo(w*0.61f, h*0.82f); lineTo(w*0.48f, h*0.82f); close() }
                    drawPath(diag, Color(0xFFE50914)) }
                IosKind.SHOPEE -> { roundBg(listOf(Color(0xFFEE4D2D), Color(0xFFF15A29)), r)
                    drawRoundRect(Color.White, Offset(w*0.24f, h*0.34f), Size(w*0.52f, h*0.44f), CornerRadius(w*0.08f, w*0.08f), style = Stroke(w*0.05f))
                    drawArc(Color.White, 180f, 180f, false, Offset(w*0.36f, h*0.24f), Size(w*0.28f, w*0.22f), style = Stroke(w*0.05f))
                    val sPath = Path().apply { moveTo(w*0.62f, h*0.48f); cubicTo(w*0.50f, h*0.40f, w*0.38f, h*0.50f, w*0.48f, h*0.57f); cubicTo(w*0.58f, h*0.64f, w*0.50f, h*0.72f, w*0.38f, h*0.66f) }
                    drawPath(sPath, Color.White, style = Stroke(w*0.055f, cap = StrokeCap.Round)) }
                IosKind.BCA -> { roundBg(listOf(Color(0xFF1E5AA8), Color(0xFF14457F)), r) }
            }
        }
        // Kilau kaca Liquid Glass di separuh atas ikon
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.52f)
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.14f), Color.Transparent))))
        if (kind == IosKind.CALENDAR) {
            val cal = Calendar.getInstance()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(SimpleDateFormat("EEEE", Locale("id")).format(cal.time).uppercase(),
                        color = Color(0xFFFF3B30), fontSize = (sizeDp * 0.16f).sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(cal.get(Calendar.DAY_OF_MONTH).toString(),
                        color = Color(0xFF1C1C1E), fontSize = (sizeDp * 0.44f).sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Light)
                }
            }
        }
        if (kind == IosKind.BCA) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("BCA", color = Color.White, fontSize = (sizeDp * 0.30f).sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

/** Kepingan salju digambar (untuk Dock & Freezer) — pengganti emoji ❄. */
@Composable
fun SnowGlyph(sizeDp: Int, color: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier.size(sizeDp.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.width * 0.42f
        for (a in 0 until 6) {
            val ang = Math.toRadians(a * 60.0)
            val ex = c.x + (r * Math.cos(ang)).toFloat(); val ey = c.y + (r * Math.sin(ang)).toFloat()
            drawLine(color, c, Offset(ex, ey), size.width * 0.055f, cap = StrokeCap.Round)
            val bx = c.x + (r * 0.62f * Math.cos(ang)).toFloat(); val by = c.y + (r * 0.62f * Math.sin(ang)).toFloat()
            for (s in listOf(-0.6, 0.6)) {
                val ba = ang + s
                drawLine(color, Offset(bx, by),
                    Offset(bx + (r * 0.34f * Math.cos(ba)).toFloat(), by + (r * 0.34f * Math.sin(ba)).toFloat()),
                    size.width * 0.05f, cap = StrokeCap.Round)
            }
        }
    }
}

/** Jenis glyph Control Center (digambar ala SF Symbols, bukan emoji). */
enum class CcKind { PLANE, CELL, WIFI, BT, TORCH, MOON, ROTLOCK, SUN, SOUND, PIC, BATT, A11Y }

/** Glyph putih Control Center digambar dengan Canvas — bersih seperti simbol iOS. */
@Composable
fun CcGlyph(kind: CcKind, sizeDp: Int = 24, color: Color = Color.White) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2
        val sw = w * 0.075f
        when (kind) {
            CcKind.PLANE -> {
                val p = Path().apply {
                    moveTo(cx, h * 0.10f); cubicTo(w * 0.58f, h * 0.30f, w * 0.60f, h * 0.48f, w * 0.60f, h * 0.62f)
                    lineTo(w * 0.86f, h * 0.80f); lineTo(w * 0.86f, h * 0.88f); lineTo(cx, h * 0.78f)
                    lineTo(w * 0.14f, h * 0.88f); lineTo(w * 0.14f, h * 0.80f); lineTo(w * 0.40f, h * 0.62f)
                    cubicTo(w * 0.40f, h * 0.48f, w * 0.42f, h * 0.30f, cx, h * 0.10f); close() }
                drawPath(p, color)
                drawRoundRect(color, Offset(cx - w * 0.05f, h * 0.82f), Size(w * 0.10f, h * 0.10f), CornerRadius(w * 0.03f, w * 0.03f)) }
            CcKind.CELL -> { for (i in 0..3) { val bh = h * (0.28f + i * 0.20f); val bw = w * 0.15f
                    drawRoundRect(color, Offset(w * 0.10f + i * (bw + w * 0.05f), cy + h * 0.32f - bh), Size(bw, bh), CornerRadius(2f, 2f)) } }
            CcKind.WIFI -> {
                drawArc(color, -155f, 50f, false, Offset(cx - w * 0.40f, cy - h * 0.34f), Size(w * 0.80f, h * 0.80f), style = Stroke(sw, cap = StrokeCap.Round))
                drawArc(color, -150f, 40f, false, Offset(cx - w * 0.26f, cy - h * 0.20f), Size(w * 0.52f, h * 0.52f), style = Stroke(sw, cap = StrokeCap.Round))
                drawCircle(color, w * 0.055f, Offset(cx, cy + h * 0.28f)) }
            CcKind.BT -> { val p = Path().apply { moveTo(cx, h * 0.10f); lineTo(cx, h * 0.90f); lineTo(w * 0.78f, h * 0.66f)
                    lineTo(w * 0.30f, h * 0.36f); lineTo(w * 0.78f, h * 0.12f); lineTo(cx, h * 0.10f); moveTo(cx, h * 0.36f); lineTo(w * 0.62f, h * 0.50f); lineTo(cx, h * 0.64f) }
                drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round)) }
            CcKind.TORCH -> {
                drawRoundRect(color, Offset(cx - w * 0.10f, h * 0.34f), Size(w * 0.20f, h * 0.52f), CornerRadius(w * 0.06f, w * 0.06f))
                drawRoundRect(color, Offset(cx - w * 0.16f, h * 0.14f), Size(w * 0.32f, h * 0.16f), CornerRadius(w * 0.05f, w * 0.05f))
                drawCircle(color.copy(0.9f), w * 0.045f, Offset(cx, h * 0.52f)) }
            CcKind.MOON -> { val p = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.14f, h * 0.10f, w * 0.90f, h * 0.90f)) }
                val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.38f, h * 0.02f, w * 1.06f, h * 0.74f)) }
                drawPath(Path().apply { op(p, cut, PathOperation.Difference) }, color) }
            CcKind.ROTLOCK -> {
                drawArc(color, -60f, 300f, false, Offset(cx - w * 0.30f, cy - h * 0.26f), Size(w * 0.60f, h * 0.56f), style = Stroke(sw, cap = StrokeCap.Round))
                val tri = Path().apply { moveTo(cx + w * 0.34f, cy - h * 0.30f); lineTo(cx + w * 0.16f, cy - h * 0.26f); lineTo(cx + w * 0.33f, cy - h * 0.12f); close() }
                drawPath(tri, color)
                drawRoundRect(color, Offset(cx - w * 0.13f, cy - h * 0.02f), Size(w * 0.26f, h * 0.30f), CornerRadius(w * 0.05f, w * 0.05f))
                drawArc(color, 180f, 180f, false, Offset(cx - w * 0.08f, cy - h * 0.14f), Size(w * 0.16f, h * 0.18f), style = Stroke(sw * 0.8f)) }
            CcKind.SUN -> { drawCircle(color, w * 0.17f, Offset(cx, cy))
                for (a in 0 until 8) { val ang = Math.toRadians(a * 45.0)
                    drawLine(color, Offset(cx + (w * 0.26f * Math.cos(ang)).toFloat(), cy + (h * 0.26f * Math.sin(ang)).toFloat()),
                        Offset(cx + (w * 0.38f * Math.cos(ang)).toFloat(), cy + (h * 0.38f * Math.sin(ang)).toFloat()), sw, cap = StrokeCap.Round) } }
            CcKind.SOUND -> {
                val p = Path().apply { moveTo(w * 0.14f, h * 0.40f); lineTo(w * 0.30f, h * 0.40f); lineTo(w * 0.56f, h * 0.18f)
                    lineTo(w * 0.56f, h * 0.82f); lineTo(w * 0.30f, h * 0.60f); lineTo(w * 0.14f, h * 0.60f); close() }
                drawPath(p, color)
                drawArc(color, -60f, 120f, false, Offset(cx - w * 0.02f, cy - h * 0.22f), Size(w * 0.36f, h * 0.44f), style = Stroke(sw, cap = StrokeCap.Round))
                drawArc(color, -60f, 120f, false, Offset(cx + w * 0.10f, cy - h * 0.32f), Size(w * 0.40f, h * 0.64f), style = Stroke(sw, cap = StrokeCap.Round)) }
            CcKind.PIC -> {
                drawRoundRect(color, Offset(w * 0.12f, h * 0.16f), Size(w * 0.76f, h * 0.68f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(sw))
                drawCircle(color, w * 0.06f, Offset(w * 0.32f, h * 0.34f))
                val m = Path().apply { moveTo(w * 0.14f, h * 0.80f); lineTo(w * 0.42f, h * 0.52f); lineTo(w * 0.58f, h * 0.66f); lineTo(w * 0.70f, h * 0.54f); lineTo(w * 0.86f, h * 0.80f); close() }
                drawPath(m, color) }
            CcKind.BATT -> {
                drawRoundRect(color, Offset(w * 0.08f, h * 0.30f), Size(w * 0.76f, h * 0.40f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(sw))
                drawRoundRect(color, Offset(w * 0.13f, h * 0.35f), Size(w * 0.52f, h * 0.30f), CornerRadius(w * 0.05f, w * 0.05f))
                drawRoundRect(color, Offset(w * 0.87f, h * 0.42f), Size(w * 0.05f, h * 0.16f), CornerRadius(2f, 2f)) }
            CcKind.A11Y -> {
                drawCircle(color, w * 0.20f, Offset(cx, cy), style = Stroke(sw * 0.85f))
                drawCircle(color, w * 0.045f, Offset(cx, cy - h * 0.105f))
                drawLine(color, Offset(cx - w * 0.105f, cy - h * 0.045f), Offset(cx + w * 0.105f, cy - h * 0.045f), sw * 0.8f, cap = StrokeCap.Round)
                val body = Path().apply { moveTo(cx, cy - h * 0.04f); lineTo(cx, cy + h * 0.05f); lineTo(cx - w * 0.085f, cy + h * 0.155f); moveTo(cx, cy + h * 0.05f); lineTo(cx + w * 0.085f, cy + h * 0.155f) }
                drawPath(body, color, style = Stroke(sw * 0.8f, cap = StrokeCap.Round)) }
        }
    }
}

/** Pin lokasi kecil digambar (untuk info lokasi di widget XL). */
@Composable
fun PinGlyph(sizeDp: Int, color: Color = Color.White) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val cx = size.width / 2
        drawCircle(color, size.width * 0.30f, Offset(cx, size.height * 0.34f))
        val p = Path().apply { moveTo(cx - size.width * 0.28f, size.height * 0.50f)
            lineTo(cx + size.width * 0.28f, size.height * 0.50f); lineTo(cx, size.height * 0.98f); close() }
        drawPath(p, color)
        drawCircle(Color.Black.copy(0.25f), size.width * 0.13f, Offset(cx, size.height * 0.34f))
    }
}
