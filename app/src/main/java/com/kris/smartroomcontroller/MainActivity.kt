package com.kris.smartroomcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val LED_MODE_TOPIC = "smartroom/bed/led/set"
private const val LED_BRIGHTNESS_TOPIC = "smartroom/bed/led/brightness"
private const val LED_COLOR_TOPIC = "smartroom/bed/led/color"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartRoomControllerApp()
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SmartRoomControllerApp() {
    var mode by remember { mutableStateOf("manual") }
    var selectedZone by remember { mutableStateOf("main_led") }
    var customPayload by remember { mutableStateOf<String?>(null) }

    var ledOn by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(128f) }

    var red by remember { mutableStateOf(255f) }
    var green by remember { mutableStateOf(0f) }
    var blue by remember { mutableStateOf(0f) }

    var mqttStatus by remember { mutableStateOf("MQTT: łączenie...") }

    val mqttManager =
        remember {
            MqttManager(
                brokerHost = "192.168.233.198",
                brokerPort = 1883,
            )
        }

    LaunchedEffect(Unit) {
        mqttManager.connect(
            onConnected = {
                mqttStatus = "MQTT: połączono"
            },
            onError = { error ->
                mqttStatus = "MQTT: błąd - $error"
            },
        )
    }

    val previewColor =
        Color(
            red = red.toInt(),
            green = green.toInt(),
            blue = blue.toInt(),
        )

    val jsonPayload =
        customPayload ?: buildLedPayload(
            mode = mode,
            zone = selectedZone,
            power = ledOn,
            brightness = brightness.toInt(),
            red = red.toInt(),
            green = green.toInt(),
            blue = blue.toInt(),
        )

    LaunchedEffect(brightness) {
        if (!mqttStatus.contains("połączono", ignoreCase = true)) {
            return@LaunchedEffect
        }

        delay(200)

        mqttManager.publish(
            topic = LED_BRIGHTNESS_TOPIC,
            payload = brightness.toInt().toString(),
        )
    }

    LaunchedEffect(red, green, blue) {
        if (!mqttStatus.contains("połączono", ignoreCase = true)) {
            return@LaunchedEffect
        }

        delay(200)

        mqttManager.publish(
            topic = LED_COLOR_TOPIC,
            payload = "${red.toInt()},${green.toInt()},${blue.toInt()}",
        )
    }

    MaterialTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101010))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Smart Room Controller",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = mqttStatus,
                color = if (mqttStatus.contains("połączono")) Color.Green else Color.Yellow,
            )
            BedLedControlCard(
                mqttManager = mqttManager,
                mqttConnected =
                    mqttStatus.contains(
                        "połączono",
                        ignoreCase = true,
                    ),
            )
            DeskPowerCard(
                mqttManager = mqttManager,
                mqttConnected =
                    mqttStatus.contains(
                        "połączono",
                        ignoreCase = true,
                    ),
            )
            ColorPreview(previewColor = previewColor)

            MainLedCard(
                ledOn = ledOn,
                brightness = brightness,
                red = red,
                green = green,
                blue = blue,
                onPowerChange = {
                    customPayload = null
                    ledOn = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
                onBrightnessChange = {
                    customPayload = null
                    brightness = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
                onRedChange = {
                    customPayload = null
                    red = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
                onGreenChange = {
                    customPayload = null
                    green = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
                onBlueChange = {
                    customPayload = null
                    blue = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
            )

            SleepModeCard(
                onFullSleep = {
                    mode = "sleep_full"
                    selectedZone = "all_sleep_zones"
                    ledOn = true
                    brightness = 15f
                    red = 255f
                    green = 50f
                    blue = 0f
                    customPayload = buildSleepModePayload()
                },
                onReadingLamp = {
                    mode = "reading_lamp"
                    selectedZone = "reading_lamp"
                    ledOn = true
                    brightness = 70f
                    red = 255f
                    green = 110f
                    blue = 20f
                    customPayload = buildReadingLampPayload()
                },
                onAllOff = {
                    mode = "all_off"
                    selectedZone = "all"
                    ledOn = false
                    brightness = 0f
                    red = 0f
                    green = 0f
                    blue = 0f
                    customPayload = buildAllOffPayload()
                },
            )

            MorningStudioCard(
                onMorning = {
                    mode = "morning"
                    selectedZone = "studio_power_sequence"
                    ledOn = true
                    brightness = 180f
                    red = 255f
                    green = 220f
                    blue = 160f
                    customPayload = buildMorningPayload()
                },
                onStudio = {
                    customPayload = null
                    mode = "studio"
                    selectedZone = "studio_lights"
                    ledOn = true
                    brightness = 160f
                    red = 180f
                    green = 0f
                    blue = 255f
                },
                onNight = {
                    customPayload = null
                    mode = "night"
                    selectedZone = "main_led"
                    ledOn = true
                    brightness = 35f
                    red = 255f
                    green = 55f
                    blue = 0f
                },
            )

            SensorsPreviewCard()

            JsonPreviewCard(jsonPayload = jsonPayload)

            Button(
                onClick = {
                    mqttManager.publish(
                        topic = LED_BRIGHTNESS_TOPIC,
                        payload = brightness.toInt().toString(),
                    )

                    mqttManager.publish(
                        topic = LED_COLOR_TOPIC,
                        payload = "${red.toInt()},${green.toInt()},${blue.toInt()}",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Wyślij do MQTT")
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun ColorPreview(previewColor: Color) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(previewColor, RoundedCornerShape(16.dp)),
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF1D1D1D),
            ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )

            content()
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MainLedCard(
    ledOn: Boolean,
    brightness: Float,
    red: Float,
    green: Float,
    blue: Float,
    onPowerChange: (Boolean) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onRedChange: (Float) -> Unit,
    onGreenChange: (Float) -> Unit,
    onBlueChange: (Float) -> Unit,
) {
    SectionCard(title = "Ręczne sterowanie LED") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (ledOn) "LED: WŁĄCZONE" else "LED: WYŁĄCZONE",
                color = Color.White,
                modifier = Modifier.weight(1f),
            )

            Switch(
                checked = ledOn,
                onCheckedChange = onPowerChange,
            )
        }

        Text("Jasność: ${brightness.toInt()}", color = Color.White)
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0f..255f,
        )

        Text("Czerwony: ${red.toInt()}", color = Color.White)
        Slider(
            value = red,
            onValueChange = onRedChange,
            valueRange = 0f..255f,
        )

        Text("Zielony: ${green.toInt()}", color = Color.White)
        Slider(
            value = green,
            onValueChange = onGreenChange,
            valueRange = 0f..255f,
        )

        Text("Niebieski: ${blue.toInt()}", color = Color.White)
        Slider(
            value = blue,
            onValueChange = onBlueChange,
            valueRange = 0f..255f,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun BedLedControlCard(
    mqttManager: MqttManager,
    mqttConnected: Boolean,
) {
    SectionCard(title = "LED łóżka") {
        Text(
            text =
                if (mqttConnected) {
                    "Sterowanie ESP32-S3 przez MQTT"
                } else {
                    "Brak połączenia z MQTT"
                },
            color =
                if (mqttConnected) {
                    Color.LightGray
                } else {
                    Color(0xFFFFCC66)
                },
        )

        Button(
            onClick = {
                mqttManager.publish(
                    topic = "smartroom/bed/led/set",
                    payload = "auto",
                )
            },
            enabled = mqttConnected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("AUTO — reaguj na łóżko")
        }

        Button(
            onClick = {
                mqttManager.publish(
                    topic = "smartroom/bed/led/set",
                    payload = "night",
                )
            },
            enabled = mqttConnected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("ŚWIATŁO NOCNE")
        }

        Button(
            onClick = {
                mqttManager.publish(
                    topic = "smartroom/bed/led/set",
                    payload = "off",
                )
            },
            enabled = mqttConnected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("WYŁĄCZ LED")
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun DeskPowerCard(
    mqttManager: MqttManager,
    mqttConnected: Boolean,
) {
    SectionCard(title = "Listwa przy biurku") {
        Text(
            text =
                if (mqttConnected) {
                    "Athom Smart Plug — MQTT przez Node-RED"
                } else {
                    "Brak połączenia z MQTT"
                },
            color =
                if (mqttConnected) {
                    Color.LightGray
                } else {
                    Color(0xFFFFCC66)
                },
        )

        Button(
            onClick = {
                mqttManager.publish(
                    topic = "smartroom/power/desk/set",
                    payload = "on",
                )
            },
            enabled = mqttConnected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("LISTWA BIURKO — WŁĄCZ")
        }

        Button(
            onClick = {
                mqttManager.publish(
                    topic = "smartroom/power/desk/set",
                    payload = "off",
                )
            },
            enabled = mqttConnected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("LISTWA BIURKO — WYŁĄCZ")
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SleepModeCard(
    onFullSleep: () -> Unit,
    onReadingLamp: () -> Unit,
    onAllOff: () -> Unit,
) {
    SectionCard(title = "Tryb Sen") {
        Text(
            text = "Jeden klik: łóżko + próg. Ciepłe światło, niska jasność, bez telefonu przy łóżku.",
            color = Color.LightGray,
        )

        Button(
            onClick = onFullSleep,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Tryb Sen — łóżko + próg")
        }

        Button(
            onClick = onReadingLamp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Wysuwana lampka do czytania")
        }

        Button(
            onClick = onAllOff,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Wszystko OFF")
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun MorningStudioCard(
    onMorning: () -> Unit,
    onStudio: () -> Unit,
    onNight: () -> Unit,
) {
    SectionCard(title = "Poranek / Studio") {
        Text(
            text = "Docelowo: Tuya, zasilacz, mikrofony, monitory i KRK w bezpiecznej kolejności.",
            color = Color.LightGray,
        )

        Button(
            onClick = onMorning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scena Poranek")
        }

        Button(
            onClick = onStudio,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scena Studio")
        }

        Button(
            onClick = onNight,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scena Noc")
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SensorsPreviewCard() {
    SectionCard(title = "Czujniki — podgląd planowany") {
        Text(
            text = "Pokój: 23.8°C / 48%",
            color = Color.LightGray,
        )

        Text(
            text = "Serwer: 39.5°C",
            color = Color.LightGray,
        )

        Text(
            text = "Carolina Reaper — gleba: 620",
            color = Color.LightGray,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Później te dane przyjdą z MQTT z ESP32 / Arduino.",
            color = Color(0xFFFFCC66),
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun JsonPreviewCard(jsonPayload: String) {
    SectionCard(title = "JSON payload do MQTT") {
        Text(
            text = jsonPayload,
            color = Color.LightGray,
        )
    }
}

fun buildLedPayload(
    mode: String,
    zone: String,
    power: Boolean,
    brightness: Int,
    red: Int,
    green: Int,
    blue: Int,
): String =
    """
    {
      "mode": "$mode",
      "zone": "$zone",
      "power": $power,
      "brightness": $brightness,
      "r": $red,
      "g": $green,
      "b": $blue
    }
    """.trimIndent()

fun buildSleepModePayload(): String =
    """
    {
      "mode": "sleep_full",
      "zones": {
        "under_bed": {
          "power": true,
          "brightness": 8,
          "r": 255,
          "g": 35,
          "b": 0
        },
        "threshold": {
          "power": true,
          "brightness": 25,
          "r": 255,
          "g": 75,
          "b": 0
        },
        "reading_lamp": {
          "power": false,
          "brightness": 0,
          "r": 0,
          "g": 0,
          "b": 0
        }
      }
    }
    """.trimIndent()

fun buildReadingLampPayload(): String =
    """
    {
      "mode": "reading_lamp",
      "zones": {
        "under_bed": {
          "power": true,
          "brightness": 8,
          "r": 255,
          "g": 35,
          "b": 0
        },
        "threshold": {
          "power": true,
          "brightness": 25,
          "r": 255,
          "g": 75,
          "b": 0
        },
        "reading_lamp": {
          "power": true,
          "brightness": 70,
          "r": 255,
          "g": 110,
          "b": 20
        }
      }
    }
    """.trimIndent()

fun buildMorningPayload(): String =
    """
    {
      "mode": "morning",
      "sequence": [
        {
          "device": "main_power_supply",
          "power": true,
          "delayMs": 0
        },
        {
          "device": "monitors",
          "power": true,
          "delayMs": 3000
        },
        {
          "device": "audio_interface_microphones",
          "power": true,
          "delayMs": 5000
        },
        {
          "device": "studio_monitors_krk",
          "power": true,
          "delayMs": 8000
        }
      ],
      "led": {
        "power": true,
        "brightness": 180,
        "r": 255,
        "g": 220,
        "b": 160
      }
    }
    """.trimIndent()

fun buildAllOffPayload(): String =
    """
    {
      "mode": "all_off",
      "zones": {
        "under_bed": {
          "power": false,
          "brightness": 0,
          "r": 0,
          "g": 0,
          "b": 0
        },
        "threshold": {
          "power": false,
          "brightness": 0,
          "r": 0,
          "g": 0,
          "b": 0
        },
        "reading_lamp": {
          "power": false,
          "brightness": 0,
          "r": 0,
          "g": 0,
          "b": 0
        },
        "main_led": {
          "power": false,
          "brightness": 0,
          "r": 0,
          "g": 0,
          "b": 0
        }
      }
    }
    """.trimIndent()
