package com.wol.app.data

/**
 * Data class representing all persistent settings of the application.
 */
data class AppSettings(
    val brokerAddress: String = "",
    val brokerPort: Int = 8883,
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val commandTopic: String = "casa/pc/wol",
    val statusTopic: String = "casa/pc/status",
    val pcTailscaleIp: String = "",
    val pcCheckPort: Int = 3389
) {
    val isBrokerConfigured: Boolean
        get() = brokerAddress.isNotBlank() && brokerPort > 0

    val isPcConfigured: Boolean
        get() = pcTailscaleIp.isNotBlank() && pcCheckPort > 0
}
