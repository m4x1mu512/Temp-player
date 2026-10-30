# Google Play release checklist

This project produces an Android App Bundle (AAB) for Google Play. The release
variant uses R8 and resource shrinking. GitHub Actions builds an **unsigned**
release bundle; the local upload key must never be committed to this repository.

## Local upload-key setup

1. In Android Studio, choose **Build > Generate Signed Bundle / APK**.
2. Select **Android App Bundle** and create an upload keystore. Use a validity
   period that extends beyond 22 October 2033 (25+ years is recommended).
3. Store the keystore outside this repository and keep a secure offline backup.
   For example: `C:/AndroidKeys/temp-player-upload.jks`.
4. Copy `keystore.properties.example` to `keystore.properties` in the
   repository root and fill in the real path, passwords, and alias.
5. Confirm that `keystore.properties` and the keystore are ignored by Git.
6. Sync Gradle, then run `./gradlew bundleRelease` (Windows:
   `gradlew.bat bundleRelease`). The signed bundle is written to
   `app/build/outputs/bundle/release/`.

Example properties file (use your own values):

```properties
storeFile=C:/AndroidKeys/temp-player-upload.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=upload
keyPassword=YOUR_KEY_PASSWORD
```

Do not reuse a debug key. Do not commit the keystore, passwords, or exported
private key. For a new Play app, enroll in Play App Signing; Google manages the
app-signing key and your local keystore is the upload key.

## Current project configuration

- Application ID: `com.example.temp` (must remain stable after publication).
- Minimum Android version: API 26.
- Target Android version: API 36.
- First Play release can use versionCode 1 / versionName 1.0. Increase
  versionCode for every subsequent Play upload.
- CI runs Debug assembly, unit tests, and the minified Release AAB build. The
  CI AAB is unsigned by design; sign locally for Play Console.

## Before submitting to Play Console

- Build and install a signed release from the local machine; test playback,
  media-library permission flows, folder selection, queue/playlist persistence,
  notification controls, background playback, and upgrade over the previous
  installed build.
- Inspect the release AAB with Android Studio's App Bundle Explorer / APK
  Analyzer and verify the merged manifest and permissions.
- Complete Play Console app content: privacy policy URL, Data safety form,
  content rating, target audience, ads declaration, app access instructions
  (if applicable), and permission declarations.
- Prepare the store listing: app name, short/full descriptions, icon, phone
  screenshots, feature graphic, and support contact details.
- Run internal testing first, review Play Console pre-launch reports, and fix
  crashes, ANRs, policy warnings, and accessibility/layout issues before
  production rollout.

The Data safety form and privacy policy must reflect the app's actual behavior
and every included SDK. Do not claim that no data is collected until the final
dependency/runtime behavior has been reviewed. If the app only reads local
audio and does not transmit user data, describe that accurately, including any
diagnostics or third-party SDK behavior that may be present.
