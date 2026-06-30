package com.kris.smartroomcontroller

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import java.nio.charset.StandardCharsets

class MqttManager(
    private val brokerHost: String = gBuildConfig.MQTT_HOST,
    private val brokerPort: Int = BuildConfig.MQTT_PORT,
) {
    private val client: Mqtt3AsyncClient =
        MqttClient
            .builder()
            .useMqttVersion3()
            .identifier("android-smart-room-${System.currentTimeMillis()}")
            .serverHost(brokerHost)
            .serverPort(brokerPort)
            .automaticReconnectWithDefaultConfig()
            .buildAsync()

    fun connect(
        onConnected: () -> Unit,
        onError: (String) -> Unit,
    ) {
        client
            .connectWith()
            .cleanSession(true)
            .send()
            .whenComplete { _, throwable ->
                if (throwable == null) {
                    Log.d("MQTT", "Connected")
                    onConnected()
                } else {
                    Log.e("MQTT", "Connection error", throwable)
                    onError(throwable.message ?: "Unknown MQTT error")
                }
            }
    }

    fun publish(
        topic: String,
        payload: String,
    ) {
        if (!client.state.isConnected) {
            Log.e("MQTT", "Not connected. Payload not sent.")
            return
        }

        client
            .publishWith()
            .topic(topic)
            .qos(MqttQos.AT_MOST_ONCE)
            .payload(payload.toByteArray(StandardCharsets.UTF_8))
            .send()
            .whenComplete { _, throwable ->
                if (throwable == null) {
                    Log.d("MQTT", "Published to $topic: $payload")
                } else {
                    Log.e("MQTT", "Publish error", throwable)
                }
            }
    }
}
