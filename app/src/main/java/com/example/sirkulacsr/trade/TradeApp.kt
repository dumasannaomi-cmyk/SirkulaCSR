package com.example.sirkulacsr.trade

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class TradeScreen(private val heading: String) {
    HOME("Beranda"), MARKET("Marketplace"), LISTINGS("Listing"),
    NEW_LISTING("Buat Listing Baru"), LISTING("Detail Listing"), OFFER("Ajukan Penawaran"),
    SENT("Penawaran Terkirim"), INCOMING("Penawaran Masuk"), DETAIL("Detail Penawaran"),
    COUNTER("Tawar Balik"), CHAT("Chat"), MESSAGES("Pesan"), AGREEMENT("Detail Kesepakatan"),
    TRANSACTIONS("Pembelian"), HISTORY("Riwayat Transaksi"), ACCEPTED("Penawaran Diterima"),
    CONFIRM("Konfirmasi Transaksi"), WAITING("Status Transaksi"), COMPLETE("Transaksi Selesai"),
    REJECTED("Status Penawaran"), IMPACT("Dampak"), PROFILE("Profil");

    fun title(role: TradeRole) = if (this == TRANSACTIONS && role == TradeRole.PENGHASIL)
        "Penawaran & Transaksi" else heading
}

private fun destination(offer: TradeOffer, role: TradeRole) = when (offer.status) {
    OfferStatus.PENDING, OfferStatus.COUNTERED -> TradeScreen.DETAIL
    OfferStatus.AGREED -> TradeScreen.AGREEMENT
    OfferStatus.CONFIRMING -> if (offer.confirmed(role)) TradeScreen.WAITING else TradeScreen.AGREEMENT
    OfferStatus.COMPLETE -> TradeScreen.COMPLETE
    OfferStatus.REJECTED -> TradeScreen.REJECTED
}

