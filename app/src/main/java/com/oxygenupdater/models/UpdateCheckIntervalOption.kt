package com.oxygenupdater.models

/**
 * A selectable background update check interval, in minutes.
 * Used in the Settings screen's interval picker.
 */
data class UpdateCheckIntervalOption(
    override val id: Long,
) : SelectableModel {
    override val name: String = "$id min"
    override val subtitle: String? = null
}
