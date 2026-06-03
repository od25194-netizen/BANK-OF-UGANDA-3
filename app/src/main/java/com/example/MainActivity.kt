package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CreditViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: CreditViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val isDarkThemePref by viewModel.isDarkTheme.collectAsState()
      val isDark = isDarkThemePref ?: androidx.compose.foundation.isSystemInDarkTheme()

      MyApplicationTheme(darkTheme = isDark) {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
        ) { innerPadding ->
          MainAppScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}
