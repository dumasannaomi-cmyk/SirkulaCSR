package com.example.sirkulacsr

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sirkulacsr.trade.TradeApp
import com.example.sirkulacsr.ui.theme.SirkulaCSRTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemBars = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE)
        enableEdgeToEdge(statusBarStyle = systemBars, navigationBarStyle = systemBars)
        setContent {
            SirkulaCSRTheme(darkTheme = false, dynamicColor = false) { TradeApp() }
        }
    }
}
