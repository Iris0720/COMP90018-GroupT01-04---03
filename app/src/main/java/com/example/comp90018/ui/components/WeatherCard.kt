package com.example.comp90018.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.comp90018.data.weather.WeatherRepository
import com.example.comp90018.data.weather.WeatherResult
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Optional presentation component. Its screen/ViewModel owns loading, location and refresh. */
@Composable
fun WeatherCard(
    result: WeatherResult?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Local weather", style = MaterialTheme.typography.titleMedium)
            when (result) {
                is WeatherResult.Available -> {
                    val weather = result.weather
                    Text(String.format(Locale.getDefault(), "%.1f °C · %s", weather.temperatureCelsius, weather.description))
                    Text(String.format(Locale.getDefault(), "Wind: %.1f km/h", weather.windSpeedKmh))
                    val observed = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(weather.observedAtEpochSeconds))
                    Text("Observed: $observed", style = MaterialTheme.typography.bodySmall)
                    if (result.stale) Text("Last known weather — refresh failed", color = MaterialTheme.colorScheme.error)
                }
                is WeatherResult.Unavailable -> Text(
                    if (result.reason == WeatherResult.Reason.INVALID_LOCATION) "Weather needs a valid location."
                    else "Weather is unavailable. Your activity can continue."
                )
                null -> if (!isLoading) Text("Weather has not been loaded yet.")
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            TextButton(onClick = onRefresh, enabled = !isLoading) { Text("Refresh weather") }
            TextButton(onClick = { uriHandler.openUri(WeatherRepository.ATTRIBUTION_URL) }) {
                Text(WeatherRepository.ATTRIBUTION, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
