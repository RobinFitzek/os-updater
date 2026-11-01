# OS Updater — lightweight rebuild (debug APK)

Built from branch `vibe/lightweight-auto-pull-f3dea3` (PR #1) with Corretto 17 + Android SDK 37:
`./gradlew :app:assembleDebug` → BUILD SUCCESSFUL, `:app:test` → 34/34 pass.

- **Release APK (use this one — it talks to the live prod server `oxygenupdater.com`)**:
  `os-updater-7.1.0-lightweight-release.apk` (4.4 MB, minified, signed with local key `CN=OS Updater,O=RobinFitzek`)
  - SHA-256: `31b51e1e1f0a5ae2c8890737800affd800926b1524aa63e8046b1d2f5bc1cb87`
  - applicationId `com.arjanvlek.oxygenupdater`, versionName `7.1.0`, V2-signed (verified with apksigner)
- Older debug APK (`os-updater-7.1.0-lightweight-debug.apk`) points at the dead test server — do NOT use it.

Because the signature differs from the Play Store build, **uninstall the Play version first**,
then install the release APK. Enable "Install unknown apps" for your browser/file manager if prompted.

## What's inside
No ads, no billing, no root contribution feature, no analytics/crashlytics telemetry (settings toggle removed; nothing is sent). Background update check across **all**
update methods of your device (full AND incremental) at a configurable interval — Settings →
Background update check: 1, 2, 5, 10, 15, 30, 60, ... min (1 min possible via self-rescheduling
one-shot chain; below 15 min WorkManager periodic isn't allowed). Homepage also shows a
per-method × per-region rollout table (method, region EU/IN/CN/GLO/NA, build, build date) — probed on every check, manual pull-to-refresh AND background worker., high-priority notification on new builds, `Cache-Control: no-store` so every
refresh is a real fetch, ColorOS 17-correct labels (no OxygenOS 17), and a client-side
per-method rollout table on the homepage.
