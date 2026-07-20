#include <WiFi.h>
#include <PubSubClient.h>
#include <Wire.h>
#include <FastLED.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>

// =====================================================
// WI-FI I MQTT
// =====================================================

const char* WIFI_SSID = "SPEED-NET-6820-2.4G";
const char* WIFI_PASSWORD = "BB068STE03633";

const char* MQTT_HOST = "192.168.233.198";
constexpr uint16_t MQTT_PORT = 1883;

const char* TOPIC_STATUS   = "smartroom/bed/status";
const char* TOPIC_STATE    = "smartroom/bed/state";
const char* TOPIC_PITCH    = "smartroom/bed/pitch";
const char* TOPIC_LED_SET        = "smartroom/bed/led/set";
const char* TOPIC_LED_MODE       = "smartroom/bed/led/mode";
const char* TOPIC_LED_BRIGHTNESS = "smartroom/bed/led/brightness";
const char* TOPIC_LED_COLOR      = "smartroom/bed/led/color";

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

// =====================================================
// LED WS2812B
// =====================================================

#define LED_DATA_PIN 4
#define NUM_LEDS 300
#define LED_TYPE WS2812B
#define COLOR_ORDER GRB

constexpr uint8_t AUTO_BRIGHTNESS = 25;

CRGB leds[NUM_LEDS];

// =====================================================
// MPU6050
// =====================================================

constexpr uint8_t MPU_ADDR = 0x68;

constexpr int SDA_PIN = 8;
constexpr int SCL_PIN = 9;

constexpr uint8_t REG_ACCEL_XOUT_H = 0x3B;
constexpr uint8_t REG_PWR_MGMT_1   = 0x6B;
constexpr uint8_t REG_WHO_AM_I     = 0x75;

// =====================================================
// STANY ŁÓŻKA I TRYBY LED
// =====================================================

enum BedState : uint8_t {
  BED_CLOSED,
  BED_MOVING,
  BED_OPEN
};

enum LedMode : uint8_t {
  LED_MODE_AUTO,
  LED_MODE_NIGHT,
  LED_MODE_OFF,
  LED_MODE_MANUAL
};

BedState currentState = BED_MOVING;
BedState candidateState = BED_MOVING;
LedMode ledMode = LED_MODE_AUTO;

uint8_t manualBrightness = 128;
CRGB manualColor = CRGB(255, 50, 0);
bool ledRenderRequired = true;

unsigned long candidateSince = 0;

constexpr float CLOSED_THRESHOLD = 12.0f;
constexpr float OPEN_THRESHOLD = 75.0f;
constexpr unsigned long STABLE_TIME_MS = 800;

float latestPitch = 0.0f;

// =====================================================
// DEKLARACJE FUNKCJI
// =====================================================

bool writeRegister(uint8_t reg, uint8_t value);
bool readRegisters(uint8_t reg, uint8_t* buffer, size_t length);
int16_t combineBytes(uint8_t highByte, uint8_t lowByte);

BedState detectBedState(float pitch);
const char* bedStateName(BedState state);
const char* bedStatePayload(BedState state);
const char* ledModePayload(LedMode mode);

void updateLedEffect();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void maintainConnections();
void publishBedState();
void publishLedMode();
void publishPitch(float pitch);

// =====================================================
// MPU6050
// =====================================================

bool writeRegister(uint8_t reg, uint8_t value) {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(reg);
  Wire.write(value);

  return Wire.endTransmission() == 0;
}

bool readRegisters(
  uint8_t reg,
  uint8_t* buffer,
  size_t length
) {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(reg);

  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  const size_t received = Wire.requestFrom(
    MPU_ADDR,
    static_cast<uint8_t>(length),
    true
  );

  if (received != length) {
    return false;
  }

  for (size_t i = 0; i < length; i++) {
    buffer[i] = Wire.read();
  }

  return true;
}

