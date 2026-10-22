# iOS27Launcher — aturan R8 tambahan (audit v0.12).
# Komponen yang dirujuk AndroidManifest (MainActivity, BadgeListener) otomatis dipertahankan R8.
# Refleksi hanya dipakai ke kelas SISTEM (StatusBarManager), bukan kelas app — aman.
