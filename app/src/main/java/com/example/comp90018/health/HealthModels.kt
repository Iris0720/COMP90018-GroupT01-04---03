package com.example.comp90018.health

enum class Feeling { GOOD, OKAY, UNWELL }

enum class SymptomLevel { NONE, MILD, SEVERE }

enum class HealthCondition { GOOD, CAUTION, UNKNOWN }

data class CheckInDraft(
    val id: String,
    val feeling: Feeling?,
    val breathing: SymptomLevel?,
    val fatigue: SymptomLevel?
)

data class SavedCheckIn(
    val id: String,
    val feeling: Feeling,
    val breathing: SymptomLevel,
    val fatigue: SymptomLevel,
    val condition: HealthCondition
)

data class MealFields(
    val id: String,
    val name: String,
    val serving: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbohydrateGrams: Int,
    val fatGrams: Int
)

data class MealEstimate(
    val fields: MealFields,
    val sourceLabel: String = "Demo estimate"
)

data class SavedMeal(val fields: MealFields)

sealed interface FeatureResult<out T> {
    data class Success<T>(val value: T) : FeatureResult<T>
    data class Failure(val message: String, val retryable: Boolean) : FeatureResult<Nothing>
}
