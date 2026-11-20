package com.wol.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wol.app.ui.theme.CyanPrimary
import com.wol.app.ui.theme.CyanPrimaryVariant
import com.wol.app.ui.theme.DarkBorder
import com.wol.app.ui.theme.DarkSurface
import com.wol.app.ui.theme.DarkSurfaceVariant
import com.wol.app.ui.theme.StatusGreen
import com.wol.app.ui.theme.TextMuted
import com.wol.app.ui.theme.TextPrimary
import com.wol.app.ui.theme.TextSecondary
import com.wol.app.ui.viewmodel.WakeButtonState

@Composable
fun GiantWakeButton(
    state: WakeButtonState,
    onWakeClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = state !is WakeButtonState.Idle

    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlowScale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            // Pulsing background glow ring when active
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CyanPrimary.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Outer decorative ring
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = if (isLoading) {
                            Brush.sweepGradient(
                                listOf(CyanPrimary, CyanPrimaryVariant, CyanPrimary)
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(DarkBorder, DarkSurfaceVariant)
                            )
                        },
                        shape = CircleShape
                    )
            )

            // Progress indicator ring when loading
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(200.dp),
                    color = CyanPrimary,
                    strokeWidth = 3.dp,
                    trackColor = DarkSurfaceVariant
                )
            }

            // Main clickable circle button
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .shadow(elevation = if (isLoading) 16.dp else 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = if (isLoading) {
                                listOf(DarkSurfaceVariant, DarkSurface)
                            } else {
                                listOf(DarkSurface, Color(0xFF0F1116))
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isLoading) CyanPrimary.copy(alpha = 0.6f) else DarkBorder,
                        shape = CircleShape
                    )
                    .clickable(enabled = !isLoading) {
                        onWakeClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Encender PC",
                        tint = if (isLoading) CyanPrimary else TextPrimary,
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ENCENDER PC",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (isLoading) CyanPrimary else TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // State description and progress info
        val (subtitleText, subtitleColor) = when (state) {
            is WakeButtonState.Idle ->
                Pair("Toca para enviar orden Wake-on-LAN", TextSecondary)
            is WakeButtonState.Publishing ->
                Pair("Publicando comando WAKE (QoS 1)…", CyanPrimary)
            is WakeButtonState.AwaitingConfirmation ->
                Pair("Esperando confirmación ESP32 (${state.secondsRemaining}s)…", CyanPrimary)
            is WakeButtonState.PollingPc ->
                Pair("Comprobando encendido del PC (${state.attempt}/${state.maxAttempts})…", StatusGreen)
        }

        Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = subtitleColor,
            textAlign = TextAlign.Center
        )

        // Cancel button during long polling
        AnimatedVisibility(visible = state is WakeButtonState.PollingPc) {
            TextButton(
                onClick = onCancelClick,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Detener",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "Detener sondeo",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
        }
    }
}
