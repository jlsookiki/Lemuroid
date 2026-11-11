package com.swordfish.lemuroid.lib.storage.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.leanback.preference.LeanbackPreferenceFragment
import com.swordfish.lemuroid.common.kotlin.extractEntryToFile
import com.swordfish.lemuroid.common.kotlin.isZipped
import com.swordfish.lemuroid.common.kotlin.writeToFile
import com.swordfish.lemuroid.lib.R
import com.swordfish.lemuroid.lib.library.db.entity.DataFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metadata.LocalCovers
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.BaseStorageFile
import com.swordfish.lemuroid.lib.storage.RomFiles
import com.swordfish.lemuroid.lib.storage.StorageFile
import com.swordfish.lemuroid.lib.storage.StorageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

class StorageAccessFrameworkProvider(private val context: Context) : StorageProvider {

    override val id: String = "access_framework"

    override val name: String = context.getString(R.string.local_storage)

    override val uriSchemes = listOf("content")

    override val prefsFragmentClass: Class<LeanbackPreferenceFragment>? = null

    override val enabledByDefault = true

    override fun listBaseStorageFiles(): Flow<List<BaseStorageFile>> {
        return getExternalFolder()?.let { folder ->
            traverseDirectoryEntries(Uri.parse(folder))
        } ?: emptyFlow()
    }

    override fun getStorageFile(baseStorageFile: BaseStorageFile): StorageFile? {
        return DocumentFileParser.parseDocumentFile(context, baseStorageFile)
    }

    override fun findLocalCover(baseStorageFile: BaseStorageFile): Uri? {
        val treeUri = getExternalFolder()?.let { Uri.parse(it) } ?: return null
        val documentId = runCatching { DocumentsContract.getDocumentId(baseStorageFile.uri) }.getOrNull() ?: return null

        // Document ids of the external storage provider look like "primary:ROMs/gba/Game.gba".
        val separator = documentId.lastIndexOf('/')
        val parentPrefix =
            if (separator >= 0) {
                documentId.substring(0, separator + 1)
            } else {
                documentId.substringBefore(':') + ":"
            }

        return LocalCovers.candidateRelativePaths(baseStorageFile.extensionlessName)
            .asSequence()
            .map { DocumentsContract.buildDocumentUriUsingTree(treeUri, parentPrefix + it) }
            .firstOrNull { documentExists(it) }
    }

    override fun openSibling(
        game: Game,
        relativePath: String,
    ): InputStream? {
        val uri = siblingUri(game, relativePath) ?: return null
        if (!documentExists(uri)) return null
        return runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
    }

    override fun writeSibling(
        game: Game,
        relativePath: String,
        data: ByteArray,
    ): Boolean {
        val treeUri = getExternalFolder()?.let { Uri.parse(it) } ?: return false
        val documentId = runCatching { DocumentsContract.getDocumentId(Uri.parse(game.fileUri)) }.getOrNull() ?: return false
        val resolver = context.contentResolver

        return runCatching {
            android.util.Log.d(SIBLING_TAG, "writeSibling doc=$documentId path=$relativePath")
            var directoryId = parentDocumentId(documentId)
            val parts = relativePath.split('/').filter { it.isNotEmpty() }
            for (directory in parts.dropLast(1)) {
                val childId = joinDocumentId(directoryId, directory)
                if (!documentExists(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId))) {
                    DocumentsContract.createDocument(
                        resolver,
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, directoryId),
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        directory,
                    ) ?: return false
                }
                directoryId = childId
            }

