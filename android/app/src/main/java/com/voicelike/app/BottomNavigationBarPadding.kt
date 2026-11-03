package com.voicelike.app

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

internal fun Modifier.bottomNavigationBarPadding(extraBottom: Dp): Modifier =
    navigationBarsPadding().padding(bottom = extraBottom)
