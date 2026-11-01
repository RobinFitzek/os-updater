package com.oxygenupdater.internal.settings

/**
 * Added in v5.4.0
 *
 * @see [com.oxygenupdater.BuildConfig.VERSION_CODE]
 */
const val KeyVersionCode = "version_code"

// Settings properties
const val KeyDevice = "device"
const val KeyDeviceId = "device_id"
const val KeyUpdateMethod = "update_method"
const val KeyUpdateMethodId = "update_method_id"

const val KeyThemeId = "theme_id"

const val KeyAdvancedMode = "advanced_mode"
const val KeySetupDone = "setup_done"
const val KeySqlToRoomMigrationDone = "sql_to_room_migration_done"
const val KeyIgnoreUnsupportedDeviceWarnings = "ignore_unsupported_device_warnings"
const val KeyIgnoreIncorrectDeviceWarnings = "ignore_incorrect_device_warnings"
const val KeyIgnoreNotificationPermissionSheet = "ignore_notification_permission_sheet"
const val KeyDownloadBytesDone = "download_bytes_done"

/**
 * Value cannot be changed - is from older version where it was called 'upload_logs'
 */
const val KeyShareAnalyticsAndLogs = "upload_logs"

const val KeyFlexibleAppUpdateIgnoreCount = "flexibleAppUpdateIgnoreCount"

/**
 * Interval (in minutes) between automatic background update checks.
 *
 * WorkManager enforces a minimum periodic interval of 15 minutes.
 */
const val KeyUpdateCheckIntervalMinutes = "update_check_interval_minutes"

/** OTA version of the newest update that a background check has notified about */
const val KeyLastNotifiedOtaVersion = "last_notified_ota_version"

/** JSON map of methodId -> [com.oxygenupdater.models.MethodRolloutSnapshot], written by the background check */
const val KeyMethodRolloutSnapshots = "method_rollout_snapshots"

// Notifications properties
const val KeyFirebaseToken = "firebase_token"
const val KeyFirebaseInstallationId = "firebase_installation_id"

/**
 * Value must not be changed for legacy reasons
 *
 * @see [com.oxygenupdater.utils.FcmUtils.currentFullTopic]
 */
const val KeyNotificationFullTopic = "notification_topic"

/**
 * Since v6.2.0
 *
 * @see [com.oxygenupdater.utils.FcmUtils.currentDeviceTopic]
 */
const val KeyNotificationDeviceTopic = "notification_topic_device"

const val KeyNotificationDelayInSeconds = "notification_delay_in_seconds"
