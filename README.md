# ColorOS AOD Enhance

![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)

An Xposed module that enhances Always-On Display (AOD) on ColorOS. Provides a visual configuration UI; changes are saved automatically.

## Features

**Brightness adjustment**
- Initial AOD brightness in a dark environment before screen off
- Initial AOD brightness in a bright environment before screen off
- AOD auto-brightness multiplier while the screen is off (1.0–2.0)

**Feature toggles**
- All-day panoramic AOD support
- AOD panoramic display setting
- Block AOD single-tap wake

> The brightness values come with sensible presets, so it works out of the box. If it feels too bright/dim, adjust it in the app.
>
> The default AOD auto-brightness multiplier while the screen is off is **1.6**. Reason: after the screen turns off, the system multiplies the current ambient auto-brightness by **0.6** to compute the AOD brightness, which makes AOD too dim. A multiplier of 1.6 cancels out this reduction (1.6 × 0.6 ≈ 1.0), keeping AOD brightness in line with the current ambient brightness.

## Usage

1. Install the APK and enable the module in Xposed/LSPosed.
2. Select the target scopes: `System UI (com.android.systemui)` and `AOD (com.oplus.aod)`.
3. Restart System UI for it to take effect.
4. Open the module's app to configure each option; changes are saved automatically.

> ⚠️ Once the UI saves, the config is written immediately. How the Hook side reads it depends on the feature:
> - **Brightness-related**: read directly from the Provider on every trigger, so changes take effect immediately (≤1s).
> - **Feature toggles**: read once at process startup, so changes require **restarting the System UI / AOD process or the device** to take effect.

## Build

```bash
# Release build (R8 obfuscation + signing)
./gradlew assembleRelease

# Debug build
./gradlew assembleDebug
```

By default the APKs are split per architecture. Output path:

```
app/build/outputs/apk/
├── release/
│   ├── app-arm64-v8a-release.apk
│   └── app-armeabi-v7a-release.apk
└── debug/
    ├── app-arm64-v8a-debug.apk
    └── app-armeabi-v7a-debug.apk
```

> Release builds require a signing configuration (`keystore.properties`); debug builds work out of the box. See `app/build.gradle` for details.

## Tech stack

| Component | Purpose |
|---|---|
| YukiHookAPI + KavaRef | Xposed hook framework |
| Jetpack Compose + Miuix | UI (MIUI-style components) |
| ContentProvider + SharedPreferences | Cross-process config storage |
| AGP 9.2.1 / Kotlin 2.4.0 / Gradle 9.5.1 | Build system |

## License

MIT

## Changelog

### v1.5 — Config refactor and performance optimization

- Added `AodConfigContract.readRow()` to unify the Cursor parsing logic on the UI and Hook sides
- The SingleClickBlockHook callback now only does an int comparison — no config reads, no reflection
- Replaced `AtomicReference` with `@Volatile var`, removing a layer of object wrapping from the read/write path

### v1.4 — Compatibility with ColorOS 16.0.5 and above (Android 16)

- Adapted to the `setBrightnessForFallbackStrategy` method rename, supporting ColorOS 16.0.5 and above
- Fixed first-config loss: the first settings change made after opening the UI is no longer dropped because the Provider wasn't ready
- Fixed redundant Activity writes: entering a page no longer triggers a meaningless full save
- Improved fault tolerance: every Hook registration is wrapped in `runCatching`, so one failure doesn't break the others
- Refactored the config channel: unified column indices and key names into `AodConfigContract`, eliminating hardcoded ordinals and the risk of maintaining two mirrors
- Added ProGuard rules to keep data classes from being obfuscated
