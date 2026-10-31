package com.example.musicapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.fallbackGradientFor

@Composable
fun ContentCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 175.dp,
    isArtist: Boolean = false,
    isPlaying: Boolean = false,
    badgeText: String? = null,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val cardShape = if (isArtist) RoundedCornerShape(12.dp) else RoundedCornerShape(8.dp)
    val coverShape = if (isArtist) CircleShape else RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .width(cardWidth)
            .clip(cardShape)
            .hoverable(interactionSource)
            .background(if (isHovered) AppColors.CardHover else AppColors.Surface)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Cover Box with hover Play button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(coverShape)
                    .background(fallbackGradientFor(title)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isArtist) Icons.Rounded.Person else Icons.Rounded.Audiotrack,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(if (isArtist) 52.dp else 44.dp)
                )

                // Optional top badge (e.g. Jamendo / Audius / Cloud)
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Accent
                        )
                    }
                }

                // Floating Accent Play Button on Hover
                this@Column.AnimatedVisibility(
                    visible = isHovered || isPlaying,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(AppColors.Accent)
                            .clickable { onPlayClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Пауза" else "Слушать",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) AppColors.Accent else AppColors.TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}
