package com.avabuilds.packetrunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.avabuilds.packetrunner.ui.theme.PacketRunnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PacketRunnerTheme {
                // The Scaffold paints the status and navigation bar areas, so it must follow the
                // current screen's background or a differently colored strip shows at the edges.
                var background by remember { mutableStateOf(PR.Bg) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = background,
                ) { innerPadding ->
                    PacketRunnerApp(
                        modifier = Modifier.padding(innerPadding),
                        onBackgroundChange = { background = it },
                    )
                }
            }
        }
    }
}
