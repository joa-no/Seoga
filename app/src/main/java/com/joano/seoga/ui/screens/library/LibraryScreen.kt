package com.joano.seoga.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joano.seoga.R
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.ui.components.ConfirmationDialog
import com.joano.seoga.ui.components.EmptyState
import com.joano.seoga.ui.components.ManageCollectionsDialog
import com.joano.seoga.ui.theme.DesignTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onWorkClick: (Long) -> Unit,
    onAddWorkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val collections by viewModel.collectionsWithCounts.collectAsStateWithLifecycle()
    val totalWorksCount by viewModel.totalWorksCount.collectAsStateWithLifecycle()
    val allCollectionWorksCount by viewModel.allCollectionWorksCount.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val allWorksWithCols by viewModel.allWorksWithCollections.collectAsStateWithLifecycle()
    val allCollections by viewModel.allCollections.collectAsStateWithLifecycle()
    val selectedWorkIds by viewModel.selectedWorkIds.collectAsStateWithLifecycle()
    val isSelectionMode = selectedWorkIds.isNotEmpty()

    val defaultAllName = stringResource(R.string.tab_all)
    val effectiveAllName = if (userSettings.allCollectionName.isNotBlank()) userSettings.allCollectionName else defaultAllName

    var showFilterSheet by remember { mutableStateOf(false) }
    var showManageCollectionsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    val hasActiveFilter = filterState.selectedType != null ||
            filterState.progressFilter != "ALL" ||
            filterState.sortOption != SortOption.RECENT_UPDATED

    // HorizontalPager state: 0 is "Todos", followed by collections
    val totalPages = collections.size + 1
    val initialPage = remember(filterState.selectedCollectionId, collections) {
        if (filterState.selectedCollectionId == null) {
            0
        } else {
            val idx = collections.indexOfFirst { it.id == filterState.selectedCollectionId }
            if (idx >= 0) idx + 1 else 0
        }
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPages }
    )

    val currentTabCollectionId = if (pagerState.currentPage == 0) {
        null
    } else {
        collections.getOrNull(pagerState.currentPage - 1)?.id
    }

    // Sync Pager swipes with ViewModel collection selection and clear selection on swipe
    LaunchedEffect(pagerState.currentPage) {
        viewModel.clearSelection()
        val targetId = if (pagerState.currentPage == 0) {
            null
        } else {
            collections.getOrNull(pagerState.currentPage - 1)?.id
        }
        if (filterState.selectedCollectionId != targetId) {
            viewModel.selectCollection(targetId)
        }
    }

    // Auto-scroll the pill chip list to keep active collection visible
    val chipRowState = rememberLazyListState()
    LaunchedEffect(pagerState.currentPage) {
        chipRowState.animateScrollToItem(pagerState.currentPage.coerceAtLeast(0))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header: Normal Title or Selection Actions Header
            if (isSelectionMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.clearSelection() },
                                modifier = Modifier.testTag("selection_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pluralStringResource(
                                    R.plurals.selection_count,
                                    selectedWorkIds.size,
                                    selectedWorkIds.size
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        TextButton(
                            onClick = {
                                val currentWorks = viewModel.getWorksForCollectionPage(currentTabCollectionId)
                                viewModel.selectAll(currentWorks.map { it.work.id })
                            },
                            modifier = Modifier.testTag("selection_select_all_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_select_all),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { showManageCollectionsDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("action_manage_collections_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_manage_collections),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            onClick = { showDeleteConfirmation = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.40f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("action_delete_selected_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.delete),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.toggleViewMode() },
                            modifier = Modifier.testTag("toggle_view_mode_button")
                        ) {
                            Icon(
                                imageVector = if (filterState.viewMode == "grid") Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = stringResource(R.string.toggle_view_mode),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.testTag("filter_sort_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (hasActiveFilter) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filter_and_sort),
                                    tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.search_library_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = DesignTokens.CardShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("library_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Pill Chips row with works count
            LazyRow(
                state = chipRowState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "Todos" Pill Chip
                item {
                    val isSelected = pagerState.currentPage == 0
                    CollectionPillChip(
                        title = effectiveAllName,
                        count = allCollectionWorksCount,
                        isSelected = isSelected,
                        testTag = "tab_all_works",
                        onClick = {
                            viewModel.clearSelection()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    )
                }

                // Custom Collections Pill Chips
                items(collections, key = { it.id }) { col ->
                    val pageIndex = collections.indexOf(col) + 1
                    val isSelected = pagerState.currentPage == pageIndex
                    CollectionPillChip(
                        title = col.name,
                        count = col.workCount,
                        isSelected = isSelected,
                        testTag = "tab_collection_${col.id}",
                        onClick = {
                            viewModel.clearSelection()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pageIndex)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Pager with swipe gesture between collections
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                val targetCollectionId = if (page == 0) null else collections.getOrNull(page - 1)?.id
                val worksForPage = remember(allWorksWithCols, filterState, targetCollectionId) {
                    viewModel.getWorksForCollectionPage(targetCollectionId)
                }

                if (worksForPage.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            totalWorksCount == 0 -> {
                                EmptyState(
                                    icon = Icons.Default.AutoStories,
                                    title = stringResource(R.string.empty_library_title),
                                    description = stringResource(R.string.empty_library_subtitle),
                                    actionLabel = stringResource(R.string.add_work_title),
                                    onActionClick = onAddWorkClick
                                )
                            }
                            filterState.searchQuery.isNotBlank() -> {
                                EmptyState(
                                    icon = Icons.Default.Search,
                                    title = stringResource(R.string.empty_search_title),
                                    description = stringResource(R.string.empty_search_subtitle),
                                    actionLabel = stringResource(R.string.clear_filters),
                                    onActionClick = { viewModel.setSearchQuery("") }
                                )
                            }
                            targetCollectionId != null -> {
                                EmptyState(
                                    icon = Icons.Default.AutoStories,
                                    title = stringResource(R.string.empty_collection_title),
                                    description = stringResource(R.string.empty_collection_subtitle),
                                    actionLabel = stringResource(R.string.add_work_title),
                                    onActionClick = onAddWorkClick
                                )
                            }
                            else -> {
                                EmptyState(
                                    icon = Icons.Default.FilterList,
                                    title = stringResource(R.string.empty_search_title),
                                    description = stringResource(R.string.empty_search_subtitle),
                                    actionLabel = stringResource(R.string.clear_filters),
                                    onActionClick = {
                                        viewModel.setTypeFilter(null)
                                        viewModel.setProgressFilter("ALL")
                                        viewModel.setSortOption(SortOption.RECENT_UPDATED)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (filterState.viewMode == "grid") {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("works_grid_view")
                        ) {
                            items(worksForPage, key = { it.work.id }) { item ->
                                val isSelected = selectedWorkIds.contains(item.work.id)
                                WorkGridCard(
                                    workWithCollections = item,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleWorkSelection(item.work.id)
                                        } else {
                                            onWorkClick(item.work.id)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleWorkSelection(item.work.id)
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("works_list_view")
                        ) {
                            items(worksForPage, key = { it.work.id }) { item ->
                                val isSelected = selectedWorkIds.contains(item.work.id)
                                WorkListCard(
                                    workWithCollections = item,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleWorkSelection(item.work.id)
                                        } else {
                                            onWorkClick(item.work.id)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleWorkSelection(item.work.id)
                                    },
                                    onIncrementChapter = { viewModel.incrementChapter(item.work) },
                                    onDecrementChapter = { viewModel.decrementChapter(item.work) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            filterState = filterState,
            onSortSelected = { viewModel.setSortOption(it) },
            onTypeSelected = { viewModel.setTypeFilter(it) },
            onProgressFilterSelected = { viewModel.setProgressFilter(it) },
            onResetFilters = {
                viewModel.setSearchQuery("")
                viewModel.setTypeFilter(null)
                viewModel.setProgressFilter("ALL")
                viewModel.setSortOption(SortOption.RECENT_UPDATED)
            },
            onDismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    showFilterSheet = false
                }
            },
            sheetState = sheetState
        )
    }

    if (showManageCollectionsDialog) {
        val selectedWorksList = remember(selectedWorkIds, allWorksWithCols) {
            allWorksWithCols.filter { selectedWorkIds.contains(it.work.id) }
        }
        ManageCollectionsDialog(
            selectedWorks = selectedWorksList,
            allCollections = allCollections,
            effectiveAllName = effectiveAllName,
            onDismiss = { showManageCollectionsDialog = false },
            onConfirm = { userChanges ->
                viewModel.applyManageCollectionsChanges(
                    userChanges = userChanges,
                    allCollections = allCollections,
                    onComplete = {
                        showManageCollectionsDialog = false
                    }
                )
            }
        )
    }

    if (showDeleteConfirmation) {
        val selectedCount = selectedWorkIds.size
        val isDeletingAllLibrary = selectedCount == totalWorksCount && totalWorksCount > 0
        val isCollectionTab = currentTabCollectionId != null

        val title = if (isDeletingAllLibrary) {
            stringResource(R.string.delete_all_library_confirm_title)
        } else {
            pluralStringResource(R.plurals.delete_works_confirm_title, selectedCount, selectedCount)
        }

        val baseMessage = if (isDeletingAllLibrary) {
            pluralStringResource(R.plurals.delete_all_library_confirm_message, selectedCount, selectedCount)
        } else {
            pluralStringResource(R.plurals.delete_works_confirm_message, selectedCount, selectedCount)
        }

        val message = if (isCollectionTab) {
            "$baseMessage ${stringResource(R.string.delete_works_collection_note)}"
        } else {
            baseMessage
        }

        ConfirmationDialog(
            title = title,
            message = message,
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSelectedWorks {
                    showDeleteConfirmation = false
                }
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}

@Composable
fun CollectionPillChip(
    title: String,
    count: Int,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "pill_bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
        } else {
            DesignTokens.subtleBorderColor(isAccent = false)
        },
        animationSpec = tween(durationMillis = 200),
        label = "pill_border_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "pill_text_color"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(38.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($count)",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        }
    }
}
