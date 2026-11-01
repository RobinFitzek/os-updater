# OS Updater — lightweight rebuild (debug APK)

Built from branch `vibe/lightweight-auto-pull-f3dea3` (PR #1) with Corretto 17 + Android SDK 37:
`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL, `:app:test` → 34/34 pass.

- File: `os-updater-7.1.0-lightweight-debug.apk` (~10.2 MB, debug-signed)
- SHA-256: `f5b6866f25a5bfee44b4be9f0736edf60dd4e34815a239e87d213807ceeef7fc`

Debug-signed means you must **uninstall the Play Store version first** (different signature),
then install this APK. Enable "Install unknown apps" for your browser/file manager if prompted.

## What's inside
No ads, no billing, no root contribution feature. Background update check across **all**
update methods of your device at a configurable interval (Settings → Background update check,
15 min default), high-priority notification on new builds, `Cache-Control: no-store` so every
refresh is a real fetch, ColorOS 17-correct labels (no OxygenOS 17), and a client-side
per-method rollout table on the homepage.
