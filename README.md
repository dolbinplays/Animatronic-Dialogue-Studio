# Animatronic Dialogue Studio

**v0.1 — Android source prototype (Android 8.0+, API 26).** This first build is a proof of concept, not yet hardware-tested.

## Features

- Director, Performer, and Audience roles in a single native Android app.
- Local Director TCP server on port 48271; clients join on the same Wi-Fi/hotspot network.
- Simple `Character|dialogue` script lines and Android text-to-speech playback.
- Basic animated skull, opening jaw and red eyes while dialogue is playing.
- Test and Stop controls.

## Download the test APK

Go to [GitHub Actions](../../actions), open the latest **Build Android Debug APK** run, and download the `AnimatronicDialogueStudio-v0.1-debug-apk` artifact. Unzip it to obtain `app-debug.apk`. Install it on Android phones by enabling installation from your trusted files app when prompted. This is a debug test APK, **not** a Play Store release.

The workflow uses JDK 17, Gradle 8.7, and Android SDK 35. The workflow has to succeed before an APK can be downloaded.

## Try it

1. Install the same APK on both Android phones.
2. Enable a hotspot on the Director device, or join both devices to the same Wi-Fi network (offline operation).
3. On the Director, select **Director** and **Start Director / Join Director**.
4. On the other phone, select **Performer**, select **Spirit Ball** (or another character), enter the actual IP address of the Director phone, then tap **Start Director / Join Director**. The prefilled IP is only an example.
5. Use **Test My Character** on either phone to check audio/skull movement.
6. On the Director, enter lines such as `Skeleton|Hello!` and `Spirit Ball|Boo!` and press **Play Script (Director)**.
7. An **Audience** phone plays every script line locally. Bluetooth audio routing depends on Android and the paired speaker.

## Known limitations

- **Not yet precision synchronized.** Current scripts broadcast each turn and estimate its duration, with no clock calibration or audio preloading.
- Jaw animation is time-based, not yet measured from actual audio amplitude; eyes light up during speaking.
- Android system TTS voices/speech speed may differ across phones.
- No audio import, music/effects, fully mixed outdoor soundtrack, saved scripts, automated pairing, QR code, or Wi-Fi recovery yet.
- Keep the app in the foreground while testing; background playback services are not included.
- No authentication or encryption; only use on a private/trusted local network.
- Some prepaid phones may restrict hotspot use without a plan. A separate Wi-Fi router is an alternative.
- The app itself does not enable Android hotspot; enable it in phone settings.
- SDK target is deliberately 28 for this local prototype; this must be modernized before wider distribution.

## Build locally

Open the repository root (the directory containing `settings.gradle`) with Android Studio and allow Gradle sync. Select **Build > Build APK(s)**, or run `gradle assembleDebug` with JDK 17 and Android SDK 35 / Gradle 8.7.

## Project scope

This is the separate Halloween animatronics project. The Project Aegis repository is not part of this build.
