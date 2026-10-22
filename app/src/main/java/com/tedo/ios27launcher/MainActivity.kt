package com.tedo.ios27launcher

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tedo.ios27launcher.badges.BadgeStore
import com.tedo.ios27launcher.data.AppInfo
import com.tedo.ios27launcher.data.AppRepository
import com.tedo.ios27launcher.data.WeatherInfo
import com.tedo.ios27launcher.data.WeatherRepo
import com.tedo.ios27launcher.freezer.FreezerManager
import com.tedo.ios27launcher.ui.ControlCenterScreen
import com.tedo.ios27launcher.ui.FreezerScreen
import com.tedo.ios27launcher.ui.IconMode
import com.tedo.ios27launcher.ui.IosIcon
import com.tedo.ios27launcher.ui.LibraryScreen
import com.tedo.ios27launcher.ui.NotificationCenterScreen
import com.tedo.ios27launcher.ui.SettingsScreen
import com.tedo.ios27launcher.ui.SnowGlyph
import com.tedo.ios27launcher.ui.PinGlyph
import com.tedo.ios27launcher.ui.SearchGlyph
import com.tedo.ios27launcher.ui.SearchScreen
import com.tedo.ios27launcher.ui.TINT_PRESETS
import com.tedo.ios27launcher.ui.iconEffect
import com.tedo.ios27launcher.ui.iosKindFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class Screen { HOME, FREEZER, SEARCH, LIBRARY, CONTROL, NOTIF, SETTINGS }

/**
 * Launcher gaya iOS 27 untuk Infinix Note 12 — v0.8
 * Semua temuan riset iOS 27 (dari halaman resmi Apple + pers teknologi) yang bisa diterapkan
 * launcher, diterapkan tanpa kecuali:
 * - 4 mode ikon resmi: Default / Dark / Clear / Tinted (format iOS 27: warna vivid + glyph terang)
 * - Panel Edit > Customize ala iOS saat mode jiggle (mode ikon + warna tint + slider Liquid Glass)
 * - Widget extra-large (XL) di Smart Stack
 * - Gestur atas terbagi ala iOS 27: kiri atas = Notifikasi, kanan atas = Control Center,
 *   geser bawah di layar = Search or Ask (gelembung kaca hitam)
 * - Dynamic Island (pil hitam, jumlah notifikasi belum terbaca) di status bar gambar sendiri
 * - Lencana mode Clear jadi putih kaca (kehilangan merahnya, seperti iOS 27)
 * - Pintasan Pengaturan ala iOS di Control Center: Suara, Wallpaper, Baterai, Aksesibilitas
 * Yang memang fitur sistem/app iOS (Siri AI, Lock Screen, iPhone Handoff, isi app Apple)
 * tidak bisa diterapkan launcher mana pun di Android — dinyatakan terbuka ke user.
 */
class MainActivity : ComponentActivity() {
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent { LauncherRoot(resumeTick.intValue) }
    }
    override fun onResume() { super.onResume(); BadgeStore.launcherForeground = true; resumeTick.intValue++ }
    override fun onPause() { super.onPause(); BadgeStore.launcherForeground = false }
}

/** Buka panel Notifikasi sistem (geser kiri atas ala iOS 27). True bila berhasil. */
fun expandNotifications(context: Context): Boolean = try {
    val sbm = context.getSystemService("statusbar")!!
    sbm.javaClass.getMethod("expandNotificationsPanel").invoke(sbm)
    true
} catch (e: Exception) { false }

/** Persentase baterai asli dari sticky intent sistem. */
fun batteryPctOf(context: Context): Int = try {
    val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val s = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    if (l >= 0 && s > 0) l * 100 / s else -1
} catch (e: Exception) { -1 }

