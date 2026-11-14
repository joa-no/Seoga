package com.joano.seoga.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joano.seoga.R
import com.joano.seoga.ui.components.ConfirmationDialog
import com.joano.seoga.ui.components.CoverImage
import com.joano.seoga.ui.components.SeogaSlider
import com.joano.seoga.ui.theme.AppCard
import com.joano.seoga.ui.theme.DesignTokens
import com.joano.seoga.ui.theme.RatingStar
import com.joano.seoga.ui.util.LocalizationUtil
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkDetailScreen(
    viewModel: WorkDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditWork: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val workWithCollections by viewModel.workWithCollections.collectAsState()
    val allCollections by viewModel.allCollections.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val pendingOverLimit by viewModel.pendingChapterOverLimit.collectAsState()

    val defaultAllName = stringResource(R.string.tab_all)
    val effectiveAllName = if (userSettings.allCollectionName.isNotBlank()) userSettings.allCollectionName else defaultAllName

    var showCollectionsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChapterEditDialog by remember { mutableStateOf(false) }
    var customChapterInput by remember { mutableStateOf("") }
    var chapterEditError by remember { mutableStateOf<String?>(null) }

    val work = workWithCollections?.work
    val collections = workWithCollections?.collections ?: emptyList()
    val isInAll = work != null && (!work.hiddenFromAll || collections.isEmpty())

    val hasTotal = work?.totalChapters != null && work.totalChapters > 0
    val total = work?.totalChapters ?: 0

    var isDragging by remember { mutableStateOf(false) }
    var liveChapter by remember(work?.chaptersRead) { mutableIntStateOf(work?.chaptersRead ?: 0) }

    var dynamicMax by remember(work?.id) {
        mutableIntStateOf(calculateDynamicMax(work?.chaptersRead ?: 0))
    }

    LaunchedEffect(work?.chaptersRead) {
        val read = work?.chaptersRead ?: 0
        if (!isDragging) {
            liveChapter = read
            if (!hasTotal && read >= dynamicMax) {
                dynamicMax = calculateDynamicMax(read)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "", maxLines = 1) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (work != null) {
                        IconButton(
                            onClick = { onEditWork(work.id) },
                            modifier = Modifier.testTag("detail_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_work_btn)
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_work_btn),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (work == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.empty_library_title))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Cover with subtle border
            CoverImage(
                coverUri = work.coverUri,
                contentDescription = work.title,
                modifier = Modifier
                    .width(170.dp)
                    .height(250.dp)
                    .testTag("detail_cover_image"),
                shape = DesignTokens.CardShapeLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Work Type & External Source Pill Badges
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = LocalizationUtil.getLocalizedWorkType(work.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (work.isAdult) {
                    Spacer(modifier = Modifier.width(8.dp))
                    com.joano.seoga.ui.components.AdultBadge(
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        textColor = Color(0xFFE53935),
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        fontWeight = FontWeight.Bold,
                        horizontalPadding = 12.dp,
                        verticalPadding = 4.dp
                    )
                }

                if (!work.externalSource.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.source_badge, work.externalSource),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = work.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Authors & Artists
            if (work.authors.isNotBlank() || work.artists.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val credits = buildString {
                    if (work.authors.isNotBlank()) append(work.authors)
                    if (work.artists.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(stringResource(R.string.artists_label, work.artists))
                    }
                }
                Text(
                    text = credits,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Rating Stars
            if (work.rating > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = RatingStar,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f / 10", work.rating),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reading Progress Section
            AppCard(
                shape = DesignTokens.CardShapeLarge,
                isHighlighted = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_progress_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reading_progress_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (work.isLimitActive && work.totalSource != "MANUAL") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = work.displaySource,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Counter Display with +/- buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Decrement button with rapid repeat on hold
                        RepeatingPressButton(
                            onClick = { viewModel.decrementChapter() },
                            enabled = liveChapter > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("detail_decrement_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = stringResource(R.string.btn_decrement_chapter),
                                tint = if (liveChapter > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Number Display (clickable to type directly)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    customChapterInput = liveChapter.toString()
                                    chapterEditError = null
                                    showChapterEditDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("detail_progress_text")
                        ) {
                            Text(
                                text = stringResource(R.string.slider_chapter_label, liveChapter),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (hasTotal) {
                                Text(
                                    text = "de $total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Increment button with rapid repeat on hold
                        val canIncrement = !work.isLimitActive || !hasTotal || liveChapter < total
                        RepeatingPressButton(
                            onClick = { viewModel.incrementChapter() },
                            enabled = canIncrement,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (canIncrement) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("detail_increment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.btn_increment_chapter),
                                tint = if (canIncrement) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (hasTotal) {
                        val maxSlider = total.toFloat()

                        Column(modifier = Modifier.fillMaxWidth()) {
                            SeogaSlider(
                                value = liveChapter.toFloat().coerceIn(0f, maxSlider),
                                onValueChange = { newValue ->
                                    isDragging = true
                                    val rounded = newValue.roundToInt()
                                    liveChapter = if (work.isLimitActive && rounded > total) {
                                        total
                                    } else {
                                        rounded.coerceIn(0, total)
                                    }
                                },
                                onValueChangeFinished = {
                                    isDragging = false
                                    viewModel.setChapter(liveChapter)
                                },
                                valueRange = 0f..maxSlider,
                                steps = maxOf(0, total - 1),
                                showTicks = (total - 1) <= 30,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_progress_slider")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cap. $liveChapter / $total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val currentPct = if (total > 0) {
                                    ((liveChapter.toFloat() / total.toFloat()) * 100).roundToInt().coerceIn(0, 100)
                                } else 0
                                Text(
                                    text = "$currentPct%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { if (total > 0) (liveChapter.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    } else {
                        // Slider for works without total (dynamic range, steps of 1, expands on release at max)
                        val maxSlider = dynamicMax.toFloat()

                        Column(modifier = Modifier.fillMaxWidth()) {
                            SeogaSlider(
                                value = liveChapter.toFloat().coerceIn(0f, maxSlider),
                                onValueChange = { newValue ->
                                    isDragging = true
                                    val rounded = newValue.roundToInt()
                                    liveChapter = rounded.coerceIn(0, dynamicMax)
                                },
                                onValueChangeFinished = {
                                    isDragging = false
                                    viewModel.setChapter(liveChapter)
                                    dynamicMax = calculateDynamicMax(liveChapter)
                                },
                                valueRange = 0f..maxSlider,
                                steps = maxOf(0, dynamicMax - 1),
                                showTicks = (dynamicMax - 1) <= 30,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_progress_slider")
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$dynamicMax",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rating Section
            AppCard(
                shape = DesignTokens.CardShapeLarge,
                isHighlighted = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_rating_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    RatingEditor(
                        rating = work.rating,
                        onRatingCommitted = { newRating ->
                            viewModel.setRating(newRating)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Assigned Collections Section
            AppCard(
                shape = DesignTokens.CardShapeLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.collections_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { showCollectionsDialog = true },
                            modifier = Modifier.testTag("manage_collections_button")
                        ) {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.manage_collections_btn))
                        }
                    }

                    if (isInAll || collections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isInAll) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = effectiveAllName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            collections.forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = col.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.no_collections_assigned),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Synopsis
            if (work.synopsis.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                AppCard(
                    shape = DesignTokens.CardShapeLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.synopsis_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = work.synopsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Personal Notes
            if (work.personalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                AppCard(
                    shape = DesignTokens.CardShapeLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.personal_notes_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = work.personalNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Genres
            if (work.genres.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                AppCard(
                    shape = DesignTokens.CardShapeLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.genres_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            work.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { g ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = g,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Source Limit Confirmation Dialog
    if (pendingOverLimit != null && work != null) {
        ConfirmationDialog(
            title = stringResource(R.string.source_limit_title, work.displaySource),
            message = stringResource(
                R.string.source_limit_message,
                work.totalChapters ?: 0,
                work.displaySource,
                pendingOverLimit ?: 0
            ),
            confirmText = stringResource(R.string.release_limit),
            cancelText = stringResource(R.string.cancel),
            onConfirm = { viewModel.confirmReleaseLimitAndApply() },
            onDismiss = { viewModel.dismissReleaseLimitDialog() }
        )
    }

    // Direct Chapter Input Dialog
    if (showChapterEditDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = {
                showChapterEditDialog = false
                chapterEditError = null
            },
            title = { Text(stringResource(R.string.edit_chapter_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customChapterInput,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() }) {
                                customChapterInput = it
                                chapterEditError = null
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = chapterEditError != null,
                        label = { Text(stringResource(R.string.edit_chapter_dialog_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_chapter_text_field")
                    )
                    if (chapterEditError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = chapterEditError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("edit_chapter_error_text")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = customChapterInput.toIntOrNull() ?: 0
                        if (hasTotal && work != null && work.totalSource == "MANUAL" && num > total) {
                            chapterEditError = context.getString(R.string.manual_total_exceeded_warning, total)
                            return@Button
                        }
                        viewModel.setChapter(num)
                        if (!hasTotal) {
                            dynamicMax = calculateDynamicMax(num)
                        }
                        showChapterEditDialog = false
                        chapterEditError = null
                    },
                    modifier = Modifier.testTag("edit_chapter_confirm_button")
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChapterEditDialog = false
                    chapterEditError = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Manage Collections Dialog
    if (showCollectionsDialog && workWithCollections != null) {
        com.joano.seoga.ui.components.ManageCollectionsDialog(
            selectedWorks = listOf(workWithCollections!!),
            allCollections = allCollections,
            effectiveAllName = effectiveAllName,
            onDismiss = { showCollectionsDialog = false },
            onConfirm = { userChanges ->
                val finalCols = collections.map { it.id }.toMutableSet()
                for (col in allCollections) {
                    when (userChanges[col.id]) {
                        true -> finalCols.add(col.id)
                        false -> finalCols.remove(col.id)
                        null -> { /* untouched */ }
                    }
                }
                val inAll = when (userChanges[com.joano.seoga.ui.components.ALL_COLLECTION_PSEUDO_ID]) {
                    true -> true
                    false -> false
                    null -> isInAll
                }
                viewModel.updateCollections(finalCols.toList(), inAll = inAll)
                showCollectionsDialog = false
            }
        )
    }

    // Delete Work Confirmation Dialog
    if (showDeleteDialog && work != null) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_work_confirm_title),
            message = stringResource(R.string.delete_work_confirm_message, work.title),
            confirmText = stringResource(R.string.delete_work_btn),
            cancelText = stringResource(R.string.cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteWork(onDeleted)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
fun RepeatingPressButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onRelease: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnRelease by rememberUpdatedState(onRelease)
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            currentOnClick()
            delay(350)
            while (isPressed) {
                currentOnClick()
                delay(70)
            }
        }
    }

    Box(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        currentOnRelease?.invoke()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun RatingEditor(
    rating: Float,
    onRatingCommitted: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onRatingLiveChange: ((Float) -> Unit)? = null
) {
    var liveRating by remember(rating) { mutableFloatStateOf(rating) }
    var showDialog by remember { mutableStateOf(false) }

    fun updateLive(value: Float) {
        val clamped = (kotlin.math.round(value.coerceIn(0f, 10f) * 10f) / 10f)
        liveRating = clamped
        onRatingLiveChange?.invoke(clamped)
    }

    fun commit(value: Float) {
        val clamped = (kotlin.math.round(value.coerceIn(0f, 10f) * 10f) / 10f)
        liveRating = clamped
        onRatingCommitted(clamped)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.rating_field_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDialog = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("detail_rating_text")
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = RatingStar,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (liveRating > 0f) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", liveRating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / 10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_rating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Proportional 10 stars row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 10) {
                val fraction = (liveRating - i.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            val target = (i + 1).toFloat()
                            val newRating = if (liveRating == target) 0f else target
                            updateLive(newRating)
                            commit(newRating)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    if (fraction > 0f) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingStar,
                            modifier = Modifier
                                .size(24.dp)
                                .drawWithContent {
                                    clipRect(
                                        left = 0f,
                                        top = 0f,
                                        right = size.width * fraction,
                                        bottom = size.height
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Slider with - and + buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RepeatingPressButton(
                onClick = {
                    val next = (kotlin.math.round((liveRating - 0.1f) * 10f) / 10f).coerceIn(0f, 10f)
                    updateLive(next)
                },
                onRelease = {
                    commit(liveRating)
                },
                enabled = liveRating > 0f,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("detail_rating_minus")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.btn_decrement_rating),
                    tint = if (liveRating > 0f) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            SeogaSlider(
                value = liveRating.coerceIn(0f, 10f),
                onValueChange = { newVal ->
                    val rounded = kotlin.math.round(newVal * 10f) / 10f
                    updateLive(rounded)
                },
                onValueChangeFinished = {
                    commit(liveRating)
                },
                valueRange = 0f..10f,
                steps = 0,
                showTicks = false,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("detail_rating_slider")
            )

            Spacer(modifier = Modifier.width(8.dp))

            RepeatingPressButton(
                onClick = {
                    val next = (kotlin.math.round((liveRating + 0.1f) * 10f) / 10f).coerceIn(0f, 10f)
                    updateLive(next)
                },
                onRelease = {
                    commit(liveRating)
                },
                enabled = liveRating < 10f,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (liveRating < 10f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("detail_rating_plus")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.btn_increment_rating),
                    tint = if (liveRating < 10f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDialog) {
        var inputRatingText by remember {
            mutableStateOf(if (liveRating > 0f) String.format(Locale.getDefault(), "%.1f", liveRating) else "")
        }
        var ratingError by remember { mutableStateOf<String?>(null) }
        val errorMessage = stringResource(R.string.rating_range_error)

        AlertDialog(
            onDismissRequest = {
                showDialog = false
                ratingError = null
            },
            title = {
                Text(
                    text = stringResource(R.string.rating_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputRatingText,
                        onValueChange = { newText ->
                            inputRatingText = newText
                            ratingError = null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = ratingError != null,
                        label = { Text(stringResource(R.string.rating_dialog_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_rating_text_field")
                    )
                    if (ratingError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ratingError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = inputRatingText.trim().replace(',', '.')
                        val num = clean.toFloatOrNull()
                        val decimals = clean.substringAfter('.', "")
                        val isValid = num != null && num in 0f..10f && (!clean.contains('.') || decimals.length <= 1)
                        if (!isValid) {
                            ratingError = errorMessage
                            return@Button
                        }
                        val finalRating = (kotlin.math.round(num!! * 10f) / 10f).coerceIn(0f, 10f)
                        commit(finalRating)
                        showDialog = false
                        ratingError = null
                    },
                    modifier = Modifier.testTag("save_rating_button")
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            commit(0f)
                            showDialog = false
                            ratingError = null
                        },
                        modifier = Modifier.testTag("remove_rating_button")
                    ) {
                        Text(
                            text = stringResource(R.string.remove_rating_btn),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            showDialog = false
                            ratingError = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}

internal fun calculateDynamicMax(chaptersRead: Int): Int {
    val nextMultiple = ((chaptersRead / 50) + 1) * 50
    return maxOf(50, nextMultiple)
}
