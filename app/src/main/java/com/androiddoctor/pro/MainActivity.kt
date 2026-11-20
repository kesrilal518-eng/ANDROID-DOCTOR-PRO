package com.androiddoctor.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.androiddoctor.pro.ui.screens.DashboardScreen
import com.androiddoctor.pro.ui.screens.HistoryScreen
import com.androiddoctor.pro.ui.screens.InteractiveTestsScreen
import com.androiddoctor.pro.ui.theme.AndroidDoctorProTheme
import com.androiddoctor.pro.ui.theme.DeepBlack
import com.androiddoctor.pro.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidDoctorProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepBlack
                ) {
                    AndroidDoctorAppNav()
                }
            }
        }
    }
}

@Composable
fun AndroidDoctorAppNav() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateHistory = { navController.navigate("history") },
                onNavigateInteractive = { navController.navigate("interactive") }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("interactive") {
            InteractiveTestsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
