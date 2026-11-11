package com.swordfish.lemuroid.lib.library.metadata

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/** Extra information kept next to the games, outside Lemuroid's database. */
data class GameSidecar(
    val name: String? = null,
    val description: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val releaseYear: String? = null,
    val genre: String? = null,
    val players: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(name, description, developer, publisher, releaseYear, genre, players).all { it.isNullOrBlank() }
}

/**
 * Reads EmulationStation-style gamelist.xml files, the format Skraper and most scrapers emit:
 *   <gameList><game><path>./Game.gba</path><name>…</name><desc>…</desc>…</game></gameList>
 */
object GameSidecarParser {
    fun parseGameList(
        stream: InputStream,
        romFileName: String,
    ): GameSidecar? {
        val wanted = romFileName.substringAfterLast('/').lowercase()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        var current: MutableMap<String, String>? = null
        var field: String? = null
        var match: GameSidecar? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && match == null) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "game") {
                        current = mutableMapOf()
                    } else if (current != null) {
                        field = parser.name
                    }
                }
                XmlPullParser.TEXT -> {
                    val target = current
                    val key = field
                    if (target != null && key != null) {
                        target[key] = (target[key] ?: "") + parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "game") {
                        val entry = current
                        if (entry != null) {
                            val path = entry["path"]?.trim()?.substringAfterLast('/')?.lowercase()
                            if (path == wanted) {
                                match = toSidecar(entry)
                            }
                        }
                        current = null
                    } else {
                        field = null
                    }
                }
            }
            event = parser.next()
        }
        return match
    }

    fun parsePlainText(stream: InputStream): GameSidecar? {
        val text = stream.bufferedReader().readText().trim()
        return if (text.isEmpty()) null else GameSidecar(description = text)
    }

    private fun toSidecar(entry: Map<String, String>): GameSidecar {
        fun get(key: String) = entry[key]?.trim()?.takeIf { it.isNotEmpty() }
        return GameSidecar(
            name = get("name"),
            description = get("desc"),
            developer = get("developer"),
            publisher = get("publisher"),
            releaseYear = get("releasedate")?.take(4),
            genre = get("genre"),
            players = get("players"),
        )
    }
}
