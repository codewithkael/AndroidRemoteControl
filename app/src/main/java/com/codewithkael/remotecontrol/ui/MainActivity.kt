package com.codewithkael.remotecontrol.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codewithkael.remotecontrol.ui.theme.ProductionWebRTC
import com.codewithkael.remotecontrol.ui.screens.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.hasExtra("close_app")) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            ProductionWebRTC {
                MainScreen()
            }
        }
    }
}