package com.avabuilds.packetrunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.avabuilds.packetrunner.ui.GameScreen
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.theme.PacketRunnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PacketRunnerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PR.Bg,
                ) { innerPadding ->
                    GameScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
