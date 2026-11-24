package com.example.sirkulacsr.trade

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sirkulacsr.R

@Composable
internal fun TradeNavigation(role: TradeRole, screens: List<TradeScreen>, selected: TradeScreen, open: (TradeScreen) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White)) {
        screens.forEach { tab ->
            val icon = when (tab) {
                TradeScreen.HOME -> R.drawable.keiza_nav_home
                TradeScreen.MARKET -> R.drawable.keiza_nav_market
                TradeScreen.LISTINGS -> R.drawable.keiza_nav_list
                TradeScreen.TRANSACTIONS -> R.drawable.keiza_nav_buy
                TradeScreen.IMPACT -> R.drawable.keiza_nav_impact
                else -> R.drawable.keiza_nav_profile
            }
            val tint = if (selected == tab) role.accent else TradeMuted
            Column(Modifier.weight(1f).height(64.dp).clickable { open(tab) },
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally) {
                TradeIcon(icon, 22, tint)
                TradeText(tab.title(role), 10, tint)
            }
        }
    }
}

@Composable
internal fun TradeHome(
    role: TradeRole,
    company: String,
    profile: Boolean,
    switch: (() -> Unit)?,
    signOut: () -> Unit,
    navigate: (TradeScreen) -> Unit
) {
    if (!profile) {
        TradeTitle("SIRKULA", 24)
        TradeText("Jual limbah bernilai atau temukan bahan baku untuk usahamu.", 14, TradeMuted)
    }
    TradeCard {
        TradeTitle(company, 16)
        TradeText("Mode ${role.label}", 12, role.accentInk)
        if (switch != null) {
            TradeButton("Beralih ke ${role.other.label}", role.accent, switch, Modifier.fillMaxWidth(),
                outlined = true, ink = role.accentInk)
        }
    }
    if (!profile) {
        if (role == TradeRole.PENGHASIL) {
            TradeButton("Hitung Nilai Limbah", role.accent, { navigate(TradeScreen.ESTIMASI) },
                Modifier.fillMaxWidth())
        }
        val primary = if (role == TradeRole.MITRA) TradeScreen.MARKET else TradeScreen.INCOMING
        TradeButton(primary.title(role), role.accent, { navigate(primary) }, Modifier.fillMaxWidth())
        TradeCard(onClick = { navigate(TradeScreen.TRANSACTIONS) }) {
            TradeText(TradeScreen.TRANSACTIONS.title(role), weight = FontWeight.SemiBold)
        }
    }
    listOf(TradeScreen.MESSAGES, TradeScreen.HISTORY, TradeScreen.IMPACT).forEach {
        TradeCard(onClick = { navigate(it) }) { TradeText(it.title(role), weight = FontWeight.SemiBold) }
    }
    if (profile) TradeButton("Keluar", TradeRed, signOut, Modifier.fillMaxWidth(), outlined = true)
}

@Composable
internal fun BrowseListings(listings: List<WasteListing>, role: TradeRole, manage: Boolean,
                           open: (Long) -> Unit, navigate: (TradeScreen) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    if (manage) {
        TradeButton("Buat Listing Baru", role.accent, { navigate(TradeScreen.NEW_LISTING) }, Modifier.fillMaxWidth())
        TradeCard(onClick = { navigate(TradeScreen.INCOMING) }) { TradeTitle("Penawaran Masuk", 16) }
        TradeCard(onClick = { navigate(TradeScreen.TRANSACTIONS) }) { TradeTitle("Penawaran & Transaksi", 16) }
    }
    TradeField("Cari limbah", query, { query = it.take(100) }, "Cari nama atau kategori")
    val found = listings.filter { it.title.contains(query.trim(), true) || it.category.contains(query.trim(), true) }
    if (found.isEmpty()) TradeText("Listing tidak ditemukan.", 14, TradeMuted)
    found.forEach { listing ->
        TradeCard(onClick = { open(listing.id) }) { ListingSummary(listing) }
    }
}

internal data class ListingDraft(
    val title: String, val price: Long, val weight: String, val location: String,
    val category: String, val condition: String, val description: String
)

@Composable
internal fun NewListing(role: TradeRole, submit: (ListingDraft) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var condition by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    TradeCard {
        TradeField("Nama limbah", title, { title = it.take(80) })
        TradeField("Kategori", category, { category = it.take(60) })
        TradeField("Kondisi", condition, { condition = it.take(80) })
        TradeField("Berat (kg)", weight, { weight = it.replace(',', '.').take(10) }, keyboard = KeyboardType.Decimal)
        TradeField("Harga (Rp)", price, { price = it.filter { digit -> digit in '0'..'9' }.take(12) },
            keyboard = KeyboardType.Number)
        TradeField("Lokasi", location, { location = it.take(80) })
        TradeField("Deskripsi (Opsional)", description, { description = it.take(1_000) }, minLines = 3)
    }
    TradeButton("Terbitkan Listing", role.accent, {
        submit(ListingDraft(title, price.toLongOrNull() ?: 0, weight, location, category, condition, description))
    }, Modifier.fillMaxWidth())
}

@Composable
internal fun ImpactPage(modifier: Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/dampak_csr.html")
            }
        },
        modifier = modifier.fillMaxWidth(),
        onRelease = { it.destroy() }
    )
}
