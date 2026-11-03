package com.voicelike.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset

/**
 * Fluid Transitions Collection
 * A set of physics-based, light-weight animations for page transitions.
 */
object FluidTransitions {
    
    // Spring specs for different weights
    private val SpringStiff = spring<Float>(stiffness = Spring.StiffnessMedium)
    private val SpringBouncy = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f)
    private val SpringGentle = spring<Float>(stiffness = Spring.StiffnessLow)
    private val SpringOffset = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)

    /**
     * Shared Axis Z - Like entering a deeper layer
     * Effect: Slide Up + Scale Up + Fade In
     */
    val SheetEnter: EnterTransition = 
        slideInVertically(
            initialOffsetY = { it }, // Slide from full bottom
            animationSpec = SpringOffset
        ) + fadeIn(
            animationSpec = tween(300)
        ) + scaleIn(
            initialScale = 0.92f,
            animationSpec = SpringBouncy
        )

    val SheetExit: ExitTransition = 
        slideOutVertically(
            targetOffsetY = { it }, // Slide to full bottom
            animationSpec = SpringOffset
        ) + fadeOut(
            animationSpec = tween(300)
        ) + scaleOut(
            targetScale = 0.92f,
            animationSpec = SpringStiff
        )

    /**
     * Parallax Page Enter - For pushing a new page onto the stack
     * Effect: Slide in from Right with slight parallax feel + subtle scale
     */
    val ParallaxPushEnter: EnterTransition = 
        slideInHorizontally(
            initialOffsetX = { (it * 1.0).toInt() }, // From right
            animationSpec = SpringOffset
        ) + fadeIn(
            animationSpec = tween(200)
        ) + scaleIn(
            initialScale = 0.96f, // Subtle scale up
            animationSpec = SpringBouncy
        )

    /**
     * Parallax Page Exit - For popping the current page
     * Effect: Slide out to Right
     */
    val ParallaxPopExit: ExitTransition = 
        slideOutHorizontally(
            targetOffsetX = { (it * 1.0).toInt() }, // To right
            animationSpec = SpringOffset
        ) + fadeOut(
            animationSpec = tween(200)
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = SpringStiff
        )

    /**
     * Scale Up Enter - For overlays or dialogs
     * Effect: Pop up from center
     */
    val PopEnter: EnterTransition = 
        scaleIn(
            initialScale = 0.9f,
            animationSpec = SpringBouncy
        ) + fadeIn(
            animationSpec = tween(200)
        )

    val PopExit: ExitTransition = 
        scaleOut(
            targetScale = 0.9f,
            animationSpec = SpringStiff
        ) + fadeOut(
            animationSpec = tween(200)
        )
        
    /**
     * Subtle Slide Up - For less intrusive overlays
     */
    val SubtleSlideIn: EnterTransition =
        slideInVertically(
            initialOffsetY = { (it * 0.1).toInt() }, // Only 10% movement
            animationSpec = SpringOffset
        ) + fadeIn(
            animationSpec = tween(300)
        )
        
    val SubtleSlideOut: ExitTransition =
        slideOutVertically(
            targetOffsetY = { (it * 0.1).toInt() },
            animationSpec = SpringOffset
        ) + fadeOut(
            animationSpec = tween(300)
        )
}