package com.swordfish.lemuroid.app.shared.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.gamecrash.GameCrashActivity
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork
import com.swordfish.lemuroid.app.shared.storage.cache.CacheCleanerWork
import com.swordfish.lemuroid.ext.feature.review.ReviewManager
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SaveProfilesManager
import kotlinx.coroutines.delay

class GameLaunchTaskHandler(
    private val reviewManager: ReviewManager,
    private val retrogradeDb: RetrogradeDatabase,
    private val coresSelection: CoresSelection,
    private val saveProfilesManager: SaveProfilesManager,
) {
    fun handleGameStart(context: Context) {
        cancelBackgroundWork(context)
    }

    /**
     * Stamp the game as played when it starts, not only when it exits cleanly. Leaving a game
     * with the Home button never produces an activity result, so the recents row would otherwise
     * stay empty for anyone who relies on automatic resume.
     */
    suspend fun handleGameStart(
        context: Context,
        game: Game,
    ) {
        handleGameStart(context)
        updateGamePlayedTimestamp(game)
        mirrorActiveSaveProfile(game)
    }

    /** Keeps the active profile's file next to the ROM current with the previous session's save. */
    private suspend fun mirrorActiveSaveProfile(game: Game) {
        runCatching {
            val coreConfig = coresSelection.getCoreConfigForSystem(GameSystem.findById(game.systemId))
            saveProfilesManager.mirrorActive(game, coreConfig)
        }
    }

    suspend fun handleGameFinish(
        enableRatingFlow: Boolean,
        activity: Activity,
        resultCode: Int,
        data: Intent?,
    ) {
        rescheduleBackgroundWork(activity.applicationContext)
        when (resultCode) {
            Activity.RESULT_OK -> handleSuccessfulGameFinish(activity, enableRatingFlow, data)
            BaseGameActivity.RESULT_ERROR ->
                handleUnsuccessfulGameFinish(
                    activity,
                    data?.getStringExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR)!!,
                    null,
                )
            BaseGameActivity.RESULT_UNEXPECTED_ERROR ->
                handleUnsuccessfulGameFinish(
                    activity,
                    activity.getString(R.string.lemuroid_crash_disclamer),
                    data?.getStringExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR),
                )
        }
    }

    private fun cancelBackgroundWork(context: Context) {
        SaveSyncWork.cancelAutoWork(context)
        SaveSyncWork.cancelManualWork(context)
        CacheCleanerWork.cancelCleanCacheLRU(context)
    }

    private fun rescheduleBackgroundWork(context: Context) {
        // Let's slightly delay the sync. Maybe the user wants to play another game.
        SaveSyncWork.enqueueAutoWork(context, 5)
        CacheCleanerWork.enqueueCleanCacheLRU(context)
    }

    private fun handleUnsuccessfulGameFinish(
        activity: Activity,
        message: String,
        messageDetail: String?,
    ) {
        GameCrashActivity.launch(activity, message, messageDetail)
    }

    private suspend fun handleSuccessfulGameFinish(
        activity: Activity,
        enableRatingFlow: Boolean,
        data: Intent?,
    ) {
        val duration =
            data?.extras?.getLong(BaseGameActivity.PLAY_GAME_RESULT_SESSION_DURATION)
                ?: 0L
        val game = data?.extras?.getSerializable(BaseGameActivity.PLAY_GAME_RESULT_GAME) as Game

        updateGamePlayedTimestamp(game)
        if (enableRatingFlow) {
            displayReviewRequest(activity, duration)
        }
    }

    private suspend fun displayReviewRequest(
        activity: Activity,
        durationMillis: Long,
    ) {
        delay(500)
        reviewManager.launchReviewFlow(activity, durationMillis)
    }

    private suspend fun updateGamePlayedTimestamp(game: Game) {
        retrogradeDb.gameDao().update(game.copy(lastPlayedAt = System.currentTimeMillis()))
    }
}