@Composable
fun LauncherRoot(resumeTick: Int) {
    val context = LocalContext.current
    val freezer = remember { FreezerManager(context) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var hidden by remember { mutableStateOf(freezer.hiddenPackages()) }
    val prefs = remember { context.getSharedPreferences("glass", 0) }
    var glass by remember { mutableStateOf(prefs.getFloat("level", 0.35f)) }
    var modeName by remember {
        mutableStateOf(prefs.getString("iconMode", null)
            ?: if (prefs.getBoolean("mono", true)) "TINTED" else "DEFAULT")
    }
    val mode = IconMode.valueOf(modeName)
    var tintArgb by remember { mutableIntStateOf(prefs.getInt("tint", 0xFF4A90D9.toInt())) }
    var showLabels by remember { mutableStateOf(prefs.getBoolean("labels", true)) }
    var wpSystem by remember { mutableStateOf(prefs.getBoolean("wpSystem", true)) }
    var fakeBar by remember { mutableStateOf(prefs.getBoolean("fakeBar", true)) }
    var islandMode by remember { mutableStateOf(prefs.getString("islandMode", null)
        ?: if (prefs.getBoolean("island", true)) "POPUP" else "OFF") }
    var badgesOn by remember { mutableStateOf(prefs.getBoolean("badgesOn", true)) }
    var notifPanel by remember { mutableStateOf(prefs.getString("notifPanel", "LAUNCHER") ?: "LAUNCHER") }
    var swipeDown by remember { mutableStateOf(prefs.getString("swipeDown", "NOTIF") ?: "NOTIF") }
    // v0.14: pemetaan gestur ala pengaturan launcher referensi, disesuaikan iOS 27.
    var gSwipeUp by remember { mutableStateOf(prefs.getString("gSwipeUp", "LIBRARY") ?: "LIBRARY") }
    var gPinchIn by remember { mutableStateOf(prefs.getString("gPinchIn", "EDIT") ?: "EDIT") }
    var gPinchOut by remember { mutableStateOf(prefs.getString("gPinchOut", "NONE") ?: "NONE") }
    var gTwoUp by remember { mutableStateOf(prefs.getString("gTwoUp", "FREEZER") ?: "FREEZER") }
    var gTwoDown by remember { mutableStateOf(prefs.getString("gTwoDown", "VOICE") ?: "VOICE") }
    var gRotCw by remember { mutableStateOf(prefs.getString("gRotCw", "NONE") ?: "NONE") }
    var gRotCcw by remember { mutableStateOf(prefs.getString("gRotCcw", "NONE") ?: "NONE") }
    val gestureMap = mapOf("SWIPE_UP" to gSwipeUp, "PINCH_IN" to gPinchIn, "PINCH_OUT" to gPinchOut,
        "TWO_UP" to gTwoUp, "TWO_DOWN" to gTwoDown, "ROT_CW" to gRotCw, "ROT_CCW" to gRotCcw)
    fun setGesture(key: String, value: String) {
        when (key) { "SWIPE_UP" -> gSwipeUp = value; "PINCH_IN" -> gPinchIn = value
            "PINCH_OUT" -> gPinchOut = value; "TWO_UP" -> gTwoUp = value; "TWO_DOWN" -> gTwoDown = value
            "ROT_CW" -> gRotCw = value; "ROT_CCW" -> gRotCcw = value }
        prefs.edit().putString("g" + key.lowercase().replaceFirstChar { it.uppercase() }
            .replace("_up", "Up").replace("_in", "In").replace("_out", "Out")
            .replace("_down", "Down").replace("_cw", "Cw").replace("_ccw", "Ccw"), value).apply()
    }
    val tintColor = Color(
        red = android.graphics.Color.red(tintArgb), green = android.graphics.Color.green(tintArgb),
        blue = android.graphics.Color.blue(tintArgb), alpha = android.graphics.Color.alpha(tintArgb))
    val badgeCounts = BadgeStore.counts
    val effCounts: Map<String, Int> = if (badgesOn) badgeCounts else emptyMap()
    val activity = LocalContext.current as? Activity
    LaunchedEffect(fakeBar) {
        activity?.let {
            val c = WindowInsetsControllerCompat(it.window, it.window.decorView)
            if (fakeBar) {
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                c.hide(WindowInsetsCompat.Type.statusBars())
            } else c.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    LaunchedEffect(resumeTick) {
        val loaded = withContext(Dispatchers.Default) {
            AppRepository.loadLaunchableApps(context, force = resumeTick > 1)
        }
        apps = loaded; hidden = freezer.hiddenPackages()
    }
    fun refreshSoon() { AppRepository.invalidate(); hidden = freezer.hiddenPackages() }
    fun setMode(m: IconMode) { modeName = m.name; prefs.edit().putString("iconMode", m.name).apply() }
    fun setTint(c: Color) { tintArgb = c.toArgb(); prefs.edit().putInt("tint", c.toArgb()).apply() }

    val frozenApps = apps.filter { !it.enabled || it.packageName in hidden }
    val homeApps = apps.filter { it.enabled && it.packageName !in hidden }

    // Wallpaper HP dimuat SEKALI di root dan digambar FULL-SCREEN sampai ke belakang
    // status bar (v0.13: dulu digambar di dalam area ber-padding sehingga di belakang
    // status bar hanya ada pita hitam — tidak menyatu dengan background).
    var wpTick by remember { mutableIntStateOf(0) }
    var wallpaper by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(wpSystem, wpTick) {
        wallpaper = withContext(Dispatchers.IO) {
            // Prioritas 1: foto pilihan user dari galeri (Pengaturan > Tampilan) — jalur yang
            // PASTI tampil karena filenya milik launcher sendiri.
            val f = java.io.File(context.filesDir, "wallpaper.jpg")
            val custom = if (f.exists()) try {
                android.graphics.BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() } catch (e: Exception) { null }
            else null
            // Prioritas 2: wallpaper sistem HP. Terakhir: null -> mesh iOS.
            custom ?: if (wpSystem) loadSystemWallpaper(context) else null
        }
    }
    var wpAsked by remember { mutableStateOf(false) }
    val wpPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { wpTick++ }
    LaunchedEffect(wpSystem, wallpaper) {
        if (wpSystem && wallpaper == null && !wpAsked && Build.VERSION.SDK_INT >= 33) {
            wpAsked = true
            wpPerm.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    Box(Modifier.fillMaxSize()) {
    // Latar penuh: wallpaper HP asli, atau mesh iOS bila tidak terbaca / dimatikan.
    wallpaper?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        ?: IosMeshWallpaper()
    Box(Modifier.fillMaxSize().background(
        if (screen == Screen.HOME) Color.Black.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.38f)))
    val fade = remember { Animatable(1f) }
    LaunchedEffect(screen) { fade.snapTo(0.55f); fade.animateTo(1f, tween(200)) }
    Box(Modifier.fillMaxSize().padding(top = if (fakeBar) 30.dp else 0.dp)
        .graphicsLayer { alpha = fade.value }) {
    when (screen) {
        Screen.FREEZER -> FreezerScreen(frozenApps, homeApps + frozenApps, freezer, glass, mode, tintColor,
            onBack = { refreshSoon(); screen = Screen.HOME }, onChanged = { refreshSoon() })
        Screen.SEARCH -> SearchScreen(homeApps, mode, tintColor,
            onBack = { screen = Screen.HOME },
            onLaunch = { AppRepository.launch(context, it); screen = Screen.HOME })
        Screen.LIBRARY -> LibraryScreen(homeApps, mode, tintColor,
            onBack = { screen = Screen.HOME },
            onLaunch = { AppRepository.launch(context, it); screen = Screen.HOME })
        Screen.NOTIF -> NotificationCenterScreen(apps, glass, mode, tintColor,
            onBack = { screen = Screen.HOME })
        Screen.SETTINGS -> SettingsScreen(glass,
            onGlassChange = { glass = it; prefs.edit().putFloat("level", it).apply() },
            mode = mode, onModeChange = { setMode(it) },
            tint = tintColor, onTintChange = { setTint(it) },
            showLabels = showLabels, onLabelsChange = { showLabels = it; prefs.edit().putBoolean("labels", it).apply() },
            wpSystem = wpSystem, onWpChange = { wpSystem = it; prefs.edit().putBoolean("wpSystem", it).apply() },
            fakeBar = fakeBar, onFakeBarChange = { fakeBar = it; prefs.edit().putBoolean("fakeBar", it).apply() },
            islandMode = islandMode, onIslandModeChange = { islandMode = it; prefs.edit().putString("islandMode", it).apply() },
            badgesOn = badgesOn, onBadgesChange = { badgesOn = it; prefs.edit().putBoolean("badgesOn", it).apply() },
            notifPanel = notifPanel, onNotifPanelChange = { notifPanel = it; prefs.edit().putString("notifPanel", it).apply() },
            swipeDown = swipeDown, onSwipeDownChange = { swipeDown = it; prefs.edit().putString("swipeDown", it).apply() },
            gestures = gestureMap, onGestureChange = { k, v -> setGesture(k, v) },
            onWallpaperChanged = { wpTick++ },
            frozenCount = frozenApps.size,
            onOpenFreezer = { screen = Screen.FREEZER },
            onOpenNotifCenter = { screen = Screen.NOTIF },
            onBack = { screen = Screen.HOME })
        Screen.CONTROL -> ControlCenterScreen(glass,
            onGlassChange = { glass = it; prefs.edit().putFloat("level", it).apply() },
            mode = mode, onModeChange = { setMode(it) },
            tint = tintColor, onTintChange = { setTint(it) },
            showLabels = showLabels, onLabelsChange = { showLabels = it; prefs.edit().putBoolean("labels", it).apply() },
            wpSystem = wpSystem, onWpChange = { wpSystem = it; prefs.edit().putBoolean("wpSystem", it).apply() },
            fakeBar = fakeBar, onFakeBarChange = { fakeBar = it; prefs.edit().putBoolean("fakeBar", it).apply() },
            islandMode = islandMode, onIslandModeChange = { islandMode = it; prefs.edit().putString("islandMode", it).apply() },
            onOpenSettings = { screen = Screen.SETTINGS },
            onBack = { screen = Screen.HOME })
        Screen.HOME -> HomeScreen(homeApps, frozenApps.size, glass,
            onGlassChange = { glass = it; prefs.edit().putFloat("level", it).apply() },
            mode, tintColor, { setMode(it) }, { setTint(it) }, showLabels, wpSystem, effCounts,
            onOpenFreezer = { screen = Screen.FREEZER },
            onOpenSearch = { screen = Screen.SEARCH },
            onOpenLibrary = { screen = Screen.LIBRARY },
            onOpenControl = { screen = Screen.CONTROL },
            onOpenSettings = { screen = Screen.SETTINGS },
            gestureActions = gestureMap,
            onVoiceSearch = { try { context.startActivity(
                    Intent("android.speech.action.RECOGNIZE_SPEECH").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                catch (e: Exception) { screen = Screen.SEARCH } },
            onSwipeDownHome = { if (swipeDown == "SEARCH") screen = Screen.SEARCH
                else if (swipeDown == "NONE") { }
                else if (notifPanel == "LAUNCHER") screen = Screen.NOTIF
                else if (!expandNotifications(context)) screen = Screen.NOTIF },
            onOpenNotifications = { if (notifPanel == "LAUNCHER") screen = Screen.NOTIF
                else if (!expandNotifications(context)) screen = Screen.NOTIF },
            onFreeze = { pkg -> freezer.freeze(pkg); refreshSoon() })
    }
    }
    if (fakeBar) FakeStatusBar(Modifier.align(Alignment.TopCenter),
        onSwipeDown = if (screen == Screen.HOME) ({ frac: Float ->
            if (frac < 0.45f) { if (notifPanel == "LAUNCHER") screen = Screen.NOTIF
                else if (!expandNotifications(context)) screen = Screen.NOTIF }
            else screen = Screen.CONTROL
        }) else null)
    IslandPopup(islandMode, effCounts.values.sum(), BadgeStore.notifications.firstOrNull(),
        apps, mode, tintColor, fakeBar, Modifier.align(Alignment.TopCenter))
    }
}

/**
 * Status bar gaya iOS yang digambar launcher sendiri: jam asli kiri, sinyal/Wi-Fi + baterai
 * asli kanan, tanpa ikon notifikasi app. Tengah SENGAJA dikosongkan — di situlah kamera depan
 * Infinix Note 12 berada. Dynamic Island kini melayang di BAWAH bilah ini (lihat IslandPopup).
 * Geser ke bawah: kiri = Notifikasi, kanan = Control Center (pembagian gestur iOS 27).
 */
@Composable
fun FakeStatusBar(modifier: Modifier = Modifier,
                  onSwipeDown: ((Float) -> Unit)? = null) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); now = Date() } }
    val pct = remember(now) { batteryPctOf(context) }
    val wifiBars = remember(now) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION") val rssi = wm.connectionInfo?.rssi ?: -127
            if (rssi == -127 || rssi == 0) -1 else WifiManager.calculateSignalLevel(rssi, 4)
        } catch (e: Exception) { -1 }
    }
    val bars = if (wifiBars >= 0) wifiBars + 1 else 3
    val gesture = if (onSwipeDown != null) Modifier.pointerInput(Unit) {
        detectVerticalDragGestures { change, drag ->
            if (drag > 14f) onSwipeDown(change.position.x / size.width.coerceAtLeast(1))
        }
    } else Modifier
    Row(modifier.fillMaxWidth().height(30.dp).then(gesture).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), color = Color.White,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = Shadow(Color.Black.copy(0.4f), Offset(0f, 1f), 3f)))
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.size(width = 18.dp, height = 12.dp)) {
            val bw = size.width / 5f
            for (i in 0..3) {
                val bh = size.height * (0.3f + i * 0.23f)
                drawRoundRect(
                    if (i < bars) Color.White else Color.White.copy(0.35f),
                    Offset(i * (bw + 1.5f), size.height - bh), Size(bw, bh),
                    CornerRadius(1.5f, 1.5f))
            }
        }
        Spacer(Modifier.width(6.dp))
        Canvas(Modifier.size(16.dp)) {
            val c = Offset(size.width / 2, size.height * 0.78f)
            drawArc(Color.White, -155f, 50f, false,
                Offset(c.x - size.width * 0.46f, c.y - size.height * 0.62f),
                Size(size.width * 0.92f, size.height * 0.92f), style = Stroke(1.8f))
            drawArc(Color.White, -150f, 40f, false,
                Offset(c.x - size.width * 0.30f, c.y - size.height * 0.42f),
                Size(size.width * 0.60f, size.height * 0.60f), style = Stroke(1.8f))
            drawCircle(Color.White, 1.8f, c)
        }
        Spacer(Modifier.width(6.dp))
        if (pct >= 0) Text("$pct", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = Shadow(Color.Black.copy(0.4f), Offset(0f, 1f), 3f)))
        Spacer(Modifier.width(3.dp))
        Canvas(Modifier.size(width = 25.dp, height = 12.dp)) {
            val bodyW = size.width - 3f
            drawRoundRect(Color.White.copy(0.45f), Offset(0f, 0f), Size(bodyW, size.height),
                CornerRadius(3.5f, 3.5f), style = Stroke(1.2f))
            val fillW = (bodyW - 3f) * ((pct.coerceIn(0, 100)) / 100f)
            if (fillW > 0f) drawRoundRect(
                if (pct in 1..19) Color(0xFFFF3B30) else Color.White,
                Offset(1.5f, 1.5f), Size(fillW, size.height - 3f), CornerRadius(2f, 2f))
            drawRoundRect(Color.White.copy(0.45f), Offset(bodyW + 0.5f, size.height * 0.32f),
                Size(2.2f, size.height * 0.36f), CornerRadius(1f, 1f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>, frozenCount: Int, glass: Float, onGlassChange: (Float) -> Unit,
    mode: IconMode, tintColor: Color, onModeChange: (IconMode) -> Unit, onTintChange: (Color) -> Unit,
    showLabels: Boolean, wpSystem: Boolean, badgeCounts: Map<String, Int>,
    onOpenFreezer: () -> Unit, onOpenSearch: () -> Unit, onOpenLibrary: () -> Unit,
    onOpenControl: () -> Unit, onOpenSettings: () -> Unit, onSwipeDownHome: () -> Unit,
    onOpenNotifications: () -> Unit, onFreeze: (String) -> Unit,
    gestureActions: Map<String, String>, onVoiceSearch: () -> Unit
) {
    val context = LocalContext.current
    var weather by remember { mutableStateOf(WeatherRepo.cached) }
    LaunchedEffect(Unit) {
        // Selalu cek: repo mengembalikan cache instan bila masih segar (<30 mnt).
        weather = withContext(Dispatchers.IO) { WeatherRepo.fetchJakarta() } ?: weather
    }
    val pages = remember(apps) { apps.chunked(20) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) + 1 })
    LaunchedEffect(pagerState.settledPage, pages.size) {
        if (pages.isNotEmpty() && pagerState.settledPage == pages.size) {
            onOpenLibrary()
            pagerState.scrollToPage(pages.size - 1)
        }
    }
    val stackState = rememberPagerState(pageCount = { 3 })
    var jiggle by remember { mutableStateOf(false) }
    val glassColor = Color.White.copy(alpha = 0.16f + glass * 0.34f)
    val now = remember { Date() }
    val labelShadow = Shadow(Color.Black.copy(0.5f), Offset(0f, 2f), 4f)
    val batteryPct = remember { batteryPctOf(context) }
    val storageFreeGb = remember {
        try { StatFs(Environment.getDataDirectory().path).availableBytes / (1024L * 1024 * 1024) }
        catch (e: Exception) { -1L }
    }
    // Audit v0.12: Dulu Dock dicari dari daftar package tetap yang sering tidak cocok di
    // Infinix (dialer-nya beda nama) sehingga ikon Telepon hilang. Kini dipilih per JENIS
    // ikon iOS: Telepon, Pesan, Safari — app apa pun yang terpetakan ke jenis itu.
    val dock = listOf(com.tedo.ios27launcher.ui.IosKind.PHONE, com.tedo.ios27launcher.ui.IosKind.MESSAGES,
        com.tedo.ios27launcher.ui.IosKind.SAFARI)
        .mapNotNull { k -> apps.firstOrNull { iosKindFor(it) == k } }
        .distinctBy { it.packageName }

    fun runGesture(action: String?) {
        when (action) {
            "LIBRARY" -> onOpenLibrary(); "SEARCH" -> onOpenSearch(); "NOTIF" -> onOpenNotifications()
            "CONTROL" -> onOpenControl(); "FREEZER" -> onOpenFreezer(); "SETTINGS" -> onOpenSettings()
            "EDIT" -> jiggle = true; "VOICE" -> onVoiceSearch()
        }
    }
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        detectVerticalDragGestures { change, drag ->
            if (change.position.y > 150f && drag > 55f) onSwipeDownHome()
            if (change.position.y > 150f && drag < -70f) runGesture(gestureActions["SWIPE_UP"])
        }
    }.pointerInput(gestureActions) {
        // Detektor 2 jari: cubit masuk/keluar, putar, geser 2 jari (ala pengaturan gestur referensi).
        awaitPointerEventScope {
            var tracking = false; var fired = false
            var startDist = 0f; var startAngle = 0f; var startY = 0f
            while (true) {
                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size == 2 && !tracking) {
                    tracking = true; fired = false
                    val a = pressed[0]; val b = pressed[1]
                    startDist = (a.position - b.position).getDistance()
                    startAngle = kotlin.math.atan2(b.position.y - a.position.y, b.position.x - a.position.x)
                    startY = (a.position.y + b.position.y) / 2f
                } else if (tracking && pressed.size == 2 && !fired) {
                    val a = pressed[0]; val b = pressed[1]
                    val dDist = (a.position - b.position).getDistance() - startDist
                    var dAng = Math.toDegrees((kotlin.math.atan2(
                        b.position.y - a.position.y, b.position.x - a.position.x) - startAngle).toDouble())
                    if (dAng > 180) dAng -= 360; if (dAng < -180) dAng += 360
                    val dY = (a.position.y + b.position.y) / 2f - startY
                    val act = when {
                        dDist < -55.dp.toPx() -> gestureActions["PINCH_IN"]
                        dDist > 55.dp.toPx() -> gestureActions["PINCH_OUT"]
                        dAng > 32 -> gestureActions["ROT_CW"]
                        dAng < -32 -> gestureActions["ROT_CCW"]
                        dY < -70.dp.toPx() -> gestureActions["TWO_UP"]
                        dY > 70.dp.toPx() -> gestureActions["TWO_DOWN"]
                        else -> null
                    }
                    if (act != null) { fired = true; runGesture(act) }
                } else if (pressed.isEmpty()) tracking = false
            }
        }
    }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)) {
            // Smart Stack 3 halaman: Cuaca+Kalender | Freezer+Perangkat | Widget XL
            HorizontalPager(stackState, Modifier.fillMaxWidth().height(128.dp), beyondViewportPageCount = 1) { page ->
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (page) {
                        0 -> {
                            WidgetCell("Cuaca", Modifier.weight(1f)) { WeatherWidget(weather, now) }
                            WidgetCell("Kalender", Modifier.weight(1f)) { CalendarWidget() }
                        }
                        1 -> {
                            WidgetCell("Freezer", Modifier.weight(1f)) {
                                GlassCard(glass, Modifier.fillMaxSize(), listOf(Color(0xFF4FC3F7), Color(0xFF2F80ED))) {
                                    Box(Modifier.fillMaxSize().clickable { onOpenFreezer() }) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) { SnowGlyph(15); Spacer(Modifier.width(5.dp)); Text("Freezer", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                                            Text("$frozenCount app beku dari launcher ini",
                                                color = Color.White.copy(0.9f), fontSize = 11.sp)
                                            Text("Ketuk untuk membuka", color = Color.White.copy(0.7f), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            WidgetCell("Perangkat", Modifier.weight(1f)) {
                                GlassCard(glass, Modifier.fillMaxSize(), listOf(Color(0xFF30D158), Color(0xFF1F9D4D))) {
                                    Text("Baterai ${if (batteryPct >= 0) "$batteryPct%" else "—"}",
                                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Penyimpanan ${if (storageFreeGb >= 0) "$storageFreeGb GB" else "—"} kosong",
                                        color = Color.White.copy(0.92f), fontSize = 13.sp)
                                    Text(SimpleDateFormat("EEEE, d MMMM", Locale("id")).format(now),
                                        color = Color.White.copy(0.75f), fontSize = 10.sp)
                                }
                            }
                        }
                        else -> {
                            WidgetCell("XL — Ringkasan Hari Ini", Modifier.weight(1f)) {
                                GlassCard(glass, Modifier.fillMaxSize(), listOf(Color(0xFF5E5CE6), Color(0xFF3A4CC0))) {
                                    val w = weather
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text(if (w != null) "${w.temp}° Jakarta" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                                                color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                                            Text(if (w != null) "${WeatherRepo.textFor(w.code)} • H:${w.hi}° L:${w.lo}°"
                                                else SimpleDateFormat("EEEE, d MMMM", Locale("id")).format(now),
                                                color = Color.White.copy(0.85f), fontSize = 11.sp)
                                        }
                                        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                                            color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Light)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PinGlyph(11)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Jakarta, Indonesia", color = Color.White.copy(0.85f), fontSize = 10.5.sp)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        XlChip("$frozenCount beku") { onOpenFreezer() }
                                        XlChip("Baterai ${if (batteryPct >= 0) "$batteryPct%" else "—"}") { }
                                        XlChip("${if (storageFreeGb >= 0) "$storageFreeGb GB" else "—"} kosong") { }
                                        XlChip("Library") { onOpenLibrary() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            if (pages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                HorizontalPager(pagerState, Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
                    if (page < pages.size) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            pages[page].chunked(4).forEach { rowApps ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    rowApps.forEach { app ->
                                        AppIcon(app, labelShadow, jiggle, mode, tintColor, showLabels,
                                            badgeCounts[app.packageName] ?: 0,
                                            onJiggle = { jiggle = true }, onFreeze)
                                    }
                                    repeat(4 - rowApps.size) { Spacer(Modifier.width(72.dp)) }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(Modifier.clip(RoundedCornerShape(26.dp)).background(glassColor)
                                .border(1.dp, Color.White.copy(0.25f), RoundedCornerShape(26.dp))
                                .clickable { onOpenLibrary() }.padding(26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("▦", color = Color.White, fontSize = 40.sp)
                                Text("App Library", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text("Ketuk untuk membuka semua aplikasi",
                                    color = Color.White.copy(0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Panel Edit > Customize ala iOS 27 (muncul saat mode jiggle)
            if (jiggle) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(0.38f))
                    .border(1.dp, Color.White.copy(0.22f), RoundedCornerShape(24.dp)).padding(14.dp)) {
                    Text("Customize — Ikon Aplikasi", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ModePill("Default", mode == IconMode.DEFAULT) { onModeChange(IconMode.DEFAULT) }
                        ModePill("Dark", mode == IconMode.DARK) { onModeChange(IconMode.DARK) }
                        ModePill("Clear", mode == IconMode.CLEAR) { onModeChange(IconMode.CLEAR) }
                        ModePill("Satu Warna", mode == IconMode.TINTED) { onModeChange(IconMode.TINTED) }
                    }
                    if (mode == IconMode.TINTED) {
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            TINT_PRESETS.forEach { (c, _) ->
                                Box(Modifier.size(28.dp).clip(CircleShape).background(c)
                                    .border(if (c == tintColor) 2.5.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { onTintChange(c) })
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Liquid Glass", color = Color.White.copy(0.9f), fontSize = 11.sp)
                        Slider(value = glass, onValueChange = onGlassChange, modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = Color.White,
                                activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.35f)))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Titik halaman + pill Search (persis referensi: kaca pembesar satu warna)
            Row(Modifier.fillMaxWidth().padding(bottom = 9.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (jiggle) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(glassColor)
                        .clickable { jiggle = false }.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        Text("Selesai", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(glassColor)
                        .clickable { jiggle = false; onOpenSettings() }.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        Text("Pengaturan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    repeat(pages.size.coerceAtLeast(1)) { i ->
                        Box(Modifier.size(if (i == pagerState.currentPage.coerceAtMost(pages.size - 1)) 7.dp else 6.dp).clip(CircleShape)
                            .background(Color.White.copy(if (i == pagerState.currentPage.coerceAtMost(pages.size - 1)) 0.95f else 0.45f)))
                        Spacer(Modifier.width(6.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(Modifier.clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.20f + glass * 0.22f))
                        .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(50))
                        .clickable { onOpenSearch() }.padding(horizontal = 13.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        SearchGlyph(12, Color.White.copy(0.88f))
                        Spacer(Modifier.width(5.dp))
                        Text("Search", color = Color.White.copy(0.92f), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Dock Liquid Glass
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                .background(glassColor).border(1.dp, Color.White.copy(0.28f), RoundedCornerShape(30.dp))
                .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                dock.forEach { app ->
                    Box {
                        val kind = iosKindFor(app)
                        if (kind != null) Box(Modifier.iconEffect(mode, tintColor)) {
                            IosIcon(kind, 58, Modifier.clickable { AppRepository.launch(context, app.packageName) })
                        } else IconImage(app.icon, 58, mode, tintColor) { AppRepository.launch(context, app.packageName) }
                        BadgeBubble(badgeCounts[app.packageName] ?: 0, mode == IconMode.CLEAR,
                            Modifier.align(Alignment.TopEnd))
                    }
                }
                Box(Modifier.size(58.dp).clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF2E7FD9)).iconEffect(mode, tintColor).clickable { onOpenFreezer() },
                    contentAlignment = Alignment.Center) {
                    SnowGlyph(30)
                }
            }
        }
        // Strip atas terbagi ala iOS 27: kiri = Notifikasi, kanan = Control Center
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(54.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, drag ->
                    if (drag > 14f) {
                        if (change.position.x < size.width * 0.45f) onOpenNotifications() else onOpenControl()
                    }
                }
            })
    }
}

@Composable
private fun ModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50))
        .background(if (selected) Color.White else Color.White.copy(0.16f))
        .clickable { onClick() }.padding(horizontal = 11.dp, vertical = 6.dp)) {
        Text(label, color = if (selected) Color(0xFF1C1C1E) else Color.White,
            fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun XlChip(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(0.20f))
        .clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, color = Color.White, fontSize = 10.5.sp, maxLines = 1)
    }
}

/** Sel widget: kartu + label kecil di bawahnya (persis gaya widget iOS). */
@Composable
fun WidgetCell(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        Text(label, color = Color.White, fontSize = 10.sp,
            style = TextStyle(shadow = Shadow(Color.Black.copy(0.45f), Offset(0f, 1f), 3f)),
            modifier = Modifier.padding(top = 3.dp))
    }
}

/** Widget Cuaca biru ala referensi iOS 27 — data asli Open-Meteo; bila offline tampilkan jam. */
@Composable
fun WeatherWidget(weather: WeatherInfo?, now: Date) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF4A90D9), Color(0xFF2B5EA6))))
        .padding(12.dp)) {
        Column {
            Text("Jakarta", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (weather != null) {
                Text("${weather.temp}°", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
                Text(WeatherRepo.textFor(weather.code), color = Color.White.copy(0.92f), fontSize = 11.sp)
                Text("H:${weather.hi}°  L:${weather.lo}°", color = Color.White.copy(0.85f), fontSize = 11.sp)
            } else {
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                    color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light)
                Text(SimpleDateFormat("EEEE, d MMMM", Locale("id")).format(now),
                    color = Color.White.copy(0.9f), fontSize = 11.sp)
                Text("Memuat cuaca…", color = Color.White.copy(0.7f), fontSize = 10.sp)
            }
        }
    }
}

/** Widget Kalender sebulan putih ala referensi — hari ini dilingkari merah. */
@Composable
fun CalendarWidget() {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val first = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val startBlank = first.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells: List<Int?> = List(startBlank) { null } + (1..daysInMonth).map { it }
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)).background(Color.White)
        .padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(SimpleDateFormat("MMMM", Locale("id")).format(cal.time).uppercase(),
            color = Color(0xFFFF3B30), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "S", "R", "K", "J", "S", "M").forEach {
                Text(it, color = Color(0xFF8E8E93), fontSize = 7.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { d ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (d != null) {
                                if (d == today) {
                                    Box(Modifier.size(15.dp).clip(CircleShape).background(Color(0xFFFF3B30)),
                                        contentAlignment = Alignment.Center) {
                                        Text(d.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else Text(d.toString(), color = Color(0xFF1C1C1E), fontSize = 8.sp)
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Lencana ala iOS. Mode Clear: kehilangan warna merahnya (putih kaca, teks gelap) seperti iOS 27. */
@Composable
fun BadgeBubble(count: Int, clearMode: Boolean = false, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(modifier.offset(x = 5.dp, y = (-5).dp)
        .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
        .clip(CircleShape)
        .background(if (clearMode) Color.White.copy(0.88f) else Color(0xFFFF3B30))
        .border(1.2.dp, if (clearMode) Color.White.copy(0.6f) else Color.White, CircleShape)
        .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center) {
        Text(if (count > 99) "99+" else count.toString(),
            color = if (clearMode) Color(0xFF1C1C1E) else Color.White,
            fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Wallpaper cadangan ala iOS: mesh gradasi ungu-biru-pink lembut. */
@Composable
fun IosMeshWallpaper() {
    Box(Modifier.fillMaxSize().background(Color(0xFF1B2450))) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(Color(0xFF7E5BEF), Color.Transparent), center = Offset(150f, 250f), radius = 700f)))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(Color(0xFF3E9BDC), Color.Transparent), center = Offset(950f, 500f), radius = 800f)))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(Color(0xFFE85D9E), Color.Transparent), center = Offset(250f, 1500f), radius = 750f)))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(Color(0xFF2743A6), Color.Transparent), center = Offset(900f, 1900f), radius = 800f)))
    }
}

@Composable
fun GlassCard(glass: Float, modifier: Modifier = Modifier, colors: List<Color>? = null,
              content: @Composable ColumnScope.() -> Unit) {
    val bg = if (colors != null) Modifier.background(Brush.verticalGradient(colors))
        else Modifier.background(Color.White.copy(alpha = 0.14f + glass * 0.28f))
    Column(modifier.clip(RoundedCornerShape(22.dp)).then(bg)
        .border(1.dp, Color.White.copy(0.24f), RoundedCornerShape(22.dp))
        .padding(13.dp), content = content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(app: AppInfo, labelShadow: Shadow, jiggle: Boolean, mode: IconMode, tint: Color,
            showLabel: Boolean, badge: Int, onJiggle: () -> Unit, onFreeze: (String) -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val angle = if (jiggle) {
        val tr = rememberInfiniteTransition(label = "jiggle")
        val a by tr.animateFloat(-1.6f, 1.6f,
            infiniteRepeatable(tween(110, easing = LinearEasing), RepeatMode.Reverse), label = "angle")
        a
    } else 0f
    val shape = RoundedCornerShape(17.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.rotate(angle)) {
        Box {
            val kind = iosKindFor(app)
            val pane = if (mode == IconMode.CLEAR)
                Modifier.background(Color.White.copy(0.13f)).border(1.dp, Color.White.copy(0.28f), shape)
            else Modifier
            Box(Modifier.size(60.dp).clip(shape).then(pane)
                .combinedClickable(
                    onClick = { if (!jiggle) AppRepository.launch(context, app.packageName) },
                    onLongClick = { onJiggle() }),
                contentAlignment = Alignment.Center) {
                Box(Modifier.iconEffect(mode, tint)) {
                    if (kind != null) IosIcon(kind, 60)
                    else if (app.icon != null) Image(app.icon, null, Modifier.size(60.dp), contentScale = ContentScale.Crop)
                    else Box(Modifier.size(60.dp).background(Color.White.copy(0.2f)))
                }
            }
            BadgeBubble(badge, mode == IconMode.CLEAR, Modifier.align(Alignment.TopEnd))
            if (jiggle) {
                Box(Modifier.size(22.dp).align(Alignment.TopStart).offset((-5).dp, (-5).dp)
                    .clip(CircleShape).background(Color(0xFFD8D8DC))
                    .clickable { menu = true }, contentAlignment = Alignment.Center) {
                    Text("−", color = Color(0xFF333336), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Masukkan ke Freezer") },
                    onClick = { menu = false; onFreeze(app.packageName) })
                DropdownMenuItem(text = { Text("Info Aplikasi") },
                    onClick = { menu = false; AppRepository.openAppInfo(context, app.packageName) })
            }
        }
        if (showLabel) {
            Text(app.label, style = TextStyle(shadow = labelShadow), color = Color.White,
                fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(72.dp).padding(top = 3.dp))
        }
    }
}

@Composable
fun IconImage(icon: ImageBitmap?, sizeDp: Int, mode: IconMode, tint: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape((sizeDp * 0.29f).dp)
    val pane = if (mode == IconMode.CLEAR)
        Modifier.background(Color.White.copy(0.13f)).border(1.dp, Color.White.copy(0.28f), shape)
    else Modifier.background(Color.White.copy(0.15f))
    Box(Modifier.size(sizeDp.dp).clip(shape).then(pane).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Box(Modifier.iconEffect(mode, tint)) {
            if (icon != null) Image(icon, null, Modifier.size(sizeDp.dp), contentScale = ContentScale.Crop)
            else Text("◻", color = Color.White)
        }
    }
}

/**
 * Dynamic Island BARU (v0.11): TIDAK lagi menempel di tengah status bar (di situ ada kamera
 * depan Note 12 — kemarin tertutup kamera). Kini pil hitam melayang tepat di BAWAH status bar:
 * - Mode POPUP: muncul melebar ±4 detik setiap ada notifikasi baru (ikon app + judul + jumlah),
 *   lalu hilang sendiri — pengganti popup banner bawaan.
 * - Mode SELALU: pil ramping selalu tampil (titik merah + jumlah notifikasi).
 * - Mode MATI: tidak tampil.
 */
@Composable
fun IslandPopup(modeName: String, total: Int, latest: com.tedo.ios27launcher.badges.NotifItem?,
                apps: List<AppInfo>, iconMode: IconMode, tint: Color, fakeBar: Boolean,
                modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(false) }
    var lastKey by remember { mutableStateOf<String?>(null) }
    var primed by remember { mutableStateOf(false) }
    LaunchedEffect(latest?.key) {
        val k = latest?.key
        if (!primed) { primed = true; lastKey = k }
        else if (modeName == "POPUP" && k != null && k != lastKey) {
            lastKey = k; shown = true; delay(4200); shown = false
        }
    }
    val visible = when (modeName) { "ALWAYS" -> true; "POPUP" -> shown; else -> false }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "islandAlpha")
    if (alpha < 0.02f) return
    val app = latest?.let { n -> apps.firstOrNull { it.packageName == n.pkg } }
    Box(modifier.padding(top = if (fakeBar) 36.dp else 42.dp)
        .graphicsLayer { this.alpha = alpha }
        .clip(RoundedCornerShape(26.dp)).background(Color.Black)
        .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(26.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (modeName == "POPUP" && shown && latest != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = 300.dp)) {
                if (app != null) com.tedo.ios27launcher.ui.AppGlyph(app, 26, iconMode, tint) { }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    Text(app?.label ?: latest.pkg, color = Color.White.copy(0.65f), fontSize = 9.5.sp, maxLines = 1)
                    Text(latest.title.ifBlank { latest.text }, color = Color.White, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (total > 0) {
                    Spacer(Modifier.width(9.dp))
                    Box(Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp).clip(CircleShape)
                        .background(Color(0xFFFF3B30)).padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center) {
                        Text("$total", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.defaultMinSize(minWidth = 104.dp),
                horizontalArrangement = Arrangement.Center) {
                if (total > 0) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF3B30)))
                    Spacer(Modifier.width(6.dp))
                    Text("$total notifikasi", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Muat wallpaper sistem (wallpaper HP yang dipasang user) — v0.13 diperkuat 3 jalur:
 * drawable langsung, berkas wallpaper sistem (getWallpaperFile), lalu peekFastDrawable.
 * Gagal semua (umumnya karena izin Foto/Video belum diberikan) -> null -> mesh iOS.
 */
fun loadSystemWallpaper(context: Context): androidx.compose.ui.graphics.ImageBitmap? {
    val wm = WallpaperManager.getInstance(context)
    try { wm.drawable?.let { return it.toBitmap(540, 1200).asImageBitmap() } } catch (e: Exception) { }
    try {
        if (Build.VERSION.SDK_INT >= 24) {
            wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                android.graphics.BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)?.let {
                    return it.asImageBitmap() }
            }
        }
    } catch (e: Exception) { }
    try { wm.peekFastDrawable()?.let { return it.toBitmap(540, 1200).asImageBitmap() } } catch (e: Exception) { }
    return null
}
