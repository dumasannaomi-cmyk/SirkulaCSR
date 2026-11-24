package com.example.sirkulacsr.trade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sirkulacsr.R

private val KardusColor = TradeGold
private val PlastikColor = TradeGreen
private val MinyakColor = GoldInk
private val LogamColor = Color(0xFF5B6B8C)
private val LainnyaColor = Color(0xFFA3A3A3)

private val WasteKind.color: Color
    get() = when (this) {
        WasteKind.KARDUS -> KardusColor
        WasteKind.PLASTIK -> PlastikColor
        WasteKind.MINYAK -> MinyakColor
        WasteKind.LOGAM -> LogamColor
        WasteKind.LAINNYA -> LainnyaColor
    }

/**
 * Halaman Dampak Lingkungan & Laporan CSR. Membaca data langsung dari TradeSnapshot
 * (transaksi berstatus Selesai) sehingga ikut berubah saat PIC lain menyelesaikan transaksi.
 */
@Composable
internal fun ImpactScreen(
    snapshot: TradeSnapshot, role: TradeRole, onOpenTrade: (Long) -> Unit
) {
    val context = LocalContext.current
    var periodIndex by rememberSaveable { mutableIntStateOf(ImpactPeriod.MONTH.ordinal) }
    val period = ImpactPeriod.entries[periodIndex]
    val allTrades = remember(snapshot) { completedTrades(snapshot) }
    val now = remember(snapshot, periodIndex) { System.currentTimeMillis() }
    val summary = remember(allTrades, periodIndex, now) { calculateImpact(allTrades, period, now) }
    var printFailed by remember { mutableStateOf(false) }

    TradeTitle("Dampak Lingkungan")
    TradeText("Kontribusi daur ulang dari transaksi yang sudah selesai.", 12, TradeMuted)

    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ImpactPeriod.entries.forEach { option ->
            val selected = option == period
            Box(Modifier.heightIn(min = 34.dp).clip(CircleShape)
                .background(if (selected) role.accent else Color.White)
                .border(1.dp, if (selected) role.accent else TradeLine, CircleShape)
                .clickable { periodIndex = option.ordinal }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center) {
                TradeText(option.label, 10, if (selected) Color.White else TradeMuted)
            }
        }
    }
    TradeText(period.caption, 10, TradeMuted)

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ImpactStat(Modifier.weight(1f).fillMaxHeight(), "Total Limbah Didaur Ulang",
            formatMass(summary.totalKg), "${summary.count} transaksi selesai")
        ImpactStat(Modifier.weight(1f).fillMaxHeight(), "CO₂e Dihemat",
            formatMass(summary.totalCo2Kg), "≈ ${formatNumber(summary.trees)} pohon/tahun")
    }

    if (summary.count == 0) {
        TradeCard {
            TradeStatusHero(R.drawable.keiza_inbox, 36, TradeGreen, Mint, "Belum Ada Transaksi Selesai",
                "Dampak lingkungan dihitung dari transaksi berstatus Selesai pada periode ini.")
        }
    } else {
        CompositionCard(summary)
    }

    TradeCard {
        TradeText("Ringkasan Kontribusi Keberlanjutan", weight = FontWeight.SemiBold)
        TradeText(impactNarrative(summary, period), 12, TradeMuted)
        Column {
            TradeFact("Setara penyerapan pohon / tahun", "${formatNumber(summary.trees)} pohon")
            TradeFact("Setara jarak mobil dihindari", "${formatNumber(summary.carKm, 0)} km")
            TradeFact("Nilai transaksi", rupiah(summary.totalValue), TradeGreen)
        }
        TradeNote("Faktor emisi bersifat estimasi. Lihat catatan metodologi pada laporan.")
        TradeButton("Cetak Laporan CSR", role.accent, {
            val company = if (role == TradeRole.MITRA) MITRA_NAME else PENGHASIL_NAME
            val roleLabel = if (role == TradeRole.MITRA) "Pengepul/Mitra" else "Penghasil Limbah"
            val html = buildCsrReportHtml(company, roleLabel, period, summary, System.currentTimeMillis())
            printFailed = !printCsrReport(context, html, "Laporan CSR SIRKULA - ${period.label}")
        }, Modifier.fillMaxWidth(), enabled = summary.count > 0)
        if (printFailed) TradeText("Laporan belum dapat dicetak. Silakan coba lagi.", 12, TradeRed)
    }

    if (summary.count > 0) {
        TradeText("Transaksi Selesai", weight = FontWeight.SemiBold)
        summary.trades.forEach { trade ->
            TradeCard(onClick = { onOpenTrade(trade.listing.id) }) {
                ListingSummary(trade.listing, rupiah(trade.valueRupiah),
                    "${formatNumber(trade.weightKg)} kg · ${formatNumber(trade.weightKg * trade.kind.factor)} kg CO₂e",
                    OfferStatus.COMPLETE)
            }
        }
    }
}

@Composable
private fun ImpactStat(modifier: Modifier, label: String, value: String, note: String) {
    TradeCard(modifier) {
        TradeText(label, 10, TradeMuted)
        TradeText(value, 20, TradeGreen, FontWeight.Bold)
        TradeText(note, 10, GreenInk, FontWeight.Medium)
    }
}

@Composable
private fun CompositionCard(summary: ImpactSummary) {
    TradeCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TradeText("Komposisi Jenis Limbah", weight = FontWeight.SemiBold)
            TradeText("berdasarkan berat", 10, TradeMuted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 18.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(TradeLine, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    var start = -90f
                    summary.kinds.forEach { item ->
                        val sweep = (item.percent / 100.0 * 360.0).toFloat()
                        drawArc(item.kind.color, start, sweep, false, Offset(inset, inset), arcSize,
                            style = Stroke(stroke))
                        start += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TradeText(formatNumber(summary.totalKg, 0), 18, weight = FontWeight.Bold)
                    TradeText("kg total", 10, TradeMuted)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.kinds.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(10.dp).background(item.kind.color, CircleShape))
                        TradeText(item.kind.label, 12, modifier = Modifier.weight(1f))
                        TradeText("${formatNumber(item.percent)}%", 12, weight = FontWeight.Bold)
                    }
                }
            }
        }
        HorizontalDivider(color = TradeLine)
        summary.kinds.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TradeText(item.kind.label, 12, weight = FontWeight.SemiBold)
                    TradeText("${formatNumber(item.weightKg)} kg · ${formatNumber(item.co2Kg)} kg CO₂e", 10, TradeMuted)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(TradeBackground)) {
                    Box(Modifier.fillMaxWidth((item.percent / 100.0).toFloat().coerceIn(0.02f, 1f))
                        .fillMaxHeight().background(item.kind.color, CircleShape))
                }
            }
        }
    }
}
