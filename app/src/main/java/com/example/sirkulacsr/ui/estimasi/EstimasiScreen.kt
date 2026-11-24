package com.example.sirkulacsr.ui.estimasi

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sirkulacsr.R

private val Hijau = Color(0xFF218449)
private val HijauMuda = Color(0xFF65B987)
private val Latar = Color(0xFFF7F7F7)
private val Hitam = Color(0xFF151C1A)
private val Abu = Color(0xFF737373)
private val Border = Color(0xFFE5E5E5)
private val AbuInput = Color(0xFFA3A3A3)

private val InterEstimasi = FontFamily(
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private data class ListingData(
    val gambar: Int,
    val nama: String,
    val harga: String,
    val jenis: String,
    val jumlah: String,
    val lokasi: String,
    val status: String,
    val warnaStatus: Color,
    val latarStatus: Color,
    val waktu: String,
    val dilihat: String,
    val penawaran: String
)

@Composable
fun EstimasiScreen(
    modifier: Modifier = Modifier,
    nilaiEstimasi: Long? = null,
    isLoading: Boolean = false,
    onHitungNilai: ((String, Double, String) -> Unit)? = null,
    onJualSekarang: (() -> Unit)? = null,
    onBukaChat: (() -> Unit)? = null,
    onBukaNotifikasi: (() -> Unit)? = null,
    onLihatSemua: (() -> Unit)? = null,
    onNavigasi: ((String) -> Unit)? = null
) {
    var jenisTerpilih by rememberSaveable { mutableStateOf("") }
    var jumlah by rememberSaveable { mutableStateOf("") }
    var satuan by rememberSaveable { mutableStateOf("Kilogram (Kg)") }
    var dropdownTerbuka by rememberSaveable { mutableStateOf(false) }
    var pesanDialog by rememberSaveable { mutableStateOf("") }

    var tampilkanHasil by rememberSaveable { mutableStateOf(false) }
    var jenisHasil by rememberSaveable { mutableStateOf("") }
    var jumlahHasil by rememberSaveable { mutableStateOf(0.0) }
    var satuanHasil by rememberSaveable {
        mutableStateOf("Kilogram (Kg)")
    }

    val daftarScrollState = rememberLazyListState()

    LaunchedEffect(tampilkanHasil) {
        if (tampilkanHasil) {
            daftarScrollState.animateScrollToItem(2)
        }
    }

    val pilihanJenis = listOf(
        "Kardus",
        "Kertas",
        "Plastik",
        "Minyak",
        "Aluminium"
    )

    val daftarListing = listOf(
        ListingData(
            R.drawable.listing_kardus,
            "Kardus Bekas",
            "Rp150.000,00",
            "Kardus",
            "100 kg",
            "Jakarta Timur",
            "Tersedia",
            Color(0xFF16A765),
            Color(0xFFEAF8F0),
            "2h ago",
            "28 views",
            "3 offer"
        ),
        ListingData(
            R.drawable.listing_plastik,
            "Plastik PET",
            "Rp120.000,00",
            "Plastik",
            "80 kg",
            "Bekasi",
            "Tunggu Pickup",
            Color(0xFFFF9500),
            Color(0xFFFFF5E6),
            "5h ago",
            "29 views",
            "1 offer"
        ),
        ListingData(
            R.drawable.listing_minyak,
            "Minyak Jelantah",
            "Rp90.000,00",
            "Minyak",
            "50 L",
            "Jakarta Timur",
            "Terjual",
            Color(0xFF777B96),
            Color(0xFFF0F0F5),
            "1d ago",
            "31 views",
            "1 offer"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Latar)
    ) {
        HeaderEstimasi(
            onChat = {
                if (onBukaChat != null) {
                    onBukaChat()
                } else {
                    pesanDialog = "Fitur chat akan dihubungkan saat integrasi."
                }
            },
            onNotifikasi = {
                if (onBukaNotifikasi != null) {
                    onBukaNotifikasi()
                } else {
                    pesanDialog = "Fitur notifikasi akan dihubungkan saat integrasi."
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = daftarScrollState,
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 13.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "statistik") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KartuStatistik(
                        gambar = R.drawable.icon_kardus,
                        nilai = "420 kg",
                        label = "Total Limbah\nBulan Ini",
                        modifier = Modifier.weight(1f)
                    )

                    KartuStatistik(
                        gambar = R.drawable.icon_uang,
                        nilai = "Rp650.000",
                        label = "Potensi\nPendapatan",
                        modifier = Modifier.weight(1f)
                    )

                    KartuStatistik(
                        gambar = R.drawable.icon_bumi,
                        nilai = "1.2 Ton",
                        label = "CO₂\nTerselamatkan",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "form_estimasi") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 20.dp
                        )
                ) {
                    Text(
                        text = "Hitung Nilai Limbah",
                        fontFamily = InterEstimasi,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Hitam
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pilih Jenis Limbah",
                        fontFamily = InterEstimasi,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = Hitam
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(43.dp)
                                .border(
                                    1.dp,
                                    AbuInput,
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    dropdownTerbuka = true
                                }
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = jenisTerpilih.ifEmpty {
                                    "Pilih Jenis"
                                },
                                fontFamily = InterEstimasi,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (jenisTerpilih.isEmpty()) {
                                    AbuInput
                                } else {
                                    Hitam
                                }
                            )

                            Image(
                                painter = painterResource(
                                    R.drawable.icon_dropdown
                                ),
                                contentDescription = "Pilih jenis limbah",
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownTerbuka,
                            onDismissRequest = {
                                dropdownTerbuka = false
                            },
                            modifier = Modifier.background(Color.White)
                        ) {
                            pilihanJenis.forEach { jenis ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = jenis,
                                            fontFamily = InterEstimasi,
                                            fontSize = 14.sp,
                                            color = Hitam
                                        )
                                    },
                                    onClick = {
                                        jenisTerpilih = jenis
                                        dropdownTerbuka = false
                                        tampilkanHasil = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Volume / Berat",
                        fontFamily = InterEstimasi,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = Hitam
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TombolJumlah(
                            gambar = R.drawable.icon_minus,
                            modifier = Modifier.width(48.dp),
                            onClick = {
                                val angka = jumlah.toDoubleOrNull() ?: 0.0
                                jumlah = formatAngka(
                                    (angka - 1.0).coerceAtLeast(0.0)
                                )
                                tampilkanHasil = false
                            }
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(
                                    1.dp,
                                    AbuInput,
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = jumlah,
                                onValueChange = { input ->
                                    val normal = input.replace(',', '.')

                                    if (
                                        normal.count { it == '.' } <= 1 &&
                                        normal.all {
                                            it.isDigit() || it == '.'
                                        }
                                    ) {
                                        jumlah = normal
                                        tampilkanHasil = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                textStyle = TextStyle(
                                    fontFamily = InterEstimasi,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Hitam,
                                    textAlign = TextAlign.Center
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (jumlah.isEmpty()) {
                                            Text(
                                                text = "Isi Jumlah",
                                                fontFamily = InterEstimasi,
                                                fontSize = 14.sp,
                                                color = AbuInput
                                            )
                                        }

                                        innerTextField()
                                    }
                                }
                            )
                        }

                        TombolJumlah(
                            gambar = R.drawable.icon_plus,
                            modifier = Modifier.width(48.dp),
                            onClick = {
                                val angka = jumlah.toDoubleOrNull() ?: 0.0
                                jumlah = formatAngka(angka + 1.0)
                                tampilkanHasil = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .border(
                                1.dp,
                                Border,
                                RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        listOf(
                            "Kilogram (Kg)",
                            "Liter (L)"
                        ).forEach { pilihan ->
                            val aktif = satuan == pilihan

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (aktif) Hijau else Color.White
                                    )
                                    .clickable {
                                        satuan = pilihan
                                        tampilkanHasil = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pilihan,
                                    fontFamily = InterEstimasi,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (aktif) {
                                        Color.White
                                    } else {
                                        Abu
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Hijau)
                            .clickable(enabled = !isLoading) {
                                val angka = jumlah.toDoubleOrNull()

                                when {
                                    jenisTerpilih.isEmpty() -> {
                                        pesanDialog =
                                            "Pilih jenis limbah terlebih dahulu."
                                    }

                                    angka == null ||
                                            !angka.isFinite() ||
                                            angka <= 0.0 -> {
                                        pesanDialog =
                                            "Masukkan jumlah limbah lebih dari 0."
                                    }

                                    else -> {
                                        jenisHasil = jenisTerpilih
                                        jumlahHasil = angka
                                        satuanHasil = satuan
                                        tampilkanHasil = true

                                        onHitungNilai?.invoke(
                                            jenisTerpilih,
                                            angka,
                                            satuan
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLoading) "Menghitung..." else "Hitung Nilai",
                            fontFamily = InterEstimasi,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            if (tampilkanHasil) {
                item(key = "hasil") {
                    val hargaTampil = nilaiEstimasi ?: if (
                        jenisHasil == "Kardus" &&
                        jumlahHasil == 100.0 &&
                        satuanHasil == "Kilogram (Kg)"
                    ) {
                        150000L
                    } else {
                        null
                    }

                    HasilEstimasiScreen(
                        jenis = jenisHasil,
                        jumlah = jumlahHasil,
                        satuan = satuanHasil,
                        nilaiEstimasi = hargaTampil,
                        onJualSekarang = {
                            if (onJualSekarang != null) {
                                onJualSekarang()
                            } else {
                                pesanDialog =
                                    "Fitur Jual Sekarang akan dihubungkan ke Listing saat integrasi."
                            }
                        }
                    )
                }
            }

            item(key = "judul_listing") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Listing saya",
                        fontFamily = InterEstimasi,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Hitam
                    )

                    Text(
                        text = "Lihat semua",
                        modifier = Modifier.clickable {
                            if (onLihatSemua != null) {
                                onLihatSemua()
                            } else {
                                pesanDialog =
                                    "Halaman Listing akan dihubungkan saat integrasi."
                            }
                        },
                        fontFamily = InterEstimasi,
                        fontSize = 14.sp,
                        color = Abu
                    )
                }
            }

            items(
                items = daftarListing,
                key = { it.nama }
            ) { listing ->
                KartuListing(listing)
            }
        }

        NavigasiEstimasi(
            onItemClick = { halaman ->
                if (onNavigasi != null) {
                    onNavigasi(halaman)
                } else {
                    pesanDialog =
                        "Navigasi $halaman akan dihubungkan saat integrasi."
                }
            }
        )
    }

    if (pesanDialog.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                pesanDialog = ""
            },
            title = {
                Text(
                    text = "SIRKULA",
                    fontFamily = InterEstimasi,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = pesanDialog,
                    fontFamily = InterEstimasi,
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pesanDialog = ""
                    }
                ) {
                    Text(
                        text = "OK",
                        color = Hijau,
                        fontFamily = InterEstimasi
                    )
                }
            }
        )
    }
}

private fun formatAngka(angka: Double): String {
    return if (
        angka.isFinite() &&
        angka % 1.0 == 0.0 &&
        angka <= Int.MAX_VALUE
    ) {
        angka.toInt().toString()
    } else {
        angka.toString()
    }
}

@Composable
private fun HeaderEstimasi(
    onChat: () -> Unit,
    onNotifikasi: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Hijau)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Halo",
                fontFamily = InterEstimasi,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color.White
            )

            Text(
                text = "PT GreenCycle Indonesia",
                fontFamily = InterEstimasi,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color.White
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IkonHeader(
                gambar = R.drawable.icon_chat,
                deskripsi = "Chat",
                onClick = onChat
            )

            IkonHeader(
                gambar = R.drawable.icon_notifikasi,
                deskripsi = "Notifikasi",
                onClick = onNotifikasi
            )
        }
    }
}

@Composable
private fun IkonHeader(
    gambar: Int,
    deskripsi: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(HijauMuda)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(gambar),
            contentDescription = deskripsi,
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun KartuStatistik(
    gambar: Int,
    nilai: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .border(
                1.dp,
                Border,
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(gambar),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = nilai,
            fontFamily = InterEstimasi,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Hitam,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontFamily = InterEstimasi,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            color = Abu
        )
    }
}

@Composable
private fun TombolJumlah(
    gambar: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(
                1.dp,
                AbuInput,
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(gambar),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun KartuListing(data: ListingData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(133.dp)
            .border(
                1.dp,
                Border,
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(data.gambar),
            contentDescription = data.nama,
            modifier = Modifier
                .size(109.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.nama,
                    modifier = Modifier.weight(1f),
                    fontFamily = InterEstimasi,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Hitam,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(data.latarStatus)
                        .padding(
                            horizontal = 6.dp,
                            vertical = 4.dp
                        )
                ) {
                    Text(
                        text = data.status,
                        fontFamily = InterEstimasi,
                        fontSize = 9.sp,
                        color = data.warnaStatus,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.harga,
                    fontFamily = InterEstimasi,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Hijau
                )

                Text(
                    text = " /total",
                    fontFamily = InterEstimasi,
                    fontSize = 10.sp,
                    color = Abu
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${data.jenis} · ${data.jumlah}",
                    fontFamily = InterEstimasi,
                    fontSize = 10.sp,
                    color = Abu,
                    maxLines = 1
                )

                Image(
                    painter = painterResource(
                        R.drawable.icon_lokasi
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )

                Text(
                    text = data.lokasi,
                    fontFamily = InterEstimasi,
                    fontSize = 10.sp,
                    color = Hitam,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Border)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = data.waktu,
                    fontFamily = InterEstimasi,
                    fontSize = 10.sp,
                    color = Abu
                )

                Image(
                    painter = painterResource(
                        R.drawable.icon_views
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )

                Text(
                    text = data.dilihat,
                    fontFamily = InterEstimasi,
                    fontSize = 10.sp,
                    color = Abu
                )

                Image(
                    painter = painterResource(
                        R.drawable.icon_offer
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )

                Text(
                    text = data.penawaran,
                    fontFamily = InterEstimasi,
                    fontSize = 10.sp,
                    color = Abu
                )
            }
        }
    }
}

@Composable
private fun NavigasiEstimasi(
    onItemClick: (String) -> Unit
) {
    val menu = listOf(
        Triple(R.drawable.nav_beranda, "Beranda", true),
        Triple(R.drawable.nav_listing, "Listing", false),
        Triple(R.drawable.nav_dampak, "Dampak", false),
        Triple(R.drawable.nav_profil, "Profil", false)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.White)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        menu.forEach { item ->
            Column(
                modifier = Modifier
                    .width(70.dp)
                    .clickable {
                        onItemClick(item.second)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(item.first),
                    contentDescription = item.second,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.second,
                    fontFamily = InterEstimasi,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (item.third) Hijau else Abu
                )
            }
        }
    }
}

@Preview(
    name = "3.1 dan 3.2 Estimasi Limbah",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun PreviewEstimasiScreen() {
    EstimasiScreen()
}