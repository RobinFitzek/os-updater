# OS Updater — lightweight build

**APK:** `os-updater-7.1.2-lightweight-release.apk` (3.4 MB, minified, talks to the live prod server `oxygenupdater.com`)

- SHA-256: `bf7bb171a3a150e501e4943ece660bad4a1384e6cc0fc0049080d6386d87f3cf`
- applicationId `com.arjanvlek.oxygenupdater`, versionName `7.1.2` (versionCode 137), minSdk 26 (Android 8+)
- V2-signed with local key `CN=OS Updater,O=RobinFitzek`
  (cert SHA-256 `16ce5bfa493b15dc51625ed704d341b6abcfef73a7b4d1004961fc9d5628d384`)

Built with `./gradlew :app:assembleRelease` (JDK 17, Android SDK 37); `:app:test` passes.

Same signing key as 7.1.1, so it installs over 7.1.1 as an update. The signature differs from the
Play Store build and the earlier 7.1.0 lightweight build: coming from those, **uninstall first**, then install this APK. Enable "Install unknown apps"
for your browser/file manager if prompted.

## What's inside
No ads, no billing, no root contribution feature, no analytics/crashlytics telemetry, no Google Play
in-app updates (useless for a sideloaded build). Background update check across **all** update
methods of your device (full and incremental) at a configurable interval — Settings → Background
update check: 1, 2, 5, 10, 15, 30, 60, … min (below 15 min via a self-rescheduling one-shot chain).

The home screen shows a **rollout overview**: one card per update method, one row per region
(your device first, then Europe, India, China, Global, North America) with the build version,
variant, build date, and a *NEW* / *Installed* badge compared to the build on your device, plus a
summary of how many region/method combinations already have a newer build. Each row also shows
when the app **first saw** that build in that region (as precise as your check interval). Builds
already present on the very first check have no such date, since their real arrival is unknown. Regions are probed in
parallel on every manual refresh and every background check.

## 7.1.2
- "First seen" time per method × region, kept in a separate history so failed probes don't lose it.

## 7.1.1 vs 7.1.0
- APK 4.4 MB → 3.4 MB: dropped AppCompat (per-app language now uses the platform API on Android 13+
  and a small built-in fallback below), Google Play app-update and unused LiveData; minSdk 23 → 26
  removes the Java desugaring library; library translations limited to the app's own languages.
- Fixed: the 1-minute background chain stopped after the first run without a new update.
- Fixed: rollout table showed `yyyyMMddHHmm` build stamps as dates in 1976, and never marked
  ColorOS-style versions without a timestamp (e.g. `17.0.0.102(EX01)`) as newer.
