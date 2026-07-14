# Smart Room Controller

A long-term smart room automation project based on Android, MQTT, ESP32, Arduino, sensors, local server infrastructure and practical daily-use automation.

This project has been developed and prototyped over several years. The current stage focuses on turning the existing hardware and software experiments into a clean, documented public portfolio project.

## Main idea

The goal is to build a real automation system for my room, not just a tutorial project.

The system is planned to control and monitor:

* LED lighting and WS2812B strips
* sleep mode lighting without blue light
* physical bedside controls without using a phone near the bed
* morning automation with Tuya devices
* studio monitors, microphones and power supplies
* room and server temperature
* plant monitoring for Carolina Reaper
* ESP32 and Arduino sensor nodes
* local server based on Debian / Proxmox
* Android application written in Kotlin

## Current status

The first Android prototype is working.

Implemented so far:

* Android Studio project created
* Jetpack Compose UI running on a virtual Android device
* LED ON/OFF switch
* brightness slider
* RGB sliders
* scene buttons
* JSON payload preview for future MQTT communication

## Demo v1 goal

Demo v1 focuses on a simple but working LED control flow:

```text
Android App
    ↓
JSON payload
    ↓
MQTT
    ↓
Mosquitto on Debian / Proxmox
    ↓
ESP32
    ↓
WS2812B LED strip
```

The first demo will focus on safe low-voltage automation, especially LED sleep mode and local control.

## Planned modules

```text
android/          Kotlin Android application
esp32/            ESP32 firmware for MQTT and LED control
arduino/          Arduino sensor experiments
docs/             architecture, roadmap, devlog and safety notes
hardware/         wiring diagrams, 3D printed parts and photos
media/            screenshots and demo videos
```

## Why this project matters

This project is part of rebuilding and documenting my software engineering portfolio through a real practical system.

Instead of creating fake tutorial apps, I am documenting a working automation setup that combines:

* mobile development
* embedded systems
* Linux server administration
* MQTT communication
* hardware prototyping
* safety-first automation design

## Safety approach

Mechanical bed automation is intentionally excluded from the first demo.

The first stages focus on:

* low-voltage LED control
* sensors
* local physical buttons
* safe sleep mode lighting
* clear documentation

Any future mechanical automation requires limit switches, emergency stop, manual override, proper power isolation and physical safety testing.

Działa:
Android Kotlin → MQTT → Node-RED → MQTT → ESP8266 → WS2812B.
Auto-wysyłanie działa bez przycisku.
RGB/brightness działa logicznie.
Problem z pełną taśmą wynika z zasilania LED z płytki ESP — potrzebne osobne 5V i wspólna masa.


## Aktualny status

Projekt jest w fazie budowy MVP.

Działa już podstawowy łańcuch komunikacji:

```text
Android app
→ MQTT
→ Node-RED
→ ESP32
→ WS2812B LED
```

Aktualnie testowane są:

```text
- sterowanie LED z aplikacji Android
- tryby scen: Noc, Studio, OFF, ręczny RGB
- komunikacja MQTT
- ESP32 jako sterownik LED
- przygotowanie drugiego modułu ESP32 pod czujnik pochyłu łóżka
```

## Główne funkcje

Planowane i częściowo wdrożone funkcje:

```text
- sterowanie LED WS2812B z aplikacji Android
- tryb nocny z ciepłym światłem
- tryb studio
- automatyczne wyłączanie wybranych urządzeń przez Tuya
- czujnik pochyłu / ruchu szafołóżka
- przyszła integracja wentylatorów z czujnikiem temperatury
- rozbudowa o kolejne strefy LED: biurko, łóżko, próg
```

## Technologie

```text
Android Kotlin
Jetpack Compose
MQTT
Node-RED
Mosquitto
ESP32 / Arduino IDE
FastLED
WS2812B
Proxmox / Debian VM
```

## Architektura

```text
[Android App]
     |
     v
[MQTT Broker / Mosquitto]
     |
     v
[Node-RED]
     |
     v
[ESP32]
     |
     v
[WS2812B LED / sensors / devices]
```

## Plan najbliższych prac

```text
1. Stabilizacja komunikacji Android → MQTT → ESP32
2. Test czujnika pochyłu łóżka na MPU6050
3. Dodanie komunikatów MQTT dla stanu łóżka
4. Podział LED na strefy
5. Dodanie zasilaczy 5V 30A i zabezpieczeń bezpiecznikami
6. Przygotowanie lokalnej strony prezentacyjnej projektu
7. Publikacja demo portfolio
```

## Bezpieczeństwo

Projekt obejmuje pracę z zasilaczami, prądem stałym oraz urządzeniami podłączanymi do sieci 230V. Elementy 230V nie są sterowane bezpośrednio przez ESP32. Do sterowania urządzeniami sieciowymi używane będą gotowe moduły, np. wtyczki Tuya.

W projekcie nie należy publikować:

```text
- haseł Wi-Fi
- danych logowania MQTT
- adresów publicznych
- plików local.properties
- konfiguracji prywatnych urządzeń
```

## Status projektu

Projekt jest rozwijany jako praktyczne demo automatyzacji pokoju oraz element portfolio technicznego.
