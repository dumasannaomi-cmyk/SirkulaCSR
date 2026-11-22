package com.example.sirkulacsr.ui.estimasi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sirkulacsr.R
import java.util.Locale

private val HijauHasil = Color(0xFF218449)
private val LatarHasil = Color(0xFFEFFBF3)
private val HitamHasil = Color(0xFF151C1A)

private val InterHasil = FontFamily(
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

@Composable
fun HasilEstimasiScreen(
    jenis: String,
    jumlah: Double,
    satuan: String,
    nilaiEstimasi: Long?,
    modifier: Modifier = Modifier,
    onJualSekarang: () -> Unit = {}
) {
    val satuanSingkat = if (satuan == "Liter (L)") {
        "L"
    } else {
        "kg"
    }

    val jumlahTampil = if (
        jumlah.isFinite() &&
        jumlah % 1.0 == 0.0 &&
        jumlah <= Int.MAX_VALUE
    ) {
        jumlah.toInt().toString()
    } else {
        jumlah.toString()
    }

    val hargaTampil = if (nilaiEstimasi != null) {
        String.format(
            Locale.forLanguageTag("id-ID"),
            "Rp%,.2f",
            nilaiEstimasi.toDouble()
        )
    } else {
        "Rp—"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(145.dp)
            .border(
                1.dp,
                Color(0xFFE2EFE6),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(LatarHasil)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(
                R.drawable.potensi_pendapatan
            ),
            contentDescription = "Potensi Pendapatan",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Potensi Pendapatan",
                    fontFamily = InterHasil,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = HitamHasil,
                    maxLines = 1
                )

                Text(
                    text = hargaTampil,
                    fontFamily = InterHasil,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 36.sp,
                    color = HijauHasil,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "dari $jumlahTampil $satuanSingkat $jenis",
                    fontFamily = InterHasil,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = HitamHasil,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HijauHasil)
                    .clickable {
                        onJualSekarang()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Jual Sekarang",
                        fontFamily = InterHasil,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Canvas(
                        modifier = Modifier.size(12.dp)
                    ) {
                        val tebal = 2.dp.toPx()

                        drawLine(
                            color = Color.White,
                            start = Offset(
                                size.width * 0.3f,
                                size.height * 0.2f
                            ),
                            end = Offset(
                                size.width * 0.7f,
                                size.height * 0.5f
                            ),
                            strokeWidth = tebal
                        )

                        drawLine(
                            color = Color.White,
                            start = Offset(
                                size.width * 0.7f,
                                size.height * 0.5f
                            ),
                            end = Offset(
                                size.width * 0.3f,
                                size.height * 0.8f
                            ),
                            strokeWidth = tebal
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    name = "3.2 Potensi Pendapatan",
    showBackground = true,
    widthDp = 390,
    heightDp = 185
)
@Composable
private fun PreviewHasilEstimasiScreen() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7))
            .padding(20.dp)
    ) {
        HasilEstimasiScreen(
            jenis = "Kardus",
            jumlah = 100.0,
            satuan = "Kilogram (Kg)",
            nilaiEstimasi = 150000L
        )
    }
}