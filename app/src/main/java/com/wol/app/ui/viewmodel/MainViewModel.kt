package com.wol.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wol.app.data.AppSettings
import com.wol.app.data.SettingsRepository
import com.wol.app.network.MqttConnectionState
import com.wol.app.network.MqttManager
import com.wol.app.network.StatusMessage
import com.wol.app.network.TcpPingManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed class PcStatus {
    object Unknown : PcStatus()
    object Checking : PcStatus()
    data class Online(val lastChecked: Long = System.currentTimeMillis()) : PcStatus()
    data class Offline(val lastChecked: Long = System.currentTimeMillis()) : PcStatus()
}

sealed class WakeButtonState {
    object Idle : WakeButtonState()
    object Publishing : WakeButtonState()
    data class AwaitingConfirmation(val secondsRemaining: Int) : WakeButtonState()
    data class PollingPc(val attempt: Int, val maxAttempts: Int = 12) : WakeButtonState()
}

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val mqttManager: MqttManager = MqttManager()
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val mqttConnectionState: StateFlow<MqttConnectionState> = mqttManager.connectionState
    val lastMqttMessage: StateFlow<StatusMessage?> = mqttManager.lastReceivedMessage

    private val _pcStatus = MutableStateFlow<PcStatus>(PcStatus.Unknown)
    val pcStatus: StateFlow<PcStatus> = _pcStatus.asStateFlow()

    private val _wakeButtonState = MutableStateFlow<WakeButtonState>(WakeButtonState.Idle)
    val wakeButtonState: StateFlow<WakeButtonState> = _wakeButtonState.asStateFlow()

    private val _snackbarChannel = Channel<String>(Channel.BUFFERED)
    val snackbarEvents = _snackbarChannel.receiveAsFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private var wakeJob: Job? = null
    private var pingJob: Job? = null

    init {
        // Observe settings changes
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { newSettings ->
                val prev = _settings.value
                _settings.value = newSettings

                // Auto-connect if broker address changed and is configured
                if (newSettings.isBrokerConfigured &&
                    (newSettings.brokerAddress != prev.brokerAddress ||
                     newSettings.brokerPort != prev.brokerPort ||
                     newSettings.mqttUsername != prev.mqttUsername ||
                     newSettings.mqttPassword != prev.mqttPassword ||
                     newSettings.statusTopic != prev.statusTopic)
                ) {
                    reconnectMqtt()
                }

                // Check PC status if configured and currently unknown
                if (newSettings.isPcConfigured && _pcStatus.value is PcStatus.Unknown) {
                    checkPcStatus()
                }
            }
        }
    }

    fun openSettings() {
        _showSettingsDialog.value = true
    }

    fun closeSettings() {
        _showSettingsDialog.value = false
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.saveSettings(newSettings)
                _showSettingsDialog.value = false
                _snackbarChannel.send("Ajustes guardados correctamente")
                reconnectMqtt()
                if (newSettings.isPcConfigured) {
                    checkPcStatus()
                }
            } catch (e: Exception) {
                _snackbarChannel.send("Error al guardar ajustes: ${e.localizedMessage}")
            }
        }
    }

    fun reconnectMqtt() {
        viewModelScope.launch {
            val current = _settings.value
            if (!current.isBrokerConfigured) {
                _snackbarChannel.send("Configura los datos del Broker en Ajustes")
                return@launch
            }
            mqttManager.connect(current).onFailure { error ->
                _snackbarChannel.send("Error al conectar MQTT: ${error.localizedMessage}")
            }
        }
    }

    fun checkPcStatus() {
        val currentSettings = _settings.value
        if (!currentSettings.isPcConfigured) {
            _snackbarChannel.send("Configura la IP de Tailscale y puerto del PC en Ajustes")
            return
        }

        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            _pcStatus.value = PcStatus.Checking
            val isOnline = TcpPingManager.checkHostOnline(
                host = currentSettings.pcTailscaleIp,
                port = currentSettings.pcCheckPort,
                timeoutMs = 1500
            )
            _pcStatus.value = if (isOnline) {
                PcStatus.Online()
            } else {
                PcStatus.Offline()
            }
        }
    }

    /**
     * Executes the Wake-on-LAN flow:
     * 1. Publishes "WAKE" (QoS 1) to command topic.
     * 2. Waits for "WOL_SENT" in response topic or 5s timeout.
     * 3. Starts automatic polling of PC TCP status every 5 seconds for 1 minute (12 attempts).
     */
    fun sendWakeOnLan() {
        if (_wakeButtonState.value !is WakeButtonState.Idle) return

        val currentSettings = _settings.value
        if (!currentSettings.isBrokerConfigured) {
            viewModelScope.launch {
                _snackbarChannel.send("Configura el broker MQTT en Ajustes antes de enviar la orden")
                openSettings()
            }
            return
        }

        wakeJob?.cancel()
        wakeJob = viewModelScope.launch {
            try {
                // Ensure broker is connected
                if (mqttConnectionState.value !is MqttConnectionState.Connected) {
                    _wakeButtonState.value = WakeButtonState.Publishing
                    _snackbarChannel.send("Conectando con EMQX Cloud...")
                    val connectResult = mqttManager.connect(currentSettings)
                    if (connectResult.isFailure) {
                        _snackbarChannel.send("Fallo al conectar al broker: ${connectResult.exceptionOrNull()?.localizedMessage}")
                        _wakeButtonState.value = WakeButtonState.Idle
                        return@launch
                    }
                }

                // 1. Publish "WAKE" payload
                _wakeButtonState.value = WakeButtonState.Publishing
                val publishResult = mqttManager.publish(
                    topic = currentSettings.commandTopic,
                    payload = "WAKE"
                )

                if (publishResult.isFailure) {
                    _snackbarChannel.send("Error al publicar WAKE: ${publishResult.exceptionOrNull()?.localizedMessage}")
                    _wakeButtonState.value = WakeButtonState.Idle
                    return@launch
                }

                // 2. Await "WOL_SENT" response or 5-second timeout
                var receivedConfirmation = false
                val timeoutMs = 5000L
                val startTime = System.currentTimeMillis()

                // Launch countdown timer for UI feedback
                val countdownJob = launch {
                    for (sec in 5 downTo 1) {
                        _wakeButtonState.value = WakeButtonState.AwaitingConfirmation(sec)
                        delay(1000L)
                    }
                }

                val confirmationResult = withTimeoutOrNull(timeoutMs) {
                    mqttManager.incomingMessages.first { msg ->
                        msg.contains("WOL_SENT", ignoreCase = true)
                    }
                }

                countdownJob.cancel()

                if (confirmationResult != null) {
                    receivedConfirmation = true
                    _snackbarChannel.send("Confirmado por ESP32: WOL_SENT")
                } else {
                    _snackbarChannel.send("Sin confirmación de WOL en 5s. Comprobando PC...")
                }

                // 3. Automatic PC TCP Polling every 5 seconds for 1 minute (12 checks)
                if (currentSettings.isPcConfigured) {
                    val maxAttempts = 12
                    for (attempt in 1..maxAttempts) {
                        _wakeButtonState.value = WakeButtonState.PollingPc(
                            attempt = attempt,
                            maxAttempts = maxAttempts
                        )

                        _pcStatus.value = PcStatus.Checking
                        val isOnline = TcpPingManager.checkHostOnline(
                            host = currentSettings.pcTailscaleIp,
                            port = currentSettings.pcCheckPort,
                            timeoutMs = 1500
                        )

                        if (isOnline) {
                            _pcStatus.value = PcStatus.Online()
                            _snackbarChannel.send("¡El PC remoto está encendido y accesible!")
                            break
                        } else {
                            _pcStatus.value = PcStatus.Offline()
                        }

                        if (attempt < maxAttempts) {
                            delay(5000L)
                        }
                    }
                } else {
                    _snackbarChannel.send("Configura la IP de Tailscale para monitorizar el encendido del PC")
                }

            } catch (e: Exception) {
                _snackbarChannel.send("Error durante la operación: ${e.localizedMessage}")
            } finally {
                _wakeButtonState.value = WakeButtonState.Idle
            }
        }
    }

    fun cancelWakeOperation() {
        wakeJob?.cancel()
        _wakeButtonState.value = WakeButtonState.Idle
        viewModelScope.launch {
            _snackbarChannel.send("Operación cancelada")
        }
    }

    override fun onCleared() {
        super.onCleared()
        wakeJob?.cancel()
        pingJob?.cancel()
        viewModelScope.launch {
            mqttManager.disconnect()
        }
    }
}
