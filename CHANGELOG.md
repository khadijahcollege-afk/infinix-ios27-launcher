# Changelog & Development Log — iOS 27 Launcher

(Catatan pengembangan lengkap v0.1 -> v0.14. README publik ada di README.md.)

# iOS 27 Launcher + Freezer — untuk Infinix Note 12 (Tedo) — v0.2

Launcher Android gaya iOS 27 dengan fitur **Freezer dipertahankan**, dibuat khusus
menjawab kendala XOS Launcher: tampilannya tidak bisa diubah jadi iOS, tapi Freezer-nya dibutuhkan.

## v0.2 — TANPA Shizuku (permintaan Tedo), sesudah mempelajari Freezer asli XOS

Hasil riset: Freezer bawaan Infinix itu sebenarnya fitur **"nonaktifkan aplikasi" bawaan
Android** itu sendiri. XManager/XOS bisa "turn off nonsystem apps (the same feature as
the app's freezer)" karena mereka app SISTEM bertanda tangan Transsion. Semua app freezer
pihak ketiga di Play Store pun butuh root / device-owner untuk menekan saklar itu sendiri.

Jadi v0.2 memakai cara resmi tanpa app tambahan:
1. **Tombol "Buka Freezer Bawaan Infinix"** — Freezer aslinya tetap ada di sistem HP walau
   launcher default diganti; launcher ini membuka XManager/XOS tempat Freezer tinggal.
2. **Freezer internal**: app disembunyikan dari home + 1 ketuk mengantar ke halaman Info
   Aplikasi untuk menekan **Nonaktifkan** — saklar sistem yang sama persis. Status beku
   dibaca dari sistem, jadi app yang dibekukan XOS Freezer otomatis tampil di daftar beku sini.

Tampilan v0.2 juga disesuaikan lebih dekat ke iOS 27: wallpaper HP asli sebagai latar,
widget jam + kartu Liquid Glass, titik halaman + pill Search, App Library, dan Spotlight.

## Fitur gaya iOS 27 yang sudah ada (v0.1)

- Home Screen tanpa app drawer, grid ikon squircle + label, widget jam/tanggal
- Dock **Liquid Glass** tembus pandang di bawah
- **Slider Liquid Glass** seperti iOS 27 (Settings > Appearance): Ultra Clear <-> Fully Tinted
  — tekan lama area kosong di home untuk membukanya
- Halaman **Freezer** bergaya screenshot XOS kamu: header, hitungan app beku + MB bebas, grid, tombol **+ Add**
- Tekan lama label app -> "Masukkan ke Freezer"
- Ketuk app di Freezer -> dicairkan + langsung dibuka

Belum ada di v0.1 (roadmap): App Library di halaman terakhir, Spotlight swipe-down,
Control Center tiruan, widget stack, ganti wallpaper dari galeri, jiggle animation penuh.

## Cara build & install di Note 12 kamu

1. Di PC Windows kamu: install **Android Studio** (gratis), buka folder project ini (`File > Open`).
2. Tunggu Gradle sync selesai, lalu `Build > Build APK(s)`.
   APK jadi di `app/build/outputs/apk/debug/app-debug.apk`.
3. Kirim APK ke HP, install (izinkan "install dari sumber tidak dikenal" untuk app pengirim file).
4. Install **Shizuku** dari Play Store (gratis), buka > *Start via Wireless debugging*:
   - Aktifkan Opsi Pengembang (Ketuk Nomor Versi 7x di Tentang Ponsel)
   - Aktifkan *Wireless debugging* > *Pair device with pairing code*, masukkan kode ke notifikasi Shizuku
   - Kembali ke Shizuku > *Start*
5. Buka iOS 27 Launcher, set sebagai **launcher default** (Pengaturan > Aplikasi > Aplikasi default > Aplikasi Beranda).
6. Buka Freezer (ikon ❄ biru di Dock) — izinkan permintaan Shizuku. Status harus "Mode: Shizuku ✓".
7. Tekan **+**, pilih app yang mau dibekukan.

Balik ke tampilan awal kapan saja: ganti lagi Aplikasi Beranda ke **XOS Launcher** — tidak ada yang rusak permanen.

## Catatan penting

- Jangan bekukan: Telepon, Pesan, WhatsApp utama, keyboard, Google Play Services, launcher ini sendiri.
- Shizuku berhenti tiap HP restart — buka Shizuku dan Start lagi (30 detik, tanpa PC).
- Project ini prototipe v0.1 dari Lotus. Status per 22 Okt 2026: **SUDAH BERHASIL DI-COMPILE**
  oleh Lotus (JDK 17 + Android SDK 35 + Gradle 8.9) menjadi `iOS27Launcher.apk` (22 MB,
  package `com.tedo.ios27launcher`, minSdk 29, signature terverifikasi). APK siap install —
  langkah 1-2 di atas hanya perlu kalau kamu mau build ulang sendiri.

