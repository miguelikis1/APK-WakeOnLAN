package com.wol.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wol_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val BROKER_ADDRESS = stringPreferencesKey("broker_address")
        val BROKER_PORT = intPreferencesKey("broker_port")
        val MQTT_USERNAME = stringPreferencesKey("mqtt_username")
        val MQTT_PASSWORD = stringPreferencesKey("mqtt_password")
        val COMMAND_TOPIC = stringPreferencesKey("command_topic")
        val STATUS_TOPIC = stringPreferencesKey("status_topic")
        val PC_TAILSCALE_IP = stringPreferencesKey("pc_tailscale_ip")
        val PC_CHECK_PORT = intPreferencesKey("pc_check_port")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                brokerAddress = preferences[PreferencesKeys.BROKER_ADDRESS] ?: "",
                brokerPort = preferences[PreferencesKeys.BROKER_PORT] ?: 8883,
                mqttUsername = preferences[PreferencesKeys.MQTT_USERNAME] ?: "",
                mqttPassword = preferences[PreferencesKeys.MQTT_PASSWORD] ?: "",
                commandTopic = preferences[PreferencesKeys.COMMAND_TOPIC] ?: "casa/pc/wol",
                statusTopic = preferences[PreferencesKeys.STATUS_TOPIC] ?: "casa/pc/status",
                pcTailscaleIp = preferences[PreferencesKeys.PC_TAILSCALE_IP] ?: "",
                pcCheckPort = preferences[PreferencesKeys.PC_CHECK_PORT] ?: 3389
            )
        }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BROKER_ADDRESS] = settings.brokerAddress.trim()
            preferences[PreferencesKeys.BROKER_PORT] = settings.brokerPort
            preferences[PreferencesKeys.MQTT_USERNAME] = settings.mqttUsername.trim()
            preferences[PreferencesKeys.MQTT_PASSWORD] = settings.mqttPassword
            preferences[PreferencesKeys.COMMAND_TOPIC] = settings.commandTopic.trim()
            preferences[PreferencesKeys.STATUS_TOPIC] = settings.statusTopic.trim()
            preferences[PreferencesKeys.PC_TAILSCALE_IP] = settings.pcTailscaleIp.trim()
            preferences[PreferencesKeys.PC_CHECK_PORT] = settings.pcCheckPort
        }
    }
}
