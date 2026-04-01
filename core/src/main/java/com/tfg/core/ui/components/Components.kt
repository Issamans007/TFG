package com.tfg.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tfg.core.ui.theme.*

@Composable
fun TfgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    type: ButtonType = ButtonType.PRIMARY
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (type) {
                ButtonType.PRIMARY -> AccentBlue
                ButtonType.BUY -> Green500
                ButtonType.SELL -> Red500
                ButtonType.OUTLINE -> Color.Transparent
            },
            disabledContainerColor = DarkCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

enum class ButtonType { PRIMARY, BUY, SELL, OUTLINE }

@Composable
fun PriceText(
    price: Double,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
    prefix: String = "$"
) {
    Text(
        text = "$prefix${formatPrice(price)}",
        modifier = modifier,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Medium,
        color = TextPrimary
    )
}

@Composable
fun PnlText(
    value: Double,
    modifier: Modifier = Modifier,
    fontSize: Int = 14,
    showSign: Boolean = true,
    showPercent: Boolean = false
) {
    val color by animateColorAsState(
        targetValue = when {
            value > 0 -> ProfitGreen
            value < 0 -> LossRed
            else -> NeutralGray
        }, label = "pnl_color"
    )

    val sign = if (showSign && value > 0) "+" else ""
    val suffix = if (showPercent) "%" else ""
    Text(
        text = "$sign${formatPrice(value)}$suffix",
        modifier = modifier,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun TfgCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DarkCard)
            .border(1.dp, DarkBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentBlue)
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Red400, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", color = AccentBlue)
            }
        }
    }
}

@Composable
fun PinInput(
    pinLength: Int = 6,
    onPinComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var pin by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            BasicTextField(
                value = pin,
                onValueChange = { new ->
                    if (new.length <= pinLength && new.all { it.isDigit() }) {
                        pin = new
                        if (new.length == pinLength) onPinComplete(new)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .size(0.dp), // Hidden, we draw custom dots
                cursorBrush = SolidColor(Color.Transparent)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable { focusRequester.requestFocus() }
            ) {
                repeat(pinLength) { index ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (index < pin.length) AccentBlue else DarkCard)
                            .border(
                                1.dp,
                                if (error != null) Red400 else DarkBorder,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < pin.length) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = Red400, fontSize = 12.sp)
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, color = AccentBlue, fontSize = 13.sp)
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format("%,.2f", price)
        price >= 1 -> String.format("%.2f", price)
        price >= 0.01 -> String.format("%.4f", price)
        else -> String.format("%.8f", price)
    }
}
