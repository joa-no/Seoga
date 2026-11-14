package com.joano.seoga.ui.screens.form

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joano.seoga.R
import com.joano.seoga.ui.components.ConfirmationDialog
import com.joano.seoga.ui.components.CoverImage
import com.joano.seoga.ui.screens.detail.RatingEditor
import com.joano.seoga.ui.theme.AppCard
import com.joano.seoga.ui.theme.DesignTokens
import com.joano.seoga.ui.util.LocalizationUtil
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkFormScreen(
    viewModel: WorkFormViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (Long) -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val availableCollections by viewModel.availableCollections.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    val defaultAllName = stringResource(R.string.tab_all)
    val effectiveAllName = if (userSettings.allCollectionName.isNotBlank()) userSettings.allCollectionName else defaultAllName
    val warningMsg = stringResource(R.string.work_must_belong_to_collection, effectiveAllName)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(formState.validationToast) {
        formState.validationToast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearValidationToast()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onCoverPicked(uri)
        }
    }

    val isEditing = formState.workId != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) stringResource(R.string.edit_work_title) else stringResource(R.string.add_work_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Cover Picker Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CoverImage(
                    coverUri = formState.coverUri,
                    contentDescription = formState.title,
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pick_cover_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (formState.coverUri != null) stringResource(R.string.change_cover) else stringResource(R.string.add_cover))
                    }

                    if (formState.coverUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.removeCover() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("remove_cover_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.remove_cover), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title (Mandatory)
            OutlinedTextField(
                value = formState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text(stringResource(R.string.title_label)) },
                isError = formState.title.trim().isEmpty() && formState.errorMessage != null,
                supportingText = {
                    if (formState.title.trim().isEmpty() && formState.errorMessage != null) {
                        Text(stringResource(R.string.title_required_error), color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_title_input"),
                shape = DesignTokens.CardShape
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Type Chips (Canonical types in DB, localized display)
            Text(
                text = stringResource(R.string.type_field_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    "Mangá" to R.string.type_manga,
                    "Manhwa" to R.string.type_manhwa,
                    "Manhua" to R.string.type_manhua,
                    "HQ" to R.string.type_comic
                )
                types.forEach { (canonicalType, resId) ->
                    FilterChip(
                        selected = formState.type.equals(canonicalType, ignoreCase = true),
                        onClick = { viewModel.onTypeChange(canonicalType) },
                        label = { Text(stringResource(resId)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("form_type_${canonicalType.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Authors & Artists
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = formState.authors,
                    onValueChange = { viewModel.onAuthorsChange(it) },
                    label = { Text(stringResource(R.string.authors_field_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_authors_input"),
                    shape = DesignTokens.CardShape
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = formState.artists,
                    onValueChange = { viewModel.onArtistsChange(it) },
                    label = { Text(stringResource(R.string.artists_field_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_artists_input"),
                    shape = DesignTokens.CardShape
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Genres
            Text(
                text = stringResource(R.string.genres_field_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (formState.genres.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    formState.genres.forEach { genre ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = DesignTokens.subtleBorder(),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.delete),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.removeGenre(genre) },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.newGenreInput,
                    onValueChange = { viewModel.onNewGenreInputChange(it) },
                    placeholder = { Text(stringResource(R.string.add_genre_hint)) },
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = { viewModel.addGenre() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_genre_input"),
                    shape = DesignTokens.CardShape
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.addGenre() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("form_add_genre_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_genre_button),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reading Progress Section
            Text(
                text = stringResource(R.string.reading_progress_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chapters read
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.2f)
                ) {
                    IconButton(
                        onClick = {
                            val current = formState.chaptersRead.toIntOrNull() ?: 0
                            if (current > 0) viewModel.onChaptersReadChange((current - 1).toString())
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(R.string.btn_decrement_chapter))
                    }

                    OutlinedTextField(
                        value = formState.chaptersRead,
                        onValueChange = { viewModel.onChaptersReadChange(it) },
                        label = { Text(stringResource(R.string.chapters_read_label)) },
                        isError = formState.chaptersReadError != null,
                        supportingText = {
                            formState.chaptersReadError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.testTag("form_chapters_read_error")
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("form_chapters_read_input"),
                        shape = DesignTokens.CardShape
                    )

                    IconButton(
                        onClick = {
                            val current = formState.chaptersRead.toIntOrNull() ?: 0
                            viewModel.onChaptersReadChange((current + 1).toString())
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.btn_increment_chapter))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Total chapters (optional)
                OutlinedTextField(
                    value = formState.totalChapters,
                    onValueChange = { viewModel.onTotalChaptersChange(it) },
                    label = { Text(stringResource(R.string.total_chapters_label)) },
                    placeholder = { Text("Ex: 120") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_total_chapters_input"),
                    shape = DesignTokens.CardShape
                )
            }

            // External Source Limit option if totalSource != MANUAL
            if (formState.totalSource != "MANUAL") {
                Spacer(modifier = Modifier.height(8.dp))
                AppCard(
                    shape = DesignTokens.CardShape,
                    isHighlighted = !formState.limitReleased,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.limit_to_source_total, formState.displaySource),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (!formState.limitReleased) {
                                    stringResource(R.string.limit_source_active)
                                } else {
                                    stringResource(R.string.limit_source_released)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !formState.limitReleased,
                            onCheckedChange = { isLimited ->
                                viewModel.toggleLimitReleased(!isLimited)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Adult Content (+18) Switch
            Spacer(modifier = Modifier.height(8.dp))
            AppCard(
                shape = DesignTokens.CardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.adult_content_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = formState.isAdult,
                        onCheckedChange = { viewModel.onIsAdultChange(it) },
                        modifier = Modifier.testTag("form_adult_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = androidx.compose.ui.graphics.Color(0xFFE53935)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rating
            RatingEditor(
                rating = formState.rating,
                onRatingCommitted = { newRating ->
                    viewModel.onRatingChange(newRating)
                },
                onRatingLiveChange = { newRating ->
                    viewModel.onRatingChange(newRating)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Collections Association
            if (availableCollections.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.collections_field_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // "Todos" chip
                    FilterChip(
                        selected = formState.inAllCollection,
                        onClick = { viewModel.toggleAllCollection(warningMsg) },
                        label = { Text(effectiveAllName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("form_collection_chip_all")
                    )

                    availableCollections.forEach { col ->
                        val isSelected = formState.selectedCollectionIds.contains(col.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleCollection(col.id) },
                            label = { Text(col.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("form_collection_chip_${col.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Synopsis
            OutlinedTextField(
                value = formState.synopsis,
                onValueChange = { viewModel.onSynopsisChange(it) },
                label = { Text(stringResource(R.string.synopsis_field_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_synopsis_input"),
                minLines = 3,
                maxLines = 6,
                shape = DesignTokens.CardShape
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Personal Notes
            OutlinedTextField(
                value = formState.personalNotes,
                onValueChange = { viewModel.onPersonalNotesChange(it) },
                label = { Text(stringResource(R.string.personal_notes_field_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_notes_input"),
                minLines = 2,
                maxLines = 5,
                shape = DesignTokens.CardShape
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveWork(onSaveSuccess) },
                enabled = !formState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("form_save_button"),
                shape = DesignTokens.CardShape
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.save_work_btn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog: Confirm changing total to MANUAL
    if (formState.showChangeTotalToManualDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.change_source_total_title, formState.displaySource),
            message = stringResource(
                R.string.change_source_total_message,
                formState.initialTotalChapters,
                formState.displaySource
            ),
            confirmText = stringResource(R.string.change_to_manual_btn),
            cancelText = stringResource(R.string.cancel),
            onConfirm = { viewModel.confirmChangeTotalToManual() },
            onDismiss = { viewModel.dismissChangeTotalToManual() }
        )
    }

    // Dialog: Confirm releasing limit on Save when read > total
    if (formState.showReleaseLimitDialogOnSave) {
        ConfirmationDialog(
            title = stringResource(R.string.source_limit_title, formState.displaySource),
            message = stringResource(
                R.string.source_limit_save_message,
                formState.totalChapters,
                formState.displaySource,
                formState.chaptersRead
            ),
            confirmText = stringResource(R.string.release_limit),
            cancelText = stringResource(R.string.cancel),
            onConfirm = { viewModel.confirmReleaseLimitOnSave(onSaveSuccess) },
            onDismiss = { viewModel.dismissReleaseLimitOnSave() }
        )
    }
}
