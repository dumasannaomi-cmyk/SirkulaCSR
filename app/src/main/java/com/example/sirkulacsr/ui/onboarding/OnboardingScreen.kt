
package com.example.sirkulacsr.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sirkulacsr.R

private val SirkulaGreen = Color(0xFF218449)
private val LightGreen = Color(0xFF70D197)
private val BackgroundColor = Color(0xFFF7F7F7)
private val DescriptionColor = Color(0xFF525252)

private val InterFont = FontFamily(
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private data class OnboardingPage(
    val image: Int,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        image = R.drawable.onboarding_1,
        title = "Ubah Limbah Menjadi\nPeluang Baru",
        description = "Jual limbah bernilai atau temukan bahan baku daur ulang. " +
                "SIRKULA menghubungkan perusahaan, pengepul, dan bank sampah."
    ),
    OnboardingPage(
        image = R.drawable.onboarding_2,
        title = "Hitung Nilai Jual\nLimbah Kamu",
        description = "Masukkan jenis dan berat atau volume limbah. " +
                "Lihat estimasi nilai jualnya agar kamu lebih mudah " +
                "merencanakan transaksi melalui SIRKULA."
    ),
    OnboardingPage(
        image = R.drawable.onboarding_3,
        title = "Terhubung dengan Mitra\nDaur Ulang",
        description = "Cari pengepul atau bank sampah di sekitar. " +
                "Hubungi mitra, mulai transaksi, lalu atur waktu " +
                "penjemputan limbah melalui aplikasi SIRKULA."
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onBackFromFirst: () -> Unit,
    initialPage: Int = 0
) {
    var currentPage by rememberSaveable {
        mutableIntStateOf(initialPage.coerceIn(0, 2))
    }

    val page = onboardingPages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = page.image),
                contentDescription = page.title,
                modifier = Modifier
                    .width(308.dp)
                    .height(282.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = page.title,
                fontFamily = InterFont,
                fontSize = 24.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = page.description,
                fontFamily = InterFont,
                fontSize = 14.sp,
                lineHeight = 17.5.sp,
                fontWeight = FontWeight.Medium,
                color = DescriptionColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .width(
                                if (index == currentPage) 24.dp
                                else 8.dp
                            )
                            .height(8.dp)
                            .background(
                                color = if (index == currentPage) {
                                    SirkulaGreen
                                } else {
                                    LightGreen
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 16.dp,
                    top = 47.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.arrowback),
                contentDescription = "Kembali",
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        if (currentPage > 0) {
                            currentPage--
                        } else {
                            onBackFromFirst()
                        }
                    }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (currentPage < 2) {
                Box(
                    modifier = Modifier
                        .width(118.dp)
                        .height(40.dp)
                        .clickable {
                            onFinish()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lewati",
                        fontFamily = InterFont,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SirkulaGreen
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(118.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 28.dp
                )
        ) {
            Button(
                onClick = {
                    if (currentPage < 2) {
                        currentPage++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SirkulaGreen,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Selanjutnya",
                    fontFamily = InterFont,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(
    name = "Onboarding 1",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun PreviewOnboarding1() {
    OnboardingScreen(
        onFinish = {},
        onBackFromFirst = {},
        initialPage = 0
    )
}

@Preview(
    name = "Onboarding 2",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun PreviewOnboarding2() {
    OnboardingScreen(
        onFinish = {},
        onBackFromFirst = {},
        initialPage = 1
    )
}

@Preview(
    name = "Onboarding 3",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun PreviewOnboarding3() {
    OnboardingScreen(
        onFinish = {},
        onBackFromFirst = {},
        initialPage = 2
    )
}
