package com.wol.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wol.app.ui.components.GiantWakeButton
import com.wol.app.ui.components.StatusCardSection
import com.wol.app.ui.theme.CyanPrimary
import com.wol.app.ui.theme.DarkBackground
import com.wol.app.ui.theme.TextPrimary
import com.wol.app.ui.theme.TextSecondary
import com.wol.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val mqttState by viewModel.mqttConnectionState.collectAsState()
    val lastMessage by viewModel.lastMqttMessage.collectAsState()
    val pcStatus by viewModel.pcStatus.collectAsState()
    val wakeState by viewModel.wakeButtonState.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to one-time snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Wake On LAN",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = CyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Status indicators card section
                StatusCardSection(
                    settings = settings,
                    mqttState = mqttState,
                    lastMessage = lastMessage,
                    pcStatus = pcStatus,
                    onReconnectMqtt = { viewModel.reconnectMqtt() },
                    onRefreshPc = { viewModel.checkPcStatus() }
                )

                // Center: Giant Circular Wake-on-LAN Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GiantWakeButton(
                        state = wakeState,
                        onWakeClick = { viewModel.sendWakeOnLan() },
                        onCancelClick = { viewModel.cancelWakeOperation() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            initialSettings = settings,
            onSave = { updatedSettings ->
                viewModel.saveSettings(updatedSettings)
            },
            onDismiss = {
                viewModel.closeSettings()
            }
        )
    }
}
