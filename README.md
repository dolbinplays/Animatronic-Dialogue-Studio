# Animatronic Dialogue Studio

**v0.2 — Android networking test build (Android 8.0+).** Native Android app for a Director, multiple Performer devices, and an Audience device. Hardware timing has not yet been validated.

## Changes in v0.2

- Director displays detected IPv4 addresses and local TCP server status on port **48271**.
- Performer and Audience can tap **Find Director Automatically**. On a direct hotspot it tries the Wi-Fi gateway, followed by UDP local-network discovery on port **48272**.
- Devices use the Wi-Fi network explicitly for outgoing connections, even if Android prefers cellular for Internet access.
- Manual Director IP entry remains available where broadcasts are restricted.
- Peers identify themselves to the Director on connection, with clearer diagnostic/status messages.
- The incorrect prefilled `192.168.43.1` guess has been removed.

## Test on two phones

1. Install the APK on both phones. Enable a hotspot on the Director phone; join it from the Performer phone. You can also connect both to the same Wi-Fi router.
2. On the Director, select **Director**, assign **Skeleton**, tap **START DIRECTOR / JOIN BY ADDRESS**, and check the **DIRECTOR READY** message. The phone shows its possible local IP addresses.
3. On the Performer, select **Performer**, assign **Spirit Ball**, tap **FIND DIRECTOR AUTOMATICALLY**.
4. If it says CONNECTED TO DIRECTOR, tap PLAY SCRIPT on the Director. Each character should speak its own line.
5. If discovery fails, enter the Director's actual local IP address manually, then tap **START DIRECTOR / JOIN BY ADDRESS**. On a phone hotspot, the Performer's Wi-Fi gateway is usually the Director; on a router, the gateway is normally the router, not the Director.
6. Use **TEST MY CHARACTER** to check local TTS and skull animation. Keep the app in the foreground for this prototype.

The Director does **not** enable the hotspot for you. Enable hotspot from Android settings.

## Download

Open the latest successful [Build Android Debug APK workflow](../../actions/workflows/build-android.yml). Under **Artifacts**, download `AnimatronicDialogueStudio-v0.2-debug-apk`; extract `app-debug.apk` and install it on your phones.

## Current features

- Director/Performer/Audience role selection; remote dialogue commands via local TCP.
- Example `Character|dialogue` text script lines; Android text-to-speech.
- Simple animated skull with moving jaw and red eyes.
- Connection diagnostics, auto-find on compatible Wi-Fi/hotspots, and manual IP fallback.

## Current limitations

- **This remains a test prototype.** Dialogue timing is approximate, not synchronized to an authoritative audio timeline. Jaw motion is simulated rather than sampled from audio.
- No automatic reconnect, timed sound effects, audio imports, full audience soundtrack, secure pairing, or persistent background service.
- Director discovery may fail on Wi-Fi networks with device isolation or broadcast restrictions. Even manual IP won't fix networks that forbid client-to-client connections.
- The app uses unencrypted local commands without authentication. Use only a private, trusted Wi-Fi network.
- The app targets Android API 28 for local compatibility; target SDK and background behavior need modernization before wider distribution.

## Build

GitHub Actions uses Java 17, Gradle 8.7 and Android SDK 35 to compile and upload a debug APK. Android Studio can also open this repository directly.

Project Aegis is unrelated and remains untouched.
