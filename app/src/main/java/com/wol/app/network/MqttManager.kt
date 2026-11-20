package com.wol.app.network

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.wol.app.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

sealed class MqttConnectionState {
    object Disconnected : MqttConnectionState()
    object Connecting : MqttConnectionState()
    object Connected : MqttConnectionState()
    data class Error(val message: String) : MqttConnectionState()
}

data class StatusMessage(
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

class MqttManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var client: Mqtt5AsyncClient? = null
    private var currentSettings: AppSettings? = null

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val _lastReceivedMessage = MutableStateFlow<StatusMessage?>(null)
    val lastReceivedMessage: StateFlow<StatusMessage?> = _lastReceivedMessage.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    /**
     * Connects to EMQX Cloud Serverless broker using TLS via HiveMQ MQTT Client.
     */
    suspend fun connect(settings: AppSettings): Result<Unit> = withContext(Dispatchers.IO) {
        if (!settings.isBrokerConfigured) {
            val msg = "Broker no configurado. Por favor ingresa el host y puerto en Ajustes."
            _connectionState.value = MqttConnectionState.Error(msg)
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        // Disconnect previous client if any
        disconnectInternal()

        currentSettings = settings
        _connectionState.value = MqttConnectionState.Connecting

        try {
            val clientId = "wol-android-" + UUID.randomUUID().toString().substring(0, 8)
            val builder = Mqtt5Client.builder()
                .identifier(clientId)
                .serverHost(settings.brokerAddress.trim())
                .serverPort(settings.brokerPort)
                .sslWithDefaultConfig() // Mandatory for EMQX Cloud TLS on port 8883
                .addConnectedListener { _ ->
                    _connectionState.value = MqttConnectionState.Connected
                }
                .addDisconnectedListener { context ->
                    val cause = context.cause
                    if (cause != null) {
                        _connectionState.value = MqttConnectionState.Error(
                            cause.localizedMessage ?: "Conexión cerrada inesperadamente"
                        )
                    } else {
                        _connectionState.value = MqttConnectionState.Disconnected
                    }
                }
                .automaticReconnectWithDefaultConfig()

            if (settings.mqttUsername.isNotBlank()) {
                builder.simpleAuth()
                    .username(settings.mqttUsername.trim())
                    .password(settings.mqttPassword.toByteArray(StandardCharsets.UTF_8))
                    .applySimpleAuth()
            }

            val newClient = builder.buildAsync()
            client = newClient

            suspendCancellableCoroutine { continuation ->
                newClient.connect()
                    .whenComplete { _, throwable ->
                        if (throwable != null) {
                            val errorMsg = formatError(throwable)
                            _connectionState.value = MqttConnectionState.Error(errorMsg)
                            continuation.resume(Result.failure(throwable))
                        } else {
                            _connectionState.value = MqttConnectionState.Connected
                            subscribeToStatusTopic(settings.statusTopic)
                            continuation.resume(Result.success(Unit))
                        }
                    }
            }
        } catch (e: Exception) {
            val errorMsg = formatError(e)
            _connectionState.value = MqttConnectionState.Error(errorMsg)
            Result.failure(e)
        }
    }

    private fun subscribeToStatusTopic(topic: String) {
        val activeClient = client ?: return
        if (topic.isBlank()) return

        activeClient.subscribeWith()
            .topicFilter(topic.trim())
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { publish: Mqtt5Publish ->
                val payload = if (publish.payload.isPresent) {
                    val buffer = publish.payload.get().duplicate()
                    StandardCharsets.UTF_8.decode(buffer).toString()
                } else {
                    ""
                }

                scope.launch {
                    _lastReceivedMessage.value = StatusMessage(
                        content = payload,
                        timestamp = System.currentTimeMillis()
                    )
                    _incomingMessages.emit(payload)
                }
            }
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    _connectionState.value = MqttConnectionState.Error(
                        "Fallo al suscribir a $topic: ${throwable.localizedMessage}"
                    )
                }
            }
    }

    /**
     * Publishes a message to the specified topic with QoS 1.
     */
    suspend fun publish(topic: String, payload: String): Result<Unit> = withContext(Dispatchers.IO) {
        val activeClient = client
        if (activeClient == null || _connectionState.value !is MqttConnectionState.Connected) {
            return@withContext Result.failure(IllegalStateException("No hay conexión con el Broker MQTT"))
        }

        suspendCancellableCoroutine { continuation ->
            activeClient.publishWith()
                .topic(topic.trim())
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(payload.toByteArray(StandardCharsets.UTF_8))
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        continuation.resume(Result.failure(throwable))
                    } else {
                        continuation.resume(Result.success(Unit))
                    }
                }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        try {
            client?.disconnect()
        } catch (_: Exception) {
        } finally {
            client = null
            _connectionState.value = MqttConnectionState.Disconnected
        }
    }

    private fun formatError(t: Throwable): String {
        val message = t.localizedMessage ?: t.javaClass.simpleName
        return when {
            message.contains("SSL", ignoreCase = true) || message.contains("handshake", ignoreCase = true) ->
                "Error TLS/SSL: Verifica el host y que el puerto sea 8883"
            message.contains("auth", ignoreCase = true) || message.contains("credential", ignoreCase = true) ->
                "Error de autenticación: Verifica usuario y contraseña MQTT"
            message.contains("resolve", ignoreCase = true) || message.contains("unknown host", ignoreCase = true) ->
                "No se pudo resolver el host del broker. Verifica la dirección de EMQX"
            message.contains("timed out", ignoreCase = true) ->
                "Tiempo de espera agotado al conectar al Broker"
            else -> message
        }
    }
}
