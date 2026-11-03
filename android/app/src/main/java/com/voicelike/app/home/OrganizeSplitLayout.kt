package com.voicelike.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

// Wide landscape windows (a foldable's inner display, a tablet held sideways): the card sorting page
// keeps its phone layout in the leading pane while the current card's details (what the (i) sheet
// shows) stay docked in the trailing pane, a deeper level shown beside the deck instead of a
// stretched phone layout (iOS CardSortingView detailSplit). The split follows a vertical fold;
// without one it is the window's centerline with a 32dp gutter so the deck's buttons stay off it.

internal data class OrganizeSplit(
    val leading: Dp,
    val gap: Dp,
    val trailing: Dp,
)

// Split only when the window is landscape and large on both sides (a foldable's inner display or a
// tablet, never a phone turned sideways). A vertical fold at least half the height that does not
// touch the edges sets the panes; otherwise split at the centerline.
internal fun organizeSplit(
    width: Dp,
    height: Dp,
    foldLeft: Dp?,
    foldRight: Dp?,
    foldHeight: Dp?,
): OrganizeSplit? {
    if (minOf(width, height) < 600.dp || width <= height) return null
    if (foldLeft != null &&
        foldRight != null &&
        foldHeight != null &&
        foldHeight >= height / 2 &&
        foldLeft > 0.dp &&
        foldRight < width
    ) {
        return OrganizeSplit(foldLeft, foldRight - foldLeft, width - foldRight)
    }
    val gap = 32.dp
    val mid = (width / 2).coerceIn(width * 0.4f, width * 0.6f)
    return OrganizeSplit(mid - gap / 2, gap, width - mid - gap / 2)
}

// While the page is split the swipe edge glow is drawn here, behind both panes, so it lights from
// the real screen edges and reads as one surface instead of stopping at the fold.
@Stable
internal class OrganizeEdgeGlowHost {
    var state by mutableStateOf<HomeTopCardEdgeGlowState?>(null)
    var enabled by mutableStateOf(true)
    var leftColor by mutableStateOf(Color.Transparent)
    var rightColor by mutableStateOf(Color.Transparent)
    var topColor by mutableStateOf(Color.Transparent)
}

internal val LocalOrganizeEdgeGlowHost = staticCompositionLocalOf<OrganizeEdgeGlowHost?> { null }

@Composable
internal fun OrganizeSplitLayout(
    detailItem: MediaItem?,
    onJumpToDay: ((String) -> Unit)?,
    deck: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var windowX by remember { mutableFloatStateOf(0f) }
    val fold = rememberVerticalFold()
    val glowHost = remember { OrganizeEdgeGlowHost() }
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { windowX = it.positionInWindow().x },
    ) {
        val width = maxWidth
        val height = maxHeight
        val foldDp =
            fold?.let { bounds ->
                with(density) {
                    Triple((bounds.left - windowX).toDp(), (bounds.right - windowX).toDp(), bounds.height().toDp())
                }
            }
        val split = organizeSplit(width, height, foldDp?.first, foldDp?.second, foldDp?.third)

        if (split != null) {
            val state = glowHost.state
            if (state != null) {
                HomeTopCardEdgeGlowLayer(
                    state = state,
                    enabled = glowHost.enabled,
                    leftColor = glowHost.leftColor,
                    rightColor = glowHost.rightColor,
                    topColor = glowHost.topColor,
                )
            }
            // No panel of its own: the pane sits on the same page background as the deck.
            BoxWithConstraints(
                modifier =
                    Modifier
                        .offset(x = split.leading + split.gap)
                        .width(split.trailing)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (detailItem != null) {
                        // A new card swaps the whole pane at once (no per-card reveal: it blinked on fast swipes).
                        key(detailItem.id) {
                            DraggableCardDetailsContent(
                                currentItem = detailItem,
                                onJumpToDay = onJumpToDay,
                                modifier =
                                    Modifier
                                        .widthIn(max = 420.dp)
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 32.dp),
                            )
                        }
                    }
                }
            }
        }
        // The deck comes last so cards flung toward the details fly over that pane.
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(split?.leading ?: width),
        ) {
            CompositionLocalProvider(LocalOrganizeEdgeGlowHost provides if (split != null) glowHost else null) {
                deck()
            }
        }
    }
}

// Bounds of a vertical fold crossing the window, in window pixels; reported whether the device is
// flat or half open, so the layout does not move while it bends.
@Composable
private fun rememberVerticalFold(): android.graphics.Rect? {
    val activity = LocalContext.current.findActivity() ?: return null
    val flow = remember(activity) { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
    val info by flow.collectAsState(initial = null)
    return info
        ?.displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull { it.orientation == FoldingFeature.Orientation.VERTICAL }
        ?.bounds
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
