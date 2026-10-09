package com.example.comp90018.tracking

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

// Handles exporting an activity route as a GPX file and sharing it.
object GpxExporter {

    fun exportAndShare(
        context: Context,
        summary: ActivitySummary
    ): Result<Unit> {
        return runCatching {
            val pointCount = summary.routeSegments.sumOf { segment ->
                segment.size
            }

            // Stops the export if there are no GPS points.
            require(pointCount > 0) {
                "No GPS route is available to export."
            }

            val exportDirectory = File(
                context.cacheDir,
                "gpxexports"
            ).apply {
                mkdirs()
            }

            val safeSessionId = summary.sessionId
                .replace(Regex("[^A-Za-z0-9-]"), "-")

            val file = File(
                exportDirectory,
                "trailwise-$safeSessionId.gpx"
            )

            file.writeText(
                text = buildGpx(summary),
                charset = Charsets.UTF_8
            )

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.gpxprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "${summary.activityType} route"
                )

                clipData = ClipData.newRawUri(
                    "Trailwise GPX route",
                    uri
                )

                // Gives the receiving app temporary permission to read the file.
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Export GPX route"
                )
            )
        }
    }

    private fun buildGpx(
        summary: ActivitySummary
    ): String {
        val activityName = escapeXml(
            summary.activityType
        )

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")

            appendLine(
                """<gpx version="1.1" creator="Trailwise" xmlns="http://www.topografix.com/GPX/1/1">"""
            )

            appendLine("  <metadata>")
            appendLine("    <name>$activityName activity</name>")
            appendLine("  </metadata>")

            appendLine("  <trk>")
            appendLine("    <name>$activityName route</name>")

            summary.routeSegments.forEach { segment ->
                if (segment.isNotEmpty()) {
                    appendLine("    <trkseg>")

                    segment.forEach { point ->
                        val latitude = String.format(
                            Locale.US,
                            "%.7f",
                            point.latitude
                        )

                        val longitude = String.format(
                            Locale.US,
                            "%.7f",
                            point.longitude
                        )

                        appendLine(
                            """      <trkpt lat="$latitude" lon="$longitude" />"""
                        )
                    }

                    appendLine("    </trkseg>")
                }
            }

            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}