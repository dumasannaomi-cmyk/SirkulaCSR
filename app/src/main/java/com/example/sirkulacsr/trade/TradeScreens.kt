package com.example.sirkulacsr.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sirkulacsr.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val OFFLINE_PAYMENT =
    "Pembayaran dan serah terima limbah disepakati langsung oleh kedua pihak di luar aplikasi SIRKULA."

@Composable
internal fun ListingDetail(listing: WasteListing) {
    TradePhoto(listing, Modifier.fillMaxWidth().aspectRatio(350f / 210f), hero = true)
    TradeCard {
        TradeTitle(listing.title)
        TradeText("${rupiah(listing.price)} /total", 18, TradeGreen, FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            TradeIcon(R.drawable.keiza_icon_pack, 14, TradeMuted)
            TradeText("${listing.weight} kg", 12, TradeMuted)
            Spacer(Modifier.width(4.dp))
            TradeIcon(R.drawable.keiza_icon_pin, 14, TradeMuted)
            TradeText(listing.location, 12, TradeMuted)
        }
        HorizontalDivider(color = TradeLine)
        TradeText("GC   ${listing.company}", weight = FontWeight.SemiBold)
        TradeText("Penghasil Limbah", 12, TradeMuted)
        HorizontalDivider(color = TradeLine)
        TradeText("Informasi Limbah", weight = FontWeight.SemiBold)
        Column {
            TradeFact("Kategori", listing.category)
            TradeFact("Kondisi", listing.condition)
        }
        if (listing.description.isNotBlank()) {
            TradeText("Deskripsi", weight = FontWeight.SemiBold)
            TradeText(listing.description, 12, TradeMuted)
        }
    }
}

@Composable
internal fun OfferForm(
    listing: WasteListing, existing: TradeOffer?, amount: String, changeAmount: (String) -> Unit,
    message: String, changeMessage: (String) -> Unit
) {
    val isCounter = existing != null
    TradeCard {
        ListingSummary(listing, subtitle = if (isCounter) "${listing.weight} kg · ${listing.location}"
        else "${listing.company} · ${listing.weight} kg")
    }
    TradeCard {
        TradeText(if (isCounter) "Ajukan Tawaran Balik" else "Isi Penawaran", weight = FontWeight.SemiBold)
        if (existing != null) {
            TradeText("Kamu bisa menawarkan harga lain kepada mitra.", 12, TradeMuted)
            Column {
                TradeFact("Harga Listing", rupiah(listing.price))
                TradeFact("Penawaran Mitra", rupiah(existing.price))
            }
        }
        TradeField(
            if (isCounter) "Harga Tawaran Balik" else "Harga yang Ditawarkan",
            amount,
            { changeAmount(it.filter { char -> char in '0'..'9' }.take(12)) },
            hint = "0", keyboard = KeyboardType.Number, prefix = "Rp"
        )
        TradeField("Pesan (Opsional)", message, { changeMessage(it.take(1_000)) },
            hint = "Tulis pesan...", minLines = 3)
    }
    TradeNote(if (isCounter) "Tawaran balik akan dikirim kepada mitra untuk ditanggapi." else OFFLINE_PAYMENT)
}

@Composable
internal fun TransactionList(
    listings: List<WasteListing>, snapshot: TradeSnapshot, role: TradeRole, screen: TradeScreen,
    onOpen: (Long) -> Unit, onNew: () -> Unit
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val title = screen.title(role)
    val filteredListings = listings.filter { listing ->
        val status = snapshot.offers[listing.id]?.status ?: return@filter false
        when (filter) {
            1 -> status == OfferStatus.PENDING || status == OfferStatus.COUNTERED
            2 -> status == OfferStatus.AGREED || status == OfferStatus.CONFIRMING
            3 -> status == OfferStatus.COMPLETE
            else -> true
        }
    }.sortedByDescending { snapshot.offers.getValue(it.id).updatedAt }
    TradeTitle(title)
    TradeText(
        when (screen) {
            TradeScreen.INCOMING -> if (snapshot.offers.isEmpty()) "Semua penawaran mitra akan muncul di sini."
                else "Lihat penawaran mitra untuk listing limbahmu."
            TradeScreen.HISTORY -> "Pantau penawaran dan transaksi yang tersimpan."
            else -> if (role == TradeRole.MITRA) "Pantau penawaran dan transaksi limbahmu."
                else "Pantau penawaran pada listing limbahmu."
        }, 12, TradeMuted
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("Semua", "Menunggu", "Disepakati", "Selesai").forEachIndexed { index, label ->
            val selected = filter == index
            Box(Modifier.heightIn(min = 34.dp).clip(CircleShape)
                .background(if (selected) role.accent else Color.White)
                .border(1.dp, if (selected) role.accent else TradeLine, CircleShape)
                .clickable { filter = index }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                TradeText(label, 10, if (selected) Color.White else TradeMuted)
            }
        }
    }
    if (filteredListings.isEmpty()) {
        TradeCard {
            TradeStatusHero(R.drawable.keiza_inbox, 36, TradeGreen, Mint,
                if (filter == 0 && screen == TradeScreen.INCOMING) "Belum Ada Penawaran" else "Belum Ada Transaksi",
                if (filter == 0 && screen == TradeScreen.INCOMING)
                    "Saat ini belum ada penawaran untuk listing limbahmu. Buat listing agar mitra bisa menemukannya."
                else "Penawaran dan transaksi yang sesuai akan muncul di sini.")
            if (filter == 0 && screen == TradeScreen.INCOMING) {
                TradeButton("Buat Listing Baru", role.accent, onNew, Modifier.fillMaxWidth())
            }
        }
    }
    filteredListings.forEach { listing ->
        val offer = snapshot.offers.getValue(listing.id)
        val person = if (role == TradeRole.MITRA) listing.company else offer.buyerName
        TradeCard(onClick = { onOpen(listing.id) }) {
            ListingSummary(listing, rupiah(offer.currentPrice), "$person · ${shortDate(offer.updatedAt)}", offer.status)
        }
    }
}

