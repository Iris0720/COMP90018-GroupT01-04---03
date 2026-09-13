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

@Composable
fun AppNav() {
    var selectedTab by remember { mutableStateOf(AppTab.TODAY) }
    var showProfile by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppHeader(selectedTab.label, onProfile = { showProfile = true }) },
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

    if (showProfile) ProfileScreen(onDismiss = { showProfile = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppHeader(title: String, onProfile: () -> Unit) {
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
                    Text("GU", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppNavPreview() = TrailwiseTheme { AppNav() }
