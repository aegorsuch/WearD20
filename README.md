# WearD20

A simple, modern dice-rolling application for Wear OS devices.

## Features

- **Quick Rolling:** Tap the screen to roll the currently selected die.
- **Multiple Dice Types:** Supports d4, d6, d8, d10, d12, d20, and d100.
- **Easy Switching:** Long-press the screen to cycle through the different dice types.
- **Haptic Feedback:**
    - **Critical Success (Max Roll):** Double pulse vibration and Green text.
    - **Critical Failure (Roll of 1):** Long buzz vibration and Red text.
    - **Normal Roll:** Short tap vibration and White text.
- **Modern UI:** Built using Jetpack Compose for Wear OS with a clean, high-contrast interface.

## Installation

1.  Enable **Developer Options** and **ADB Debugging** on your Wear OS watch.
2.  Connect your watch to your computer via ADB (Wi-Fi or Bluetooth).
3.  Clone this repository.
4.  Open the project in Android Studio.
5.  Build and run the `app` module on your connected watch.

## Build Requirements

- Android Studio Jellyfish or newer.
- Wear OS API 30+ (Android 11.0 or higher).
- Kotlin 2.0+

## License

This project is licensed under the MIT License.