@Composable
internal fun TradeApp(initialRole: TradeRole, signOut: () -> Unit) {
    val store = TradeStore.instance(LocalContext.current)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val stateHolder = rememberSaveableStateHolder()
    var role by rememberSaveable { mutableStateOf(initialRole) }
    var stack by rememberSaveable { mutableStateOf(arrayListOf("HOME:0")) }
    var snapshot by remember { mutableStateOf<TradeSnapshot?>(null) }
    var failedLoad by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val entry = stack.last()
    val screen = TradeScreen.valueOf(entry.substringBefore(':'))
    val id = entry.substringAfter(':').toLong()
    val data = snapshot
    val listing = data?.listings?.firstOrNull { it.id == id }
    val offer = data?.offers?.get(id)
    val messages = data?.messages.orEmpty().filter { it.listingId == id }

    fun go(next: TradeScreen, nextId: Long = 0, root: Boolean = false, replace: Boolean = false) {
        val route = "${next.name}:$nextId"
        if (busy || entry == route) return
        if (root) stack.drop(1).forEach(stateHolder::removeState)
        if (replace) stateHolder.removeState(entry)
        stack = when {
            root && next == TradeScreen.HOME -> arrayListOf("HOME:0")
            root -> arrayListOf("HOME:0", route)
            replace -> ArrayList(stack.dropLast(1) + route)
            else -> ArrayList(stack + route)
        }
    }

    fun back() {
        if (stack.size > 1 && !busy) {
            stateHolder.removeState(entry)
            stack = ArrayList(stack.dropLast(1))
        }
    }

    fun perform(write: () -> Unit = {}, after: (TradeSnapshot) -> Unit = {}) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val latest = withContext(Dispatchers.IO) {
                    write()
                    store.snapshot()
                }
                snapshot = latest
                failedLoad = false
                busy = false
                after(latest)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (problem: Exception) {
                failedLoad = snapshot == null
                launch {
                    snackbar.showSnackbar(if (problem is IllegalArgumentException)
                        problem.message ?: "Periksa data yang kamu masukkan."
                    else "Data belum dapat dimuat. Silakan coba lagi.")
                }
            } finally {
                busy = false
            }
        }
    }

    fun switchRole() {
        if (busy) return
        role = role.other
        stack.forEach(stateHolder::removeState)
        stack = arrayListOf("HOME:0")
    }

    LaunchedEffect(Unit) { perform() }
    BackHandler(stack.size > 1, ::back)
    val navItems = if (role == TradeRole.MITRA)
        listOf(TradeScreen.HOME, TradeScreen.MARKET, TradeScreen.TRANSACTIONS, TradeScreen.PROFILE)
    else listOf(TradeScreen.HOME, TradeScreen.LISTINGS, TradeScreen.IMPACT, TradeScreen.PROFILE)
    val listPages = setOf(TradeScreen.INCOMING, TradeScreen.TRANSACTIONS, TradeScreen.HISTORY)
    val statusPages = setOf(TradeScreen.SENT, TradeScreen.ACCEPTED, TradeScreen.CONFIRM, TradeScreen.WAITING,
        TradeScreen.COMPLETE, TradeScreen.REJECTED)
    val openChat = { go(TradeScreen.CHAT, id) }
    val openAgreement = { go(TradeScreen.AGREEMENT, id) }
    val openOffer = { offerId: Long ->
        data?.offers?.get(offerId)?.let { go(destination(it, role), offerId) }
        Unit
    }

    Box(Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        stateHolder.SaveableStateProvider(entry) {
            var dialog by rememberSaveable { mutableStateOf("") }
            var amount by rememberSaveable { mutableStateOf("") }
            var message by rememberSaveable { mutableStateOf("") }
            val scroll = rememberScrollState()
            val otherCompany = if (role == TradeRole.MITRA) listing?.company.orEmpty() else MITRA_NAME

            LaunchedEffect(screen, messages.size, scroll.maxValue) {
                if (screen == TradeScreen.CHAT && messages.isNotEmpty()) scroll.scrollTo(scroll.maxValue)
            }

            Column(Modifier.fillMaxSize()) {
                TradeHeader(if (screen == TradeScreen.CHAT) otherCompany else screen.title(role),
                    role, if (stack.size > 1) ::back else null,
                    if (screen == TradeScreen.CHAT) if (role == TradeRole.MITRA) "GC" else "MD" else null)
                if (screen == TradeScreen.IMPACT) {
                    ImpactPage(Modifier.weight(1f))
                } else {
                    Column(Modifier.weight(1f).fillMaxWidth().background(TradeBackground)
                        .verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            data == null -> if (failedLoad) TradeButton("Coba Lagi", role.accent, { perform() })
                                else CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = role.accent)
                            screen == TradeScreen.HOME || screen == TradeScreen.PROFILE ->
                                TradeHome(role, screen == TradeScreen.PROFILE, ::switchRole, signOut) { go(it, root = true) }
                            screen == TradeScreen.MARKET || screen == TradeScreen.LISTINGS -> BrowseListings(
                                data.listings, role, screen == TradeScreen.LISTINGS, { go(TradeScreen.LISTING, it) }, { go(it) }
                            )
                            screen == TradeScreen.NEW_LISTING -> NewListing(role) { draft ->
                                perform({
                                    store.createListing(role, draft.title, draft.price, draft.weight, draft.location,
                                        draft.category, draft.condition, draft.description)
                                }) { go(TradeScreen.LISTINGS, root = true) }
                            }
                            screen in listPages -> TransactionList(data.listings, data, role, screen, openOffer) {
                                go(TradeScreen.NEW_LISTING)
                            }
                            screen == TradeScreen.MESSAGES -> ConversationList(data, role) { go(TradeScreen.CHAT, it) }
                            listing == null -> TradeText("Data tidak tersedia.", 14, TradeMuted)
                            screen == TradeScreen.LISTING -> ListingDetail(listing)
                            screen == TradeScreen.OFFER && role == TradeRole.MITRA ->
                                OfferForm(listing, null, amount, { amount = it }, message, { message = it })
                            screen == TradeScreen.COUNTER && role == TradeRole.PENGHASIL &&
                                offer?.status == OfferStatus.PENDING ->
                                OfferForm(listing, offer, amount, { amount = it }, message, { message = it })
                            screen == TradeScreen.OFFER || screen == TradeScreen.COUNTER ->
                                TradeText("Formulir ini sudah tidak berlaku. Kembali untuk melihat status terbaru.", 14, TradeMuted)
                            screen == TradeScreen.CHAT -> TradeChat(listing, offer, messages, role, openAgreement) { openOffer(id) }
                            offer == null -> TradeText("Penawaran tidak tersedia.", 14, TradeMuted)
                            screen == TradeScreen.DETAIL -> OfferDetail(listing, offer, messages.lastOrNull { it.price != null }, role)
                            screen == TradeScreen.AGREEMENT -> Agreement(listing, offer)
                            screen in statusPages -> TradeStatus(screen, listing, offer, role) {
                                perform(after = { latest -> latest.offers[id]?.let { go(destination(it, role), id, replace = true) } })
                            }
                            else -> Unit
                        }
                    }
                }

                if (screen in navItems || screen in listPages) {
                    val tab = if (screen in listPages) if (role == TradeRole.MITRA) TradeScreen.TRANSACTIONS
                        else TradeScreen.LISTINGS else screen
                    TradeNavigation(role, navItems, tab) { go(it, root = true) }
                }
                if (listing != null) when (screen) {
                    TradeScreen.LISTING -> if (role == TradeRole.MITRA) {
                        TradeActions(role, if (offer == null) "Ajukan Penawaran" else "Lihat Penawaran", {
                            if (offer == null) go(TradeScreen.OFFER, id) else openOffer(id)
                        }, "Chat Penghasil", openChat)
                    }
                    TradeScreen.OFFER -> if (role == TradeRole.MITRA) {
                        val price = amount.toLongOrNull()
                        TradeFooter {
                            TradeButton("Kirim Penawaran", role.accent, {
                                if (price != null) {
                                    var created = false
                                    perform({ created = store.submit(role, listing.id, price, message) }) { latest ->
                                        latest.offers[listing.id]?.let { saved ->
                                            val next = if (created) TradeScreen.SENT else destination(saved, role)
                                            go(next, listing.id, replace = true)
                                        }
                                    }
                                }
                            }, Modifier.fillMaxWidth(), enabled = !busy && price != null && price > 0)
                        }
                    }
                    TradeScreen.COUNTER -> if (role == TradeRole.PENGHASIL && offer?.status == OfferStatus.PENDING) {
                        val price = amount.toLongOrNull()
                        TradeFooter {
                            TradeButton("Kirim Tawaran Balik", role.accent, {
                                if (price != null) perform({ store.counter(role, offer.listingId, price, message) }) {
                                    go(TradeScreen.CHAT, listing.id, replace = true)
                                }
                            }, Modifier.fillMaxWidth(), enabled = !busy && price != null && price > 0)
                        }
                    }
                    TradeScreen.CHAT -> ChatComposer(role, offer?.status != OfferStatus.REJECTED && !busy) { draft, clear ->
                        perform({ store.send(role, id, draft) }) { clear() }
                    }
                    TradeScreen.DETAIL -> if (offer != null) {
                        TradeFooter {
                            if (offer.canRespond(role)) {
                                TradeButton("Terima Penawaran", role.accent, {
                                    perform({ store.respond(role, id, accept = true) }) {
                                        go(if (role == TradeRole.PENGHASIL) TradeScreen.ACCEPTED else TradeScreen.AGREEMENT,
                                            id, replace = true)
                                    }
                                }, Modifier.fillMaxWidth(), enabled = !busy)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (role == TradeRole.PENGHASIL) TradeButton("Tawar Balik", role.accent,
                                        { go(TradeScreen.COUNTER, id) }, Modifier.weight(1f), outlined = true)
                                    TradeButton("Tolak", TradeRed, { dialog = "reject" }, Modifier.weight(1f), outlined = true)
                                }
                            }
                            TextButton(onClick = openChat, modifier = Modifier.fillMaxWidth()) {
                                TradeText("Lihat Chat", 12, role.accentInk, FontWeight.SemiBold)
                            }
                        }
                    }
                    TradeScreen.AGREEMENT -> if (offer != null) {
                        val next = when {
                            offer.status == OfferStatus.COMPLETE -> TradeScreen.COMPLETE
                            offer.confirmed(role) -> TradeScreen.WAITING
                            else -> TradeScreen.CONFIRM
                        }
                        TradeActions(role, if (next == TradeScreen.CONFIRM) "Konfirmasi Selesai" else next.title(role),
                            { go(next, id) }, "Buka Chat", openChat)
                    }
                    TradeScreen.SENT -> TradeActions(role, "Lihat Penawaran", { go(TradeScreen.TRANSACTIONS, root = true) })
                    TradeScreen.ACCEPTED -> TradeActions(role, "Lihat Kesepakatan", openAgreement)
                    TradeScreen.CONFIRM -> TradeActions(role, "Konfirmasi Selesai", { dialog = "confirm" }, "Batal", ::back)
                    TradeScreen.WAITING -> TradeActions(role, "Buka Chat", openChat, "Lihat Detail", openAgreement)
                    TradeScreen.COMPLETE -> TradeActions(role, "Lihat Riwayat Transaksi", { go(TradeScreen.HISTORY, root = true) })
                    TradeScreen.REJECTED -> TradeActions(role,
                        if (role == TradeRole.MITRA) "Kembali ke Marketplace" else "Lihat Penawaran Masuk", {
                            go(if (role == TradeRole.MITRA) TradeScreen.MARKET else TradeScreen.INCOMING, root = true)
                        })
                    else -> Unit
                }
            }
            if (dialog == "confirm") TradeConfirmDialog("Konfirmasi Penyelesaian?",
                "Pastikan pembayaran dan serah terima limbah sudah selesai sesuai kesepakatan kedua pihak.",
                role, "Ya, Konfirmasi", { dialog = "" }) {
                dialog = ""
                perform({ store.confirm(role, id) }) { latest ->
                    latest.offers[id]?.let { go(destination(it, role), id, replace = true) }
                }
            }
            if (dialog == "reject") TradeConfirmDialog("Tolak penawaran?", "Penawaran ini akan ditutup.",
                role, "Tolak Penawaran", { dialog = "" }) {
                dialog = ""
                perform({ store.respond(role, id, accept = false) }) { go(TradeScreen.REJECTED, id, replace = true) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp))
    }
}
