package com.example.comp90018.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comp90018.health.*
import com.example.comp90018.ui.components.PrimaryButton
import com.example.comp90018.ui.components.StatusChip
import com.example.comp90018.ui.theme.ForestDark
import com.example.comp90018.ui.theme.Mint
import com.example.comp90018.ui.theme.Sand
import com.example.comp90018.ui.theme.Sky
import java.util.UUID

@Composable
fun HealthScreen(service: HealthMealService = remember { DemoHealthMealService() }) {
    var condition by remember { mutableStateOf(HealthCondition.UNKNOWN) }
    var showCheckIn by remember { mutableStateOf(false) }
    var feeling by remember { mutableStateOf<Feeling?>(null) }
    var breathing by remember { mutableStateOf<SymptomLevel?>(null) }
    var fatigue by remember { mutableStateOf<SymptomLevel?>(null) }
    var checkInMessage by remember { mutableStateOf<String?>(null) }
    var mealFields by remember { mutableStateOf<MealFields?>(null) }
    var mealMessage by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Health Hub", style = MaterialTheme.typography.headlineLarge)
        Text("A quick, non-diagnostic check-in for planning your activity.", color = MaterialTheme.colorScheme.secondary)
        ConditionCard(condition)

        Text("Health check-in", style = MaterialTheme.typography.titleLarge)
        InfoCard(Color.White) {
            if (!showCheckIn) {
                Text("How are you feeling?", fontWeight = FontWeight.Bold)
                Text("Feeling · Breathing · Fatigue", color = MaterialTheme.colorScheme.secondary)
                PrimaryButton(if (condition == HealthCondition.UNKNOWN) "Start check-in" else "Update check-in") {
                    showCheckIn = true
                    checkInMessage = null
                }
            } else {
                CheckInForm(feeling, breathing, fatigue, { feeling = it }, { breathing = it }, { fatigue = it })
                PrimaryButton("Save check-in") {
                    when (val result = service.submitCheckIn(CheckInDraft(UUID.randomUUID().toString(), feeling, breathing, fatigue))) {
                        is FeatureResult.Success -> {
                            condition = result.value.condition
                            checkInMessage = "Check-in saved."
                            showCheckIn = false
                        }
                        is FeatureResult.Failure -> checkInMessage = result.message
                    }
                }
                TextButton(onClick = { showCheckIn = false }) { Text("Cancel") }
            }
            checkInMessage?.let { SupportingMessage(it) }
        }

        Text("Meal log", style = MaterialTheme.typography.titleLarge)
        InfoCard(Sky) {
            Text("Add a meal", fontWeight = FontWeight.Bold)
            Text("Photo estimates are demonstrations and must be checked before saving.", color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val result = service.estimateMeal("demo-photo")
                        if (result is FeatureResult.Success) {
                            mealFields = result.value.fields
                            mealMessage = result.value.sourceLabel
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Demo photo") }
                OutlinedButton(
                    onClick = {
                        mealFields = MealFields(UUID.randomUUID().toString(), "", "", 0, 0, 0, 0)
                        mealMessage = "Manual entry"
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Enter manually") }
            }
        }

        mealFields?.let { fields ->
            MealEditor(
                fields = fields,
                sourceLabel = mealMessage.orEmpty(),
                onChange = { mealFields = it },
                onCancel = { mealFields = null },
                onSave = {
                    when (val result = service.saveMeal(fields)) {
                        is FeatureResult.Success -> {
                            mealMessage = "${result.value.fields.name} saved."
                            mealFields = null
                        }
                        is FeatureResult.Failure -> mealMessage = result.message
                    }
                }
            )
        } ?: mealMessage?.let { SupportingMessage(it) }

        Text(
            "This app provides general wellbeing information only. It does not diagnose or treat medical conditions.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ConditionCard(condition: HealthCondition) {
    val isGood = condition == HealthCondition.GOOD
    InfoCard(if (isGood) Mint else Sand) {
        Text("TODAY'S CONDITION", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        StatusChip(condition.name, if (isGood) Color(0xFFBFE9CE) else Color(0xFFFFE4A8), ForestDark)
        Text(
            when (condition) {
                HealthCondition.GOOD -> "No concerns reported. Listen to your body while moving."
                HealthCondition.CAUTION -> "Consider a lighter activity and seek professional advice if concerned."
                HealthCondition.UNKNOWN -> "Complete all questions to calculate a condition. Missing information never defaults to Good."
            }
        )
    }
}

@Composable
private fun CheckInForm(
    feeling: Feeling?,
    breathing: SymptomLevel?,
    fatigue: SymptomLevel?,
    onFeeling: (Feeling) -> Unit,
    onBreathing: (SymptomLevel) -> Unit,
    onFatigue: (SymptomLevel) -> Unit
) {
    Text("Feeling", fontWeight = FontWeight.Bold)
    ChoiceChips(Feeling.entries, feeling, onFeeling)
    Text("Breathing difficulty", fontWeight = FontWeight.Bold)
    ChoiceChips(SymptomLevel.entries, breathing, onBreathing)
    Text("Fatigue", fontWeight = FontWeight.Bold)
    ChoiceChips(SymptomLevel.entries, fatigue, onFatigue)
}

@Composable
private fun <T : Enum<T>> ChoiceChips(options: List<T>, selected: T?, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MealEditor(
    fields: MealFields,
    sourceLabel: String,
    onChange: (MealFields) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    InfoCard(Color.White) {
        Text("Review meal", style = MaterialTheme.typography.titleLarge)
        Text(sourceLabel, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        OutlinedTextField(fields.name, { onChange(fields.copy(name = it)) }, label = { Text("Meal name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(fields.serving, { onChange(fields.copy(serving = it)) }, label = { Text("Serving") }, modifier = Modifier.fillMaxWidth())
        NumberField("Calories", fields.calories) { onChange(fields.copy(calories = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Protein g", fields.proteinGrams, Modifier.weight(1f)) { onChange(fields.copy(proteinGrams = it)) }
            NumberField("Carbs g", fields.carbohydrateGrams, Modifier.weight(1f)) { onChange(fields.copy(carbohydrateGrams = it)) }
            NumberField("Fat g", fields.fatGrams, Modifier.weight(1f)) { onChange(fields.copy(fatGrams = it)) }
        }
        PrimaryButton("Save meal", onClick = onSave)
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun NumberField(label: String, value: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { input -> input.toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        modifier = modifier
    )
}

@Composable
private fun SupportingMessage(message: String) {
    Text(message, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
}

@Composable
private fun InfoCard(color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(color, RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content
    )
}
