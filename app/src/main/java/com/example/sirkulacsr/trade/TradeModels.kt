package com.example.sirkulacsr.trade

import java.text.NumberFormat
import java.util.Locale

internal enum class TradeRole(val label: String) {
    MITRA("Mitra"),
    PENGHASIL("Penghasil");

    val other: TradeRole get() = if (this == MITRA) PENGHASIL else MITRA
}

internal enum class OfferStatus(val label: String) {
    PENDING("Menunggu"),
    COUNTERED("Tawaran Balik"),
    AGREED("Disepakati"),
    CONFIRMING("Menunggu Konfirmasi"),
    COMPLETE("Selesai"),
    REJECTED("Ditolak")
}

internal data class WasteListing(
    val id: Long,
    val title: String,
    val price: Long,
    val weight: String,
    val location: String,
    val company: String,
    val category: String,
    val condition: String,
    val description: String,
    val photo: String?
)

internal data class TradeOffer(
    val listingId: Long,
    val price: Long,
    val counterPrice: Long?,
    val status: OfferStatus,
    val buyerConfirmed: Boolean,
    val sellerConfirmed: Boolean,
    val updatedAt: Long,
    val buyerName: String = MITRA_NAME
) {
    val currentPrice: Long get() = counterPrice ?: price

    fun confirmed(role: TradeRole): Boolean =
        if (role == TradeRole.MITRA) buyerConfirmed else sellerConfirmed

    fun canRespond(role: TradeRole): Boolean = when (role) {
        TradeRole.PENGHASIL -> status == OfferStatus.PENDING
        TradeRole.MITRA -> status == OfferStatus.COUNTERED
    }

    fun afterConfirmation(role: TradeRole): OfferStatus {
        if (confirmed(role)) return status
        require(status == OfferStatus.AGREED || status == OfferStatus.CONFIRMING) {
            "Transaksi ini belum dapat dikonfirmasi."
        }
        return if (confirmed(role.other)) OfferStatus.COMPLETE else OfferStatus.CONFIRMING
    }
}

internal data class TradeMessage(
    val listingId: Long,
    val author: TradeRole,
    val text: String,
    val price: Long?,
    val sentAt: Long
)

internal data class TradeSnapshot(
    val listings: List<WasteListing>,
    val offers: Map<Long, TradeOffer>,
    val messages: List<TradeMessage>
)

internal const val MITRA_NAME = "CV Mitra Daur Nusantara"
internal const val PENGHASIL_NAME = "PT GreenCycle Indonesia"
internal const val MAX_TRADE_PRICE = 999_999_999_999L

internal fun rupiah(value: Long): String =
    "Rp" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(value)
