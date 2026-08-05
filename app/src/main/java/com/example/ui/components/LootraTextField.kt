package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LootraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    testTag: String = ""
) {
    val isDark = isSystemInDarkTheme()
    var isFocused by remember { mutableStateOf(false) }

    // Glow and background colors depending on mode and state
    val backgroundColor = when {
        isError && isDark -> Color(0x1FFF4444)
        isError -> Color(0x0FFF4444)
        isFocused && isDark -> Color(0x1F10B981)
        isFocused -> Color(0x0A0F172A)
        isDark -> Color(0x0DFFFFFF) // Thin glassy white
        else -> Color(0x08000000)  // Ultra thin slate black
    }

    val borderColor = when {
        isError -> Color(0xFFEF4444)
        isFocused && isDark -> Color(0xFF10B981)
        isFocused -> Color(0xFF0F172A)
        isDark -> Color(0x18FFFFFF)
        else -> Color(0x1A000000)
    }

    val labelColor = when {
        isError -> Color(0xFFEF4444)
        isFocused && isDark -> Color(0xFF10B981)
        isFocused -> Color(0xFF0F172A)
        isDark -> Color(0xFF94A3B8)
        else -> Color(0xFF475569)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Label header text with high typography alignment
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            ),
            color = labelColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Glassmorphic interactive input shell
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isError) Color(0xFFEF4444) else labelColor,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = backgroundColor,
                unfocusedContainerColor = backgroundColor,
                errorContainerColor = backgroundColor,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                errorBorderColor = borderColor,
                cursorColor = if (isDark) Color(0xFF10B981) else Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .testTag(testTag)
        )

        // Error message animator below the text field
        AnimatedVisibility(
            visible = isError && !errorMessage.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
