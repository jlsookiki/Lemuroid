/*
 * GameLibraryProvider.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.swordfish.lemuroid.lib.storage

import android.net.Uri
import androidx.leanback.preference.LeanbackPreferenceFragment
import com.swordfish.lemuroid.lib.library.db.entity.DataFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface StorageProvider {
    val id: String

    val name: String

    val uriSchemes: List<String>

    val prefsFragmentClass: Class<out LeanbackPreferenceFragment>?

    val enabledByDefault: Boolean

    fun listBaseStorageFiles(): Flow<List<BaseStorageFile>>

    fun getInputStream(uri: Uri): InputStream?

    fun getStorageFile(baseStorageFile: BaseStorageFile): StorageFile?

    /** Returns a cover image stored next to the game (see LocalCovers), if any. */
    fun findLocalCover(baseStorageFile: BaseStorageFile): Uri? = null

    /** Opens a file stored relative to the game's own folder, e.g. "gamelist.xml" or "saves/x.srm". */
    fun openSibling(
        game: Game,
        relativePath: String,
    ): InputStream? = null

    /** Writes a file relative to the game's own folder, creating intermediate folders. */
    fun writeSibling(
        game: Game,
        relativePath: String,
        data: ByteArray,
    ): Boolean = false

    fun getGameRomFiles(
        game: Game,
        dataFiles: List<DataFile>,
        allowVirtualFiles: Boolean,
    ): RomFiles
}
