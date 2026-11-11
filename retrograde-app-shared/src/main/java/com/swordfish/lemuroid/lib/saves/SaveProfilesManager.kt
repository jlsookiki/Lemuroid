package com.swordfish.lemuroid.lib.saves

import android.content.Context
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.StorageProviderRegistry
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Named battery-save profiles, kept as plain files next to the game:
 *   saves/<rom>.srm            the "Default" profile
 *   saves/<rom>.<name>.srm     every other profile
 *
 * Lemuroid itself only ever plays from one internal battery save. The active profile is the
 * one whose contents currently sit there, and it is mirrored back to its file whenever a game
 * launches or a profile switch happens, so the files are never more than a session behind.
 */
class SaveProfilesManager(
    private val context: Context,
    private val savesManager: SavesManager,
    private val statesManager: StatesManager,
    private val storageProviderRegistry: Lazy<StorageProviderRegistry>,
) {
    fun activeProfile(game: Game): String {
        return preferences().getString(preferenceKey(game), null) ?: DEFAULT_PROFILE
    }

    suspend fun listProfiles(game: Game): List<String> =
        withContext(Dispatchers.IO) {
            val base = baseName(game)
            val names =
                runCatching { storageProviderRegistry.get().getProvider(game).listSiblings(game, SAVES_DIRECTORY) }
                    .getOrDefault(emptyList())
                    .mapNotNull { fileName ->
                        when {
                            fileName == "$base.srm" -> DEFAULT_PROFILE
                            fileName.startsWith("$base.") && fileName.endsWith(".srm") ->
                                fileName.removePrefix("$base.").removeSuffix(".srm").takeIf { it.isNotBlank() }
                            else -> null
                        }
                    }
            (names + activeProfile(game)).distinct().sortedWith(compareBy({ it != DEFAULT_PROFILE }, { it.lowercase() }))
        }

    fun profilePath(
        game: Game,
        profile: String,
    ): String {
        val base = baseName(game)
        return if (profile == DEFAULT_PROFILE) "$SAVES_DIRECTORY/$base.srm" else "$SAVES_DIRECTORY/$base.$profile.srm"
    }

    fun profileFileExists(
        game: Game,
        profile: String,
    ): Boolean {
        return runCatching {
            storageProviderRegistry.get().getProvider(game).openSibling(game, profilePath(game, profile))?.use { true } ?: false
        }.getOrDefault(false)
    }

    /** Copies the internal battery save out to the active profile's file. */
    suspend fun mirrorActive(
        game: Game,
        coreConfig: SystemCoreConfig,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val data = savesManager.getSaveRAM(game, coreConfig) ?: return@withContext false
            if (data.isEmpty()) return@withContext false
            storageProviderRegistry.get().getProvider(game).writeSibling(game, profilePath(game, activeProfile(game)), data)
        }

    /** Copies the active profile's file back over the internal battery save. */
    suspend fun restoreActive(
        game: Game,
        coreConfig: SystemCoreConfig,
    ): Boolean =
        withContext(Dispatchers.IO) {
            loadInto(game, coreConfig, activeProfile(game))
        }

    /**
     * Makes [profile] the one the game plays from. The current save is mirrored first so nothing
     * is lost. The automatic resume state is dropped because it embeds the old battery save.
     */
    suspend fun activate(
        game: Game,
        coreConfig: SystemCoreConfig,
        profile: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val name = sanitize(profile) ?: return@withContext false
            if (name == activeProfile(game)) return@withContext true

            mirrorActive(game, coreConfig)
            statesManager.deleteAutoSave(game, coreConfig.coreID)
            preferences().edit().putString(preferenceKey(game), name).apply()
            loadInto(game, coreConfig, name)
            true
        }

    /** Starts a fresh, empty profile and makes it active. */
    suspend fun create(
        game: Game,
        coreConfig: SystemCoreConfig,
        profile: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val name = sanitize(profile) ?: return@withContext false
            if (profileFileExists(game, name) || name == activeProfile(game)) return@withContext false

            mirrorActive(game, coreConfig)
            statesManager.deleteAutoSave(game, coreConfig.coreID)
            savesManager.deleteSaveRAM(game)
            preferences().edit().putString(preferenceKey(game), name).apply()
            true
        }

    private suspend fun loadInto(
        game: Game,
        coreConfig: SystemCoreConfig,
        profile: String,
    ): Boolean {
        val provider = storageProviderRegistry.get().getProvider(game)
        val data = provider.openSibling(game, profilePath(game, profile))?.use { it.readBytes() }
        if (data == null || data.isEmpty()) {
            savesManager.deleteSaveRAM(game)
            return false
        }
        savesManager.setSaveRAM(game, data)
        return true
    }

    private fun preferences() = SharedPreferencesHelper.getLegacySharedPreferences(context)

    private fun preferenceKey(game: Game) = "save_profile_${game.id}"

    private fun baseName(game: Game) = game.fileName.substringBeforeLast('.')

    companion object {
        const val DEFAULT_PROFILE = "Default"
        const val SAVES_DIRECTORY = "saves"

        fun sanitize(name: String): String? {
            val cleaned = name.trim().replace(Regex("[^A-Za-z0-9 _-]"), "").trim()
            return cleaned.takeIf { it.isNotEmpty() && it.length <= 32 }
        }
    }
}
