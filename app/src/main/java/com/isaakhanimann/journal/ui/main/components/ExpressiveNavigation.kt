package com.isaakhanimann.journal.ui.main.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Stable
class PredictiveBackState {
    var progress by mutableFloatStateOf(0f)
    var isSwipeActive by mutableStateOf(false)
}

class ScallopedPillShape(private val isScalloped: Boolean) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height
        val radius = height / 2f

        if (!isScalloped) {
            path.addRoundRect(
                RoundRect(
                    0f, 0f, width, height,
                    CornerRadius(radius)
                )
            )
            return Outline.Generic(path)
        }

        val bumpDepth = with(density) { 2.5.dp.toPx() }
        val bumpsCount = 12f
        val numPoints = 120

        fun getPoint(p: Float): Pair<Offset, Offset> {
            val straight = (width - (2 * radius)).coerceAtLeast(0f)
            val arc = PI.toFloat() * radius
            val total = 2 * straight + 2 * arc
            val d = p * total
            
            return when {
                d < straight -> {
                    Offset(radius + d, 0f) to Offset(0f, -1f)
                }
                d < straight + arc -> {
                    val angle = 1.5f * PI.toFloat() + (d - straight) / radius
                    val n = Offset(cos(angle), sin(angle))
                    Offset(width - radius, radius) + n * radius to n
                }
                d < 2 * straight + arc -> {
                    Offset(width - radius - (d - (straight + arc)), height) to Offset(0f, 1f)
                }
                else -> {
                    val angle = 0.5f * PI.toFloat() + (d - (2 * straight + arc)) / radius
                    val n = Offset(cos(angle), sin(angle))
                    Offset(radius, radius) + n * radius to n
                }
            }
        }

        for (i in 0..numPoints) {
            val p = i.toFloat() / numPoints
            val (pos, normal) = getPoint(p)
            val bump = sin(p * bumpsCount * 2 * PI.toFloat()) * bumpDepth
            val finalPos = pos + normal * bump
            if (i == 0) path.moveTo(finalPos.x, finalPos.y) else path.lineTo(finalPos.x, finalPos.y)
        }
        
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun ExpressiveNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectionAlphaOverride: Float? = null,
    onLongHold: () -> Unit = {},
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            val job = launch {
                delay(2000.milliseconds)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongHold()
            }
            scale.animateTo(
                0.88f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
            job.join()
        } else {
            scale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(300),
        label = "selection_alpha"
    )

    val selectionAlpha = selectionAlphaOverride ?: animatedAlpha
    
    val containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = selectionAlpha)
    val contentColor = if (selectionAlpha > 0.5f) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            if (selectionAlpha > 0.8f) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
