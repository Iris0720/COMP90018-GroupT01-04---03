package com.example.comp90018.ui.screens.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.comp90018.tracking.TrackPoint

@Composable
fun RoutePreview(
    segments: List<List<TrackPoint>>,
    completed: Boolean = false,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 240.dp,
    fillAvailableHeight: Boolean = false
) {
    // Counts the total number of recorded GPS points.
    val recordedPoints = segments.sumOf { it.size }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Activity route",
            style = MaterialTheme.typography.titleLarge
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (fillAvailableHeight) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.height(chartHeight)
                    }
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            ActivityRouteMap(
                segments = segments,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = when {
                recordedPoints >= 2 ->
                    "$recordedPoints GPS points recorded"

                completed ->
                    "Move a little farther to show your route"

                else ->
                    "Waiting for movement"
            },
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "© OpenStreetMap contributors · OpenFreeMap",
            style = MaterialTheme.typography.bodySmall
        )
    }
}