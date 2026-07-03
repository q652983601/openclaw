# Android Simplified Chinese Localization

This branch (`android-zh-localization`) provides a **full Simplified Chinese (zh-CN) localization** for the OpenClaw Android app.

## Scope

All user-facing English text in the following surfaces has been externalized to `apps/android/app/src/main/res/values/strings.xml` and translated in `values-zh-rCN/strings.xml`:

- **Overview / Shell** — bottom navigation, status chips, metric cards, settings home
- **Settings** — Gateway, Channels, Nodes & Devices, Approvals, Usage, Skills, Dreaming, Appearance, About, Health, Canvas, Notifications, Phone context
- **Onboarding** — welcome, gateway setup, recovery, permissions
- **Voice** — talk orb, mic status, wake errors, speaker controls
- **Chat** — composer, message list, markdown rendering, context meter
- **Sessions** — session list, empty states, runtime status
- **ViewModels / Managers** — `MainViewModel`, `NodeRuntime`, `MicCaptureManager`, `VoiceWakeManager` toast/snackbar/notification copy

Brand and protocol terms such as `OpenClaw`, `Gateway`, `Canvas`, `Cron`, `Provider`, `Agent`, `Node`, `Channel`, and `TLS` are kept in English.

## Build

```bash
cd apps/android
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/Users/wilsonlu/Library/Android/sdk
./gradlew :app:assembleThirdPartyDebug
```

## Test

```bash
./gradlew :app:testThirdPartyDebugUnitTest
```

## Install

```bash
# Connect an Android device or start an emulator
./gradlew :app:installThirdPartyDebug
adb shell am start -n ai.openclaw.app/.MainActivity
```

## Key design decisions

1. **String externalization first** — all hard-coded user-facing strings moved to `strings.xml`.
2. **Two language packs** — default `values/strings.xml` remains English; `values-zh-rCN/strings.xml` holds Chinese translations.
3. **Composable-safe** — `stringResource(...)` is used inside `@Composable` functions only. Non-composable summary helpers remain English-hardcoded to keep unit tests passing without Robolectric.
4. **AAPT-safe** — `&` is escaped as `&amp;` in XML; apostrophes are avoided or wrapped in double quotes.
5. **Locale config** — `res/xml/locale_config.xml` declares `zh-Hans-CN` so the OS can advertise the supported locale.

## Known remaining English

The following are intentionally left in English:

- Brand and protocol nouns (`OpenClaw`, `Gateway`, `Canvas`, etc.)
- A small number of non-composable helper functions that return status summaries (e.g. `voiceStatusLabel`, `contextMeterLabel`, `androidDistributionChannel`) to avoid breaking existing JVM unit tests.
- The product tagline `"Exfoliate! Exfoliate!"` pending product decision.

## Contributing

If you want to extend this localization (e.g. Traditional Chinese `zh-rTW` or another language):

1. Copy `apps/android/app/src/main/res/values-zh-rCN/strings.xml` to the new `values-*/strings.xml`.
2. Translate the string values while keeping the keys unchanged.
3. Add the locale to `res/xml/locale_config.xml`.
4. Run `./gradlew :app:assembleThirdPartyDebug` and `./gradlew :app:testThirdPartyDebugUnitTest`.

## License

This localization work is released under the same license as the upstream OpenClaw project (MIT). See [LICENSE](../LICENSE).
