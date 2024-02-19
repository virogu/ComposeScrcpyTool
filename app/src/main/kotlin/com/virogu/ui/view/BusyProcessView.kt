package com.virogu.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import theme.ClockLoader
import theme.Icon

@Composable
fun BusyProcessView(
    modifier: Modifier,
    isBusy: Boolean,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isBusy
    ) {
        val infiniteTransition by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        Icon(
            modifier = modifier.rotate(infiniteTransition),
            painter = Icon.Outlined.ClockLoader,
            contentDescription = "运行状态"
        )
    }
}