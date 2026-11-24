package com.example.sirkulacsr.trade

import java.text.NumberFormat
import java.util.Locale

/*
 * Logika Dampak Lingkungan & Laporan CSR (PIC 3).
 *
 * Sumber data: TradeSnapshot milik TradeStore (listing + offer). Hanya transaksi berstatus
 * OfferStatus.COMPLETE ("Selesai") yang dihitung. Berat diambil dari WasteListing.weight (kg),
 * nilai dari TradeOffer.currentPrice (harga akhir kesepakatan), waktu dari TradeOffer.updatedAt.
 *
 * File ini sengaja bebas dari dependensi Android/Compose agar mudah diuji.
 */

internal enum class ImpactPeriod(val label: String, val days: Int, val caption: String) {
    WEEK("Minggu", 7, "7 hari terakhir"),
    MONTH("Bulan", 30, "30 hari terakhir"),
    YEAR("Tahun", 365, "365 hari terakhir")
}

/** Faktor emisi = kg CO2e dihemat per kg limbah didaur ulang. ASUMSI ESTIMASI, bukan angka resmi. */
internal enum class WasteKind(val label: String, val factor: Double) {
    KARDUS("Kardus & Kertas", 0.90),
    PLASTIK("Plastik PET", 1.50),
    MINYAK("Minyak Jelantah", 2.50),
    LOGAM("Logam", 4.00),
    LAINNYA("Lainnya", 0.50)
}

internal const val KG_CO2_PER_TREE_YEAR = 21.0
internal const val KG_CO2_PER_KM_CAR = 0.12
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

internal data class CompletedTrade(
    val listing: WasteListing,
    val offer: TradeOffer,
    val kind: WasteKind,
    val weightKg: Double,
    val valueRupiah: Long,
    val completedAt: Long
)

internal data class KindImpact(
    val kind: WasteKind,
    val weightKg: Double,
    val co2Kg: Double,
    val percent: Double
)

internal data class ImpactSummary(
    val trades: List<CompletedTrade>,
    val totalKg: Double,
    val totalCo2Kg: Double,
    val totalValue: Long,
    val kinds: List<KindImpact>
) {
    val count: Int get() = trades.size
    val trees: Double get() = totalCo2Kg / KG_CO2_PER_TREE_YEAR
    val carKm: Double get() = totalCo2Kg / KG_CO2_PER_KM_CAR
    val top: KindImpact? get() = kinds.firstOrNull()
}

private val petWord = Regex("""\bpet\b""")

/** Kategori pada listing berupa teks bebas, jadi dikelompokkan berdasarkan kata kunci. */
internal fun wasteKindOf(listing: WasteListing): WasteKind {
    val text = "${listing.category} ${listing.title}".lowercase(Locale.ROOT)
    fun has(vararg words: String) = words.any { text.contains(it) }
    return when {
        has("kardus", "kertas", "karton") -> WasteKind.KARDUS
        has("plastik", "botol") || petWord.containsMatchIn(text) -> WasteKind.PLASTIK
        has("minyak", "jelantah") -> WasteKind.MINYAK
        has("logam", "aluminium", "alumunium", "besi", "kaleng", "tembaga") -> WasteKind.LOGAM
        else -> WasteKind.LAINNYA
    }
}

/** Semua transaksi Selesai (tanpa filter periode). Listing tanpa berat valid dilewati. */
internal fun completedTrades(snapshot: TradeSnapshot): List<CompletedTrade> {
    val byId = snapshot.listings.associateBy(WasteListing::id)
    return snapshot.offers.values.mapNotNull { offer ->
        if (offer.status != OfferStatus.COMPLETE) return@mapNotNull null
        val listing = byId[offer.listingId] ?: return@mapNotNull null
        val kg = listing.weight.replace(',', '.').trim().toDoubleOrNull()
        if (kg == null || !kg.isFinite() || kg <= 0.0) return@mapNotNull null
        CompletedTrade(listing, offer, wasteKindOf(listing), kg, offer.currentPrice, offer.updatedAt)
    }.sortedByDescending(CompletedTrade::completedAt)
}

internal fun calculateImpact(trades: List<CompletedTrade>, period: ImpactPeriod, now: Long): ImpactSummary {
    val since = now - period.days * DAY_MILLIS
    val inPeriod = trades.filter { it.completedAt in since..now }
    val totalKg = inPeriod.sumOf(CompletedTrade::weightKg)
    val kinds = inPeriod.groupBy(CompletedTrade::kind).map { (kind, list) ->
        val kg = list.sumOf(CompletedTrade::weightKg)
        KindImpact(kind, kg, kg * kind.factor, if (totalKg > 0) kg / totalKg * 100 else 0.0)
    }.sortedByDescending(KindImpact::weightKg)
    return ImpactSummary(
        trades = inPeriod,
        totalKg = totalKg,
        totalCo2Kg = kinds.sumOf(KindImpact::co2Kg),
        totalValue = inPeriod.sumOf(CompletedTrade::valueRupiah),
        kinds = kinds
    )
}

private val idLocale: Locale = Locale.forLanguageTag("id-ID")

internal fun formatNumber(value: Double, maxFraction: Int = 1): String =
    NumberFormat.getNumberInstance(idLocale).apply {
        maximumFractionDigits = maxFraction
        minimumFractionDigits = 0
    }.format(value)

/** Di atas 1.000 kg ditampilkan dalam Ton. */
internal fun formatMass(kg: Double): String =
    if (kg >= 1000) "${formatNumber(kg / 1000, 2)} Ton" else "${formatNumber(kg, 1)} kg"

internal fun impactNarrative(summary: ImpactSummary, period: ImpactPeriod): String {
    val top = summary.top ?: return "Belum ada kontribusi pada periode ini. Selesaikan transaksi hingga berstatus " +
        "Selesai agar dampak lingkunganmu terhitung."
    return "Dalam ${period.caption}, ${summary.count} transaksi selesai dan ${formatMass(summary.totalKg)} limbah " +
        "berhasil didaur ulang, terbesar dari ${top.kind.label} (${formatNumber(top.percent)}%). " +
        "Upaya ini diperkirakan menghemat ${formatMass(summary.totalCo2Kg)} CO₂e, setara penyerapan karbon " +
        "${formatNumber(summary.trees)} pohon selama satu tahun, dengan nilai transaksi ${rupiah(summary.totalValue)}."
}