@Composable
internal fun OfferDetail(
    listing: WasteListing, offer: TradeOffer, message: TradeMessage?, role: TradeRole
) {
    StatusBadge(offer.status, if (offer.status == OfferStatus.PENDING) "Menunggu Tanggapan" else offer.status.label)
    TradeCard {
        val fromBuyer = offer.status != OfferStatus.COUNTERED
        TradeText(if (fromBuyer) "Penawaran dari Mitra" else "Tawaran Balik dari Penghasil",
            weight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(if (fromBuyer) "MD" else "GC")
            Column {
                TradeText(if (fromBuyer) offer.buyerName else listing.company, weight = FontWeight.SemiBold)
                TradeText(
                    (if (fromBuyer) "Pengepul/Mitra" else "Penghasil Limbah") + " · " + shortDate(offer.updatedAt),
                    12, TradeMuted
                )
            }
        }
    }
    TradeCard { ListingSummary(listing) }
    TradeCard {
        TradeText(if (offer.status == OfferStatus.COUNTERED) "Harga Tawaran Balik" else "Harga yang Ditawarkan",
            12, TradeMuted)
        TradeText(rupiah(offer.currentPrice), 24, TradeGreen, FontWeight.Bold)
        message?.text?.takeIf(String::isNotBlank)?.let { TradeText(it) }
    }
    if (role == TradeRole.MITRA && offer.status == OfferStatus.COUNTERED) TradeNote(OFFLINE_PAYMENT)
}

@Composable
internal fun Agreement(listing: WasteListing, offer: TradeOffer) {
    TradeNote("Transaksi Disepakati. Harga akhir telah disepakati oleh kedua pihak.")
    TradeCard {
        ListingSummary(listing)
        Column {
            TradeFact("Harga Listing", rupiah(listing.price))
            TradeFact("Harga Akhir", rupiah(offer.currentPrice))
            TradeFact("Lokasi", listing.location)
        }
    }
    TradeCard {
        TradeText("Pihak yang Terlibat", weight = FontWeight.SemiBold)
        TradeText("GC   ${listing.company}", 12, weight = FontWeight.SemiBold)
        TradeText("         Penghasil Limbah", 12, TradeMuted)
        HorizontalDivider(color = TradeLine)
        TradeText("MD   ${offer.buyerName}", 12, weight = FontWeight.SemiBold)
        TradeText("         Pengepul/Mitra", 12, TradeMuted)
    }
    TradeNote("Pembayaran dan penyerahan limbah dilakukan sesuai kesepakatan langsung melalui chat. SIRKULA tidak memproses pembayaran.")
}

