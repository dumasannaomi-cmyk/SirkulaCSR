package com.example.sirkulacsr.trade

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sirkulacsr.R

internal val TradeGreen = Color(0xFF218449)
internal val TradeGold = Color(0xFFF2A51A)
internal val GreenInk = Color(0xFF155831)
internal val GoldInk = Color(0xFF9B4800)
internal val TradeInk = Color(0xFF171717)
internal val TradeMuted = Color(0xFF737373)
internal val TradeLine = Color(0xFFE5E5E5)
internal val TradeBackground = Color(0xFFF7F7F7)
internal val Mint = Color(0xFFEAFAF0)
internal val PaleGold = Color(0xFFFFEFC5)
internal val TradeRed = Color(0xFFD73528)

internal val TradeRole.accent get() = if (this == TradeRole.MITRA) TradeGold else TradeGreen
internal val TradeRole.accentInk get() = if (this == TradeRole.MITRA) GoldInk else GreenInk

@Composable
internal fun TradeText(
    value: String, size: Int = 14, color: Color = TradeInk,
    weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier,
    align: TextAlign? = null
) {
    Text(value, modifier, color, fontSize = size.sp, fontWeight = weight,
        lineHeight = (size * 1.28f).sp, letterSpacing = 0.sp, textAlign = align)
}

@Composable
internal fun TradeTitle(value: String, size: Int = 18) {
    TradeText(value, size, weight = FontWeight.Bold)
}

@Composable
internal fun TradeIcon(resource: Int, size: Int, tint: Color = Color.Unspecified) {
    Icon(painterResource(resource), contentDescription = null, modifier = Modifier.size(size.dp), tint = tint)
}

@Composable
internal fun TradeHeader(
    title: String, role: TradeRole, back: (() -> Unit)?, avatar: String? = null
) {
    Box(Modifier.fillMaxWidth().height(56.dp).background(Color.White).padding(horizontal = 14.dp)) {
        TradeText(title, 16, weight = FontWeight.SemiBold, align = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = if (avatar == null) 48.dp else 70.dp))
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            if (back != null) {
                IconButton(onClick = back, modifier = Modifier.size(42.dp)) {
                    Box(Modifier.size(32.dp).background(role.accent, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.keiza_arrow), "Kembali",
                            modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                }
            }
            if (avatar != null) Avatar(avatar, if (role == TradeRole.MITRA) PaleGold else Mint)
        }
    }
}

@Composable
internal fun TradeCard(
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Color.White).border(1.dp, TradeLine, shape)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
internal fun TradeButton(
    label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier,
    outlined: Boolean = false, enabled: Boolean = true, ink: Color = color
) {
    val shape = RoundedCornerShape(8.dp)
    if (outlined) {
        OutlinedButton(
            onClick, modifier.heightIn(min = 44.dp), enabled, shape,
            border = androidx.compose.foundation.BorderStroke(1.dp, color),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) { TradeText(label, 12, ink, FontWeight.SemiBold, align = TextAlign.Center) }
    } else {
        Button(
            onClick, modifier.heightIn(min = 48.dp), enabled, shape,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) { TradeText(label, 16, if (enabled) Color.White else TradeMuted, align = TextAlign.Center) }
    }
}

@Composable
internal fun TradeFooter(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
internal fun TradeActions(
    role: TradeRole, primary: String, onPrimary: () -> Unit,
    secondary: String? = null, onSecondary: (() -> Unit)? = null
) {
    TradeFooter {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (secondary != null && onSecondary != null) {
                TradeButton(secondary, role.accent, onSecondary, Modifier.weight(0.42f).heightIn(min = 48.dp),
                    outlined = true, ink = role.accentInk)
            }
            TradeButton(primary, role.accent, onPrimary, Modifier.weight(0.58f))
        }
    }
}

@Composable
internal fun TradePhoto(listing: WasteListing, modifier: Modifier, hero: Boolean = false) {
    val image = when (listing.photo) {
        "kardus" -> if (hero) R.drawable.keiza_kardus_hero else R.drawable.keiza_kardus
        "plastik" -> R.drawable.keiza_plastik
        "minyak" -> R.drawable.keiza_minyak
        else -> null
    }
    val shape = RoundedCornerShape(8.dp)
    if (image == null) {
        Box(modifier.clip(shape).background(Mint), contentAlignment = Alignment.Center) {
            TradeIcon(R.drawable.keiza_icon_pack, 24, TradeGreen)
        }
    } else {
        Image(painterResource(image), listing.title, modifier.clip(shape), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun ListingSummary(
    listing: WasteListing,
    formattedPrice: String = rupiah(listing.price),
    subtitle: String = "${listing.weight} kg · ${listing.location}",
    status: OfferStatus? = null
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TradePhoto(listing, Modifier.size(76.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TradeText(listing.title, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (status != null) StatusBadge(status)
            }
            TradeText(subtitle, 10, TradeMuted)
            TradeText(formattedPrice, 14, TradeGreen, FontWeight.Bold)
        }
    }
}

@Composable
internal fun StatusBadge(status: OfferStatus, text: String = status.label) {
    val (background, color) = when (status) {
        OfferStatus.PENDING, OfferStatus.COUNTERED, OfferStatus.CONFIRMING -> PaleGold to GoldInk
        OfferStatus.REJECTED -> Color(0xFFFEEBE9) to TradeRed
        OfferStatus.AGREED, OfferStatus.COMPLETE -> Mint to GreenInk
    }
    Box(Modifier.background(background, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        TradeText(text, 10, color, FontWeight.Medium, align = TextAlign.Center)
    }
}

@Composable
internal fun TradeFact(label: String, value: String, valueColor: Color = TradeInk) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TradeText(label, 12, TradeMuted, modifier = Modifier.weight(1f))
            TradeText(value, 12, valueColor, FontWeight.SemiBold, Modifier.weight(1.3f), TextAlign.End)
        }
        HorizontalDivider(color = TradeLine)
    }
}

@Composable
internal fun TradeNote(text: String, extra: (@Composable ColumnScope.() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(Mint, RoundedCornerShape(10.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TradeIcon(R.drawable.keiza_icon_info, 12, GreenInk)
            TradeText(text, 12, GreenInk)
        }
        extra?.invoke(this)
    }
}

@Composable
internal fun Avatar(initials: String, background: Color = Mint) {
    Box(Modifier.size(32.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        TradeText(initials, 12, if (background == PaleGold) GoldInk else GreenInk)
    }
}

@Composable
internal fun TradeField(
    label: String, value: String, change: (String) -> Unit, hint: String = "",
    keyboard: KeyboardType = KeyboardType.Text, minLines: Int = 1, prefix: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TradeText(label, 16)
        OutlinedTextField(
            value = value, onValueChange = change, modifier = Modifier.fillMaxWidth(),
            placeholder = if (hint.isBlank()) null else { { TradeText(hint, 14, TradeMuted) } },
            prefix = prefix?.let { currency -> { TradeText(currency, 14) } },
            singleLine = minLines == 1, minLines = minLines, maxLines = if (minLines == 1) 1 else 4,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TradeGreen, unfocusedBorderColor = TradeLine,
                focusedTextColor = TradeInk, unfocusedTextColor = TradeInk
            )
        )
    }
}

@Composable
internal fun TradeStatusHero(
    icon: Int, iconSize: Int, tint: Color, background: Color, heading: String, detail: String
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(90.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
            TradeIcon(icon, iconSize, tint)
        }
        TradeText(heading, 16, weight = FontWeight.Bold, align = TextAlign.Center)
        TradeText(detail, 12, TradeMuted, align = TextAlign.Center)
    }
}
