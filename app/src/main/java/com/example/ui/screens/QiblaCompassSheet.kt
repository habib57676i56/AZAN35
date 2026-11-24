package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.hardware.LocationAndMosqueLocator
import com.example.core.hardware.observeCompassAzimuth
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.formatNumerals
import com.example.domain.CityPreset
import com.example.domain.PrayerTimesEngine
import com.example.ui.icons.MihrabIcons
import com.example.ui.theme.IslamicGeometricPatternCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalLiquidGlassPalette
import com.example.ui.theme.SanctuaryEmerald
import com.example.ui.theme.withTabularNums
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QiblaAndLocationDialogContent(
    currentCity: String,
    qiblaBearing: Double,
    distanceKm: Int,
    isArabicUi: Boolean = true,
    useIndic: Boolean = true,
    arQiblaEnabled: Boolean = true,
    appLanguage: AppLanguage = if (isArabicUi) AppLanguage.ARABIC else AppLanguage.ENGLISH,
    onSelectCity: (CityPreset) -> Unit,
    onGpsLocation: (String, Double, Double) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalLiquidGlassPalette.current
    val useArabicDigits = useIndic && appLanguage.isArabicUi
    val azimuth by remember(context) { observeCompassAzimuth(context) }
        .collectAsStateWithLifecycle(initialValue = 0f)

    var arModeEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(arQiblaEnabled) {
        if (!arQiblaEnabled) arModeEnabled = false
    }
    var permissionNotice by remember { mutableStateOf<String?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var gpsDisabledHint by remember { mutableStateOf(false) }

    fun triggerGpsFix() {
        if (isLocating) return
        scope.launch {
            isLocating = true
            gpsDisabledHint = false
            if (!LocationAndMosqueLocator.isLocationEnabled(context)) {
                isLocating = false
                gpsDisabledHint = true
                permissionNotice = appLanguage.pick(
                    "خدمة الموقع (GPS) متوقفة في الهاتف؛ شغّلها أولاً ثم أعد المحاولة.",
                    "Device Location (GPS) is disabled; please turn it on first.",
                    "Qurilma joylashuvi (GPS) o'chirilgan; avval uni yoqing."
                )
                return@launch
            }
            val detected = LocationAndMosqueLocator.detectCurrentLocation(context)
            isLocating = false
            if (detected != null) {
                onGpsLocation(detected.cityLabel, detected.latitude, detected.longitude)
                permissionNotice = appLanguage.pick(
                    "تم تحديد الموقع: ${detected.cityLabel} وتحديث القبلة وأوقات الصلاة.",
                    "Located ${detected.cityLabel} — Qibla & prayer times updated.",
                    "Manzil aniqlandi: ${detected.cityLabel} — Qibla va namoz vaqtlari yangilandi."
                )
            } else {
                permissionNotice = appLanguage.pick(
                    "تعذر التقاط إشارة GPS؛ جرّب في مكان مفتوح أو اختر مدينتك أدناه.",
                    "GPS signal unavailable right now; try outdoors or pick a city below.",
                    "Hozircha GPS signali topilmadi; ochiq joyda urinib ko'ring yoki shaharni tanlang."
                )
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            arModeEnabled = true
            permissionNotice = null
        } else {
            arModeEnabled = false
            permissionNotice = appLanguage.pick(
                "إذن الكاميرا اختياري؛ يتم استخدام البوصلة المغناطيسية.",
                "Camera permission is optional; using magnetic compass mode.",
                "Kamera ruxsati ixtiyoriy; magnit kompas ishlatilmoqda."
            )
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            triggerGpsFix()
        } else {
            permissionNotice = appLanguage.pick(
                "تم رفض إذن الموقع؛ اختر مدينتك من القائمة أدناه.",
                "Location permission denied; select your city preset below.",
                "Joylashuv ruxsati berilmadi; quyidan shahringizni tanlang."
            )
        }
    }

    val relativeAngle = ((qiblaBearing.toFloat() - azimuth) + 360f) % 360f
    val angularDiff = minOf(relativeAngle, 360f - relativeAngle)
    val isAligned = angularDiff <= 6f

    val animatedAzimuth by animateFloatAsState(
        targetValue = azimuth,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "azimuth"
    )
    val animatedNeedle by animateFloatAsState(
        targetValue = relativeAngle,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "needle"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, glass.glassBorder),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(MihrabIcons.KaabaCube, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column {
                    Text(
                        appLanguage.pick("القبلة والواقع المعزز", "Qibla & AR Sighting", "Qibla va AR kompas"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appLanguage.pick(
                            "$currentCity • ${formatNumerals("${qiblaBearing.roundToInt()}°", useArabicDigits)} من الشمال • ${formatNumerals("$distanceKm", useArabicDigits)} كم إلى مكة",
                            "$currentCity • ${qiblaBearing.roundToInt()}° from North • $distanceKm km to Makkah",
                            "$currentCity • Shimoldan ${qiblaBearing.roundToInt()}° • Makkagacha $distanceKm km"
                        ),
                        style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.testTag("close_qibla_button")) {
                Icon(Icons.Rounded.Close, contentDescription = appLanguage.pick("إغلاق", "Close Qibla", "Yopish"))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassPill(
                selected = !arModeEnabled,
                onClick = { arModeEnabled = false },
                label = appLanguage.pick("البوصلة", "Compass", "Kompas"),
                icon = Icons.Rounded.Explore
            )
            if (arQiblaEnabled) {
                LiquidGlassPill(
                    selected = arModeEnabled,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            arModeEnabled = true
                        } else {
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    label = appLanguage.pick("الواقع المعزز AR", "AR Camera", "AR Kamera"),
                    icon = Icons.Rounded.CameraAlt,
                    modifier = Modifier.testTag("ar_qibla_toggle")
                )
            }
        }

        if (arModeEnabled && arQiblaEnabled) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(
                    width = if (isAligned) 2.dp else 1.dp,
                    color = if (isAligned) glass.accentGold else glass.glassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(274.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ArCameraPreview(modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = if (isAligned) SanctuaryEmerald.copy(alpha = 0.9f) else Color(0xA6081411),
                                border = BorderStroke(1.dp, if (isAligned) glass.accentGold else Color.White.copy(alpha = 0.25f)),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = if (isAligned) {
                                        appLanguage.pick(
                                            "أنت متجه نحو الكعبة المشرفة",
                                            "Facing Qibla • Kaaba Direction",
                                            "Siz Ka'ba tomoniga yuzlandingiz"
                                        )
                                    } else {
                                        appLanguage.pick(
                                            "استدر ${if (relativeAngle < 180f) "يميناً" else "يساراً"} ${formatNumerals("${angularDiff.roundToInt()}°", useArabicDigits)}",
                                            "Turn ${if (relativeAngle < 180f) "Right" else "Left"} ${angularDiff.roundToInt()}°",
                                            "${if (relativeAngle < 180f) "O'ngga" else "Chapga"} ${angularDiff.roundToInt()}° buriling"
                                        )
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .size(98.dp)
                                    .clip(CircleShape)
                                    .background(if (isAligned) glass.accentGold.copy(alpha = 0.30f) else Color(0x44000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = MihrabIcons.KaabaCube,
                                    contentDescription = "AR Qibla Marker",
                                    tint = if (isAligned) glass.accentGold else Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outline
            val dialSurface = glass.glassFillStrong
            val goldCol = glass.accentGold
            Box(
                modifier = Modifier
                    .size(244.dp)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                IslamicGeometricPatternCanvas(
                    modifier = Modifier
                        .size(204.dp)
                        .clip(CircleShape),
                    tint = if (isAligned) goldCol else primaryColor,
                    alpha = 0.055f
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 10f

                    if (isAligned) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(goldCol.copy(alpha = 0.28f), Color.Transparent),
                                center = center,
                                radius = radius * 1.15f
                            ),
                            radius = radius * 1.15f,
                            center = center
                        )
                    }

                    drawCircle(color = dialSurface, radius = radius, center = center)
                    drawCircle(
                        color = if (isAligned) goldCol else outlineColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = if (isAligned) 3.5.dp.toPx() else 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.35f),
                        radius = radius * 0.72f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    rotate(degrees = -animatedAzimuth, pivot = center) {
                        for (deg in 0 until 360 step 15) {
                            rotate(degrees = deg.toFloat(), pivot = center) {
                                val isMajor = deg % 90 == 0
                                val isMedium = deg % 30 == 0
                                drawLine(
                                    color = when {
                                        deg == 0 -> Color(0xFFD9534F)
                                        isMajor -> primaryColor
                                        else -> outlineColor.copy(alpha = 0.65f)
                                    },
                                    start = Offset(center.x, center.y - radius + 6f),
                                    end = Offset(
                                        center.x,
                                        center.y - radius + (if (isMajor) 24f else if (isMedium) 18f else 12f)
                                    ),
                                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    rotate(degrees = animatedNeedle, pivot = center) {
                        val needle = Path().apply {
                            moveTo(center.x, center.y - radius + 28f)
                            lineTo(center.x - 16f, center.y + 18f)
                            lineTo(center.x, center.y + 6f)
                            lineTo(center.x + 16f, center.y + 18f)
                            close()
                        }
                        drawPath(needle, color = if (isAligned) goldCol else primaryColor)
                    }
                    drawCircle(color = goldCol, radius = 9.dp.toPx(), center = center)
                    drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = center)
                }
            }
        }

        LiquidGlassCard(
            containerColor = if (isAligned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else glass.glassFill,
            borderColor = if (isAligned) glass.accentGold.copy(alpha = 0.6f) else glass.glassBorder,
            contentPadding = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isAligned) {
                    appLanguage.pick(
                        "اتجاه القبلة مطابق الآن!",
                        "Aligned with the Qibla!",
                        "Qibla yo'nalishi mos keldi!"
                    )
                } else {
                    appLanguage.pick(
                        "الاتجاه ${formatNumerals("${azimuth.roundToInt()}°", useArabicDigits)} • القبلة ${formatNumerals("${qiblaBearing.roundToInt()}°", useArabicDigits)}",
                        "Heading ${azimuth.roundToInt()}° • Qibla ${qiblaBearing.roundToInt()}°",
                        "Yo'nalish ${azimuth.roundToInt()}° • Qibla ${qiblaBearing.roundToInt()}°"
                    )
                },
                style = MaterialTheme.typography.titleMedium.withTabularNums(),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = appLanguage.pick(
                    "مناسب للمنازل والفنادق والمطارات والسفر. أبقِ الهاتف بعيداً عن المعادن المغناطيسية.",
                    "Suitable for hotel rooms, airports, and outdoor travel. Keep away from strong magnets.",
                    "Uy, mehmonxona, aeroport va safar uchun qulay. Telefonni kuchli magnitlardan uzoqroq tuting."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (permissionNotice != null) {
            Text(
                text = permissionNotice!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (gpsDisabledHint) {
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appLanguage.pick("فتح إعدادات الموقع (GPS)", "Open Device Location Settings", "Qurilma GPS sozlamalarini ochish"))
            }
        }

        OutlinedButton(
            onClick = {
                val fineOk = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseOk = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (fineOk || coarseOk) {
                    triggerGpsFix()
                } else {
                    locationLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            },
            enabled = !isLocating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLocating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.MyLocation, null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isLocating) {
                    appLanguage.pick("جارٍ تحديد الموقع...", "Detecting GPS...", "Joylashuv aniqlanmoqda...")
                } else {
                    appLanguage.pick("تحديد موقعي عبر GPS", "Detect My GPS Location", "GPS orqali joylashuvni aniqlash")
                }
            )
        }

        Text(
            appLanguage.pick("مدن سريعة الاختيار", "Quick City Presets", "Tezkor shahar tanlash"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PrayerTimesEngine.cityPresets.forEach { preset ->
                LiquidGlassPill(
                    selected = currentCity == preset.name,
                    onClick = { onSelectCity(preset) },
                    icon = Icons.Rounded.LocationCity,
                    label = preset.name
                )
            }
        }

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(appLanguage.pick("تم", "Done", "Tayyor"))
        }
    }
}

@Composable
private fun ArCameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (_: Exception) {
            }
        }, executor)

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
