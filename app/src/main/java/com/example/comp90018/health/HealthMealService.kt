package com.example.comp90018.health

interface HealthMealService {
    fun submitCheckIn(draft: CheckInDraft): FeatureResult<SavedCheckIn>
    fun estimateMeal(photoReference: String): FeatureResult<MealEstimate>
    fun saveMeal(fields: MealFields): FeatureResult<SavedMeal>
}

/** Demo implementation. Replace its save calls with the Package 4 repository at integration time. */
class DemoHealthMealService : HealthMealService {
    private val checkIns = linkedMapOf<String, SavedCheckIn>()
    private val meals = linkedMapOf<String, SavedMeal>()

    override fun submitCheckIn(draft: CheckInDraft): FeatureResult<SavedCheckIn> {
        val feeling = draft.feeling
        val breathing = draft.breathing
        val fatigue = draft.fatigue
        if (draft.id.isBlank() || feeling == null || breathing == null || fatigue == null) {
            return FeatureResult.Failure("Complete all check-in fields before saving.", false)
        }
        val saved = SavedCheckIn(
            id = draft.id,
            feeling = feeling,
            breathing = breathing,
            fatigue = fatigue,
            condition = HealthRules.conditionFor(feeling, breathing, fatigue)
        )
        checkIns[draft.id] = saved
        return FeatureResult.Success(saved)
    }

    override fun estimateMeal(photoReference: String): FeatureResult<MealEstimate> {
        if (photoReference.isBlank()) {
            return FeatureResult.Failure("Choose a photo before estimating the meal.", false)
        }
        return FeatureResult.Success(
            MealEstimate(
                fields = MealFields(
                    id = "meal-${photoReference.hashCode()}",
                    name = "Chicken rice bowl",
                    serving = "1 bowl",
                    calories = 620,
                    proteinGrams = 38,
                    carbohydrateGrams = 72,
                    fatGrams = 18
                )
            )
        )
    }

    override fun saveMeal(fields: MealFields): FeatureResult<SavedMeal> {
        if (fields.id.isBlank() || fields.name.isBlank() || fields.serving.isBlank()) {
            return FeatureResult.Failure("Meal name and serving are required.", false)
        }
        if (listOf(fields.calories, fields.proteinGrams, fields.carbohydrateGrams, fields.fatGrams).any { it < 0 }) {
            return FeatureResult.Failure("Nutrition values cannot be negative.", false)
        }
        val saved = SavedMeal(fields)
        meals[fields.id] = saved
        return FeatureResult.Success(saved)
    }
}
