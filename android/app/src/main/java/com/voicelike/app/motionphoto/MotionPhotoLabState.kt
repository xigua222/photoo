package com.voicelike.app.motionphoto

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal data class MotionPhotoLabUiState(
    val preparingWorkspace: Boolean,
    val preparedSession: MotionPhotoSessionPaths?
)

internal class MotionPhotoLabState(
    private val repository: MotionPhotoFileRepository
) {
    var uiState by mutableStateOf(
        MotionPhotoLabUiState(
            preparingWorkspace = false,
            preparedSession = null
        )
    )
        private set

    fun prepareWorkspace(displayName: String): MotionPhotoSessionPaths {
        uiState = uiState.copy(preparingWorkspace = true)
        val session = repository.createSession(displayName)
        uiState = MotionPhotoLabUiState(
            preparingWorkspace = false,
            preparedSession = session
        )
        return session
    }
}

@Composable
internal fun rememberMotionPhotoLabState(
    context: Context
): MotionPhotoLabState {
    val applicationContext = context.applicationContext
    return remember(applicationContext) {
        MotionPhotoLabState(
            repository = MotionPhotoFileRepository(applicationContext)
        )
    }
}