int16_t combineBytes(
  uint8_t highByte,
  uint8_t lowByte
) {
  return static_cast<int16_t>(
    (static_cast<uint16_t>(highByte) << 8) | lowByte
  );
}

// =====================================================
// STAN ŁÓŻKA
// =====================================================

BedState detectBedState(float pitch) {
  if (pitch <= CLOSED_THRESHOLD) {
    return BED_CLOSED;
  }

  if (pitch >= OPEN_THRESHOLD) {
    return BED_OPEN;
  }

  return BED_MOVING;
}

const char* bedStateName(BedState state) {
  switch (state) {
    case BED_CLOSED:
      return "ZAMKNIETE";

    case BED_OPEN:
      return "OTWARTE";

    case BED_MOVING:
      return "RUCH / POSREDNIE";

    default:
      return "NIEZNANE";
  }
}

const char* bedStatePayload(BedState state) {
  switch (state) {
    case BED_CLOSED:
      return "closed";

    case BED_OPEN:
      return "open";

    case BED_MOVING:
      return "moving";

    default:
      return "unknown";
  }
}

const char* ledModePayload(LedMode mode) {
  switch (mode) {
    case LED_MODE_AUTO:
      return "auto";

    case LED_MODE_NIGHT:
      return "night";

    case LED_MODE_OFF:
      return "off";

    case LED_MODE_MANUAL:
      return "manual";

    default:
      return "unknown";
  }
}

// =====================================================
// LED
// =====================================================

void updateLedEffect() {
  static unsigned long lastAnimationFrame = 0;
  static uint16_t animationPosition = 0;

  if (ledRenderRequired) {
    ledRenderRequired = false;
    animationPosition = 0;

    FastLED.clear();

    if (ledMode == LED_MODE_OFF) {
      FastLED.setBrightness(AUTO_BRIGHTNESS);
      FastLED.show();
      return;
    }

    if (ledMode == LED_MODE_NIGHT) {
      FastLED.setBrightness(AUTO_BRIGHTNESS);
      fill_solid(
        leds,
        NUM_LEDS,
        CRGB(255, 55, 5)
      );
      FastLED.show();
      return;
    }

    if (ledMode == LED_MODE_MANUAL) {
      FastLED.setBrightness(manualBrightness);
      fill_solid(
        leds,
        NUM_LEDS,
        manualColor
      );
      FastLED.show();
      return;
    }

    // Tryb AUTO
    FastLED.setBrightness(AUTO_BRIGHTNESS);

    if (currentState == BED_CLOSED) {
      FastLED.clear();
      FastLED.show();
    } else if (currentState == BED_OPEN) {
      fill_solid(
        leds,
        NUM_LEDS,
        CRGB(255, 70, 10)
      );
      FastLED.show();
    } else {
      FastLED.clear();
      FastLED.show();
    }
  }

  if (
    ledMode == LED_MODE_AUTO &&
    currentState == BED_MOVING &&
    millis() - lastAnimationFrame >= 25
  ) {
    lastAnimationFrame = millis();

    fadeToBlackBy(leds, NUM_LEDS, 45);
    leds[animationPosition] = CRGB(255, 90, 0);
    FastLED.show();

    animationPosition++;

    if (animationPosition >= NUM_LEDS) {
      animationPosition = 0;
    }
  }
}

// =====================================================
// MQTT
// =====================================================

void publishBedState() {
  if (!mqttClient.connected()) {
    return;
  }

  mqttClient.publish(
    TOPIC_STATE,
    bedStatePayload(currentState),
    true
  );
}

void publishLedMode() {
  if (!mqttClient.connected()) {
    return;
  }

  mqttClient.publish(
    TOPIC_LED_MODE,
    ledModePayload(ledMode),
    true
  );
}

void publishPitch(float pitch) {
  if (!mqttClient.connected()) {
    return;
  }

  char payload[16];

  snprintf(
    payload,
    sizeof(payload),
    "%.1f",
    pitch
  );

  mqttClient.publish(
    TOPIC_PITCH,
    payload,
    false
  );
}

