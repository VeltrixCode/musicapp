package com.example.musicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unique JVXMusic signature gradient for "Любимые треки" (Favorites):
 * Distinct emerald-teal tones replacing the third-party purple look.
 */
val FavoritesGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0D3328), Color(0xFF0F5A3E), Color(0xFF1DB954)),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

@Composable
fun FavoritesCoverBox(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(FavoritesGradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = "Любимые треки",
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}
