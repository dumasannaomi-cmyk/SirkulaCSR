package com.example.sirkulacsr.trade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpactCalculatorTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    private fun listing(id: Long, title: String, category: String, weight: String) =
        WasteListing(id, title, 100_000, weight, "Jakarta", PENGHASIL_NAME, category, "Baik", "", null)

    private fun offer(id: Long, status: OfferStatus, daysAgo: Long, price: Long = 100_000) =
        TradeOffer(id, price, null, status, false, false, now - daysAgo * day)

    private val snapshot = TradeSnapshot(
        listings = listOf(
            listing(1, "Kardus Bekas", "Kardus", "100"),
            listing(2, "Plastik PET", "Plastik", "80"),
            listing(3, "Minyak Jelantah", "Minyak", "30"),
            listing(4, "Kardus Lama", "Kardus", "500"),
            listing(5, "Botol", "Plastik", "50")
        ),
        offers = listOf(
            offer(1, OfferStatus.COMPLETE, 1, 150_000),
            offer(2, OfferStatus.COMPLETE, 3, 135_000),
            offer(3, OfferStatus.COMPLETE, 10, 100_000),
            offer(4, OfferStatus.COMPLETE, 100),
            offer(5, OfferStatus.AGREED, 1)
        ).associateBy(TradeOffer::listingId),
        messages = emptyList()
    )

    @Test
    fun onlyCompletedTradesInPeriodAreCounted() {
        val month = calculateImpact(completedTrades(snapshot), ImpactPeriod.MONTH, now)
        assertEquals(3, month.count)
        assertEquals(210.0, month.totalKg, 1e-9)
        assertEquals(100 * 0.9 + 80 * 1.5 + 30 * 2.5, month.totalCo2Kg, 1e-9)
        assertEquals(385_000L, month.totalValue)
    }

    @Test
    fun weekExcludesOlderAndYearIncludesAllCompleted() {
        assertEquals(2, calculateImpact(completedTrades(snapshot), ImpactPeriod.WEEK, now).count)
        assertEquals(4, calculateImpact(completedTrades(snapshot), ImpactPeriod.YEAR, now).count)
    }

    @Test
    fun compositionIsSortedAndSumsToHundred() {
        val s = calculateImpact(completedTrades(snapshot), ImpactPeriod.MONTH, now)
        assertEquals(WasteKind.KARDUS, s.top?.kind)
        assertEquals(100.0, s.kinds.sumOf(KindImpact::percent), 1e-9)
    }

    @Test
    fun emptyPeriodGivesZeroes() {
        val s = calculateImpact(emptyList(), ImpactPeriod.MONTH, now)
        assertEquals(0, s.count)
        assertEquals(0.0, s.totalCo2Kg, 0.0)
        assertTrue(impactNarrative(s, ImpactPeriod.MONTH).startsWith("Belum ada"))
    }

    @Test
    fun kindDetectionFromFreeText() {
        assertEquals(WasteKind.PLASTIK, wasteKindOf(listing(9, "Botol PET bersih", "Lain", "1")))
        assertEquals(WasteKind.LOGAM, wasteKindOf(listing(9, "Kaleng", "Aluminium", "1")))
        assertEquals(WasteKind.LAINNYA, wasteKindOf(listing(9, "Kain perca", "Tekstil", "1")))
    }
}
