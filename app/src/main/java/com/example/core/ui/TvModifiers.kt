package com.example.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.tvFocusableBorder(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: CornerBasedShape = RoundedCornerShape(12.dp),
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    focusedScale: Float = 1.04f,
    focusable: Boolean = true
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val borderStroke = if (isFocused) {
        BorderStroke(3.dp, focusedBorderColor)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    return this
        .scale(scale)
        .then(if (focusable) Modifier.focusable(interactionSource = interactionSource) else Modifier)
        .border(borderStroke, shape)
}