@Composable
internal fun TradeStatus(
    screen: TradeScreen, listing: WasteListing, offer: TradeOffer, role: TradeRole, reload: () -> Unit
) {
    when (screen) {
        TradeScreen.SENT -> TradeStatusHero(R.drawable.keiza_success, 38, Color.White, Color(0xFF12B268),
            "Penawaran Berhasil Dikirim", "Penawaranmu sudah dikirim ke ${listing.company}. " +
                "Cek tanggapan di Pembelian.")
        TradeScreen.ACCEPTED -> TradeStatusHero(R.drawable.keiza_success, 38, Color.White, Color(0xFF12B268),
            "Penawaran Diterima", "Kamu menerima penawaran awal mitra tanpa tawar balik.")
        TradeScreen.CONFIRM -> TradeStatusHero(R.drawable.keiza_confirmation, 34, TradeGreen, Mint,
            "Konfirmasi Transaksi Selesai?",
            "Lakukan konfirmasi hanya jika pembayaran dan serah terima limbah sudah benar-benar selesai.")
        TradeScreen.WAITING -> TradeStatusHero(R.drawable.keiza_clock, 36, Color(0xFFF78D00), PaleGold,
            "Menunggu Konfirmasi", if (offer.confirmed(role)) "Konfirmasi kamu sudah tercatat. " +
                "Menunggu konfirmasi dari ${if (role == TradeRole.MITRA) listing.company else offer.buyerName}."
            else "Pihak lain sudah mengonfirmasi. Selesaikan konfirmasi untuk menutup transaksi.")
        TradeScreen.COMPLETE -> TradeStatusHero(R.drawable.keiza_success, 38, Color.White, Color(0xFF12B268),
            "Transaksi Selesai!", "Penyelesaian transaksi telah dikonfirmasi oleh kedua pihak.")
        TradeScreen.REJECTED -> TradeStatusHero(R.drawable.keiza_rejected, 30, Color.White, Color(0xFFF53E36),
            "Penawaran Ditolak", "Penawaran ${rupiah(offer.currentPrice)} telah ditolak.")
        else -> return
    }
    TradeCard {
        ListingSummary(listing, rupiah(offer.currentPrice))
        Column {
            when (screen) {
                TradeScreen.SENT -> TradeFact("Status", "Menunggu Tanggapan")
                TradeScreen.ACCEPTED -> {
                    TradeFact("Harga Listing", rupiah(listing.price))
                    TradeFact("Harga Disepakati", rupiah(offer.currentPrice))
                    TradeFact("Status", "Disepakati")
                }
                TradeScreen.WAITING -> {
                    TradeRole.entries.forEach {
                        val status = if (offer.confirmed(it)) "Terkonfirmasi" else "Menunggu"
                        TradeFact("Konfirmasi ${it.label}", status, if (offer.confirmed(it)) TradeGreen else GoldInk)
                    }
                }
                TradeScreen.COMPLETE -> {
                    TradeFact("Harga Akhir", rupiah(offer.currentPrice))
                    TradeFact("Penghasil", listing.company)
                    TradeFact("Mitra", offer.buyerName)
                    TradeFact("Tanggal", dateFormat(offer.updatedAt, "d MMMM yyyy"))
                }
                TradeScreen.REJECTED -> TradeFact("Status", "Ditolak")
                else -> Unit
            }
        }
    }
    when (screen) {
        TradeScreen.ACCEPTED -> TradeNote(OFFLINE_PAYMENT)
        TradeScreen.CONFIRM -> TradeNote("Sistem akan meminta konfirmasi dari pihak lain sebelum status berubah menjadi Selesai.")
        TradeScreen.WAITING -> TradeNote("Status akan berubah menjadi Selesai setelah kedua pihak mengonfirmasi.") {
            TradeText("Lihat pembaruan konfirmasi", 14, role.accentInk, modifier = Modifier.clickable(onClick = reload))
        }
        TradeScreen.COMPLETE -> TradeNote("Transaksi ini tersimpan di Riwayat Transaksi.")
        TradeScreen.REJECTED -> TradeNote("Penawaran ini telah ditutup. Kamu bisa mencari listing limbah lain di Marketplace.")
        else -> Unit
    }
}

