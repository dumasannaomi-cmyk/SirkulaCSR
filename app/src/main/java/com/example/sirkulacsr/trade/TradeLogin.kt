package com.example.sirkulacsr.trade

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private const val MITRA_EMAIL = "mitra@sirkula.example"
private const val PENGHASIL_EMAIL = "penghasil@sirkula.example"
private const val DEMO_PASSWORD = "sirkula123"
private const val SESSION_ROLE = "signed_in_demo_role"

@Composable
internal fun SirkulaApp() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("sirkula_demo_session", Context.MODE_PRIVATE)
    }
    var signedInRole by remember(prefs) {
        val savedRole = prefs.getString(SESSION_ROLE, null)
        mutableStateOf(TradeRole.entries.firstOrNull { it.name == savedRole })
    }

    val role = signedInRole
    if (role == null) {
        TradeLogin { selectedRole ->
            prefs.edit().putString(SESSION_ROLE, selectedRole.name).apply()
            signedInRole = selectedRole
        }
    } else {
        TradeApp(role) {
            prefs.edit().remove(SESSION_ROLE).apply()
            signedInRole = null
        }
    }
}

@Composable
private fun TradeLogin(signIn: (TradeRole) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val emailRole = when (email.trim().lowercase()) {
        MITRA_EMAIL -> TradeRole.MITRA
        PENGHASIL_EMAIL -> TradeRole.PENGHASIL
        else -> null
    }

    fun submit() {
        if (emailRole != null && password == DEMO_PASSWORD) {
            focus.clearFocus()
            signIn(emailRole)
        } else {
            error = true
        }
    }

    fun updateEmail(value: String) {
        email = value.take(120)
        error = false
    }

    Column(
        Modifier.fillMaxSize().background(TradeBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TradeText("SIRKULA", 16, TradeGreen, weight = FontWeight.Bold)
            TradeTitle("Masuk ke akunmu", 28)
            TradeText("Gunakan akun demo untuk mencoba penawaran dan transaksi.", 14, TradeMuted)
        }
        TradeCard {
            OutlinedTextField(
                value = email,
                onValueChange = ::updateEmail,
                modifier = Modifier.fillMaxWidth(),
                label = { TradeText("Email") },
                singleLine = true,
                isError = error,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TradeGreen),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it.take(120)
                    error = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { TradeText("Kata sandi") },
                singleLine = true,
                isError = error,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TradeGreen),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        TradeText(if (showPassword) "Sembunyikan" else "Lihat", 12, GreenInk)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() })
            )
            if (error) TradeText("Email atau kata sandi demo tidak sesuai.", 13, TradeRed)
            Spacer(Modifier.height(2.dp))
            TradeButton("Masuk", TradeGreen, ::submit, Modifier.fillMaxWidth())
        }
        TradeCard {
            TradeTitle("Akun demo", 16)
            TradeText("Pilih peran untuk mengisi email. Kata sandi demo $DEMO_PASSWORD.", 13, TradeMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TradeButton("Mitra", TradeGreen, { updateEmail(MITRA_EMAIL) },
                    Modifier.weight(1f), outlined = true, ink = GreenInk)
                TradeButton("Penghasil", TradeGreen, { updateEmail(PENGHASIL_EMAIL) },
                    Modifier.weight(1f), outlined = true, ink = GreenInk)
            }
        }
    }
}
