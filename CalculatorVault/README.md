# Calculator — private app launcher (source project)

A small Android launcher project for Android 9+ with a classic calculator screen and fingerprint-gated private area. It is designed to be built as an APK in Android Studio.

## Important limitations
- This is source code, not a prebuilt APK. The environment that created this ZIP does not have Android SDK/Gradle installed, so it could not compile or verify an APK.
- To remove selected apps from the app list shown by this launcher, set Calculator as the default Home app. This does **not** remove apps from Settings, Play Store, notification history, or other launchers. If you switch back to Xiaomi's launcher, its app list may show them again.
- Fingerprint unlock requires a fingerprint enrolled in Android settings. Android may allow device PIN/pattern fallback depending on system behavior; this app itself does not store fingerprint data.
- The private area saves a list of apps to filter from this launcher's app list; it does not encrypt app data or move apps into a secure container.

## Build on Windows 10
1. Install Android Studio from the official Android Developers website on your Windows PC.
2. Extract this ZIP.
3. In Android Studio choose **Open**, select the extracted `CalculatorVault` folder, and wait for Gradle sync.
4. If prompted, install Android SDK Platform 35 and the Android Gradle plugin dependencies.
5. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. The APK is usually created at `app\build\outputs\apk\debug\app-debug.apk`.
7. Transfer it to your Redmi, open it, and allow installation from that source if Android asks.
8. Open Calculator. Tap the small “Calculator” label seven times quickly, or long-press `=` to trigger fingerprint unlock.
9. In the private area, select the apps you want excluded from this launcher's app list. Tap Done.
10. For launcher filtering, go to Settings > Apps > Default apps > Home app (wording may vary) and select Calculator. You can return to the Xiaomi launcher from the same setting.

## Note
The app icon is a simple calculator-style vector icon. For broader Android compatibility, this project targets Android 9+ and compiles against SDK 35.
