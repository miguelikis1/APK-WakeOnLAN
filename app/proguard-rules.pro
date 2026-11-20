# HiveMQ MQTT Client & Netty Proguard rules
-keep class com.hivemq.client.** { *; }
-dontwarn com.hivemq.client.**
-keep class io.netty.** { *; }
-dontwarn io.netty.**
