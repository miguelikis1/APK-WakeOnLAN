package com.wol.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wol.app.data.AppSettings
import com.wol.app.ui.theme.CyanPrimary
import com.wol.app.ui.theme.DarkBorder
import com.wol.app.ui.theme.DarkSurface
import com.wol.app.ui.theme.DarkSurfaceVariant
import com.wol.app.ui.theme.TextMuted
import com.wol.app.ui.theme.TextPrimary
import com.wol.app.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    initialSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var brokerAddress by remember { mutableStateOf(initialSettings.brokerAddress) }
    var brokerPort by remember { mutableStateOf(initialSettings.brokerPort.toString()) }
    var mqttUsername by remember { mutableStateOf(initialSettings.mqttUsername) }
    var mqttPassword by remember { mutableStateOf(initialSettings.mqttPassword) }
    var commandTopic by remember { mutableStateOf(initialSettings.commandTopic) }
    var statusTopic by remember { mutableStateOf(initialSettings.statusTopic) }
    var pcTailscaleIp by remember { mutableStateOf(initialSettings.pcTailscaleIp) }
    var pcCheckPort by remember { mutableStateOf(initialSettings.pcCheckPort.toString()) }

    var isPasswordVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: EMQX Broker & TLS
                SettingsSectionHeader(title = "Broker MQTT (EMQX Cloud TLS)")

                CustomTextField(
                    value = brokerAddress,
                    onValueChange = { brokerAddress = it },
                    label = "Broker Address",
                    placeholder = "ej. xyz123.emqxsl.com",
                    leadingIcon = Icons.Default.Dns
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomTextField(
                    value = brokerPort,
                    onValueChange = { brokerPort = it },
                    label = "Puerto TLS",
                    placeholder = "8883",
                    keyboardType = KeyboardType.Number,
                    leadingIcon = Icons.Default.Lock
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomTextField(
                    value = mqttUsername,
                    onValueChange = { mqttUsername = it },
                    label = "Usuario MQTT",
                    placeholder = "usuario",
                    leadingIcon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password with show/hide toggle
                OutlinedTextField(
                    value = mqttPassword,
                    onValueChange = { mqttPassword = it },
                    label = { Text("Contraseña MQTT", color = TextSecondary) },
                    placeholder = { Text("contraseña", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CyanPrimary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Ocultar" else "Mostrar",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = customTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: MQTT Topics
                SettingsSectionHeader(title = "Topics MQTT")

                CustomTextField(
                    value = commandTopic,
                    onValueChange = { commandTopic = it },
                    label = "Topic de Comando (WoL)",
                    placeholder = "casa/pc/wol",
                    leadingIcon = Icons.Default.Send
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomTextField(
                    value = statusTopic,
                    onValueChange = { statusTopic = it },
                    label = "Topic de Respuesta / Estado",
                    placeholder = "casa/pc/status",
                    leadingIcon = Icons.Default.Sensors
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Remote PC Check (Tailscale)
                SettingsSectionHeader(title = "Comprobación PC Remoto (Tailscale)")

                CustomTextField(
                    value = pcTailscaleIp,
                    onValueChange = { pcTailscaleIp = it },
                    label = "IP de Tailscale del PC",
                    placeholder = "ej. 100.85.12.34",
                    leadingIcon = Icons.Default.Lan
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomTextField(
                    value = pcCheckPort,
                    onValueChange = { pcCheckPort = it },
                    label = "Puerto TCP (RDP 3389 / SSH 22)",
                    placeholder = "3389",
                    keyboardType = KeyboardType.Number,
                    leadingIcon = Icons.Default.Lock
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val parsedPort = brokerPort.toIntOrNull() ?: 8883
                            val parsedPcPort = pcCheckPort.toIntOrNull() ?: 3389
                            val updated = AppSettings(
                                brokerAddress = brokerAddress.trim(),
                                brokerPort = parsedPort,
                                mqttUsername = mqttUsername.trim(),
                                mqttPassword = mqttPassword,
                                commandTopic = commandTopic.trim().ifBlank { "casa/pc/wol" },
                                statusTopic = statusTopic.trim().ifBlank { "casa/pc/status" },
                                pcTailscaleIp = pcTailscaleIp.trim(),
                                pcCheckPort = parsedPcPort
                            )
                            onSave(updated)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF002229)
                        )
                    ) {
                        Text("Guardar y Conectar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        ),
        color = CyanPrimary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        placeholder = { Text(placeholder, color = TextMuted) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = CyanPrimary
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = customTextFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkSurfaceVariant,
    unfocusedContainerColor = DarkSurfaceVariant,
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = DarkBorder,
    cursorColor = CyanPrimary
)
