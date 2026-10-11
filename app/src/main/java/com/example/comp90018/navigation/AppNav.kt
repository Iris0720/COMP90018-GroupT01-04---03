package com.example.comp90018.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comp90018.data.ProfileData
import com.example.comp90018.health.MealFields
import com.example.comp90018.health.HealthCondition
import com.example.comp90018.model.AppTab
import com.example.comp90018.model.SessionState
import com.example.comp90018.permissions.rememberPermissionController
import com.example.comp90018.tracking.TrackingViewModel
import com.example.comp90018.ui.screens.activity.ActivityScreen
import com.example.comp90018.ui.screens.cards.EnterDisplayName
import com.example.comp90018.ui.screens.health.HealthScreen
import com.example.comp90018.ui.screens.login.DisplayNameViewModel
import com.example.comp90018.ui.screens.login.LoginScreen
import com.example.comp90018.ui.screens.login.LoginViewModel
import com.example.comp90018.ui.screens.login.RegisterViewModel
import com.example.comp90018.ui.screens.profile.ProfileScreen
import com.example.comp90018.ui.screens.today.TodayScreen
import com.example.comp90018.ui.theme.AppBackground
import com.example.comp90018.ui.theme.Forest
import com.example.comp90018.ui.theme.ForestDark
import com.example.comp90018.ui.theme.TrailwiseTheme
import java.time.LocalDate

@Composable
fun AppNav() {
    val permissions = rememberPermissionController()
    val loginViewModel: LoginViewModel = viewModel()
    val registerViewModel: RegisterViewModel = viewModel()
    val profileViewModel: DisplayNameViewModel = viewModel()

    val loginState by loginViewModel.state.collectAsState()
    val registerState by registerViewModel.state.collectAsState()
    val profile by profileViewModel.profile.collectAsState()

    LaunchedEffect(loginState.loggedIn) {
        if (loginState.loggedIn) profileViewModel.loadProfile()
    }

    if (!loginState.loggedIn) {
        LoginScreen(
            isLoading = loginState.isLoading,
            isRegistering = registerState.isRegistering,
            errorMessage = registerState.error ?: loginState.error,
            successMessage = if (registerState.success) {
                "Account created. Check your email to confirm, then sign in."
            } else {
                null
            },
            onSignIn = loginViewModel::signIn,
            onCreateAccount = registerViewModel::register
        )
        return
    }

    if (profile?.displayName?.isBlank() == true) {
        EnterDisplayName(onSubmit = profileViewModel::setProfileDataDisplayName)
    }

    MainScaffold(
        permissions = permissions,
        profile = profile,
        onLogout = loginViewModel::signOut,
        onSaveProfile = profileViewModel::saveProfile
    )
}

@Composable
private fun MainScaffold(
    permissions: com.example.comp90018.permissions.PermissionController,
    profile: ProfileData?,
    onLogout: () -> Unit,
    onSaveProfile: (String, Int, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.TODAY) }
    var todayCondition by rememberSaveable {
        mutableStateOf(loadTodayCondition(context, profile?.id))
    }
    LaunchedEffect(profile?.id) {
        todayCondition = loadTodayCondition(context, profile?.id)
    }
    var showProfile by remember { mutableStateOf(false) }
    val trackingViewModel: TrackingViewModel = viewModel()
    LaunchedEffect(trackingViewModel) {
        trackingViewModel.loadHistory()
    }
    val appScope = rememberCoroutineScope()
    var pendingMealFields by remember { mutableStateOf<MealFields?>(null) }
    var pendingMealSourceLabel by remember { mutableStateOf("") }

    if (showProfile) {
        ProfileScreen(
            permissions = permissions,
            initialName = profile?.displayName.orEmpty(),
            initialStepGoal = profile?.dailyStepTarget ?: 10_000,
            initialUnits = profile?.unitSystem ?: "metric",
            onDismiss = { showProfile = false },
            onLogout = onLogout,
            onSave = onSaveProfile
        )
        return
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AppHeader(
                title = selectedTab.label,
                initial = initialsOf(profile?.displayName.orEmpty()),
                onProfile = { showProfile = true }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.mark, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                AppTab.TODAY -> TodayScreen(
                    historyState = trackingViewModel.historyState,
                    condition = todayCondition,
                    onStartActivity = {
                        if (trackingViewModel.uiState.phase == SessionState.COMPLETE) {
                            trackingViewModel.reset()
                        }
                        selectedTab = AppTab.ACTIVITY
                    }
                )
                AppTab.ACTIVITY -> ActivityScreen(trackingViewModel = trackingViewModel)
                AppTab.HEALTH -> HealthScreen(
                    permissions = permissions,
                    condition = todayCondition,
                    onConditionChange = { condition ->
                        todayCondition = condition
                        saveTodayCondition(context, profile?.id, condition)
                    },
                    operationScope = appScope,
                    mealFields = pendingMealFields,
                    onMealFieldsChange = { pendingMealFields = it },
                    mealSourceLabel = pendingMealSourceLabel,
                    onMealSourceLabelChange = { pendingMealSourceLabel = it }
                )
            }
        }
    }
}

private fun loadTodayCondition(context: android.content.Context, userId: String?): HealthCondition {
    if (userId.isNullOrBlank()) return HealthCondition.UNKNOWN
    val preferences = context.getSharedPreferences(HEALTH_CONDITION_PREFERENCES, android.content.Context.MODE_PRIVATE)
    val today = LocalDate.now().toString()
    if (preferences.getString("date_$userId", null) != today) return HealthCondition.UNKNOWN
    return runCatching {
        HealthCondition.valueOf(preferences.getString("condition_$userId", null) ?: return HealthCondition.UNKNOWN)
    }.getOrDefault(HealthCondition.UNKNOWN)
}

private fun saveTodayCondition(
    context: android.content.Context,
    userId: String?,
    condition: HealthCondition
) {
    if (userId.isNullOrBlank()) return
    context.getSharedPreferences(HEALTH_CONDITION_PREFERENCES, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString("date_$userId", LocalDate.now().toString())
        .putString("condition_$userId", condition.name)
        .apply()
}

private const val HEALTH_CONDITION_PREFERENCES = "today_health_condition"

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "GU"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppHeader(title: String, initial: String, onProfile: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("TRAILWISE", color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        actions = {
            Surface(
                onClick = onProfile,
                modifier = Modifier.padding(end = 16.dp).size(44.dp),
                shape = CircleShape,
                color = ForestDark
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initial, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNavPreview() = TrailwiseTheme { AppNav() }
