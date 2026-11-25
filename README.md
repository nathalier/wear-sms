# Wear SMS

A small default SMS app for Wear OS watches that have their own SIM, built with Kotlin and Jetpack Compose for Wear OS.
It sends and receives plain SMS through the mobile carrier only, so it does not need a paired phone.

Developed and tested on a Pixel Watch 4 (LTE) with its own phone number.

## Features

- Conversation list and threads, kept in the system SMS database
- Read and write messages, with sent, delivered and failed states and tap-to-retry
- Contact search, and messaging numbers that are not in your contacts
- Notifications with inline reply that open the right conversation
- Delete a single message or a whole conversation
- Export every message to a JSON file in `Download/wear-sms`

## Limitations

- SMS only: no MMS, RCS or group messaging
- Delivery reports depend on your carrier, and some carriers never send them
- Prebuilt binaries are not provided, so build the app yourself

## Requirements

- JDK 17 and the Android SDK (platform 37.2 and matching build tools)
- A Wear OS watch with a SIM that can send SMS
- `adb` with wireless debugging enabled on the watch

## Build and test

```
export JAVA_HOME=<path to JDK 17>
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install on the watch

1. On the watch, tap "Build number" 7 times (Settings, System, About, Versions) to enable Developer options.
2. Turn on ADB debugging and Wireless debugging, with the watch on the same Wi-Fi as your computer.
3. Choose "Pair new device" on the watch, then run `adb pair <ip>:<pairing-port>` and enter the code.
4. Run `adb connect <ip>:<connect-port>`, using the port shown on the main Wireless debugging screen.
5. Install with `adb -s <ip>:<port> install -r app/build/outputs/apk/debug/app-debug.apk`.
6. Make it the default SMS app with `adb -s <ip>:<port> shell cmd role add-role-holder --user 0 android.app.role.SMS io.github.nathalier.wearsms`.

Allow the contacts and notification permissions when the app asks for them.

## Release build

1. Create an upload keystore once, for example with `keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`.
2. Copy `keystore.properties.example` to `keystore.properties` and fill in your values.
3. Run `./gradlew bundleRelease` for the Play Store, or `./gradlew assembleRelease` for a signed APK.

The keystore and `keystore.properties` are ignored by git, so keep a backup of them somewhere safe.
Without `keystore.properties` the release build is unsigned.

## Privacy

The app has no network access and no analytics.
Messages stay in the watch's SMS database, and exports are written only where you ask.
See [PRIVACY.md](PRIVACY.md).

## License

CC0 1.0 Universal, see [LICENSE](LICENSE).