            val fileName = parts.last()
            val fileId = joinDocumentId(directoryId, fileName)
            val existingUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, fileId)
            val fileUri =
                if (documentExists(existingUri)) {
                    existingUri
                } else {
                    DocumentsContract.createDocument(
                        resolver,
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, directoryId),
                        "application/octet-stream",
                        fileName,
                    ) ?: return false
                }

            android.util.Log.d(SIBLING_TAG, "writing ${data.size} bytes to $fileUri")
            resolver.openOutputStream(fileUri, "wt")?.use { it.write(data) } ?: return false
            true
        }.onFailure { android.util.Log.e(SIBLING_TAG, "writeSibling failed", it) }.getOrDefault(false)
    }

    private fun siblingUri(
        game: Game,
        relativePath: String,
    ): Uri? {
        val treeUri = getExternalFolder()?.let { Uri.parse(it) } ?: return null
        val documentId = runCatching { DocumentsContract.getDocumentId(Uri.parse(game.fileUri)) }.getOrNull() ?: return null
        var id = parentDocumentId(documentId)
        relativePath.split('/').filter { it.isNotEmpty() }.forEach { id = joinDocumentId(id, it) }
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
    }

    /** "primary:ROMs/gba/Game.gba" -> "primary:ROMs/gba"; "primary:Game.gba" -> "primary:" */
    private fun parentDocumentId(documentId: String): String {
        val separator = documentId.lastIndexOf('/')
        return if (separator >= 0) documentId.substring(0, separator) else documentId.substringBefore(':') + ":"
    }

    private fun joinDocumentId(
        parent: String,
        child: String,
    ): String {
        return if (parent.endsWith(":")) parent + child else "$parent/$child"
    }

    private fun documentExists(uri: Uri): Boolean {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }

    private fun getExternalFolder(): String? {
        val prefString = context.getString(R.string.pref_key_extenral_folder)
        val preferenceManager = SharedPreferencesHelper.getLegacySharedPreferences(context)
        return preferenceManager.getString(prefString, null)
    }

    private fun traverseDirectoryEntries(rootUri: Uri): Flow<List<BaseStorageFile>> =
        flow {
            val directoryDocumentIds = mutableListOf<String>()
            DocumentsContract.getTreeDocumentId(rootUri)?.let { directoryDocumentIds.add(it) }

            while (directoryDocumentIds.isNotEmpty()) {
                val currentDirectoryDocumentId = directoryDocumentIds.removeAt(0)

                val result =
                    runCatching {
                        listBaseStorageFiles(rootUri, currentDirectoryDocumentId)
                    }
                if (result.isFailure) {
                    Timber.e(result.exceptionOrNull(), "Error while listing files")
                }

                val (files, directories) =
                    result.getOrDefault(
                        listOf<BaseStorageFile>() to listOf<String>(),
                    )

                emit(files)
                directoryDocumentIds.addAll(directories)
            }
        }

    private fun listBaseStorageFiles(
        treeUri: Uri,
        rootDocumentId: String,
    ): Pair<List<BaseStorageFile>, List<String>> {
        val resultFiles = mutableListOf<BaseStorageFile>()
        val resultDirectories = mutableListOf<String>()

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocumentId)

        Timber.d("Querying files in directory: $childrenUri")

        val projection =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use {
            while (it.moveToNext()) {
                val documentId = it.getString(0)
                val documentName = it.getString(1)
                val documentSize = it.getLong(2)
                val mimeType = it.getString(3)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    resultDirectories.add(documentId)
                } else {
                    val documentUri =
                        DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            documentId,
                        )
                    resultFiles.add(
                        BaseStorageFile(
                            name = documentName,
                            size = documentSize,
                            uri = documentUri,
                            path = documentUri.path,
                        ),
                    )
                }
            }
        }

        return resultFiles to resultDirectories
    }

    override fun getGameRomFiles(
        game: Game,
        dataFiles: List<DataFile>,
        allowVirtualFiles: Boolean,
    ): RomFiles {
        val originalDocumentUri = Uri.parse(game.fileUri)
        val originalDocument = DocumentFile.fromSingleUri(context, originalDocumentUri)!!

        val isZipped = originalDocument.isZipped() && originalDocument.name != game.fileName

        return when {
            isZipped && dataFiles.isEmpty() -> getGameRomFilesZipped(game, originalDocument)
            allowVirtualFiles -> getGameRomFilesVirtual(game, dataFiles)
            else -> getGameRomFilesStandard(game, dataFiles, originalDocument)
        }
    }

    private fun getGameRomFilesStandard(
        game: Game,
        dataFiles: List<DataFile>,
        originalDocument: DocumentFile,
    ): RomFiles {
        val gameEntry = getGameRomStandard(game, originalDocument)
        val dataEntries = dataFiles.map { getDataFileStandard(game, it) }
        return RomFiles.Standard(listOf(gameEntry) + dataEntries)
    }

    private fun getGameRomFilesZipped(
        game: Game,
        originalDocument: DocumentFile,
    ): RomFiles {
        val cacheFile = GameCacheUtils.getCacheFileForGame(SAF_CACHE_SUBFOLDER, context, game)
        if (cacheFile.exists()) {
            return RomFiles.Standard(listOf(cacheFile))
        }

        val stream =
            ZipInputStream(
                context.contentResolver.openInputStream(originalDocument.uri),
            )
        stream.extractEntryToFile(game.fileName, cacheFile)
        return RomFiles.Standard(listOf(cacheFile))
    }

    private fun getGameRomFilesVirtual(
        game: Game,
        dataFiles: List<DataFile>,
    ): RomFiles {
        val gameEntry = getGameRomVirtual(game)
        val dataEntries = dataFiles.map { getDataFileVirtual(it) }
        return RomFiles.Virtual(listOf(gameEntry) + dataEntries)
    }

    private fun getDataFileVirtual(dataFile: DataFile): RomFiles.Virtual.Entry {
        return RomFiles.Virtual.Entry(
            "$VIRTUAL_FILE_PATH/${dataFile.fileName}",
            context.contentResolver.openFileDescriptor(Uri.parse(dataFile.fileUri), "r")!!,
        )
    }

    private fun getDataFileStandard(
        game: Game,
        dataFile: DataFile,
    ): File {
        val cacheFile =
            GameCacheUtils.getDataFileForGame(
                SAF_CACHE_SUBFOLDER,
                context,
                game,
                dataFile,
            )

        if (cacheFile.exists()) {
            return cacheFile
        }

        val stream = context.contentResolver.openInputStream(Uri.parse(dataFile.fileUri))!!
        stream.writeToFile(cacheFile)
        return cacheFile
    }

    private fun getGameRomVirtual(game: Game): RomFiles.Virtual.Entry {
        return RomFiles.Virtual.Entry(
            "$VIRTUAL_FILE_PATH/${game.fileName}",
            context.contentResolver.openFileDescriptor(Uri.parse(game.fileUri), "r")!!,
        )
    }

    private fun getGameRomStandard(
        game: Game,
        originalDocument: DocumentFile,
    ): File {
        val cacheFile = GameCacheUtils.getCacheFileForGame(SAF_CACHE_SUBFOLDER, context, game)

        if (cacheFile.exists()) {
            return cacheFile
        }

        val stream = context.contentResolver.openInputStream(originalDocument.uri)!!
        stream.writeToFile(cacheFile)
        return cacheFile
    }

    override fun getInputStream(uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    companion object {
        private const val SIBLING_TAG = "LemuroidSibling"
        const val SAF_CACHE_SUBFOLDER = "storage-framework-games"
        const val VIRTUAL_FILE_PATH = "/virtual/file/path"
    }
}
