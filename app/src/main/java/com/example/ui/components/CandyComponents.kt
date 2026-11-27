package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.BoardTheme
import com.example.ui.theme.BgGradientOrange
import com.example.ui.theme.BgGradientPurple
import com.example.ui.theme.BgGradientYellow
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGold
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.NunitoBoldFontFamily
import com.example.ui.theme.RainbowColors
import com.example.ui.theme.SurfaceDarkPurple
import com.example.ui.theme.style
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun CandyGradientBackground(
    boardTheme: BoardTheme = BoardTheme.CANDY_CLASSIC,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val stops = remember(boardTheme) { boardTheme.style().backgroundStops }

    val infiniteTransition = rememberInfiniteTransition(label = "bg_sparkles")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(stops))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bubblePositions = listOf(
                Offset(0.12f, 0.14f) to 38f,
                Offset(0.86f, 0.18f) to 52f,
                Offset(0.22f, 0.48f) to 28f,
                Offset(0.90f, 0.56f) to 44f,
                Offset(0.15f, 0.82f) to 46f,
                Offset(0.80f, 0.85f) to 34f
            )
            bubblePositions.forEachIndexed { idx, (pos, radius) ->
                val yOffset = sin((drift + idx * 0.25f) * 6.28f) * 16f
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = radius,
                    center = Offset(size.width * pos.x, size.height * pos.y + yOffset)
                )
            }
        }
        content()
    }
}

@Composable
fun GlossyCandyButton(
    text: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "btn_scale"
    )

    val activeColors = if (enabled) {
        gradientColors
    } else {
        listOf(Color(0xFF9E98B3), Color(0xFF7D7694))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .shadow(elevation = if (enabled) 10.dp else 2.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(activeColors))
            .border(
                width = 2.5.dp,
                color = Color.White.copy(alpha = 0.72f),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
        contentAlignment = Alignment.Center
    ) {
        // Glossy top highlight sheen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.36f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = NunitoBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 3f),
                        blurRadius = 4f
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun TopResourceBar(
    coins: Int,
    hints: Int,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingContent != null) {
                leadingContent()
            }
            ResourcePill(
                icon = Icons.Rounded.MonetizationOn,
                iconTint = CandyGold,
                value = "%,d".format(coins),
                testTag = "coins_pill"
            )
            ResourcePill(
                icon = Icons.Rounded.Lightbulb,
                iconTint = CandyYellow,
                value = hints.toString(),
                testTag = "hints_pill"
            )
        }

        LanguageTogglePill(
            language = language,
            onToggle = onToggleLanguage
        )
    }
}

@Composable
fun ResourcePill(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    testTag: String
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDarkPurple.copy(alpha = 0.85f))
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@Composable
fun LanguageTogglePill(
    language: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDarkPurple.copy(alpha = 0.88f))
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .clickable(onClick = onToggle)
            .padding(4.dp)
            .testTag("language_toggle_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageChip(label = "EN", selected = language == AppLanguage.EN)
        LanguageChip(label = "FR", selected = language == AppLanguage.FR)
    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(CandyOrange, CandyPinkRed))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun RainbowProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(stiffness = 220f),
        label = "rainbow_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Color.White.copy(alpha = 0.30f))
            .border(1.5.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(height / 2))
    ) {
        if (animatedProgress > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(height / 2))
                    .background(Brush.horizontalGradient(RainbowColors))
            )
        }
    }
}

private enum class ConfettiShape {
    RIBBON,
    DOT,
    STAR
}

private class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotationDeg: Float,
    var angularVel: Float,
    var flipPhase: Float,
    var flipSpeed: Float,
    val size: Float,
    val color: Color,
    val shape: ConfettiShape,
    var alpha: Float = 1f
)

