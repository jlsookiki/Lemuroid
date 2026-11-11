package com.swordfish.lemuroid.app.mobile.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel,
    onGameClick: (Game) -> Unit,
    onGamePlay: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    val context = LocalContext.current
    val state = viewModel.state.collectAsState(LibraryViewModel.UIState()).value

    LibraryScreen(
        modifier = modifier,
        state = state,
        onFilterSelected = { viewModel.setFilter(it) },
        onGameClick = onGameClick,
        onGamePlay = onGamePlay,
        onGameLongClick = onGameLongClick,
        onSetDirectoryClicked = { viewModel.changeLocalStorageFolder(context) },
    )
}

@Composable
private fun LibraryScreen(
    modifier: Modifier = Modifier,
    state: LibraryViewModel.UIState,
    onFilterSelected: (LibraryViewModel.Filter) -> Unit,
    onGameClick: (Game) -> Unit,
    onGamePlay: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onSetDirectoryClicked: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.libraryIsEmpty) {
            EmptyLibrary(
                enabled = !state.indexInProgress,
                onSetDirectoryClicked = onSetDirectoryClicked,
            )
            return
        }

        FilterRow(
            systems = state.systems,
            selected = state.filter,
            onFilterSelected = onFilterSelected,
        )

        if (state.games.isEmpty()) {
            LemuroidEmptyView()
            return
        }

        LibraryGrid(
            state = state,
            onGameClick = onGameClick,
            onGamePlay = onGamePlay,
            onGameLongClick = onGameLongClick,
        )
    }
}

@Composable
private fun FilterRow(
    systems: List<MetaSystemInfo>,
    selected: LibraryViewModel.Filter,
    onFilterSelected: (LibraryViewModel.Filter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected is LibraryViewModel.Filter.All,
                onClick = { onFilterSelected(LibraryViewModel.Filter.All) },
                label = { Text(stringResource(R.string.library_filter_all)) },
            )
        }
        item(key = "favorites") {
            FilterChip(
                selected = selected is LibraryViewModel.Filter.Favorites,
                onClick = { onFilterSelected(LibraryViewModel.Filter.Favorites) },
                label = { Text(stringResource(R.string.favorites)) },
                leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            )
        }
        items(systems.size, key = { systems[it].metaSystem.name }) { index ->
            val system = systems[index]
            val isSelected =
                selected is LibraryViewModel.Filter.System && selected.metaSystem == system.metaSystem
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(LibraryViewModel.Filter.System(system.metaSystem)) },
                label = { Text(stringResource(system.metaSystem.titleResId)) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGrid(
    state: LibraryViewModel.UIState,
    onGameClick: (Game) -> Unit,
    onGamePlay: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    val heroGame =
        state.recentGames.firstOrNull()
            ?.takeIf { state.filter is LibraryViewModel.Filter.All }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        columns = GridCells.Adaptive(132.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (heroGame != null) {
            item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                HeroBanner(
                    modifier = Modifier.padding(bottom = 8.dp),
                    game = heroGame,
                    onClick = { onGameClick(heroGame) },
                    onResume = { onGamePlay(heroGame) },
                    onLongClick = { onGameLongClick(heroGame) },
                )
            }
            item(key = "all-title", span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.library_all_games))
            }
        }
        items(state.games.size, key = { state.games[it].id }) { index ->
            val game = state.games[index]
            SteamGameTile(
                modifier = Modifier.animateItem(),
                game = game,
                onClick = { onGameClick(game) },
                onLongClick = { onGameLongClick(game) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyLibrary(
    enabled: Boolean,
    onSetDirectoryClicked: () -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_empty_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onSetDirectoryClicked,
                enabled = enabled,
            ) {
                Text(stringResource(id = R.string.home_empty_action))
            }
        }
    }
}
