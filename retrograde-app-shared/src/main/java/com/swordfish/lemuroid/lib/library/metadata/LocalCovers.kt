package com.swordfish.lemuroid.lib.library.metadata

/**
 * Conventions for cover art stored next to the games themselves.
 *
 * For a rom called "Game (USA).gba" the following files are tried, in order:
 *   Game (USA).png            (same folder)
 *   media/Game (USA).png      (subfolder)
 *   covers/Game (USA).png
 *   images/Game (USA).png
 * with png, jpg, jpeg and webp extensions.
 */
object LocalCovers {
    val EXTENSIONS = listOf("png", "jpg", "jpeg", "webp")

    val FOLDERS = listOf("", "media/", "covers/", "images/")

    fun isLocalUri(uri: String?): Boolean {
        return uri != null && (uri.startsWith("content://") || uri.startsWith("file://"))
    }

    fun candidateRelativePaths(extensionlessName: String): List<String> {
        return FOLDERS.flatMap { folder ->
            EXTENSIONS.map { extension -> "$folder$extensionlessName.$extension" }
        }
    }
}
