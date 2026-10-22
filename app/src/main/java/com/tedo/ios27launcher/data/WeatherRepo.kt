package com.tedo.ios27launcher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherInfo(val temp: Int, val code: Int, val hi: Int, val lo: Int)

/** Cuaca asli untuk widget ala iOS — Open-Meteo (gratis, tanpa API key). Default: Jakarta. */
object WeatherRepo {
    @Volatile var cached: WeatherInfo? = null
    @Volatile private var fetchedAtMs: Long = 0

    fun fetchJakarta(): WeatherInfo? {
        // Audit v0.12: cache lama tidak pernah kedaluwarsa (suhu basi seharian).
        // Kini data >30 menit diambil ulang; bila gagal, tampilkan cache lama.
        cached?.let { if (System.currentTimeMillis() - fetchedAtMs < 30 * 60_000) return it }
        return try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=-6.2088&longitude=106.8456" +
                "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=Asia%2FJakarta")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000; conn.readTimeout = 6000
            val text = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(text)
            val cur = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            WeatherInfo(
                temp = cur.getDouble("temperature_2m").toInt(),
                code = cur.getInt("weather_code"),
                hi = daily.getJSONArray("temperature_2m_max").getDouble(0).toInt(),
                lo = daily.getJSONArray("temperature_2m_min").getDouble(0).toInt()
            ).also { cached = it; fetchedAtMs = System.currentTimeMillis() }
        } catch (e: Exception) { cached }
    }

    fun textFor(code: Int): String = when (code) {
        0 -> "Cerah"; 1 -> "Cerah Berawan"; 2 -> "Berawan Sebagian"; 3 -> "Berawan"
        45, 48 -> "Berkabut"; 51, 53, 55 -> "Gerimis"; 56, 57 -> "Gerimis Beku"
        61, 63, 65 -> "Hujan"; 66, 67 -> "Hujan Beku"; 71, 73, 75, 77 -> "Salju"
        80, 81, 82 -> "Hujan Lokal"; 85, 86 -> "Salju Lokal"
        95 -> "Badai Petir"; 96, 99 -> "Badai + Hujan Es"; else -> "—"
    }
}