## v0.5 (2026-10-22)
- Grid home 20 app/halaman (5 baris) — perbaiki ikon terbawah yang terpotong/gepeng di v0.4.
- Ikon Satu Warna (mode Tinted iOS 27) untuk SEMUA app: glyph gambar sendiri + duotone GPU untuk ikon asli. Warna tint (Biru/Ungu/Abu/Hijau/Pink) & mode Berwarna Asli diatur di Control Center.
- Glyph baru: WhatsApp, Instagram, Facebook, YouTube, Telegram, TikTok, App Store (untuk Play Store), Kompas, File; Gmail -> Mail.
- Titik ◐ melayang dihapus. Control Center: geser ke bawah dari tepi paling atas layar, atau pill "Control".
- Smart Stack widget: Jam+Freezer <-> Baterai asli + Penyimpanan kosong asli (geser).
- Status/nav bar: isNavigationBarContrastEnforced/isStatusBarContrastEnforced = false.
- Freezer: UI kaca menyatu launcher; kartu Freezer Bawaan + daftar "Sedang Dibekukan" + daftar pilih baris dengan tombol Bekukan.
- Performa: animasi jiggle hanya saat aktif; ikon dimuat 128px.
- Pengaturan baru di Control Center: Label ikon tampil/sembunyi, Wallpaper HP / Mesh iOS.

## v0.6 (2026-10-22) — diselaraskan dgn referensi resmi iOS 27
- Widget Cuaca biru berisi cuaca ASLI Open-Meteo (Jakarta: suhu, kondisi, H/L) + widget Kalender sebulan putih, tanggal hari ini dilingkari merah; label kecil di bawah widget seperti iOS.
- Lencana notifikasi angka merah di ikon home & dock (NotificationListenerService; aktifkan di Control Center › Tampilan › Lencana Notifikasi).
- Halaman pintu App Library: geser melewati halaman terakhir membuka Library (persis iOS); geser ke atas juga membuka Library.
- Baris titik: titik + pill Search saja (seperti referensi).
- Ikon default BERWARNA seperti referensi; mode Satu Warna tetap opsi di Control Center.
- Glyph baru: FaceTime, Wallet (OVO/DANA/GoPay/dompet), Health, Reminders, Podcasts + kilau gloss Liquid Glass di semua glyph.
- Stack halaman 2: Freezer + Perangkat (baterai % & penyimpanan kosong asli).
- Izin baru: INTERNET (widget cuaca). Siri AI & UI dalam-app iOS tidak bisa ditiru launcher.

## v0.7 (2026-10-22)
- Tombol Search persis referensi: pil kaca lembut + kaca pembesar SATU WARNA digambar Canvas + teks "Search" (emoji 🔍 dihapus, termasuk placeholder layar Search).
- Ikon aplikasi: default kembali SATU WARNA (permintaan Tedo); Berwarna Asli tetap opsi di Control Center.
- Status bar gaya iOS digambar launcher sendiri (FakeStatusBar): jam asli kiri; batang sinyal (level Wi-Fi asli), kipas Wi-Fi, angka + pil baterai asli di kanan; TANPA ikon notifikasi aplikasi seperti iPhone. Bilah XOS disembunyikan via WindowInsetsController (transient by swipe) hanya selama launcher tampil. Toggle di Control Center › Tampilan › Status bar: Gaya iOS / Bawaan XOS. Izin baru: ACCESS_WIFI_STATE.
- Geser ke bawah pada status bar gambar sendiri (di home) tetap membuka Control Center.

## v0.8.0 (2026-10-22)
Semua temuan riset iOS 27 yang bisa diterapkan launcher:
- 4 mode ikon resmi: Default / Dark / Clear (kaca transparan, glyph putih, panel kaca) / Tinted vivid (format iOS 27: latar warna + glyph terang) via RenderEffect; panel Edit > Customize muncul saat mode jiggle (mode + warna tint + slider Liquid Glass).
- Widget XL (Smart Stack halaman 3): cuaca Jakarta besar + jam + chip Freezer/baterai/penyimpanan/Library.
- Gestur atas terbagi ala iOS 27: kiri atas = panel Notifikasi sistem, kanan atas = Control Center; geser bawah di layar = "Search or Ask" (gelembung kaca hitam, layar pencarian direstil).
- Dynamic Island di status bar gambar sendiri: pil hitam + jumlah notifikasi belum terbaca (bisa dimatikan di CC).
- Mode Clear: lencana kehilangan merahnya (putih kaca, teks gelap) seperti iOS 27.
- Control Center: pintasan Pengaturan Suara, Wallpaper, Baterai, Aksesibilitas.
- APK: 22.814.826 byte, sha256 c52b6120…, signature v2 terverifikasi.

