package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.swordfish.lemuroid.R

fun NavGraphBuilder.composable(
    route: MainRoute,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable(route = route.route, arguments = route.arguments, content = content)
}

fun NavController.navigateToRoute(route: MainRoute) {
    this.navigate(route.route)
}

enum class MainRoute(
    val route: String,
    @StringRes val titleId: Int,
    val parent: MainRoute? = null,
    val arguments: List<NamedNavArgument> = emptyList(),
    val showBottomNavigation: Boolean = true,
) {
    LIBRARY(
        route = "library",
        titleId = R.string.title_library,
        showBottomNavigation = false,
    ),
    APPS(
        route = "apps",
        titleId = R.string.title_apps,
        parent = LIBRARY,
        showBottomNavigation = false,
    ),
    HOME(
        route = "home",
        titleId = R.string.title_home,
    ),
    FAVORITES(
        route = "favorites",
        titleId = R.string.favorites,
    ),
    SEARCH(
        route = "search",
        titleId = R.string.title_search,
        showBottomNavigation = false,
    ),
    SYSTEMS(
        route = "systems/home",
        titleId = R.string.title_systems,
    ),
    SYSTEM_GAMES(
        route = "systems/{metaSystemId}",
        titleId = R.string.title_games,
        parent = SYSTEMS,
        listOf(navArgument("metaSystemId") { type = NavType.StringType }),
    ),
    SETTINGS(
        route = "settings/home",
        titleId = R.string.title_settings,
        showBottomNavigation = false,
    ),
    SETTINGS_ADVANCED(
        route = "settings/advanced",
        titleId = R.string.settings_title_advanced_settings,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    SETTINGS_BIOS(
        route = "settings/bios",
        titleId = R.string.settings_title_display_bios_info,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    SETTINGS_CORES_SELECTION(
        route = "settings/cores",
        titleId = R.string.settings_title_open_cores_selection,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    SETTINGS_INPUT_DEVICES(
        route = "settings/inputdevices",
        titleId = R.string.settings_title_gamepad_settings,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    SETTINGS_SAVE_SYNC(
        route = "settings/savesync",
        titleId = R.string.settings_title_save_sync,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    ;

    val root = root()

    private fun root(): MainRoute {
        return parent?.root() ?: this
    }

    companion object {
        fun findByRoute(route: String): MainRoute {
            return values().first { it.route == route }
        }
    }
}

enum class MainNavigationRoutes(
    val route: MainRoute,
    @StringRes val titleId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    LIBRARY(MainRoute.LIBRARY, R.string.title_library, Icons.Filled.GridView, Icons.Outlined.GridView),
    SEARCH(MainRoute.SEARCH, R.string.title_search, Icons.Filled.Search, Icons.Outlined.Search),
}
