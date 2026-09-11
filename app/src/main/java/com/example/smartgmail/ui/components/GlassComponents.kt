package com.example.smartgmail.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(
                BorderStroke(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GlassBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B101E), // Dark Navy
                        Color(0xFF1E2544), // Indigo
                        Color(0xFF2D1B4D)  // Deep Purple hint at bottom
                    )
                )
            )
    ) {
        // Ambient Blue Glow (Top Left)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4285F4).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = 1200f
                    )
                )
        )

        // Ambient Purple Glow (Bottom Right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9334E6).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(1000f, 2000f),
                        radius = 1500f
                    )
                )
        )
        
        content()
    }
}
