package com.example.sirkulacsr

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.sirkulacsr.ui.onboarding.OnboardingScreen

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OnboardingScreen(
                onFinish = {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                    )
                    finish()
                },
                onBackFromFirst = {
                    finish()
                }
            )
        }
    }
}