@Composable
fun ConfettiOverlay(
    burstTrigger: Int,
    continuous: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (burstTrigger <= 0 && !continuous) return

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var frameTick by remember { mutableLongStateOf(0L) }
    val particles = remember(burstTrigger, continuous) { mutableListOf<ConfettiParticle>() }
    var isEmitting by remember(burstTrigger, continuous) { mutableStateOf(true) }

    LaunchedEffect(burstTrigger, continuous, canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect

        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        val rnd = Random(burstTrigger * 313 + if (continuous) 99 else 7)

        fun spawnCannonParticle(originX: Float, originY: Float, angleRad: Float, speedMin: Float, speedMax: Float): ConfettiParticle {
            val speed = speedMin + rnd.nextFloat() * (speedMax - speedMin)
            val spread = angleRad + (rnd.nextFloat() - 0.5f) * 0.85f
            return ConfettiParticle(
                x = originX,
                y = originY,
                vx = cos(spread) * speed,
                vy = sin(spread) * speed,
                rotationDeg = rnd.nextFloat() * 360f,
                angularVel = (rnd.nextFloat() - 0.5f) * 420f,
                flipPhase = rnd.nextFloat() * 6.28f,
                flipSpeed = 4f + rnd.nextFloat() * 7f,
                size = 11f + rnd.nextFloat() * 14f,
                color = RainbowColors[rnd.nextInt(RainbowColors.size)],
                shape = ConfettiShape.entries[rnd.nextInt(ConfettiShape.entries.size)]
            )
        }

        fun spawnTopShowerParticle(startAbove: Boolean = true): ConfettiParticle {
            return ConfettiParticle(
                x = rnd.nextFloat() * w,
                y = if (startAbove) -24f - rnd.nextFloat() * 60f else rnd.nextFloat() * h * 0.2f,
                vx = (rnd.nextFloat() - 0.5f) * 140f,
                vy = 140f + rnd.nextFloat() * 220f,
                rotationDeg = rnd.nextFloat() * 360f,
                angularVel = (rnd.nextFloat() - 0.5f) * 320f,
                flipPhase = rnd.nextFloat() * 6.28f,
                flipSpeed = 3.5f + rnd.nextFloat() * 6f,
                size = 10f + rnd.nextFloat() * 13f,
                color = RainbowColors[rnd.nextInt(RainbowColors.size)],
                shape = ConfettiShape.entries[rnd.nextInt(ConfettiShape.entries.size)]
            )
        }

        particles.clear()
        isEmitting = true
        if (continuous) {
            // Win celebration: dual side cannons + center burst
            repeat(32) {
                particles.add(spawnCannonParticle(w * 0.08f, h * 0.62f, -1.05f, 480f, 1050f))
                particles.add(spawnCannonParticle(w * 0.92f, h * 0.62f, -2.09f, 480f, 1050f))
            }
            repeat(28) {
                particles.add(spawnTopShowerParticle(startAbove = false))
            }
        } else {
            // Word-found burst from upper center
            repeat(46) {
                particles.add(spawnCannonParticle(w * 0.5f, h * 0.25f, -1.57f, 240f, 680f))
            }
        }

        var lastNanos = 0L
        var elapsedTotal = 0f
        val gravity = 620f
        val drag = 0.986f

        while (isEmitting || particles.isNotEmpty()) {
            val now = withFrameNanos { it }
            if (lastNanos == 0L) {
                lastNanos = now
                continue
            }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
            lastNanos = now
            elapsedTotal += dt

            if (!continuous && elapsedTotal > 1.9f) {
                isEmitting = false
            }

            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.vx *= drag
                p.vy = p.vy * drag + gravity * dt
                p.x += p.vx * dt + sin(p.flipPhase) * 26f * dt
                p.y += p.vy * dt
                p.rotationDeg += p.angularVel * dt
                p.flipPhase += p.flipSpeed * dt

                if (!continuous && elapsedTotal > 1.2f) {
                    p.alpha = (1f - (elapsedTotal - 1.2f) / 0.75f).coerceIn(0f, 1f)
                }

                if (p.y > h + 48f || p.alpha <= 0.01f) {
                    if (continuous) {
                        iterator.set(spawnTopShowerParticle(startAbove = true))
                    } else {
                        iterator.remove()
                    }
                }
            }
            frameTick = now
        }
    }

    if (!isEmitting && particles.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .testTag("confetti_canvas")
    ) {
        // Read frameTick to trigger Canvas redraw each frame
        val tickRead = frameTick
        if (tickRead >= 0L) {
            particles.forEach { p ->
                val flipScaleX = cos(p.flipPhase).coerceIn(-1f, 1f)
                val drawColor = p.color.copy(alpha = p.alpha)
                rotate(degrees = p.rotationDeg, pivot = Offset(p.x, p.y)) {
                    scale(scaleX = flipScaleX, scaleY = 1f, pivot = Offset(p.x, p.y)) {
                        when (p.shape) {
                            ConfettiShape.RIBBON -> {
                                drawRoundRect(
                                    color = drawColor,
                                    topLeft = Offset(p.x - p.size * 0.5f, p.y - p.size * 0.28f),
                                    size = Size(p.size, p.size * 0.56f),
                                    cornerRadius = CornerRadius(3f, 3f)
                                )
                            }
                            ConfettiShape.DOT -> {
                                drawCircle(
                                    color = drawColor,
                                    radius = p.size * 0.34f,
                                    center = Offset(p.x, p.y)
                                )
                            }
                            ConfettiShape.STAR -> {
                                val rOuter = p.size * 0.48f
                                val rInner = p.size * 0.20f
                                val starPath = Path().apply {
                                    moveTo(p.x, p.y - rOuter)
                                    lineTo(p.x + rInner, p.y - rInner)
                                    lineTo(p.x + rOuter, p.y)
                                    lineTo(p.x + rInner, p.y + rInner)
                                    lineTo(p.x, p.y + rOuter)
                                    lineTo(p.x - rInner, p.y + rInner)
                                    lineTo(p.x - rOuter, p.y)
                                    lineTo(p.x - rInner, p.y - rInner)
                                    close()
                                }
                                drawPath(path = starPath, color = drawColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CandyNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun BottomCandyNavBar(
    items: List<CandyNavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceDarkPurple.copy(alpha = 0.92f),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.route == selectedRoute
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) Brush.horizontalGradient(listOf(CandyOrange, CandyPinkRed))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onSelect(item.route) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun WordSplashLogoTitle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = CandyYellow,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Word Candy",
            style = MaterialTheme.typography.displayLarge.copy(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, CandyYellow, CandyOrange)
                ),
                shadow = Shadow(
                    color = SurfaceDarkPurple.copy(alpha = 0.65f),
                    offset = Offset(0f, 5f),
                    blurRadius = 10f
                )
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = CandyYellow,
            modifier = Modifier.size(28.dp)
        )
    }
}
