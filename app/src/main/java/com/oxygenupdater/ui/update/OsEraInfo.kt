package com.oxygenupdater.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.oxygenupdater.R
import com.oxygenupdater.icons.MobileInfo
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.models.OsBrand
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.common.modifierDefaultPaddingStart
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd

/**
 * Shows the OS era (OxygenOS vs unified ColorOS — there is no OxygenOS 17) and the
 * variant (region) of the currently installed build, e.g. `ColorOS 17 · EU (EX01)`.
 */
@Composable
fun OsEraInfo() {
    val current = OsBrand.current
    val region = current.region
    val variantCode = current.variantCode

    val subtitle = buildString {
        if (current.major > 0) append(current.name).append(' ').append(current.major)
        else append(current.name)
        if (region != null) append(" · ").append(region)
        if (variantCode != null) append(" (").append(variantCode).append(')')
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifierDefaultPaddingStartTopEnd,
    ) {
        Icon(
            imageVector = Symbols.MobileInfo,
            contentDescription = stringResource(R.string.icon),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifierDefaultPaddingStart) {
            Text(
                text = stringResource(R.string.update_information_os_era),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (current.isColorOsEra && current.major >= 17) {
                Text(
                    text = stringResource(R.string.update_information_os_era_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
