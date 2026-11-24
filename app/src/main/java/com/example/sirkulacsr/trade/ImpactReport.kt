package com.example.sirkulacsr.trade

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idLocale: Locale = Locale.forLanguageTag("id-ID")

private fun String.html() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun longDate(time: Long) = SimpleDateFormat("d MMMM yyyy", idLocale).format(Date(time))
private fun shortDate(time: Long) = SimpleDateFormat("d MMM yyyy", idLocale).format(Date(time))

/** Dokumen Laporan CSR (A4) berisi ringkasan, tabel komposisi/emisi, rincian transaksi, dan metodologi. */
internal fun buildCsrReportHtml(
    company: String, roleLabel: String, period: ImpactPeriod, summary: ImpactSummary, printedAt: Long
): String {
    val kpi = listOf(
        "Jumlah transaksi selesai" to formatNumber(summary.count.toDouble(), 0),
        "Total limbah didaur ulang" to formatMass(summary.totalKg),
        "Estimasi reduksi emisi (CO₂e)" to formatMass(summary.totalCo2Kg),
        "Setara penyerapan karbon pohon / tahun" to "${formatNumber(summary.trees)} pohon",
        "Setara jarak mobil penumpang dihindari" to "${formatNumber(summary.carKm, 0)} km",
        "Nilai transaksi" to rupiah(summary.totalValue)
    ).joinToString("") { (k, v) -> "<tr><td>${k.html()}</td><td class=\"r b\">${v.html()}</td></tr>" }

    val kinds = summary.kinds.joinToString("") {
        "<tr><td>${it.kind.label.html()}</td><td class=\"r\">${formatNumber(it.weightKg)}</td>" +
            "<td class=\"r\">${formatNumber(it.percent)}%</td><td class=\"r\">${formatNumber(it.kind.factor, 2)}</td>" +
            "<td class=\"r\">${formatNumber(it.co2Kg)}</td></tr>"
    } + "<tr class=\"tot\"><td>Total</td><td class=\"r\">${formatNumber(summary.totalKg)}</td><td class=\"r\">100%</td>" +
        "<td class=\"r\">–</td><td class=\"r\">${formatNumber(summary.totalCo2Kg)}</td></tr>"

    val trades = summary.trades.joinToString("") {
        "<tr><td>${shortDate(it.completedAt)}</td><td>${it.listing.title.html()}</td>" +
            "<td>${it.kind.label.html()}</td><td class=\"r\">${formatNumber(it.weightKg)}</td>" +
            "<td class=\"r\">${rupiah(it.valueRupiah)}</td></tr>"
    }

    return """<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"/>
<style>
  body{font-family:sans-serif;color:#171717;font-size:12px;line-height:1.5;margin:0}
  .head{border-bottom:3px solid #218449;padding-bottom:10px;display:flex;justify-content:space-between}
  .brand{color:#155831;font-weight:bold;letter-spacing:2px;font-size:10px}
  h1{font-size:18px;margin:2px 0 0}
  h2{font-size:13px;margin:20px 0 6px;color:#155831}
  .meta{text-align:right;font-size:11px}
  table{width:100%;border-collapse:collapse;font-size:11px}
  th,td{border:1px solid #d4d4d4;padding:5px 7px;text-align:left}
  th{background:#EAFAF0}
  .r{text-align:right}.b{font-weight:bold}.tot td{font-weight:bold;background:#f7f7f7}
  ul{margin:4px 0 0 18px;padding:0}
  .sign{margin-top:36px;text-align:right;font-size:11px}
  .sign div{display:inline-block;width:200px;text-align:center}
  .line{margin-top:60px;border-top:1px solid #737373;padding-top:3px;font-weight:bold}
</style></head><body>
<div class="head">
  <div><div class="brand">SIRKULA · EcoSync</div><h1>Laporan Dampak Lingkungan &amp; Kontribusi CSR</h1></div>
  <div class="meta"><b>${company.html()}</b><br/>${roleLabel.html()}<br/>Periode: ${period.label} (${period.caption})<br/>
  Dicetak: ${longDate(printedAt)}</div>
</div>
<h2>1. Ringkasan Eksekutif</h2><p>${impactNarrative(summary, period).html()}</p>
<h2>2. Indikator Utama</h2><table><tbody>$kpi</tbody></table>
<h2>3. Komposisi Limbah &amp; Reduksi Emisi</h2>
<table><thead><tr><th>Jenis Limbah</th><th class="r">Berat (kg)</th><th class="r">Porsi</th>
<th class="r">Faktor (kg CO₂e/kg)</th><th class="r">CO₂e Dihemat (kg)</th></tr></thead><tbody>$kinds</tbody></table>
<h2>4. Rincian Transaksi Selesai</h2>
<table><thead><tr><th>Tanggal</th><th>Limbah</th><th>Jenis</th><th class="r">Berat (kg)</th>
<th class="r">Nilai</th></tr></thead><tbody>$trades</tbody></table>
<h2>5. Catatan Metodologi</h2>
<ul>
<li>Data bersumber dari transaksi berstatus <b>Selesai</b> pada aplikasi SIRKULA dalam periode laporan.</li>
<li>Reduksi emisi = Σ (berat kg × faktor emisi jenis limbah). Faktor emisi adalah asumsi estimasi dan perlu
diselaraskan dengan referensi resmi sebelum dipublikasikan.</li>
<li>Ekuivalensi: 1 pohon ≈ 21 kg CO₂ per tahun; 1 km mobil penumpang ≈ 0,12 kg CO₂.</li>
<li>Pembayaran dan serah terima limbah dilakukan langsung oleh kedua pihak di luar aplikasi SIRKULA.</li>
</ul>
<div class="sign"><div>Disahkan oleh,<div class="line">Penanggung Jawab CSR</div></div></div>
</body></html>"""
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// WebView harus tetap hidup selama dialog cetak terbuka, jadi referensinya dipegang sampai cetak berikutnya.
private var printView: WebView? = null

/** Membuka dialog cetak Android (bisa "Simpan sebagai PDF") untuk dokumen laporan. Return false bila gagal. */
internal fun printCsrReport(context: Context, html: String, jobName: String): Boolean {
    val activity = context.findActivity() ?: return false
    val manager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
    printView?.destroy()
    val view = WebView(activity)
    view.settings.javaScriptEnabled = false
    view.webViewClient = object : WebViewClient() {
        override fun onPageFinished(webView: WebView, url: String?) {
            manager.print(
                jobName, webView.createPrintDocumentAdapter(jobName),
                PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
            )
        }
    }
    printView = view
    view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    return true
}
