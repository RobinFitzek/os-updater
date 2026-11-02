package com.oxygenupdater.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.models.MethodRolloutSnapshot
import com.oxygenupdater.models.MethodRolloutSnapshot.Comparison
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.utils.json
import java.text.DateFormat
import java.util.Date

/**
 * Client-side rollout overview: what the last check (manual or background) saw for each
 * update method of the selected device, in every probed region. No backend endpoint
 * required — snapshots are persisted locally as JSON after every check.
 *
 * One card per method; inside, one row per region (device's own first) with the build,
 * its date, and whether it's newer than / the same as the build installed on this device.
 */
@Composable
fun MethodRolloutTable(
    snapshotsJson: String,
    refreshing: Boolean,
) {
    val methods = remember(snapshotsJson) { groupSnapshots(snapshotsJson) }
    val currentOta = remember { MethodRolloutSnapshot.currentOtaProvider() }

    Column(modifier = modifierDefaultPaddingStartTopEnd) {
        Text(
            text = stringResource(R.string.update_information_rollout_table_header),
            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary),
        )

        val lastChecked = methods.maxOfOrNull { method -> method.rows.maxOf { it.checkedAtEpochMs } }
        val newerRegions = methods.sumOf { method -> method.rows.count { it.compareToCurrent(currentOta) == Comparison.Newer } }
        val totalRegions = methods.sumOf { it.rows.size }
        val summary = when {
            methods.isEmpty() -> stringResource(R.string.update_information_rollout_table_empty)
            newerRegions == 0 -> stringResource(R.string.update_information_rollout_summary_none_newer)
            else -> pluralStringResource(
                R.plurals.update_information_rollout_summary_newer, newerRegions, newerRegions, totalRegions
            )
        }
        Text(
            text = summary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (lastChecked != null) Text(
            text = stringResource(
                R.string.update_information_rollout_last_checked,
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastChecked)),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        if (refreshing) LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }

    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    methods.forEach { method ->
        MethodCard(method = method, currentOta = currentOta, dateFormat = dateFormat)
    }
}

@Composable
private fun MethodCard(
    method: MethodRollout,
    currentOta: String,
    dateFormat: DateFormat,
) = Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = MaterialTheme.shapes.medium,
    modifier = modifierDefaultPaddingStartTopEnd.fillMaxWidth(),
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            text = method.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        method.rows.forEachIndexed { index, snapshot ->
            if (index > 0) HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            RegionRow(snapshot, snapshot.compareToCurrent(currentOta), dateFormat)
        }
    }
}

@Composable
private fun RegionRow(
    snapshot: MethodRolloutSnapshot,
    comparison: Comparison,
    dateFormat: DateFormat,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
) {
    val ownRegion = snapshot.region.isEmpty()
    Text(
        text = if (ownRegion) stringResource(R.string.update_information_rollout_own_region) else regionName(snapshot.region),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (ownRegion) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 2,
        modifier = Modifier.width(76.dp),
    )

    Column(Modifier.weight(1f)) {
        val version = snapshot.displayVersion
        val variant = snapshot.variantCode
        Text(
            text = when {
                version == null -> "—"
                variant == null -> version
                else -> "$version · $variant"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = if (comparison == Comparison.Newer) MaterialTheme.colorScheme.primary else Color.Unspecified,
            fontWeight = if (comparison == Comparison.Newer) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        snapshot.buildDateMillis?.let {
            Text(
                text = dateFormat.format(Date(it)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    when (comparison) {
        Comparison.Newer -> Badge(
            text = stringResource(R.string.update_information_rollout_badge_new),
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
        )

        Comparison.Same -> Badge(
            text = stringResource(R.string.update_information_rollout_badge_installed),
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        else -> {}
    }
}

@Composable
private fun Badge(text: String, container: Color, content: Color) = Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.SemiBold,
    color = content,
    maxLines = 1,
    modifier = Modifier
        .background(container, RoundedCornerShape(50))
        .padding(horizontal = 8.dp, vertical = 2.dp),
)

@Composable
private fun regionName(code: String) = when (code) {
    "EU" -> stringResource(R.string.update_information_rollout_region_eu)
    "IN" -> stringResource(R.string.update_information_rollout_region_in)
    "CN" -> stringResource(R.string.update_information_rollout_region_cn)
    "GLO" -> stringResource(R.string.update_information_rollout_region_glo)
    "NA" -> stringResource(R.string.update_information_rollout_region_na)
    else -> code
}

private class MethodRollout(val name: String, val rows: List<MethodRolloutSnapshot>)

/** Groups by method (in name order); regions within a method in [MethodRolloutSnapshot.RolloutRegions] order, own first */
private fun groupSnapshots(snapshotsJson: String): List<MethodRollout> {
    if (snapshotsJson.isEmpty()) return emptyList()
    val snapshots = try {
        json.decodeFromString<Map<String, MethodRolloutSnapshot>>(snapshotsJson).values
    } catch (_: Exception) {
        return emptyList()
    }

    val regionOrder = listOf("") + MethodRolloutSnapshot.RolloutRegions
    return snapshots.groupBy { it.methodId }.values.map { rows ->
        val first = rows.first()
        MethodRollout(
            name = first.methodName ?: "#${first.methodId}",
            rows = rows.sortedBy { regionOrder.indexOf(it.region).let { i -> if (i < 0) Int.MAX_VALUE else i } },
        )
    }.sortedBy { it.name }
}
