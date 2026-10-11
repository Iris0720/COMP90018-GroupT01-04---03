package com.example.comp90018.health

import android.content.Context
import com.example.comp90018.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject

interface HealthMealRecordStore {
    suspend fun saveMeal(fields: MealFields): FeatureResult<SavedMeal>
    suspend fun getSavedMeals(limit: Int): FeatureResult<List<SavedMeal>>
}

/**
 * Keeps meal history usable before the authentication branch is merged and
 * provides an offline cache after sign-in. Cloud data wins when the same ID
 * exists in both stores.
 */
class OfflineFirstMealRecordStore(
    context: Context,
    private val cloud: HealthMealRecordStore = SupabaseMealRecordStore()
) : HealthMealRecordStore {
    private val local = LocalMealRecordStore(context.applicationContext)

    override suspend fun saveMeal(fields: MealFields): FeatureResult<SavedMeal> {
        return when (val cloudResult = cloud.saveMeal(fields)) {
            is FeatureResult.Success -> cloudResult.also { local.saveSavedMeal(it.value) }
            is FeatureResult.Failure -> if (cloudResult.message.startsWith("Sign in")) {
                when (val localResult = local.saveMeal(fields)) {
                    is FeatureResult.Success -> FeatureResult.Success(
                        localResult.value.copy(sourceLabel = "Saved on this device · sign in to sync future meals")
                    )
                    is FeatureResult.Failure -> localResult
                }
            } else {
                cloudResult
            }
        }
    }

    override suspend fun getSavedMeals(limit: Int): FeatureResult<List<SavedMeal>> {
        val localMeals = (local.getSavedMeals(limit) as? FeatureResult.Success)?.value.orEmpty()
        return when (val cloudResult = cloud.getSavedMeals(limit)) {
            is FeatureResult.Success -> {
                val merged = (cloudResult.value + localMeals)
                    .distinctBy { it.fields.id }
                    .sortedByDescending { it.recordedAt }
                    .take(limit)
                FeatureResult.Success(merged)
            }
            is FeatureResult.Failure -> if (localMeals.isNotEmpty() || cloudResult.message.startsWith("Sign in")) {
                FeatureResult.Success(localMeals)
            } else {
                cloudResult
            }
        }
    }
}

private class LocalMealRecordStore(context: Context) : HealthMealRecordStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun saveMeal(fields: MealFields): FeatureResult<SavedMeal> {
        if (fields.id.isBlank() || fields.name.isBlank() || fields.serving.isBlank()) {
            return FeatureResult.Failure("Meal name and serving are required.", false)
        }
        if (listOf(fields.calories, fields.proteinGrams, fields.carbohydrateGrams, fields.fatGrams).any { it < 0 }) {
            return FeatureResult.Failure("Nutrition values cannot be negative.", false)
        }
        val saved = SavedMeal(fields, Instant.now().toString(), "Saved on this device")
        saveSavedMeal(saved)
        return FeatureResult.Success(saved)
    }

    fun saveSavedMeal(meal: SavedMeal) {
        val meals = readMeals().filterNot { it.fields.id == meal.fields.id } + meal
        preferences.edit().putString(MEALS_KEY, JSONArray(meals.map(::toJson)).toString()).apply()
    }

    override suspend fun getSavedMeals(limit: Int): FeatureResult<List<SavedMeal>> {
        return FeatureResult.Success(readMeals().sortedByDescending { it.recordedAt }.take(limit))
    }

    private fun readMeals(): List<SavedMeal> = runCatching {
        val array = JSONArray(preferences.getString(MEALS_KEY, "[]"))
        buildList {
            for (index in 0 until array.length()) add(fromJson(array.getJSONObject(index)))
        }
    }.getOrDefault(emptyList())

    private fun toJson(meal: SavedMeal) = JSONObject().apply {
        put("id", meal.fields.id)
        put("name", meal.fields.name)
        put("serving", meal.fields.serving)
        put("calories", meal.fields.calories)
        put("proteinGrams", meal.fields.proteinGrams)
        put("carbohydrateGrams", meal.fields.carbohydrateGrams)
        put("fatGrams", meal.fields.fatGrams)
        put("recordedAt", meal.recordedAt)
        put("sourceLabel", meal.sourceLabel)
    }

    private fun fromJson(value: JSONObject) = SavedMeal(
        fields = MealFields(
            id = value.getString("id"),
            name = value.getString("name"),
            serving = value.getString("serving"),
            calories = value.optInt("calories"),
            proteinGrams = value.optInt("proteinGrams"),
            carbohydrateGrams = value.optInt("carbohydrateGrams"),
            fatGrams = value.optInt("fatGrams")
        ),
        recordedAt = value.optString("recordedAt"),
        sourceLabel = value.optString("sourceLabel", "Saved on this device")
    )

    private companion object {
        const val PREFERENCES_NAME = "meal_history"
        const val MEALS_KEY = "saved_meals"
    }
}

