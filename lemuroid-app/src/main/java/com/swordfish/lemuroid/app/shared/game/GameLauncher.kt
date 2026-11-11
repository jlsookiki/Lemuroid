package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GameLauncher(
    private val coresSelection: CoresSelection,
    private val gameLaunchTaskHandler: GameLaunchTaskHandler,
) {
    @OptIn(DelicateCoroutinesApi::class)
    fun launchGameAsync(
        activity: Activity,
        game: Game,
        loadSave: Boolean,
        leanback: Boolean,
    ): Boolean {
        val appContext = activity.applicationContext
        val runningGameId = GameProcessController.runningGameId(appContext)
        val resumeRunningGame = runningGameId == game.id && loadSave
        android.util.Log.d(
            "GameProcessController",
            "launch requested game=${game.id} running=$runningGameId resume=$resumeRunningGame",
        )

        GlobalScope.launch {
            if (!resumeRunningGame && GameProcessController.isGameRunning(appContext)) {
                val stopped = GameProcessController.stopRunningGame(appContext)
                if (!stopped) {
                    activity.runOnUiThread { activity.displayToast(R.string.game_process_another_game_running) }
                    return@launch
                }
            }

            val system = GameSystem.findById(game.systemId)
            val coreConfig = coresSelection.getCoreConfigForSystem(system)
            if (!resumeRunningGame) {
                gameLaunchTaskHandler.handleGameStart(appContext, game)
            }
            BaseGameActivity.launchGame(activity, coreConfig, game, loadSave, leanback)
        }

        return true
    }
}
