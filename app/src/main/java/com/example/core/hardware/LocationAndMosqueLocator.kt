package com.example.core.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import com.example.core.i18n.AppLanguage
import com.example.domain.DailyPrayerSchedule
import com.example.domain.MosquePlace
import com.example.domain.PrayerTimesEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class DetectedLocationResult(
    val latitude: Double,
    val longitude: Double,
    val cityLabel: String,
    val countryLabel: String
)

object LocationAndMosqueLocator {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isLocationEnabled(context: Context): Boolean {
        val mgr = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching {
            mgr.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentLocation(context: Context): DetectedLocationResult? {
        val mgr = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val providers = buildList {
            if (runCatching { mgr.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (runCatching { mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                runCatching { mgr.isProviderEnabled(LocationManager.FUSED_PROVIDER) }.getOrDefault(false)
            ) {
                add(LocationManager.FUSED_PROVIDER)
            }
        }

        var fix: Location? = null

        // Try active single location update first (up to 7 seconds)
        for (provider in providers) {
            val candidate = withTimeoutOrNull(4500L) {
                requestSingleFix(context, mgr, provider)
            }
            if (candidate != null) {
                fix = candidate
                break
            }
        }

        // Fallback to freshest lastKnownLocation if GPS fix timed out indoors
        if (fix == null) {
            val allProviders = listOfNotNull(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            fix = allProviders.mapNotNull { p ->
                runCatching { mgr.getLastKnownLocation(p) }.getOrNull()
            }.maxByOrNull { it.time }
        }

        val resolved = fix ?: return null
        val (city, country) = reverseGeocodeCity(context, resolved.latitude, resolved.longitude)
        val label = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
            .ifBlank { "${String.format(Locale.US, "%.2f", resolved.latitude)}, ${String.format(Locale.US, "%.2f", resolved.longitude)}" }

        return DetectedLocationResult(
            latitude = resolved.latitude,
            longitude = resolved.longitude,
            cityLabel = label.take(80),
            countryLabel = country
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(
        context: Context,
        mgr: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            runCatching {
                mgr.getCurrentLocation(
                    provider,
                    signal,
                    context.mainExecutor
                ) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { mgr.removeUpdates(this) }
                    if (cont.isActive) cont.resume(location)
                }
                @Deprecated("Deprecated in Android API")
                override fun onStatusChanged(p: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }
            cont.invokeOnCancellation { runCatching { mgr.removeUpdates(listener) } }
            runCatching {
                mgr.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private suspend fun reverseGeocodeCity(
        context: Context,
        lat: Double,
        lng: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val addr = addresses?.firstOrNull()
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: ""
            val country = addr?.countryName ?: ""
            city to country
        }.getOrDefault("" to "")
    }

    suspend fun fetchNearbyMosques(
        latitude: Double,
        longitude: Double,
        cityLabel: String,
        schedule: DailyPrayerSchedule,
        language: AppLanguage,
        radiusMeters: Int = 5500
    ): List<MosquePlace> = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:12];
            (
              node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$latitude,$longitude);
              way["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$latitude,$longitude);
            );
            out center 20;
        """.trimIndent()

        val osmMosques = runCatching {
            val body = "data=${Uri.encode(query)}".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val req = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .header("User-Agent", "MihrabAndroidApp/1.0")
                .post(body)
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val raw = resp.body?.string() ?: return@use emptyList()
                parseOverpassMosques(raw, latitude, longitude, schedule, language)
            }
        }.getOrDefault(emptyList())

        if (osmMosques.isNotEmpty()) osmMosques
        else PrayerTimesEngine.getMosquesForCity(cityLabel, schedule, language)
    }

    private fun parseOverpassMosques(
        rawJson: String,
        userLat: Double,
        userLng: Double,
        schedule: DailyPrayerSchedule,
        language: AppLanguage
    ): List<MosquePlace> {
        val root = JSONObject(rawJson)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        fun addMin(min: Int, delta: Int) = PrayerTimesEngine.formatMinutes((min + delta) % 1440)

        data class RankedMosque(val distKm: Double, val place: MosquePlace)

        val list = mutableListOf<RankedMosque>()
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            val lat = el.optDouble("lat", Double.NaN).let {
                if (!it.isNaN()) it else el.optJSONObject("center")?.optDouble("lat", Double.NaN) ?: Double.NaN
            }
            val lng = el.optDouble("lon", Double.NaN).let {
                if (!it.isNaN()) it else el.optJSONObject("center")?.optDouble("lon", Double.NaN) ?: Double.NaN
            }
            if (lat.isNaN() || lng.isNaN()) continue

            val tags = el.optJSONObject("tags")
            val name = when (language) {
                AppLanguage.ARABIC, AppLanguage.URDU ->
                    tags?.optString("name:ar")?.takeIf { it.isNotBlank() }
                        ?: tags?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: "مسجد قريب"
                AppLanguage.UZBEK ->
                    tags?.optString("name:uz")?.takeIf { it.isNotBlank() }
                        ?: tags?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: "Yaqin masjid"
                else ->
                    tags?.optString("name:en")?.takeIf { it.isNotBlank() }
                        ?: tags?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: "Nearby Masjid"
            }

            val street = tags?.optString("addr:street")?.takeIf { it.isNotBlank() }
            val cityTag = tags?.optString("addr:city")?.takeIf { it.isNotBlank() }
            val address = listOfNotNull(street, cityTag).joinToString(", ")

            val distKm = haversineKm(userLat, userLng, lat, lng)
            val distLabel = if (distKm < 1.0) {
                val meters = (distKm * 1000).roundToInt()
                language.pick("$meters م", "$meters m", "$meters m")
            } else {
                val formatted = String.format(Locale.US, "%.1f", distKm)
                language.pick("$formatted كم", "$formatted km", "$formatted km")
            }

            val facilities = buildList {
                add(language.pick("موقع فعلي عبر GPS", "Verified GPS Map", "Haqiqiy GPS xarita"))
                if (tags?.optString("wheelchair") == "yes") {
                    add(language.pick("مدخل ميسّر", "Wheelchair Access", "Aravacha kirishi"))
                }
                if (tags?.optString("female_prayer") == "yes" || tags?.optString("women") == "yes") {
                    add(language.pick("مصلى نساء", "Women's Section", "Ayollar bo'limi"))
                }
            }

            list.add(
                RankedMosque(
                    distKm = distKm,
                    place = MosquePlace(
                        name = name,
                        distanceLabel = distLabel,
                        iqamahFajr = addMin(schedule.fajr.timeMinutes, 20),
                        iqamahDhuhr = addMin(schedule.dhuhr.timeMinutes, 15),
                        iqamahAsr = addMin(schedule.asr.timeMinutes, 15),
                        iqamahMaghrib = addMin(schedule.maghrib.timeMinutes, 7),
                        iqamahIsha = addMin(schedule.isha.timeMinutes, 15),
                        jummahKhutbah = "1:15 PM",
                        facilities = facilities,
                        latitude = lat,
                        longitude = lng,
                        isRealOsmData = true,
                        address = address
                    )
                )
            )
        }
        return list.sortedBy { it.distKm }.take(12).map { it.place }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun openMosqueDirections(context: Context, mosque: MosquePlace) {
        val uri = if (mosque.latitude != null && mosque.longitude != null) {
            Uri.parse("geo:${mosque.latitude},${mosque.longitude}?q=${mosque.latitude},${mosque.longitude}(${Uri.encode(mosque.name)})")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(mosque.name)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