class SupabaseMealRecordStore(
    suppliedClient: SupabaseClient? = null
) : HealthMealRecordStore {
    private val client by lazy { suppliedClient ?: SupabaseHealthClient.client }

    override suspend fun saveMeal(fields: MealFields): FeatureResult<SavedMeal> {
        validate(fields)?.let { return FeatureResult.Failure(it, false) }
        return withOwner { ownerId ->
            val recordedAt = Instant.now().toString()
            val row = MealEntryWriteRow(
                id = fields.id,
                ownerId = ownerId,
                recordedAt = recordedAt,
                name = fields.name.trim(),
                serving = fields.serving.trim(),
                calories = fields.calories,
                proteinGrams = fields.proteinGrams,
                carbohydrateGrams = fields.carbohydrateGrams,
                fatGrams = fields.fatGrams,
                sourceLabel = "Meal entry"
            )
            client.from(MEAL_ENTRIES).upsert(row) { onConflict = "id" }
            FeatureResult.Success(
                SavedMeal(fields.copy(name = row.name, serving = row.serving), recordedAt, row.sourceLabel)
            )
        }
    }

    override suspend fun getSavedMeals(limit: Int): FeatureResult<List<SavedMeal>> {
        if (limit !in 1..200) {
            return FeatureResult.Failure("Meal history limit must be between 1 and 200.", false)
        }
        return withOwner { ownerId ->
            val rows = client.from(MEAL_ENTRIES).select {
                filter { eq("owner_id", ownerId) }
                order("recorded_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<MealEntryRow>()
            FeatureResult.Success(rows.map(MealEntryRow::toSavedMeal))
        }
    }

    private suspend fun <T> withOwner(
        block: suspend (String) -> FeatureResult<T>
    ): FeatureResult<T> {
        return try {
            if (client.auth.config.autoLoadFromStorage) client.auth.awaitInitialization()
            val ownerId = client.auth.currentUserOrNull()?.id
                ?: return FeatureResult.Failure("Sign in before accessing saved meal records.", false)
            block(ownerId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IllegalStateException) {
            FeatureResult.Failure(error.message ?: "Supabase is not configured.", false)
        } catch (_: Exception) {
            FeatureResult.Failure("Could not access saved meals. Check your connection and sign-in session.", true)
        }
    }

    private fun validate(fields: MealFields): String? = when {
        fields.id.isBlank() -> "Meal ID is required."
        fields.name.isBlank() || fields.serving.isBlank() -> "Meal name and serving are required."
        listOf(fields.calories, fields.proteinGrams, fields.carbohydrateGrams, fields.fatGrams).any { it < 0 } ->
            "Nutrition values cannot be negative."
        else -> null
    }

    private companion object {
        const val MEAL_ENTRIES = "meal_entries"
    }
}

private object SupabaseHealthClient {
    val client by lazy {
        check(BuildConfig.SUPABASE_URL.isNotBlank()) { "SUPABASE_URL is missing from local.properties" }
        check(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "SUPABASE_PUBLISHABLE_KEY is missing from local.properties"
        }
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            install(Auth)
            install(Postgrest)
        }
    }
}

@Serializable
private data class MealEntryWriteRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("recorded_at") val recordedAt: String,
    val name: String,
    val serving: String,
    val calories: Int,
    @SerialName("protein_grams") val proteinGrams: Int,
    @SerialName("carbohydrate_grams") val carbohydrateGrams: Int,
    @SerialName("fat_grams") val fatGrams: Int,
    @SerialName("source_label") val sourceLabel: String
)

@Serializable
private data class MealEntryRow(
    val id: String,
    @SerialName("recorded_at") val recordedAt: String,
    val name: String,
    val serving: String,
    val calories: Int,
    @SerialName("protein_grams") val proteinGrams: Int,
    @SerialName("carbohydrate_grams") val carbohydrateGrams: Int,
    @SerialName("fat_grams") val fatGrams: Int,
    @SerialName("source_label") val sourceLabel: String
) {
    fun toSavedMeal() = SavedMeal(
        fields = MealFields(id, name, serving, calories, proteinGrams, carbohydrateGrams, fatGrams),
        recordedAt = recordedAt,
        sourceLabel = sourceLabel
    )
}
