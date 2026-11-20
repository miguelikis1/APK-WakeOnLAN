package com.wol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.wol.app.ui.screens.MainScreen
import com.wol.app.ui.theme.WakeOnLANTheme
import com.wol.app.ui.viewmodel.MainViewModel
import com.wol.app.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as WolApplication
        MainViewModelFactory(app.settingsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WakeOnLANTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
