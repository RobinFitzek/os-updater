# OS Updater — lightweight rebuild (debug APK)

Built from branch `vibe/lightweight-auto-pull-f3dea3` (PR #1) with Corretto 17 + Android SDK 37:
`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL, `:app:test` → 34/34 pass.

- File: `os-updater-7.1.0-lightweight-debug.apk` (~10.2 MB, debug-signed)
- SHA-256 (latest): `7d6e0be037fdf925971753ff0bd1432f5c1fd1f0d315d6cafa2b418a014249ed`

Debug-signed means you must **uninstall the Play Store version first** (different signature),
then install this APK. Enable "Install unknown apps" for your browser/file manager if prompted.

## What's inside
No ads, no billing, no root contribution feature, no analytics/crashlytics telemetry (settings toggle removed; nothing is sent). Background update check across **all**
update methods of your device at a configurable interval (Settings → Background update check,
15 min default), high-priority notification on new builds, `Cache-Control: no-store` so every
refresh is a real fetch, ColorOS 17-correct labels (no OxygenOS 17), and a client-side
per-method rollout table on the homepage.