## v0.9.0 (2026-10-22)
Atas masukan Tedo dari screenshot XOS (panel QS, shade notifikasi, Desktop Settings XOS):
- Notification Center milik launcher: isi notifikasi asli per app (judul/teks/waktu relatif), ketuk membuka app, tombol ✕ per item + "Hapus Semua" (lewat NotificationListenerService). Gestur kiri atas & geser bawah layar utama membukanya (pilihan Panel Notifikasi: Launcher/XOS di Pengaturan; gestur geser bawah bisa dialihkan ke Search or Ask).
- Layar Pengaturan Launcher lengkap: Launcher Bawaan (ACTION_HOME_SETTINGS), Tampilan (semua kontrol), Notifikasi (panel, lencana master on/off, status izin), Gestur, Freezer, Tentang. Dibuka dari Control Center / panel Customize.
- Ikon digambar ala SF Symbols: semua tombol Control Center (sebelumnya emoji 📶🔦☾🔒☀🔊🖼🔋♿✈ᛒ) kini Canvas CcGlyph; kepingan salju Dock/Freezer = SnowGlyph; emoji di widget/chip dibersihkan.
- Glyph app baru: Discord, Spotify (khusus, sebelumnya Music), X, Netflix, Shopee, myBCA (teks BCA). Photos/Maps/Clock sudah ada sebelumnya.
- APK: 22.995.050 byte, sha256 83cfeb14…, signature terverifikasi.

## v0.10.0 (2026-10-22)
Masukan Tedo dari screenshot v0.9 (mode Clear):
- Control Center SATU WARNA: semua tombol memakai warna tint pilihan (biru) dengan glyph putih; Senter menyala ditandai cincin putih, bukan warna lain.
- Widget atas kini berwarna seperti Cuaca (biru) & Kalender (putih): Freezer = gradasi biru es, Perangkat/Baterai = gradasi hijau, XL = gradasi indigo + baris info lokasi "Jakarta, Indonesia" dengan PinGlyph tergambar.
- Chip XL dirampingkan (label pendek, padding kecil) — tidak ada lagi teks patah "App Li…".
- APK: 22.995.054 byte, sha256 355e72a6…, signature terverifikasi.

## v0.11.0 (2026-10-22)
Masukan Tedo: Freezer kurang bagus + Dynamic Island tertutup kamera depan Note 12 (punch-hole di tengah atas).
- Dynamic Island PINDAH: tidak lagi di tengah status bar. Kini IslandPopup melayang di bawah status bar dengan 3 mode (pref islandMode): POPUP (default — melebar ±4 detik saat notifikasi baru: ikon app + judul + jumlah, lalu hilang), ALWAYS (pil ramping), OFF. FakeStatusBar tengah dikosongkan untuk kamera.
- Freezer didesain ulang gaya daftar grup iOS: header ikon biru es + ringkasan, kartu baris "Freezer Bawaan Infinix" (ketuk, chevron), baris app beku dengan tombol Cairkan, daftar bekukan dengan tombol Bekukan per baris — tanpa status picking/tombol melayang.
- Pengaturan: mode Island 3 pilihan + baris "Matikan popup banner bawaan XOS" (membuka Pengaturan Notifikasi Android; launcher tidak bisa mematikannya langsung — dinyatakan di UI).
- APK: 23.027.822 byte, sha256 e292514c…, signature terverifikasi.

## v0.12.0 (2026-10-22) — HASIL AUDIT MENYELURUH
Diminta Tedo: audit semua fitur + performa/baterai.
PERFORMA (diperbaiki):
1. APK kini build RELEASE (R8 minify + shrinkResources): 23 MB -> 2.2 MB, jalan lebih cepat.
   Ditandatangani keystore yang sama dengan build debug lama (keystore kini disimpan di
   folder proyek: debug.keystore) -> bisa ditimpa tanpa uninstall (sertifikat identik terverifikasi).
2. Muat ulang daftar app: dulu SETIAP onResume memindai semua paket. Kini throttle 120 dtk,
   kecuali invalidate() dari aksi Freezer.
3. iconEffect di-memo per (mode, warna) — tidak dibuat ulang tiap rekomposisi.
4. Cuaca: cache kini kedaluwarsa 30 menit (dulu basi seharian selama proses hidup).
FITUR (diperbaiki):
5. Dock dipilih per JENIS ikon (PHONE/MESSAGES/SAFARI) — ikon Telepon tidak hilang lagi
   gara-gara nama paket dialer Infinix berbeda.
