package com.example.comp90018

import com.example.comp90018.health.Feeling
import com.example.comp90018.health.HealthCondition
import com.example.comp90018.health.HealthRules
import com.example.comp90018.health.SymptomLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthRulesTest {
    @Test
    fun missingAnswer_isUnknown() {
        assertEquals(
            HealthCondition.UNKNOWN,
            HealthRules.conditionFor(Feeling.GOOD, null, SymptomLevel.NONE)
        )
    }

    @Test
    fun severeSymptom_isCaution() {
        assertEquals(
            HealthCondition.CAUTION,
            HealthRules.conditionFor(Feeling.OKAY, SymptomLevel.SEVERE, SymptomLevel.NONE)
        )
    }

    @Test
    fun completeLowRiskAnswers_areGood() {
        assertEquals(
            HealthCondition.GOOD,
            HealthRules.conditionFor(Feeling.GOOD, SymptomLevel.NONE, SymptomLevel.MILD)
        )
    }
}
