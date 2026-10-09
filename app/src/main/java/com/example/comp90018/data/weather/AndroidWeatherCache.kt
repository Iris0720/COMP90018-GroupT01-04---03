package com.example.comp90018.data.weather

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Single latest location only; app-private, excluded from backup. No user account or route data. */
class AndroidWeatherCache internal constructor(context: Context, fileName: String) : WeatherCache {
    constructor(context: Context) : this(context, "weather-v1.json")

    private val file = AtomicFile(File((context.applicationContext ?: context).noBackupFilesDir, fileName))
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(): WeatherSnapshot? = withContext(Dispatchers.IO) {
        if (!file.baseFile.exists()) null
        else json.decodeFromString<WeatherSnapshot>(file.openRead().use { it.readBytes().decodeToString() })
    }

    override suspend fun write(snapshot: WeatherSnapshot) = withContext(Dispatchers.IO) {
        val output = file.startWrite()
        try {
            output.write(json.encodeToString(snapshot).encodeToByteArray())
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
