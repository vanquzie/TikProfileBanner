# TikProfileBanner

Small LSPosed module that turns on TikTok's profile banner feature.
TikTok hides the "Add background" button unless your account is on their
allow list. This module lifts that check, so the button shows up.

No app icon, no settings screen. Install it, enable it in LSPosed,
restart TikTok. Done.

## Requirements

- LSPosed, recent version (API 102 or newer). Works with KernelSU,
  Magisk, or APatch as long as LSPosed runs on top.
- TikTok **47.1.3**. Other versions are untested and will probably not
  work, since TikTok renames its internal classes with every update.

## Install

1. Grab `TikProfileBanner.apk` from the
   [releases](https://github.com/vanquzie/TikProfileBanner/releases) page.
2. Install it.
3. In LSPosed, enable TikProfileBanner and tick the TikTok scope.
4. Force stop TikTok and open it again.

> **Note:** your banner is saved on TikTok's servers and other people can
> see it. But TikTok does not send the banner back when you look at your
> own profile, so you will not see it yourself. Check from a second
> account.

## For non-rooted (LSPatch)

No root? Use [LSPatch](https://github.com/JingMatrix/LSPatch) (v1.2 or
newer) with TikTok 47.1.3 ready to patch.

1. Install the LSPatch manager app from the link above.
2. Patch TikTok with TikProfileBanner included (pick the option that
   bundles the module into the app).
3. Install the patched TikTok it produces.
4. Force stop TikTok and open it again.

You still will not see your own banner, check it from a second account.

## Build it yourself

You need JDK 21 and the Android SDK. Linux and macOS are supported, it
is a normal Gradle project and `gradlew` ships executable. Build the
debug APK, it needs no signing key.

<details>
<summary>Windows (PowerShell)</summary>

```powershell
cd lsposed
.\gradlew.bat assembleDebug
```

</details>

<details>
<summary>Linux / macOS (bash)</summary>

```bash
cd lsposed
./gradlew assembleDebug
```

</details>

A signed release build (`assembleRelease`) needs your own keystore, see
`app/build.gradle.kts` for the env var names.

## License

GPL-3.0, see LICENSE.
