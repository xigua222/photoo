package com.voicelike.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun PageAnimatedVisibility(
    visible: Boolean,
    enter: EnterTransition,
    exit: ExitTransition,
    modifier: Modifier = Modifier,
    restartContentOnOpen: Boolean = true,
    content: @Composable () -> Unit
) {
    var openKey by remember { mutableIntStateOf(0) }
    val blockerInteraction = remember { MutableInteractionSource() }
    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(visible) {
        visibleState.targetState = visible
        if (restartContentOnOpen) {
            if (visible) {
                openKey++
            }
        }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = enter,
        exit = exit,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = blockerInteraction,
                    indication = null
                ) {}
        ) {
            if (restartContentOnOpen) {
                key(openKey) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}
