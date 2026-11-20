package com.wol.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wol.app.data.AppSettings
import com.wol.app.network.MqttConnectionState
import com.wol.app.network.StatusMessage
import com.wol.app.ui.theme.DarkBorder
import com.wol.app.ui.theme.DarkSurface
import com.wol.app.ui.theme.DarkSurfaceVariant
import com.wol.app.ui.theme.StatusGreen
import com.wol.app.ui.theme.StatusGreenGlow
import com.wol.app.ui.theme.StatusRed
import com.wol.app.ui.theme.StatusRedGlow
import com.wol.app.ui.theme.StatusYellow
import com.wol.app.ui.theme.TextMuted
import com.wol.app.ui.theme.TextPrimary
import com.wol.app.ui.theme.TextSecondary
import com.wol.app.ui.viewmodel.PcStatus

@Composable
fun StatusCardSection(
    settings: AppSettings,
    mqttState: MqttConnectionState,
    lastMessage: StatusMessage?,
    pcStatus: PcStatus,
    onReconnectMqtt: () -> Unit,
    onRefreshPc: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. MQTT Broker & ESP32 Status Card
        MqttBrokerStatusCard(
            settings = settings,
            mqttState = mqttState,
            lastMessage = lastMessage,
            onReconnect = onReconnectMqtt
        )

        // 2. Remote PC (Tailscale) Status Card
        RemotePcStatusCard(
            settings = settings,
            pcStatus = pcStatus,
            onRefresh = onRefreshPc
        )
    }
}

@Composable
fun MqttBrokerStatusCard(
    settings: AppSettings,
    mqttState: MqttConnectionState,
    lastMessage: StatusMessage?,
    onReconnect: () -> Unit
) {
    val (statusLabel, statusColor, glowColor) = when (mqttState) {
        is MqttConnectionState.Connected -> Triple("Conectado", StatusGreen, StatusGreenGlow)
        is MqttConnectionState.Connecting -> Triple("Conectando…", StatusYellow, Color(0x33FFB300))
        is MqttConnectionState.Disconnected -> Triple("Desconectado", StatusRed, StatusRedGlow)
        is MqttConnectionState.Error -> Triple("Error", StatusRed, StatusRedGlow)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Broker",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "EMQX Cloud / ESP32",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (settings.brokerAddress.isNotBlank())
                                "${settings.brokerAddress}:${settings.brokerPort}"
                            else
                                "Broker no configurado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIndicatorBadge(
                        label = statusLabel,
                        color = statusColor,
                        glowColor = glowColor
                    )
                    IconButton(
                        onClick = onReconnect,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reconectar Broker",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Subscribed topic and last message
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Topic: ${settings.statusTopic}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = TextMuted
                        )
                        if (lastMessage != null) {
                            Text(
                                text = lastMessage.formattedTime,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Último estado: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = lastMessage?.content ?: "Sin mensajes aún",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (lastMessage != null) MaterialTheme.colorScheme.primary else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemotePcStatusCard(
    settings: AppSettings,
    pcStatus: PcStatus,
    onRefresh: () -> Unit
) {
    val (statusLabel, statusColor, glowColor) = when (pcStatus) {
        is PcStatus.Online -> Triple("ONLINE", StatusGreen, StatusGreenGlow)
        is PcStatus.Offline -> Triple("OFFLINE", StatusRed, StatusRedGlow)
        is PcStatus.Checking -> Triple("COMPROBANDO", StatusYellow, Color(0x33FFB300))
        is PcStatus.Unknown -> Triple("DESCONOCIDO", TextMuted, Color.Transparent)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = "PC",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PC Remoto (Tailscale)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (settings.pcTailscaleIp.isNotBlank())
                            "${settings.pcTailscaleIp}:${settings.pcCheckPort} (TCP)"
                        else
                            "IP no configurada",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIndicatorBadge(
                    label = statusLabel,
                    color = statusColor,
                    glowColor = glowColor,
                    isChecking = pcStatus is PcStatus.Checking
                )
                IconButton(
                    onClick = onRefresh,
                    enabled = pcStatus !is PcStatus.Checking,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (pcStatus is PcStatus.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Comprobar PC",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicatorBadge(
    label: String,
    color: Color,
    glowColor: Color,
    isChecking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isChecking) Modifier.alpha(alphaAnim) else Modifier
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = color
            )
        }
    }
}
