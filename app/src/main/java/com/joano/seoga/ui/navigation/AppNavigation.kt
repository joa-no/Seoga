package com.joano.seoga.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.joano.seoga.R
import com.joano.seoga.ui.screens.collections.CollectionsScreen
import com.joano.seoga.ui.screens.collections.CollectionsViewModel
import com.joano.seoga.ui.screens.detail.WorkDetailScreen
import com.joano.seoga.ui.screens.detail.WorkDetailViewModel
import com.joano.seoga.ui.screens.form.WorkFormScreen
import com.joano.seoga.ui.screens.form.WorkFormViewModel
import com.joano.seoga.ui.screens.library.LibraryScreen
import com.joano.seoga.ui.screens.library.LibraryViewModel
import com.joano.seoga.ui.screens.search.SearchMangaDexScreen
import com.joano.seoga.ui.screens.search.SearchMangaDexViewModel
import com.joano.seoga.ui.screens.settings.SettingsScreen
import com.joano.seoga.ui.screens.settings.SettingsViewModel
import com.joano.seoga.ui.theme.SeogaTheme

data class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AppNavigation(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val userSettings by settingsViewModel.userSettings.collectAsStateWithLifecycle()

    val darkTheme = when (userSettings.themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true // default dark
    }

    SeogaTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val bottomNavItems = listOf(
            BottomNavItem(Screen.Library.route, R.string.nav_library, Icons.Default.AutoStories, "nav_library"),
            BottomNavItem(Screen.SearchAdd.route, R.string.nav_add, Icons.Default.AddCircleOutline, "nav_add"),
            BottomNavItem(Screen.Collections.route, R.string.nav_collections, Icons.Default.Folder, "nav_collections"),
            BottomNavItem(Screen.Settings.route, R.string.nav_settings, Icons.Default.Settings, "nav_settings")
        )

        val showBottomNav = bottomNavItems.any { it.route == currentDestination?.route }

        // Centralized top-level destination navigation helper
        val navigateToTopLevel: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            if (isExpandedScreen) {
                // Wide screen: NavigationRail
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showBottomNav) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = currentDestination?.route == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = stringResource(item.titleRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.titleRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.weight(1f)
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Library.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            defineAppGraph(navController, navigateToTopLevel)
                        }
                    }
                }
            } else {
                // Compact / Phone: Standard NavigationBar at bottom
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomNav) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 3.dp
                            ) {
                                bottomNavItems.forEach { item ->
                                    val selected = currentDestination?.route == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTopLevel(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = stringResource(item.titleRes)
                                            )
                                        },
                                        label = { Text(stringResource(item.titleRes)) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Library.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        defineAppGraph(navController, navigateToTopLevel)
                    }
                }
            }
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.defineAppGraph(
    navController: androidx.navigation.NavHostController,
    navigateToTopLevel: (String) -> Unit
) {
    composable(Screen.Library.route) {
        val libraryViewModel: LibraryViewModel = viewModel()
        LibraryScreen(
            viewModel = libraryViewModel,
            onWorkClick = { workId ->
                navController.navigate(Screen.WorkDetail.createRoute(workId))
            },
            onAddWorkClick = {
                navController.navigate(Screen.SearchAdd.route)
            }
        )
    }

    composable(Screen.SearchAdd.route) {
        val searchViewModel: SearchMangaDexViewModel = viewModel()
        SearchMangaDexScreen(
            viewModel = searchViewModel,
            onNavigateBack = { navController.popBackStack() },
            onManualAddClick = {
                navController.navigate(Screen.WorkForm.createRoute(workId = 0))
            },
            onSelectResult = { result ->
                searchViewModel.selectedResultForReview = result
                navController.navigate(Screen.WorkForm.createRoute(workId = 0, externalId = result.id))
            }
        )
    }

    composable(Screen.Collections.route) {
        val collectionsViewModel: CollectionsViewModel = viewModel()
        CollectionsScreen(
            viewModel = collectionsViewModel,
            onCollectionClick = { colId ->
                if (!navController.popBackStack()) {
                    navigateToTopLevel(Screen.Library.route)
                }
            }
        )
    }

    composable(Screen.Settings.route) {
        val settingsViewModel: SettingsViewModel = viewModel()
        SettingsScreen(viewModel = settingsViewModel)
    }

    composable(
        route = Screen.WorkForm.route,
        arguments = listOf(
            navArgument("workId") {
                type = NavType.LongType
                defaultValue = 0L
            },
            navArgument("externalId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val workId = backStackEntry.arguments?.getLong("workId") ?: 0L
        val externalId = backStackEntry.arguments?.getString("externalId")
        val formViewModel: WorkFormViewModel = viewModel()

        val prevEntry = navController.previousBackStackEntry
        val searchViewModel: SearchMangaDexViewModel? = if (prevEntry != null && prevEntry.destination.route == Screen.SearchAdd.route) {
            viewModel(prevEntry)
        } else {
            null
        }

        remember(workId, externalId) {
            if (workId != 0L) {
                formViewModel.initializeForEdit(workId)
            } else if (!externalId.isNullOrBlank() && searchViewModel?.selectedResultForReview?.id == externalId) {
                formViewModel.initializeFromExternal(searchViewModel.selectedResultForReview!!)
            } else {
                formViewModel.initializeForNew()
            }
            true
        }

        WorkFormScreen(
            viewModel = formViewModel,
            onNavigateBack = { navController.popBackStack() },
            onSaveSuccess = { savedWorkId ->
                navController.popBackStack(Screen.Library.route, inclusive = false)
            }
        )
    }

    composable(
        route = Screen.WorkDetail.route,
        arguments = listOf(
            navArgument("workId") {
                type = NavType.LongType
            }
        )
    ) { backStackEntry ->
        val workId = backStackEntry.arguments?.getLong("workId") ?: 0L
        val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        val detailViewModel = remember(workId) {
            WorkDetailViewModel(app, workId)
        }

        WorkDetailScreen(
            viewModel = detailViewModel,
            onNavigateBack = { navController.popBackStack() },
            onEditWork = { idToEdit ->
                navController.navigate(Screen.WorkForm.createRoute(workId = idToEdit))
            },
            onDeleted = {
                navController.popBackStack(Screen.Library.route, inclusive = false)
            }
        )
    }
}
