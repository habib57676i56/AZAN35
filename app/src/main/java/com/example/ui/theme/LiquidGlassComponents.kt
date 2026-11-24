package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fluid spring-like easing curve matching cubic-bezier(0.22, 1, 0.36, 1)
 */
val SacredEaseOutExpo = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Ambient atmospheric canvas with deep Obsidian-Emerald gradient (Dark) or Warm Alabaster
 * luminous radial glows (Light).
 */
@Composable
fun SacredAmbientCanvas(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(glass.canvasTop, glass.canvasBottom)
                )
            )
            .drawWithContent {
                drawContent()
            }
    ) {
        // Subtle radial ambient atmospheric glows (painted behind screen content)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientGlowPrimary, Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.08f),
                    radius = size.width * 0.85f
                ),
                center = Offset(size.width * 0.88f, size.height * 0.08f),
                radius = size.width * 0.85f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientGlowGold, Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.36f),
                    radius = size.width * 0.72f
                ),
                center = Offset(size.width * 0.12f, size.height * 0.36f),
                radius = size.width * 0.72f
            )
        }
        content()
    }
}

/**
 * Ultra-subtle procedural Islamic 8-pointed Khatam star & geometric lattice (3%–6% opacity)
 * strictly for atmospheric luxury inside Hero Cards and Mushaf frames.
 */
@Composable
fun IslamicGeometricPatternCanvas(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    alpha: Float = 0.055f
) {
    Canvas(modifier = modifier) {
        val step = 64.dp.toPx()
        val strokeCol = tint.copy(alpha = alpha.coerceIn(0.02f, 0.08f))
        val cols = (size.width / step).toInt() + 2
        val rows = (size.height / step).toInt() + 2
        val rOuter = step * 0.36f
        val rInner = step * 0.16f

        for (row in 0..rows) {
            for (col in 0..cols) {
                val cx = col * step + (if (row % 2 == 1) step * 0.5f else 0f)
                val cy = row * step
                val path = Path()
                for (i in 0 until 16) {
                    val angle = (i * Math.PI / 8.0)
                    val radius = if (i % 2 == 0) rOuter else rInner
                    val px = cx + (cos(angle) * radius).toFloat()
                    val py = cy + (sin(angle) * radius).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path = path, color = strokeCol, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = strokeCol, radius = rInner * 0.45f, center = Offset(cx, cy), style = Stroke(width = 0.8.dp.toPx()))
            }
        }
    }
}

/**
 * Layered Translucent Frosted Liquid Glass Card with:
 * - Translucent frosted fill
 * - Top specular edge highlight
 * - Hairline luminous border
 * - Tactile spring press scale when clickable
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val fill = containerColor ?: glass.glassFill
    val border = borderColor ?: glass.glassBorder

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassScale"
    )

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(),
                    onClick = onClick
                )
            } else Modifier
        )

    Surface(
        modifier = cardModifier,
        shape = shape,
        color = fill,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, border)
    ) {
        Box(
            modifier = Modifier.drawWithContent {
                drawContent()
                // Specular top-edge highlight shimmer
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            glass.specularHighlight,
                            Color.Transparent
                        )
                    ),
                    start = Offset(size.width * 0.08f, 1.dp.toPx()),
                    end = Offset(size.width * 0.92f, 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

/**
 * Tactile Liquid Glass Pill for horizontal tabs, quick-actions & category filters.
 */
@Composable
fun LiquidGlassPill(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val glass = LocalLiquidGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "pillScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (selected) activeColor else glass.glassFill,
        animationSpec = tween(220, easing = SacredEaseOutExpo),
        label = "pillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(220, easing = SacredEaseOutExpo),
        label = "pillContent"
    )
    val borderCol = if (selected) activeColor.copy(alpha = 0.85f) else glass.glassBorder

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(40.dp),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Floating pill-shaped frosted glass Bottom Navigation Dock with spring tap indicator
 * and safe navigation bar inset handling.
 */
@Composable
fun FloatingLiquidGlassDock(
    items: List<Triple<String, ImageVector, ImageVector>>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = glass.dockBackground,
            shadowElevation = if (glass.isDark) 12.dp else 8.dp,
            border = BorderStroke(1.dp, glass.glassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, glass.specularHighlight, Color.Transparent)
                            ),
                            start = Offset(size.width * 0.1f, 1.dp.toPx()),
                            end = Offset(size.width * 0.9f, 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { idx, (label, filledIcon, outlinedIcon) ->
                    val isSelected = selectedTab == idx
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = when {
                            pressed -> 0.92f
                            isSelected -> 1.03f
                            else -> 1f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "dockItemScale"
                    )
                    val indicatorColor by animateColorAsState(
                        targetValue = if (isSelected) glass.dockActiveIndicator else Color.Transparent,
                        animationSpec = tween(240, easing = SacredEaseOutExpo),
                        label = "dockIndicator"
                    )
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(220),
                        label = "dockTint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .background(indicatorColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.material3.ripple(bounded = true),
                                onClick = { onSelectTab(idx) }
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("nav_tab_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = label,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dynamic Sky-Mood gradient palette driven by the active prayer period.
 */
enum class SkyMoodPeriod {
    FAJR_DAWN,
    DHUHR_DAYLIGHT,
    ASR_GOLDEN,
    MAGHRIB_TWILIGHT,
    ISHA_NIGHT
}

fun skyMoodGradient(period: SkyMoodPeriod): List<Color> = when (period) {
    SkyMoodPeriod.FAJR_DAWN -> listOf(Color(0xFF1D2448), Color(0xFF3A3564), Color(0xFF6A486B))
    SkyMoodPeriod.DHUHR_DAYLIGHT -> listOf(Color(0xFF0A473A), Color(0xFF0F6B56), Color(0xFF1A8772))
    SkyMoodPeriod.ASR_GOLDEN -> listOf(Color(0xFF0E4739), Color(0xFF285F47), Color(0xFF7B632E))
    SkyMoodPeriod.MAGHRIB_TWILIGHT -> listOf(Color(0xFF251A3B), Color(0xFF532A4B), Color(0xFF8E4D36))
    SkyMoodPeriod.ISHA_NIGHT -> listOf(Color(0xFF050F0E), Color(0xFF0B221E), Color(0xFF12332E))
}

@Composable
fun SacredSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
