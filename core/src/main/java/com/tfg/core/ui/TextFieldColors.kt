package com.tfg.core.ui

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import com.tfg.core.ui.theme.AccentBlue
import com.tfg.core.ui.theme.DarkBorder
import com.tfg.core.ui.theme.TextPrimary
import com.tfg.core.ui.theme.TextSecondary

@Composable
fun tfgTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = DarkBorder,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
