package com.swordfish.lemuroid.app.mobile.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameImage
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.RetryingAsyncImage
import com.swordfish.lemuroid.app.utils.games.GameUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game

private val TileShape = RoundedCornerShape(12.dp)
private val HeroShape = RoundedCornerShape(16.dp)

/** Cover-first tile that grows and lights up when the gamepad focuses it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SteamGameTile(
    modifier: Modifier = Modifier,
    game: Game,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "tileScale")
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent
    val subtitle = remember(game.id) { GameUtils.getGameSubtitle(context, game) }

    Column(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .onFocusChanged { focused = it.isFocused }
                .clip(TileShape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(TileShape)
                    .border(2.dp, borderColor, TileShape),
        ) {
            LemuroidGameImage(game = game)
        }
        Text(
            text = game.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Full-width banner for the game last played, with its cover blurred behind it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBanner(
    modifier: Modifier = Modifier,
    game: Game,
    onClick: () -> Unit,
    onResume: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent
    val subtitle = remember(game.id) { GameUtils.getGameSubtitle(context, game) }
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(176.dp)
                .onFocusChanged { focused = it.isFocused }
                .clip(HeroShape)
                .border(2.dp, borderColor, HeroShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        RetryingAsyncImage(
            model = game.coverFrontUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.55f,
            modifier =
                Modifier
                    .fillMaxSize()
                    .blur(28.dp),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(background.copy(alpha = 0.92f), background.copy(alpha = 0.35f)),
                        ),
                    ),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(144.dp)
                        .clip(TileShape),
            ) {
                LemuroidGameImage(game = game)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_continue_playing).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onResume) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.library_resume))
                }
            }
        }
    }
}
