package com.example.sirkulacsr.trade

import android.content.Context
import android.util.Patterns
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MITRA_EMAIL = "mitra@sirkula.example"
private const val PENGHASIL_EMAIL = "penghasil@sirkula.example"
private const val DEMO_PASSWORD = "sirkula123"
private const val SESSION_ROLE = "signed_in_demo_role"
private const val SESSION_EMAIL = "local_account_email"

private val demoMitra = LocalTradeAccount(MITRA_EMAIL, MITRA_NAME, TradeRole.MITRA)
private val demoPenghasil = LocalTradeAccount(PENGHASIL_EMAIL, PENGHASIL_NAME, TradeRole.PENGHASIL)
private val demoAccounts = listOf(demoMitra, demoPenghasil).associateBy(LocalTradeAccount::email)

@Composable
internal fun SirkulaApp() {
    val context = LocalContext.current
    val store = remember(context) { TradeStore.instance(context) }
    val prefs = remember(context) {
        context.getSharedPreferences("sirkula_demo_session", Context.MODE_PRIVATE)
    }
    var account by remember(prefs) { mutableStateOf<LocalTradeAccount?>(null) }
    var loading by remember(prefs) { mutableStateOf(true) }
    LaunchedEffect(prefs) {
        try {
            val email = prefs.getString(SESSION_EMAIL, null)
            account = if (email != null) demoAccounts[email] ?: withContext(Dispatchers.IO) { store.account(email) }
                else demoAccounts.values.firstOrNull { it.role.name == prefs.getString(SESSION_ROLE, null) }
        } finally {
            loading = false
        }
    }

    val active = account
    if (loading) {
        return
    } else if (active == null) {
        TradeLogin(store) { selected ->
            prefs.edit().remove(SESSION_ROLE).putString(SESSION_EMAIL, selected.email).apply()
            account = selected
        }
    } else {
        TradeApp(active, active.email in demoAccounts) {
            prefs.edit().remove(SESSION_ROLE).remove(SESSION_EMAIL).apply()
            account = null
        }
    }
}

@Composable
private fun TradeLogin(store: TradeStore, signIn: (LocalTradeAccount) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var registering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(TradeRole.MITRA) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    fun submit() {
        if (busy) return
        val address = email.trim().lowercase()
        val inputPassword = password
        val newAccount = LocalTradeAccount(address, name.trim(), role)
        val isRegistration = registering
        error = when {
            !Patterns.EMAIL_ADDRESS.matcher(address).matches() -> "Masukkan alamat email yang valid."
            inputPassword.length < 6 -> "Gunakan minimal 6 karakter untuk kata sandi."
            isRegistration && newAccount.name.isBlank() -> "Masukkan nama usaha."
            isRegistration && address in demoAccounts -> "Email ini dipakai untuk akun demo."
            else -> null
        }
        if (error != null) return
        busy = true
        focus.clearFocus()
        scope.launch {
            try {
                val selected = withContext(Dispatchers.IO) {
                    if (isRegistration) store.register(newAccount, inputPassword)
                    else if (address in demoAccounts) demoAccounts[address]?.takeIf { inputPassword == DEMO_PASSWORD }
                    else store.signIn(address, inputPassword)
                }
                if (selected != null) signIn(selected)
                else error = "Email atau kata sandi tidak sesuai. Akun lokal dibuat di HP ini."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (problem: IllegalArgumentException) {
                error = problem.message ?: "Periksa kembali data akun."
            } catch (_: Exception) {
                error = "Data akun lokal belum bisa dibuka. Coba lagi."
            } finally {
                busy = false
            }
        }
    }

    fun updateEmail(value: String) {
        email = value.take(120)
        error = null
    }

    Column(
        Modifier.fillMaxSize().background(TradeBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TradeText("SIRKULA", 16, TradeGreen, weight = FontWeight.Bold)
            TradeTitle(if (registering) "Daftar akun lokal" else "Masuk ke akunmu", 28)
            TradeText("Akun dan transaksi tersimpan di HP ini.", 14, TradeMuted)
        }
        TradeCard {
            if (registering) {
                TradeField("Nama usaha", name, { name = it.take(100) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TradeRole.entries.forEach { option ->
                        TradeButton(option.label, option.accent, { role = option }, Modifier.weight(1f),
                            outlined = role != option, enabled = !busy)
                    }
                }
            }
            OutlinedTextField(
                value = email,
                onValueChange = ::updateEmail,
                modifier = Modifier.fillMaxWidth(),
                label = { TradeText("Email") },
                singleLine = true,
                isError = error != null,
                enabled = !busy,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TradeGreen),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it.take(120)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { TradeText("Kata sandi") },
                singleLine = true,
                isError = error != null,
                enabled = !busy,
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
            error?.let { TradeText(it, 13, TradeRed) }
            Spacer(Modifier.height(2.dp))
            TradeButton(if (registering) "Daftar" else "Masuk", TradeGreen, ::submit,
                Modifier.fillMaxWidth(), enabled = !busy)
            TextButton(enabled = !busy, onClick = { registering = !registering; error = null }) {
                TradeText(if (registering) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar")
            }
        }
        if (!registering) TradeCard {
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
