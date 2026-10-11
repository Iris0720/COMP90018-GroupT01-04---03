package com.example.comp90018.health

object HealthRules {
    fun conditionFor(
        feeling: Feeling?,
        breathing: SymptomLevel?,
        fatigue: SymptomLevel?
    ): HealthCondition {
        if (feeling == null || breathing == null || fatigue == null) {
            return HealthCondition.UNKNOWN
        }
        return if (
            feeling == Feeling.UNWELL ||
            breathing == SymptomLevel.SEVERE ||
            fatigue == SymptomLevel.SEVERE
        ) HealthCondition.CAUTION else HealthCondition.GOOD
    }
}