void mqttCallback(
  char* topic,
  byte* payload,
  unsigned int length
) {
  String command;
  command.reserve(length);

  for (unsigned int i = 0; i < length; i++) {
    command += static_cast<char>(payload[i]);
  }

  command.trim();
  command.toLowerCase();

  Serial.print("MQTT RX [");
  Serial.print(topic);
  Serial.print("]: ");
  Serial.println(command);

  const String receivedTopic(topic);

  if (receivedTopic == TOPIC_LED_SET) {
    if (command == "auto") {
      ledMode = LED_MODE_AUTO;
    } else if (command == "night") {
      ledMode = LED_MODE_NIGHT;
    } else if (command == "off") {
      ledMode = LED_MODE_OFF;
    } else {
      Serial.println("Nieznana komenda LED.");
      return;
    }

    ledRenderRequired = true;
    publishLedMode();
    return;
  }

  if (receivedTopic == TOPIC_LED_BRIGHTNESS) {
    char* endPointer = nullptr;
    const long parsedValue = strtol(
      command.c_str(),
      &endPointer,
      10
    );

    if (
      endPointer == command.c_str() ||
      *endPointer != '\0' ||
      parsedValue < 0 ||
      parsedValue > 255
    ) {
      Serial.println("Bledna jasnosc. Zakres: 0-255.");
      return;
    }

    manualBrightness =
        static_cast<uint8_t>(parsedValue);

    ledMode = LED_MODE_MANUAL;
    ledRenderRequired = true;
    publishLedMode();
    return;
  }

  if (receivedTopic == TOPIC_LED_COLOR) {
    int red = 0;
    int green = 0;
    int blue = 0;
    char extra = '\0';

    const int parsedFields = sscanf(
      command.c_str(),
      "%d,%d,%d%c",
      &red,
      &green,
      &blue,
      &extra
    );

    if (
      parsedFields != 3 ||
      red < 0 || red > 255 ||
      green < 0 || green > 255 ||
      blue < 0 || blue > 255
    ) {
      Serial.println(
        "Bledny kolor. Format: R,G,B; zakres 0-255."
      );
      return;
    }

    manualColor = CRGB(
      static_cast<uint8_t>(red),
      static_cast<uint8_t>(green),
      static_cast<uint8_t>(blue)
    );

    ledMode = LED_MODE_MANUAL;
    ledRenderRequired = true;
    publishLedMode();
  }
}

void maintainConnections() {
  static unsigned long lastWiFiAttempt = 0;
  static unsigned long lastMqttAttempt = 0;

  if (WiFi.status() != WL_CONNECTED) {
    if (millis() - lastWiFiAttempt >= 10000) {
      lastWiFiAttempt = millis();

      Serial.println("Ponawiam Wi-Fi...");
      WiFi.disconnect();
      WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    }

    return;
  }

  if (!mqttClient.connected()) {
    if (millis() - lastMqttAttempt < 5000) {
      return;
    }

    lastMqttAttempt = millis();

    char clientId[32];

    snprintf(
      clientId,
      sizeof(clientId),
      "bed-s3-%04X",
      static_cast<uint16_t>(
        ESP.getEfuseMac() & 0xFFFF
      )
    );

    Serial.print("Laczenie MQTT jako ");
    Serial.println(clientId);

    const bool connected = mqttClient.connect(
      clientId,
      TOPIC_STATUS,
      0,
      true,
      "offline"
    );

    if (connected) {
      Serial.println("MQTT polaczony.");

      mqttClient.publish(
        TOPIC_STATUS,
        "online",
        true
      );

      mqttClient.subscribe(TOPIC_LED_SET);
      mqttClient.subscribe(TOPIC_LED_BRIGHTNESS);
      mqttClient.subscribe(TOPIC_LED_COLOR);

      publishBedState();
      publishLedMode();
      publishPitch(latestPitch);
    } else {
      Serial.print("Blad MQTT, kod: ");
      Serial.println(mqttClient.state());
    }

    return;
  }

  mqttClient.loop();
}

