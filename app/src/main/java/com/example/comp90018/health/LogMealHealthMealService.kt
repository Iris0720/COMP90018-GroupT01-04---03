package com.example.comp90018.health

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream
import java.util.UUID

class LogMealHealthMealService(
    private val contentResolver: ContentResolver,
    private val token: String
) : HealthMealService {
    private val local = DemoHealthMealService()

    override fun submitCheckIn(draft: CheckInDraft) = local.submitCheckIn(draft)
    override fun saveMeal(fields: MealFields) = local.saveMeal(fields)

    override suspend fun estimateMeal(photoReference: String): FeatureResult<MealEstimate> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext FeatureResult.Failure("LogMeal API token is not configured.", false)
        try {
            val recognition = uploadPhoto(Uri.parse(photoReference))
            val imageId = recognition.optLong("imageId", -1L)
            if (imageId < 0) return@withContext FeatureResult.Failure("The API did not return an image ID.", true)
            val nutrition = requestNutrition(imageId)
            val name = findFirstString(recognition, setOf("foodName", "name")) ?: "Recognised meal"
            FeatureResult.Success(
                MealEstimate(
                    fields = MealFields(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        serving = "1 estimated serving",
                        calories = findNutrient(nutrition, setOf("ENERC_KCAL", "calories", "energy")),
                        proteinGrams = findNutrient(nutrition, setOf("PROCNT", "protein")),
                        carbohydrateGrams = findNutrient(nutrition, setOf("CHOCDF", "carbohydrates", "carbs")),
                        fatGrams = findNutrient(nutrition, setOf("FAT", "fat"))
                    ),
                    sourceLabel = "LogMeal estimate · review before saving"
                )
            )
        } catch (error: ApiException) {
            FeatureResult.Failure(error.message ?: "Food recognition failed.", error.retryable)
        } catch (_: Exception) {
            FeatureResult.Failure("Could not analyse this photo. Check your connection and try again.", true)
        }
    }

    private fun uploadPhoto(uri: Uri): JSONObject {
        val boundary = "Trailwise-${UUID.randomUUID()}"
        val connection = open("https://api.logmeal.com/v2/image/segmentation/complete/v1.0")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.doOutput = true
        val bytes = prepareUpload(uri)
        connection.outputStream.use { output ->
            output.write("--$boundary\r\n".toByteArray())
            output.write("Content-Disposition: form-data; name=\"image\"; filename=\"meal.jpg\"\r\n".toByteArray())
            output.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
            output.write(bytes)
            output.write("\r\n--$boundary--\r\n".toByteArray())
        }
        return response(connection)
    }

    /** LogMeal rejects uploads at or above 1 MiB, so normalise camera/gallery images first. */
    private fun prepareUpload(uri: Uri): ByteArray {
        val bitmap = contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: throw ApiException("The selected photo could not be read.", false)
        val longestSide = maxOf(bitmap.width, bitmap.height)
        val scale = minOf(1f, 1600f / longestSide)
        val uploadBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { bitmap.recycle() }
        } else {
            bitmap
        }

        var quality = 88
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { output ->
                uploadBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                output.toByteArray()
            }
            quality -= 8
        } while (bytes.size > MAX_UPLOAD_BYTES && quality >= 40)
        uploadBitmap.recycle()

        if (bytes.size > MAX_UPLOAD_BYTES) {
            throw ApiException("The selected photo is too large to upload.", false)
        }
        return bytes
    }

    private fun requestNutrition(imageId: Long): JSONObject {
        val connection = open("https://api.logmeal.com/v2/nutrition/recipe/nutritionalInfo/v1.0")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.use { it.write(JSONObject().put("imageId", imageId).toString().toByteArray()) }
        return response(connection)
    }

    private fun open(address: String) = (URL(address).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 30_000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
    }

    private fun response(connection: HttpURLConnection): JSONObject {
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = when (code) {
                401 -> "The LogMeal token is invalid or expired."
                403 -> "This LogMeal account cannot use the requested feature."
                413 -> "The selected photo is too large to upload."
                429 -> "The food recognition limit has been reached. Try again later."
                else -> "Food recognition failed (HTTP $code)."
            }
            throw ApiException(message, code == 429 || code >= 500)
        }
        return JSONObject(body)
    }

    private fun findFirstString(value: Any?, keys: Set<String>): String? {
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                if (key in keys) {
                    val found = value.opt(key)
                    if (found is String && found.isNotBlank()) return found
                    if (found is JSONArray && found.length() > 0) return found.optString(0)
                }
                findFirstString(value.opt(key), keys)?.let { return it }
            }
            is JSONArray -> for (index in 0 until value.length()) findFirstString(value.opt(index), keys)?.let { return it }
        }
        return null
    }

    private fun findNutrient(value: Any?, keys: Set<String>): Int {
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                if (keys.any { it.equals(key, ignoreCase = true) }) {
                    val raw = value.opt(key)
                    val number = when (raw) {
                        is Number -> raw.toDouble()
                        is JSONObject -> raw.optDouble("quantity", raw.optDouble("value", Double.NaN))
                        else -> Double.NaN
                    }
                    if (!number.isNaN()) return number.toInt().coerceAtLeast(0)
                }
                val nested = findNutrient(value.opt(key), keys)
                if (nested > 0) return nested
            }
            is JSONArray -> for (index in 0 until value.length()) {
                val nested = findNutrient(value.opt(index), keys)
                if (nested > 0) return nested
            }
        }
        return 0
    }

    private class ApiException(message: String, val retryable: Boolean) : Exception(message)

    private companion object {
        const val MAX_UPLOAD_BYTES = 950_000
    }
}
