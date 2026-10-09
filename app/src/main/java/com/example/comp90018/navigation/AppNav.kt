package com.example.comp90018.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comp90018.model.AppTab
import com.example.comp90018.ui.screens.activity.ActivityScreen
import com.example.comp90018.ui.screens.health.HealthScreen
import com.example.comp90018.ui.screens.profile.ProfileScreen
import com.example.comp90018.ui.screens.today.TodayScreen
import com.example.comp90018.ui.theme.AppBackground
import com.example.comp90018.ui.theme.Forest
import com.example.comp90018.ui.theme.ForestDark
import com.example.comp90018.ui.theme.TrailwiseTheme
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.comp90018.ui.screens.login.LoginScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comp90018.ui.screens.login.LoginViewModel
import com.example.comp90018.ui.screens.login.RegisterViewModel
import com.example.comp90018.ui.screens.cards.EnterDisplayName
import com.example.comp90018.ui.screens.login.DisplayNameViewModel
import com.example.comp90018.data.ProfileData
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun AppNav() {

    val loginVm: LoginViewModel = viewModel()
    val registerVm: RegisterViewModel = viewModel()
    val displayNameVm: DisplayNameViewModel = viewModel()

    val registerState by registerVm.state.collectAsState()
    val loginState by loginVm.state.collectAsState()
    val profile by displayNameVm.profile.collectAsState()

    LaunchedEffect(loginState.loggedIn) {
        if (loginState.loggedIn) {
            displayNameVm.loadProfile()
        }
    }
    if (loginState.loggedIn) {
        MainScaffold( profile = profile, 
            onLogout = { loginVm.signOut() }, 
            onSaveProfile = { name, goal, units -> 
                displayNameVm.saveProfile(name, goal, units)
            }
        )
        if (profile?.displayName?.isBlank() == true) {
            EnterDisplayName(
                onSubmit = {displayName ->
                    displayNameVm.setProfileDataDisplayName(displayName)    
                }
            )
        }
    } else {
        LoginScreen(
            isLoading = loginState.isLoading,
            isRegistering = registerState.isRegistering,
            errorMessage = registerState.error ?: loginState.error,
            successMessage = if (registerState.success) {
                "Account created. Check your email to confirm, then sign in."
            } else { 
                null
            },
            onSignIn = { email, password -> loginVm.signIn(email, password) },
            onCreateAccount = { email, password -> registerVm.register(email, password) },
            onForgotPassword = { /* TODO */},
        )
    }

}


@Composable
fun MainScaffold(
    profile: ProfileData?,
    onLogout: () -> Unit,
    onSaveProfile: (String, Int, String) -> Unit
    ) {
    var selectedTab by remember { mutableStateOf(AppTab.TODAY) }
    var showProfile by remember { mutableStateOf(false) }

    if (showProfile) {
        ProfileScreen( 
            initialName = profile?.displayName.orEmpty(),
            initialStepGoal = profile?.dailyStepTarget ?: 10_000,
            initialUnits = profile?.unitSystem ?: "Metric",
            onDismiss = { showProfile = false }, onLogout = { onLogout() },
            onSave = onSaveProfile
        )
    } else {
        Scaffold(
            containerColor = AppBackground,
            topBar = { AppHeader(initial = initialsOf(profile?.displayName.orEmpty()) ,selectedTab.label, onProfile = { showProfile = true }) },
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
                    AppTab.TODAY -> TodayScreen(onStartActivity = { selectedTab = AppTab.ACTIVITY })
                    AppTab.ACTIVITY -> ActivityScreen()
                    AppTab.HEALTH -> HealthScreen()
                }
            }
        }
    }

}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppHeader(initial: String, title: String, onProfile: () -> Unit) {
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
