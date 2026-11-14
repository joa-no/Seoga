package com.joano.seoga.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joano.seoga.R
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.data.local.WorkWithCollections

const val ALL_COLLECTION_PSEUDO_ID = -1L

@Composable
fun ManageCollectionsDialog(
    selectedWorks: List<WorkWithCollections>,
    allCollections: List<CollectionEntity>,
    effectiveAllName: String,
    onDismiss: () -> Unit,
    onConfirm: (userChanges: Map<Long, Boolean>) -> Unit
) {
    val context = LocalContext.current
    var validationError by remember { mutableStateOf<String?>(null) }
    var userChanges by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }

    // Initial state calculation for "Todos"
    val initialTodosState = remember(selectedWorks) {
        if (selectedWorks.isEmpty()) {
            ToggleableState.Off
        } else {
            val countInTodos = selectedWorks.count { !it.work.hiddenFromAll || it.collections.isEmpty() }
            when (countInTodos) {
                selectedWorks.size -> ToggleableState.On
                0 -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
        }
    }

    // Initial state calculation for each custom collection
    val initialCollectionStates = remember(selectedWorks, allCollections) {
        allCollections.associate { col ->
            val countInCol = selectedWorks.count { w -> w.collections.any { it.id == col.id } }
            val state = when (countInCol) {
                selectedWorks.size -> ToggleableState.On
                0 -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
            col.id to state
        }
    }

    fun getEffectiveState(id: Long, initialState: ToggleableState): ToggleableState {
        val changed = userChanges[id]
        return when (changed) {
            true -> ToggleableState.On
            false -> ToggleableState.Off
            null -> initialState
        }
    }

    fun toggleState(id: Long, currentState: ToggleableState) {
        val nextState = when (currentState) {
            ToggleableState.Indeterminate -> ToggleableState.On
            ToggleableState.On -> ToggleableState.Off
            ToggleableState.Off -> ToggleableState.On
        }
        userChanges = userChanges + (id to (nextState == ToggleableState.On))
        validationError = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.manage_collections_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.selection_count,
                        selectedWorks.size,
                        selectedWorks.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // If there are no custom collections created yet
                if (allCollections.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TriStateCheckbox(
                            state = ToggleableState.On,
                            onClick = null,
                            enabled = false,
                            colors = CheckboxDefaults.colors(
                                disabledCheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = effectiveAllName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.no_collections_created_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // "Todos" row
                    val effectiveTodos = getEffectiveState(ALL_COLLECTION_PSEUDO_ID, initialTodosState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggleState(ALL_COLLECTION_PSEUDO_ID, effectiveTodos) }
                            .padding(vertical = 4.dp)
                            .testTag("manage_collections_row_all"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TriStateCheckbox(
                            state = effectiveTodos,
                            onClick = { toggleState(ALL_COLLECTION_PSEUDO_ID, effectiveTodos) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = effectiveAllName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Custom collections
                    allCollections.forEach { col ->
                        val initialColState = initialCollectionStates[col.id] ?: ToggleableState.Off
                        val effectiveColState = getEffectiveState(col.id, initialColState)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleState(col.id, effectiveColState) }
                                .padding(vertical = 4.dp)
                                .testTag("manage_collections_row_${col.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TriStateCheckbox(
                                state = effectiveColState,
                                onClick = { toggleState(col.id, effectiveColState) },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = col.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("manage_collections_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (allCollections.isEmpty()) {
                        onDismiss()
                        return@Button
                    }

                    // Simulate the result for each selected work
                    val hasOrphan = selectedWorks.any { workWithCols ->
                        val work = workWithCols.work
                        val finalCols = workWithCols.collections.map { it.id }.toMutableSet()

                        for (col in allCollections) {
                            when (userChanges[col.id]) {
                                true -> finalCols.add(col.id)
                                false -> finalCols.remove(col.id)
                                null -> { /* untouched */ }
                            }
                        }

                        val finalHidden = when (userChanges[ALL_COLLECTION_PSEUDO_ID]) {
                            true -> false
                            false -> true
                            null -> work.hiddenFromAll
                        }

                        finalHidden && finalCols.isEmpty()
                    }

                    if (hasOrphan) {
                        validationError = context.getString(
                            R.string.work_must_belong_to_collection,
                            effectiveAllName
                        )
                    } else {
                        onConfirm(userChanges)
                    }
                },
                modifier = Modifier.testTag("manage_collections_confirm_button")
            ) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("manage_collections_cancel_button")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
