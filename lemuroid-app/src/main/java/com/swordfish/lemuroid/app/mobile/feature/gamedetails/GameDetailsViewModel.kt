package com.swordfish.lemuroid.app.mobile.feature.gamedetails

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.app.shared.gamemenu.GameMenuHelper
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metadata.GameSidecar
import com.swordfish.lemuroid.lib.library.metadata.GameSidecarParser
import com.swordfish.lemuroid.lib.saves.SaveInfo
import com.swordfish.lemuroid.lib.saves.SaveProfilesManager
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import com.swordfish.lemuroid.lib.storage.StorageProviderRegistry
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class GameDetailsViewModel(
    private val appContext: Context,
    private val gameId: Int,
    private val retrogradeDb: RetrogradeDatabase,
    private val storageProviderRegistry: Lazy<StorageProviderRegistry>,
    private val coresSelection: CoresSelection,
    private val statesManager: StatesManager,
    private val savesManager: SavesManager,
    private val statesPreviewManager: StatesPreviewManager,
    private val saveProfilesManager: SaveProfilesManager,
) : ViewModel() {
    class Factory(
        private val appContext: Context,
        private val gameId: Int,
        private val retrogradeDb: RetrogradeDatabase,
        private val storageProviderRegistry: Lazy<StorageProviderRegistry>,
        private val coresSelection: CoresSelection,
        private val statesManager: StatesManager,
        private val savesManager: SavesManager,
        private val statesPreviewManager: StatesPreviewManager,
        private val saveProfilesManager: SaveProfilesManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameDetailsViewModel(
                appContext,
                gameId,
                retrogradeDb,
                storageProviderRegistry,
                coresSelection,
                statesManager,
                savesManager,
                statesPreviewManager,
                saveProfilesManager,
            ) as T
        }
    }

    data class StateSlot(
        val index: Int,
        val info: SaveInfo,
        val preview: Bitmap?,
    )

    data class UIState(
        val game: Game? = null,
        val sidecar: GameSidecar = GameSidecar(),
        val coreName: String? = null,
        val statesSupported: Boolean = true,
        val autoSave: SaveInfo = SaveInfo(false, 0),
        val slots: List<StateSlot> = emptyList(),
        val batterySave: SaveInfo = SaveInfo(false, 0),
        val exportedBatterySaveExists: Boolean = false,
        val activeProfile: String = SaveProfilesManager.DEFAULT_PROFILE,
        val profiles: List<String> = emptyList(),
        val busy: Boolean = false,
        val message: String? = null,
    )

    private val uiState = MutableStateFlow(UIState())
    val state: Flow<UIState> = uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { loadState() }
                .onSuccess { uiState.value = it }
                .onFailure { Timber.e(it, "Failed to load game details") }
        }
    }

    fun dismissMessage() {
        uiState.value = uiState.value.copy(message = null)
    }

    /** Copies a manual slot over the automatic resume state so the next launch continues from it. */
    fun setResumePoint(slotIndex: Int) {
        runAction { game, coreConfig ->
            val slotState = statesManager.getSlotSave(game, coreConfig.coreID, slotIndex) ?: return@runAction false
            statesManager.setAutoSave(game, coreConfig.coreID, slotState)
            true
        }
    }

    fun exportBatterySave() {
        runAction { game, coreConfig -> saveProfilesManager.mirrorActive(game, coreConfig) }
    }

    fun importBatterySave() {
        runAction { game, coreConfig -> saveProfilesManager.restoreActive(game, coreConfig) }
    }

    fun activateProfile(profile: String) {
        runAction { game, coreConfig -> saveProfilesManager.activate(game, coreConfig, profile) }
    }

    fun createProfile(profile: String) {
        runAction { game, coreConfig -> saveProfilesManager.create(game, coreConfig, profile) }
    }

    fun toggleFavorite() {
        val game = uiState.value.game ?: return
        viewModelScope.launch(Dispatchers.IO) {
            retrogradeDb.gameDao().update(game.copy(isFavorite = !game.isFavorite))
            refresh()
        }
    }

    private fun runAction(action: suspend (Game, SystemCoreConfig) -> Boolean) {
        val game = uiState.value.game ?: return
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value = uiState.value.copy(busy = true)
            val coreConfig = coresSelection.getCoreConfigForSystem(GameSystem.findById(game.systemId))
            val succeeded =
                runCatching { action(game, coreConfig) }
                    .getOrElse {
                        android.util.Log.e("LemuroidSibling", "game action failed", it)
                        false
                    }
            val refreshed = runCatching { loadState() }.getOrDefault(uiState.value)
            uiState.value = refreshed.copy(busy = false, message = if (succeeded) MESSAGE_DONE else MESSAGE_FAILED)
        }
    }

    private suspend fun loadState(): UIState {
        val game = retrogradeDb.gameDao().selectById(gameId) ?: return UIState()
        val system = GameSystem.findById(game.systemId)
        val coreConfig = coresSelection.getCoreConfigForSystem(system)
        val provider = storageProviderRegistry.get().getProvider(game)

        val sidecar =
            runCatching {
                provider.openSibling(game, "gamelist.xml")?.use { GameSidecarParser.parseGameList(it, game.fileName) }
                    ?: provider.openSibling(game, game.fileName.substringBeforeLast('.') + ".txt")
                        ?.use { GameSidecarParser.parsePlainText(it) }
            }.getOrNull() ?: GameSidecar()

        val slotsInfo = statesManager.getSavedSlotsInfo(game, coreConfig.coreID)
        val slots =
            slotsInfo.mapIndexed { index, info ->
                val preview =
                    GameMenuHelper.getSaveStateBitmap(
                        appContext,
                        statesPreviewManager,
                        info,
                        game,
                        coreConfig.coreID,
                        index,
                    )
                StateSlot(index, info, preview)
            }

        val activeProfile = saveProfilesManager.activeProfile(game)
        val exportedExists = saveProfilesManager.profileFileExists(game, activeProfile)
        val profiles = saveProfilesManager.listProfiles(game)

        return UIState(
            game = game,
            sidecar = sidecar,
            coreName = coreConfig.coreID.coreName,
            statesSupported = coreConfig.statesSupported,
            autoSave = statesManager.getAutoSaveInfo(game, coreConfig.coreID),
            slots = slots,
            batterySave = savesManager.getSaveRAMInfo(game),
            exportedBatterySaveExists = exportedExists,
            activeProfile = activeProfile,
            profiles = profiles,
        )
    }

    companion object {
        const val MESSAGE_DONE = "done"
        const val MESSAGE_FAILED = "failed"
    }
}

