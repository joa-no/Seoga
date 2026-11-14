package com.joano.seoga.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object DesignTokens {
    val CardShape = RoundedCornerShape(16.dp)
    val CardShapeLarge = RoundedCornerShape(20.dp)
    val PillShape = CircleShape
    val BorderWidth = 1.dp

    val DarkCardBackground = Color(0xFF1B1C24)
    val DarkHighlightBackground = Color(0xFF202232) // Subtle purple-blue tint in dark surface

    @Composable
    fun subtleBorder(isAccent: Boolean = false): BorderStroke {
        val color = if (isAccent) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        }
        return BorderStroke(BorderWidth, color)
    }

    @Composable
    fun subtleBorderColor(isAccent: Boolean = false): Color {
        return if (isAccent) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = DesignTokens.CardShape,
    isHighlighted: Boolean = false,
    border: BorderStroke? = DesignTokens.subtleBorder(isAccent = isHighlighted),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val containerColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    if (onLongClick != null) {
        Card(
            modifier = modifier
                .clip(shape)
                .combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = onLongClick
                ),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    } else if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    }
}
