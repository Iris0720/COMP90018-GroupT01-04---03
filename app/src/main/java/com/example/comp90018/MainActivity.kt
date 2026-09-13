package com.example.comp90018

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.comp90018.navigation.AppNav
import com.example.comp90018.ui.theme.TrailwiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TrailwiseTheme { AppNav() } }
    }
}
