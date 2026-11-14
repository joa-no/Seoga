package com.joano.seoga.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joano.seoga.R
import com.joano.seoga.data.local.WorkWithCollections
import com.joano.seoga.ui.components.CoverImage
import com.joano.seoga.ui.components.ReadingProgressBar
import com.joano.seoga.ui.theme.AppCard
import com.joano.seoga.ui.theme.DesignTokens
import com.joano.seoga.ui.theme.RatingStar

@Composable
fun WorkGridCard(
    workWithCollections: WorkWithCollections,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val work = workWithCollections.work
    val isCompleted = work.totalChapters != null && work.chaptersRead >= work.totalChapters
    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        DesignTokens.subtleBorder()
    }

    AppCard(
        onClick = onClick,
        onLongClick = onLongClick,
        isHighlighted = isSelected,
        border = border,
        shape = DesignTokens.CardShape,
        modifier = modifier
            .fillMaxWidth()
            .testTag("work_card_${work.id}")
    ) {
        Column {
            // Cover with 2:3 aspect ratio and overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                CoverImage(
                    coverUri = work.coverUri,
                    contentDescription = work.title,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )

                // Type badge and +18 badge on top-left
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = com.joano.seoga.ui.util.LocalizationUtil.getLocalizedWorkType(work.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }

                    if (work.isAdult) {
                        com.joano.seoga.ui.components.AdultBadge(
                            fontSize = 10.sp,
                            horizontalPadding = 8.dp,
                            verticalPadding = 3.dp
                        )
                    }
                }

                // Selection check on top-right, or rating if not selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (work.rating > 0f) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .align(Alignment.TopEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingStar,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", work.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Completed badge on bottom-left
                if (isCompleted) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .align(Alignment.BottomStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.status_completed),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Title & reading progress below cover
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                ReadingProgressBar(
                    chaptersRead = work.chaptersRead,
                    totalChapters = work.totalChapters,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WorkListCard(
    workWithCollections: WorkWithCollections,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onIncrementChapter: () -> Unit,
    onDecrementChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val work = workWithCollections.work
    val isCompleted = work.totalChapters != null && work.chaptersRead >= work.totalChapters
    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        DesignTokens.subtleBorder()
    }

    AppCard(
        onClick = onClick,
        onLongClick = onLongClick,
        isHighlighted = isSelected,
        border = border,
        shape = DesignTokens.CardShape,
        modifier = modifier
            .fillMaxWidth()
            .testTag("work_list_card_${work.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Cover with selection overlay
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
            ) {
                CoverImage(
                    coverUri = work.coverUri,
                    contentDescription = work.title,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = com.joano.seoga.ui.util.LocalizationUtil.getLocalizedWorkType(work.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }

                    if (work.isAdult) {
                        Spacer(modifier = Modifier.width(6.dp))
                        com.joano.seoga.ui.components.AdultBadge(
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            textColor = Color(0xFFE53935),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            horizontalPadding = 8.dp,
                            verticalPadding = 2.dp
                        )
                    }

                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.status_completed),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (work.rating > 0f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = RatingStar,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", work.rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = work.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (work.authors.isNotBlank()) {
                    Text(
                        text = work.authors,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                ReadingProgressBar(
                    chaptersRead = work.chaptersRead,
                    totalChapters = work.totalChapters,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stepper buttons for quick progress
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onIncrementChapter,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("quick_increment_${work.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Incrementar capítulo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (work.chaptersRead > 0) {
                    IconButton(
                        onClick = onDecrementChapter,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("quick_decrement_${work.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrementar capítulo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