// =====================================================
// SETUP
// =====================================================

void setup() {
  Serial.begin(115200);
  delay(1500);

  Serial.println();
  Serial.println("=== BED: MPU + LED + MQTT ===");

  FastLED.addLeds<
    LED_TYPE,
    LED_DATA_PIN,
    COLOR_ORDER
  >(leds, NUM_LEDS);

  FastLED.setBrightness(AUTO_BRIGHTNESS);

  FastLED.setMaxPowerInVoltsAndMilliamps(
    5,
    3000
  );

  FastLED.clear(true);

  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);

  uint8_t whoAmI = 0;

  if (!readRegisters(REG_WHO_AM_I, &whoAmI, 1)) {
    Serial.println("BLAD: brak MPU6050.");

    while (true) {
      FastLED.clear();
      leds[0] = CRGB::Red;
      FastLED.show();
      delay(500);

      FastLED.clear(true);
      delay(500);
    }
  }

  Serial.print("WHO_AM_I: 0x");
  Serial.println(whoAmI, HEX);

  if (!writeRegister(REG_PWR_MGMT_1, 0x00)) {
    Serial.println("BLAD: nie mozna wybudzic MPU.");
    while (true) {
      delay(1000);
    }
  }

  delay(100);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setBufferSize(256);

  Serial.println("System uruchomiony.");
}

// =====================================================
// LOOP
// =====================================================

void loop() {
  static unsigned long lastMpuRead = 0;
  static unsigned long lastPitchPublish = 0;
  static unsigned long lastSerialPrint = 0;

  maintainConnections();

  if (millis() - lastMpuRead >= 100) {
    lastMpuRead = millis();

    uint8_t data[14];

    if (!readRegisters(
          REG_ACCEL_XOUT_H,
          data,
          sizeof(data)
        )) {
      Serial.println("BLAD ODCZYTU MPU6050");
      return;
    }

    const int16_t accelXRaw =
        combineBytes(data[0], data[1]);

    const int16_t accelYRaw =
        combineBytes(data[2], data[3]);

    const int16_t accelZRaw =
        combineBytes(data[4], data[5]);

    const float accelX =
        accelXRaw / 16384.0f;

    const float accelY =
        accelYRaw / 16384.0f;

    const float accelZ =
        accelZRaw / 16384.0f;

    latestPitch =
        atan2(
          -accelX,
          sqrt(
            accelY * accelY +
            accelZ * accelZ
          )
        ) * 180.0f / PI;

    const BedState detectedState =
        detectBedState(latestPitch);

    if (detectedState != candidateState) {
      candidateState = detectedState;
      candidateSince = millis();
    }

    if (
      candidateState != currentState &&
      millis() - candidateSince >= STABLE_TIME_MS
    ) {
      currentState = candidateState;

      Serial.print("NOWY STAN: ");
      Serial.println(bedStateName(currentState));

      ledRenderRequired = true;
      publishBedState();
    }
  }

  if (millis() - lastPitchPublish >= 1000) {
    lastPitchPublish = millis();
    publishPitch(latestPitch);
  }

  if (millis() - lastSerialPrint >= 1000) {
    lastSerialPrint = millis();

    Serial.print("WiFi: ");
    Serial.print(
      WiFi.status() == WL_CONNECTED
        ? "OK"
        : "BRAK"
    );

    Serial.print(" | MQTT: ");
    Serial.print(
      mqttClient.connected()
        ? "OK"
        : "BRAK"
    );

    Serial.print(" | Pitch: ");
    Serial.print(latestPitch, 1);

    Serial.print(" | Stan: ");
    Serial.println(bedStateName(currentState));
  }

  updateLedEffect();
}
