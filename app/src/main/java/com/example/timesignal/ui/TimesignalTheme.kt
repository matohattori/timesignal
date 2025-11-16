package com.example.timesignal.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Typography

@Composable
fun TimesignalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(),
        typography = Typography(),
        content = content,
    )
}
