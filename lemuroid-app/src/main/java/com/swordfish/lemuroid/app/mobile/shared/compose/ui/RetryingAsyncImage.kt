package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swordfish.lemuroid.lib.library.metadata.LocalCovers
import kotlinx.coroutines.delay

private const val MAX_ATTEMPTS = 6
private const val RETRY_BASE_DELAY_MS = 1500L

/**
 * An [AsyncImage] that retries local covers a few times. When Lemuroid is the home screen it
 * starts before Android's storage provider at boot, so the first load of a content:// cover can
 * fail even though the file is fine.
 */
@Composable
fun RetryingAsyncImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
    fallback: Painter? = null,
    error: Painter? = null,
) {
    var attempt by remember(model) { mutableIntStateOf(0) }
    var failed by remember(model) { mutableStateOf(false) }
    val retryable = LocalCovers.isLocalUri(model)

    LaunchedEffect(model, failed, attempt) {
        if (failed && retryable && attempt < MAX_ATTEMPTS) {
            delay(RETRY_BASE_DELAY_MS * (attempt + 1))
            failed = false
            attempt += 1
        }
    }

    val request =
        ImageRequest.Builder(LocalContext.current)
            .data(model)
            .apply {
                if (model != null) {
                    memoryCacheKey("$model#$attempt")
                }
            }
            .build()

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        fallback = fallback,
        error = error,
        onError = { failed = true },
    )
}