6. Notification Center: ketuk membuka NOTIFIKASINYA langsung (PendingIntent asli disimpan).
KEKURANGAN YANG DICATAT JUJUR (belum dibangun): folder app di home; widget dari app lain
(AppWidgetHost); popup Island hanya muncul saat launcher di depan (overlay di atas app lain
butuh izin khusus); sinyal di status bar dari Wi-Fi (bukan seluler); lokasi cuaca tetap Jakarta.
BATERAI: tidak ada foreground service/wakelock/kerja berkala; listener notifikasi digerakkan
sistem; timer status bar 15 dtk hanya saat layar launcher tampil.
APK: 2.214.727 byte, sha256 673d018d…, sertifikat sama dgn v0.11 (21a5e3d9…).

## v0.13.0 (2026-10-22)
Tiga permintaan Tedo (sangat penting: notifikasi harus tembus ke luar launcher):
1. ISLAND MELAYANG DI ATAS APP LAIN: BadgeListener kini menggambar pil Island via WindowManager
   (TYPE_APPLICATION_OVERLAY, NOT_TOUCHABLE) saat notifikasi baru masuk — ikon app + nama +
   judul + lencana total, ±4,2 dtk. Mode ALWAYS = pil ramping persisten; saat launcher di depan
   overlay dilewati (BadgeStore.launcherForeground) agar tidak dobel. Wajib izin overlay:
   deklarasi SYSTEM_ALERT_WINDOW ditambahkan; pengaktifan lewat Pengaturan > Notifikasi.
2. STATUS BAR MENYATU: background kini digambar LauncherRoot FULL-SCREEN (wallpaper/mesh +
   lapisan gelap per layar); HomeScreen & SecondaryBg tidak menggambar latar sendiri lagi —
   tidak ada lagi pita gelap 30dp di belakang status bar.
3. WALLPAPER HP TERBACA: loadSystemWallpaper 3 jalur (drawable, getWallpaperFile FLAG_SYSTEM,
   peekFastDrawable) + izin runtime READ_MEDIA_IMAGES diminta sekali (manifest + maxSdk 32
   READ_EXTERNAL_STORAGE). Bila ditolak tetap mesh iOS.
APK: 2.231.227 byte, sha256 b5e286b5…, sertifikat sama (21a5e3d9…).

## v0.14.0 (2026-10-22)
Permintaan Tedo: wallpaper masih belum berubah + pengaturan gestur seperti launcher referensi
(screenshot "Gestures": slide up/down, 2 jari, pinch, rotate) disesuaikan iOS 27 + lebih smooth.
1. WALLPAPER GALERI SENDIRI: Pengaturan > Tampilan > "Pilih wallpaper dari galeri" menyimpan
   foto pilihan user ke filesDir/wallpaper.jpg (maks lebar 1080, JPEG 88) — prioritas tertinggi
   di root, dimuat ASYNC (Dispatchers.IO). Ada juga "Hapus wallpaper galeri". Wallpaper sistem
   tetap dicoba bila tidak ada foto galeri. (Sistem XOS terbukti tidak selalu membagikan
   wallpaper sistemnya ke app lain — jalur galeri ini tidak bergantung XOS.)
2. GESTUR BISA DIPETAKAN (8): SWIPE_UP(1 jari)=LIBRARY/SEARCH/NONE; swipeDown existing +
   opsi NONE; TWO_UP=FREEZER/LIBRARY/NONE; TWO_DOWN=VOICE/SEARCH/NONE; PINCH_IN=EDIT/SETTINGS/
   NONE; PINCH_OUT=NONE/LIBRARY/SETTINGS; ROT_CW & ROT_CCW=NONE/CONTROL/FREEZER. UI: daftar
   GestureRowSet di Pengaturan > Gestur. Detektor 2 jari baru di HomeScreen (awaitPointerEvent
   Pass.Initial, ambang cubit 55dp, putar 32°, geser 70dp, sekali tembak per gestur).
   VOICE = intent android.speech.action.RECOGNIZE_SPEECH, fallback Search.
3. SMOOTH: pager ikon & stack pakai beyondViewportPageCount=1 (preload), transisi antar layar
   fade 200ms (Animatable+graphicsLayer), wallpaper tidak lagi memblokir thread utama.
APK: 2.247.611 byte, sha256 53e7d8b0…, sertifikat sama (21a5e3d9…).
Catatan: build pertama gagal (smart cast wallpaper + PickVisualMediaRequest tak tersedia di
versi activity proyek) -> diperbaiki: wallpaper?.let{} dan picker GetContent("image/*").
