package com.swordfish.lemuroid.app.mobile.feature.gamedetails

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameImage
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.RetryingAsyncImage
import com.swordfish.lemuroid.app.shared.gamemenu.GameMenuHelper
import com.swordfish.lemuroid.app.utils.games.GameUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SaveInfo

private val CoverShape = RoundedCornerShape(12.dp)

@Composable
fun GameDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: GameDetailsViewModel,
    onGamePlay: (Game) -> Unit,
    onGameRestart: (Game) -> Unit,
) {
    val context = LocalContext.current
    val state = viewModel.state.collectAsState(GameDetailsViewModel.UIState()).value
    val game = state.game

    LaunchedEffect(state.message) {
        when (state.message) {
            GameDetailsViewModel.MESSAGE_DONE ->
                Toast.makeText(context, R.string.game_details_action_done, Toast.LENGTH_SHORT).show()
            GameDetailsViewModel.MESSAGE_FAILED ->
                Toast.makeText(context, R.string.game_details_action_failed, Toast.LENGTH_SHORT).show()
        }
        if (state.message != null) viewModel.dismissMessage()
    }

    if (game == null) {
        Box(modifier = modifier.fillMaxSize()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs =
        listOf(
            stringResource(R.string.game_details_tab_about),
            stringResource(R.string.game_details_tab_saves),
            stringResource(R.string.game_details_tab_achievements),
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        GameHeader(
            game = game,
            state = state,
            onPlay = { onGamePlay(game) },
            onRestart = { onGameRestart(game) },
            onToggleFavorite = { viewModel.toggleFavorite() },
        )
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Box(modifier = Modifier.padding(16.dp)) {
            when (selectedTab) {
                0 -> AboutTab(state)
                1 -> SavesTab(state, viewModel)
                else -> AchievementsTab()
            }
        }
    }
}

@Composable
private fun GameHeader(
    game: Game,
    state: GameDetailsViewModel.UIState,
    onPlay: () -> Unit,
    onRestart: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.background
    val subtitle = remember(game.id) { GameUtils.getGameSubtitle(context, game) }
    val title = state.sidecar.name ?: game.title
    val facts =
        listOfNotNull(
            state.sidecar.developer ?: game.developer,
            state.sidecar.releaseYear,
            state.sidecar.genre,
        ).filter { it.isNotBlank() }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(232.dp),
    ) {
        RetryingAsyncImage(
            model = game.coverFrontUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
            modifier =
                Modifier
                    .fillMaxSize()
                    .blur(32.dp),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(background.copy(alpha = 0.35f), background.copy(alpha = 0.95f)),
                        ),
                    ),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(184.dp)
                        .clip(CoverShape),
            ) {
                LemuroidGameImage(game = game)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                if (facts.isNotEmpty()) {
                    Text(
                        text = facts.joinToString("  •  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onPlay) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (state.autoSave.exists) {
                                stringResource(R.string.library_resume)
                            } else {
                                stringResource(R.string.game_details_play)
                            },
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = onRestart) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.game_context_menu_restart))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorites),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutTab(state: GameDetailsViewModel.UIState) {
    val description = state.sidecar.description
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (description.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.game_details_no_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        val details =
            listOfNotNull(
                state.sidecar.publisher?.let { stringResource(R.string.game_details_publisher) to it },
                state.sidecar.players?.let { stringResource(R.string.game_details_players) to it },
                state.coreName?.let { stringResource(R.string.game_details_core) to it },
                state.game?.fileName?.let { stringResource(R.string.game_details_file) to it },
            )
        if (details.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            details.forEach { (label, value) ->
                Row {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(120.dp),
                    )
                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SavesTab(
    state: GameDetailsViewModel.UIState,
    viewModel: GameDetailsViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionLabel(stringResource(R.string.game_details_battery_save))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = saveDescription(state.batterySave, stringResource(R.string.game_details_no_save)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text =
                        if (state.exportedBatterySaveExists) {
                            stringResource(R.string.game_details_exported_copy_present)
                        } else {
                            stringResource(R.string.game_details_exported_copy_absent)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.exportBatterySave() },
                        enabled = state.batterySave.exists && !state.busy,
                    ) { Text(stringResource(R.string.game_details_export_save)) }
                    OutlinedButton(
                        onClick = { viewModel.importBatterySave() },
                        enabled = state.exportedBatterySaveExists && !state.busy,
                    ) { Text(stringResource(R.string.game_details_import_save)) }
                }
            }
        }

        SectionLabel(stringResource(R.string.game_details_profiles))
        ProfilesCard(state = state, viewModel = viewModel)

        if (state.statesSupported) {
            SectionLabel(stringResource(R.string.game_details_states))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.game_details_auto_resume),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = saveDescription(state.autoSave, stringResource(R.string.game_details_no_auto_resume)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.slots.forEach { slot ->
                StateSlotCard(slot = slot, busy = state.busy, onSetResumePoint = { viewModel.setResumePoint(slot.index) })
            }
        }
    }
}

@Composable
private fun ProfilesCard(
    state: GameDetailsViewModel.UIState,
    viewModel: GameDetailsViewModel,
) {
    var showNewProfileDialog by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.profiles.forEach { profile ->
                val isActive = profile == state.activeProfile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (isActive) {
                            Text(
                                text = stringResource(R.string.game_details_profile_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.activateProfile(profile) },
                        enabled = !isActive && !state.busy,
                    ) { Text(stringResource(R.string.game_details_profile_use)) }
                }
            }
            Text(
                text = stringResource(R.string.game_details_profile_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { showNewProfileDialog = true }, enabled = !state.busy) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.game_details_profile_new))
            }
        }
    }

    if (showNewProfileDialog) {
        NewProfileDialog(
            onDismiss = { showNewProfileDialog = false },
            onCreate = { name ->
                showNewProfileDialog = false
                viewModel.createProfile(name)
            },
        )
    }
}

@Composable
private fun NewProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.game_details_profile_new)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.game_details_profile_name)) },
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.game_details_profile_create))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) }
        },
    )
}

@Composable
private fun StateSlotCard(
    slot: GameDetailsViewModel.StateSlot,
    busy: Boolean,
    onSetResumePoint: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatePreview(slot.preview)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.game_menu_state, (slot.index + 1).toString()),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = saveDescription(slot.info, stringResource(R.string.game_details_empty_slot)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onSetResumePoint, enabled = slot.info.exists && !busy) {
                Text(stringResource(R.string.game_details_set_resume_point))
            }
        }
    }
}

@Composable
private fun StatePreview(bitmap: Bitmap?) {
    Box(
        modifier =
            Modifier
                .size(width = 96.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AchievementsTab() {
    Text(
        text = stringResource(R.string.game_details_achievements_placeholder),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun saveDescription(
    info: SaveInfo,
    emptyText: String,
): String {
    return if (info.exists) GameMenuHelper.getSaveStateDescription(info) else emptyText
}
