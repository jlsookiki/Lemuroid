package com.swordfish.lemuroid.app.mobile.feature.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import com.swordfish.lemuroid.app.shared.settings.StorageFrameworkPickerLauncher
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metaSystemID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Backs the single "Library" front: every game in one grid, narrowed by an optional filter.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    appContext: Context,
    private val retrogradeDb: RetrogradeDatabase,
) : ViewModel() {
    companion object {
        const val RECENTS_MAX_ITEMS = 10
        const val DEBOUNCE_TIME = 100L
    }

    class Factory(
        private val appContext: Context,
        private val retrogradeDb: RetrogradeDatabase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(appContext, retrogradeDb) as T
        }
    }

    sealed class Filter {
        object All : Filter()

        object Favorites : Filter()

        data class System(val metaSystem: MetaSystemID) : Filter()
    }

    data class UIState(
        val games: List<Game> = emptyList(),
        val recentGames: List<Game> = emptyList(),
        val systems: List<MetaSystemInfo> = emptyList(),
        val filter: Filter = Filter.All,
        val indexInProgress: Boolean = true,
        val libraryIsEmpty: Boolean = false,
    )

    private val filter = MutableStateFlow<Filter>(Filter.All)
    private val uiStates = MutableStateFlow(UIState())

    val state: Flow<UIState> = uiStates

    fun setFilter(newFilter: Filter) {
        filter.value = newFilter
    }

    fun changeLocalStorageFolder(context: Context) {
        StorageFrameworkPickerLauncher.pickFolder(context)
    }

    init {
        viewModelScope.launch {
            combine(
                filteredGames(),
                recentGames(),
                availableSystems(appContext),
                indexingInProgress(appContext),
                filter,
            ) { games, recents, systems, indexing, currentFilter ->
                UIState(
                    games = games,
                    recentGames = recents,
                    systems = systems,
                    filter = currentFilter,
                    indexInProgress = indexing,
                    libraryIsEmpty = systems.isEmpty(),
                )
            }
                .debounce(DEBOUNCE_TIME)
                .flowOn(Dispatchers.IO)
                .collect { uiStates.value = it }
        }
    }

    private fun filteredGames(): Flow<List<Game>> {
        return filter.flatMapLatest { currentFilter ->
            when (currentFilter) {
                is Filter.All -> retrogradeDb.gameDao().selectAllSortedByTitle()
                is Filter.Favorites -> retrogradeDb.gameDao().selectFavoritesSortedByTitle()
                is Filter.System ->
                    retrogradeDb.gameDao().selectBySystemsSortedByTitle(
                        currentFilter.metaSystem.systemIDs.map { it.dbname },
                    )
            }
        }
    }

    private fun recentGames(): Flow<List<Game>> {
        return retrogradeDb.gameDao().selectFirstRecents(RECENTS_MAX_ITEMS)
    }

    private fun availableSystems(appContext: Context): Flow<List<MetaSystemInfo>> {
        return retrogradeDb.gameDao()
            .selectSystemsWithCount()
            .map { systemCounts ->
                systemCounts.asSequence()
                    .filter { (_, count) -> count > 0 }
                    .map { (systemId, count) -> GameSystem.findById(systemId).metaSystemID() to count }
                    .groupBy { (metaSystemId, _) -> metaSystemId }
                    .map { (metaSystemId, counts) -> MetaSystemInfo(metaSystemId, counts.sumOf { it.second }) }
                    .sortedBy { it.getName(appContext) }
                    .toList()
            }
    }

    private fun indexingInProgress(appContext: Context): Flow<Boolean> {
        return PendingOperationsMonitor(appContext).anyLibraryOperationInProgress()
    }
}