@Composable
internal fun TradeChat(
    listing: WasteListing, offer: TradeOffer?, messages: List<TradeMessage>, role: TradeRole,
    onAgreement: () -> Unit, onOffer: () -> Unit
) {
    TradeText(if (messages.lastOrNull()?.let { shortDate(it.sentAt) != shortDate(System.currentTimeMillis()) } == true)
        shortDate(messages.last().sentAt) else "Hari ini", 10, TradeMuted,
        modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
    TradeCard { ListingSummary(listing, "Harga listing ${rupiah(listing.price)}") }
    messages.forEach { message ->
        val mine = message.author == role
        val shape = RoundedCornerShape(12.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
            Column(Modifier.fillMaxWidth(0.79f).clip(shape).background(
                if (!mine) Color.White else if (role == TradeRole.MITRA) PaleGold else Color(0xFFCEF0DE)
            ).border(1.dp, if (mine) Color.Transparent else TradeLine, shape).padding(12.dp)) {
                if (message.price != null) {
                    val label = if (message.author == TradeRole.MITRA) "Penawaran" else "Tawaran Balik"
                    TradeText("$label • ${rupiah(message.price)}", 12,
                        if (message.author == TradeRole.MITRA) GoldInk else GreenInk)
                    if (message.text.isNotBlank()) Spacer(Modifier.height(6.dp))
                }
                if (message.text.isNotBlank()) TradeText(message.text)
                Spacer(Modifier.height(5.dp))
                TradeText(dateFormat(message.sentAt, "HH.mm"), 10, TradeMuted, modifier = Modifier.align(Alignment.End))
            }
        }
    }
    when (offer?.status) {
        OfferStatus.AGREED, OfferStatus.CONFIRMING, OfferStatus.COMPLETE -> TradeCard(onClick = onAgreement) {
            TradeText("✓ Penawaran Disepakati", 14, GreenInk, FontWeight.SemiBold)
            TradeText("Harga akhir ${rupiah(offer.currentPrice)}", 16, TradeGreen, FontWeight.Bold)
            TradeText("Lihat Kesepakatan ›", 12, role.accentInk, FontWeight.Medium)
        }
        OfferStatus.PENDING, OfferStatus.COUNTERED -> TradeCard(onClick = onOffer) {
            TradeText("Lihat Penawaran ›", 12, role.accentInk, FontWeight.Medium)
        }
        else -> Unit
    }
}

@Composable
internal fun ChatComposer(role: TradeRole, enabled: Boolean, onSend: (String, () -> Unit) -> Unit) {
    var message by rememberSaveable { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(message, { message = it.take(1_000) }, Modifier.weight(1f), enabled = enabled,
            placeholder = { TradeText("Tulis pesan...", 12, TradeMuted) }, maxLines = 3, shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TradeBackground, unfocusedContainerColor = TradeBackground,
                focusedBorderColor = role.accent, unfocusedBorderColor = TradeLine
            ))
        TradeButton("Kirim", role.accent, {
            onSend(message) { message = "" }
        }, enabled = enabled && message.isNotBlank())
    }
}

@Composable
internal fun ConversationList(snapshot: TradeSnapshot, role: TradeRole, onChat: (Long) -> Unit) {
    TradeTitle("Percakapan")
    TradeText("Lanjutkan obrolan tentang penawaran limbahmu.", 12, TradeMuted)
    val latest = snapshot.messages.groupBy(TradeMessage::listingId).values.map { it.last() }
        .sortedByDescending(TradeMessage::sentAt)
    if (latest.isEmpty()) TradeCard { TradeText("Belum ada percakapan.", 14, TradeMuted) }
    latest.forEach { message ->
        val listing = snapshot.listings.firstOrNull { it.id == message.listingId } ?: return@forEach
        TradeCard(onClick = { onChat(listing.id) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(if (role == TradeRole.MITRA) "GC" else "MD")
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    TradeText(if (role == TradeRole.MITRA) listing.company
                        else snapshot.offers[listing.id]?.buyerName ?: MITRA_NAME, weight = FontWeight.SemiBold)
                    TradeText("${if (role == TradeRole.MITRA) "Penghasil Limbah" else "Pengepul/Mitra"} · " +
                        "${listing.title}, ${listing.weight} kg", 10, TradeMuted)
                }
            }
            HorizontalDivider(color = TradeLine)
            TradeText((if (message.author == role) "Kamu: " else "") +
                message.text.ifBlank { message.price?.let(::rupiah).orEmpty() }, 12, TradeMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                val status = snapshot.offers[listing.id]?.status
                if (status != null) StatusBadge(status) else Spacer(Modifier.width(1.dp))
                TradeText(dateFormat(message.sentAt, "HH.mm"), 10, TradeMuted)
            }
        }
    }
}

@Composable
internal fun TradeConfirmDialog(title: String, text: String, role: TradeRole, action: String,
                                cancel: () -> Unit, confirm: () -> Unit) {
    Dialog(onDismissRequest = cancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(12.dp)
        Column(Modifier.padding(horizontal = 20.dp).widthIn(max = 350.dp).fillMaxWidth()
            .clip(shape).background(Color.White).border(1.dp, TradeLine, shape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TradeText(title, 18, weight = FontWeight.SemiBold)
            TradeText(text, 14, TradeMuted)
            TradeButton(action, role.accent, confirm, Modifier.fillMaxWidth())
            TradeButton("Batal", TradeLine, cancel, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                outlined = true, ink = TradeInk)
        }
    }
}

private fun shortDate(date: Long) = dateFormat(date, "d MMM")

private fun dateFormat(date: Long, pattern: String) =
    SimpleDateFormat(pattern, Locale.forLanguageTag("id-ID")).format(Date(date))
