package com.oxygenupdater.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.models.MethodRolloutSnapshot
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.utils.json
import java.text.DateFormat
import java.util.Date

/**
 * Client-side rollout overview: what the background [com.oxygenupdater.workers.UpdateCheckWorker]
 * last saw for each update method of the selected device. No backend endpoint required —
 * snapshots are persisted locally as JSON after every background check.
 *
 * Shows per method: newest known build (OTA version + variant/region), build date,
 * and whether it's newer than the build currently installed on this device.
 */
@Composable
fun MethodRolloutTable(
    snapshotsJson: String,
    refreshing: Boolean,
) {
    val snapshots = remember(snapshotsJson) {
        if (snapshotsJson.isEmpty()) emptyList() else try {
            json.decodeFromString<Map<Long, MethodRolloutSnapshot>>(snapshotsJson)
                .values.sortedWith(
                    compareByDescending<MethodRolloutSnapshot> { it.buildDate }
                        .thenBy { it.methodName ?: "" }
                )
        } catch (_: Exception) {
            emptyList()
        }
    }

    Column(modifier = modifierDefaultPaddingStartTopEnd) {
        Text(
            text = stringResource(R.string.update_information_rollout_table_header),
            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary),
        )
        Text(
            text = stringResource(R.string.update_information_rollout_table_caption),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (snapshots.isEmpty()) {
        Text(
            text = stringResource(R.string.update_information_rollout_table_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifierDefaultPaddingStartTopEnd,
        )
        return
    }

    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val currentOta = SystemVersionProperties.otaVersion
    val currentBuildDate = UpdateData.getBuildDate(currentOta)

    Column(modifier = modifierDefaultPadding) {
        // Header row
        RolloutRow(
            method = stringResource(R.string.update_information_rollout_col_method),
            version = stringResource(R.string.update_information_rollout_col_build),
            variant = stringResource(R.string.update_information_rollout_col_variant),
            date = stringResource(R.string.update_information_rollout_col_date),
            isHeader = true,
        )
        snapshots.forEachIndexed { index, snap ->
            if (index > 0) HorizontalDivider()
            RolloutRow(
                method = snap.methodName ?: "#${snap.methodId}",
                version = snap.otaVersionNumber ?: "—",
                variant = snap.variantCode?.let { code -> snap.region?.let { "$it ($code)" } ?: code } ?: "—",
                date = if (snap.buildDate > 0) dateFormat.format(Date(snap.buildDate)) else "—",
                isNewer = snap.buildDate > currentBuildDate,
                isHeader = false,
            )
        }
    }
}

@Composable
private fun RolloutRow(
    method: String,
    version: String,
    variant: String,
    date: String,
    isNewer: Boolean = false,
    isHeader: Boolean,
) {
    val style = if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = method,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            text = version,
            style = style,
            color = if (isNewer && !isHeader) MaterialTheme.colorScheme.primary else color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f),
        )
        Text(
            text = variant,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = date,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.4f),
        )
    }
}
