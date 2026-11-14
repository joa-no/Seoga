package com.joano.seoga.ui.screens.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joano.seoga.R
import com.joano.seoga.data.local.CollectionWithCount
import com.joano.seoga.ui.components.ConfirmationDialog
import com.joano.seoga.ui.components.EmptyState
import com.joano.seoga.ui.theme.AppCard
import com.joano.seoga.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    viewModel: CollectionsViewModel,
    onCollectionClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val collections by viewModel.collectionsWithCounts.collectAsStateWithLifecycle()

    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val allCount by viewModel.allCollectionWorksCount.collectAsStateWithLifecycle()
    val defaultAllName = stringResource(R.string.tab_all)
    val effectiveAllName = if (userSettings.allCollectionName.isNotBlank()) userSettings.allCollectionName else defaultAllName

    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }

    var showRenameAllDialog by remember { mutableStateOf(false) }
    var renameAllInput by remember { mutableStateOf("") }

    var collectionToRename by remember { mutableStateOf<CollectionWithCount?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var collectionToDelete by remember { mutableStateOf<CollectionWithCount?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.collections_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newCollectionName = ""
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_create_collection")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_collection)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Informational tip card with subtle border
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = DesignTokens.CardShape,
                isHighlighted = false
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.empty_collections_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("collections_list")
            ) {
                // Fixed "Todos" collection item
                item(key = "collection_item_all") {
                    AppCard(
                        onClick = if (onCollectionClick != null) { { onCollectionClick(-1L) } } else null,
                        shape = DesignTokens.CardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collection_item_all")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = effectiveAllName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pluralStringResource(R.plurals.works_count, allCount, allCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Rename button ONLY
                            IconButton(
                                onClick = {
                                    renameAllInput = userSettings.allCollectionName.ifBlank { defaultAllName }
                                    showRenameAllDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("rename_collection_all")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileRenameOutline,
                                    contentDescription = stringResource(R.string.edit_collection_title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(collections, key = { _, it -> it.id }) { index, col ->
                    AppCard(
                        onClick = if (onCollectionClick != null) { { onCollectionClick(col.id) } } else null,
                        shape = DesignTokens.CardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collection_item_${col.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = col.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pluralStringResource(R.plurals.works_count, col.workCount, col.workCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Reordering buttons
                            Column {
                                IconButton(
                                    onClick = { viewModel.moveCollection(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.moveCollection(index, index + 1) },
                                    enabled = index < collections.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Rename button
                            IconButton(
                                onClick = {
                                    collectionToRename = col
                                    renameInput = col.name
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("rename_collection_${col.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileRenameOutline,
                                    contentDescription = stringResource(R.string.edit_collection_title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { collectionToDelete = col },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("delete_collection_${col.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_collection_title),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename 'Todos' Collection Dialog
    if (showRenameAllDialog) {
        AlertDialog(
            onDismissRequest = { showRenameAllDialog = false },
            title = { Text(stringResource(R.string.edit_collection_title)) },
            text = {
                OutlinedTextField(
                    value = renameAllInput,
                    onValueChange = { renameAllInput = it },
                    label = { Text(stringResource(R.string.collection_name_label)) },
                    placeholder = { Text(defaultAllName) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_all_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameAllCollection(renameAllInput)
                        showRenameAllDialog = false
                    },
                    modifier = Modifier.testTag("confirm_rename_all")
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Create Collection Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.create_collection_title)) },
            text = {
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    label = { Text(stringResource(R.string.collection_name_label)) },
                    placeholder = { Text(stringResource(R.string.collection_name_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_collection_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createCollection(newCollectionName)
                        showCreateDialog = false
                    },
                    enabled = newCollectionName.trim().isNotEmpty(),
                    modifier = Modifier.testTag("confirm_create_collection")
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Rename Collection Dialog
    if (collectionToRename != null) {
        AlertDialog(
            onDismissRequest = { collectionToRename = null },
            title = { Text(stringResource(R.string.edit_collection_title)) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text(stringResource(R.string.collection_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameCollection(collectionToRename!!.id, renameInput)
                        collectionToRename = null
                    },
                    enabled = renameInput.trim().isNotEmpty()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Collection Confirmation Dialog
    if (collectionToDelete != null) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_collection_title),
            message = stringResource(R.string.delete_collection_message, collectionToDelete!!.name),
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteCollection(collectionToDelete!!.id)
                collectionToDelete = null
            },
            onDismiss = { collectionToDelete = null }
        )
    }
}
