package com.swordfish.lemuroid.app.shared.game

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.delay
import java.io.File

/**
 * Lets the main process talk to a game running in the ":game" process. Games live in their own
 * task so the Library can act as the home screen; this is how the Library asks a backgrounded
 * game to save and quit before another one starts or a save profile changes.
 */
object GameProcessController {
    const val ACTION_STOP_GAME = "com.swordfish.lemuroid.action.STOP_GAME"

    private const val TAG = "GameProcessController"

    private const val RUNNING_GAME_FILE = "running_game"
    private const val GAME_PROCESS_SUFFIX = ":game"
    private const val STOP_TIMEOUT_MS = 6000L
    private const val POLL_INTERVAL_MS = 100L

    /** True while the ":game" process is alive, whether its activity is in front or backgrounded. */
    fun isGameRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val gameProcessName = context.packageName + GAME_PROCESS_SUFFIX
        return activityManager.runningAppProcesses?.any { it.processName == gameProcessName } == true
    }

    fun runningGameId(context: Context): Int? {
        if (!isGameRunning(context)) return null
        return runCatching { runningGameFile(context).readText().trim().toInt() }.getOrNull()
    }

    fun markRunning(
        context: Context,
        gameId: Int,
    ) {
        runCatching { runningGameFile(context).writeText(gameId.toString()) }
    }

    fun clearRunning(context: Context) {
        runCatching { runningGameFile(context).delete() }
    }

    /** Asks the running game to save and quit, then waits for its process to let go. */
    suspend fun stopRunningGame(context: Context): Boolean {
        if (!isGameRunning(context)) return true

        Log.d(TAG, "asking the running game to stop")
        val intent = Intent(ACTION_STOP_GAME).setPackage(context.packageName)
        context.sendBroadcast(intent)

        var waited = 0L
        while (waited < STOP_TIMEOUT_MS) {
            delay(POLL_INTERVAL_MS)
            waited += POLL_INTERVAL_MS
            if (!isGameRunning(context)) {
                Log.d(TAG, "game stopped after $waited ms")
                return true
            }
        }
        Log.w(TAG, "game did not stop within $STOP_TIMEOUT_MS ms")
        return false
    }

    private fun runningGameFile(context: Context) = File(context.filesDir, RUNNING_GAME_FILE)
}
