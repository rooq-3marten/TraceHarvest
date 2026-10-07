package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.zone.GeopoliticalZone
import com.example.ui.theme.*
import java.io.File

/**
 * Authentic, human-centered Nigerian farmer avatar system.
 * Explicitly replaces generic AI stock faces with:
 * 1. Real photo capture (when consent is granted)
 * 2. Culturally accurate zone-specific illustrated attire (Hula, Fila, Okpu Agu, Gele, Mayafi)
 */
@Composable
fun FarmerAvatarView(
    photoPath: String?,
    farmerName: String,
    zone: GeopoliticalZone,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    gender: String = "MALE" // "MALE" or "FEMALE"
) {
    val localFile = photoPath?.let { File(it) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, LateriteRedPrimary.copy(alpha = 0.4f), CircleShape)
            .background(SoftCreamSurface),
        contentAlignment = Alignment.Center
    ) {
        if (localFile != null && localFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Photo of $farmerName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ZoneIllustratedAvatarCanvas(zone, gender, farmerName)
            }
        } else {
            ZoneIllustratedAvatarCanvas(zone, gender, farmerName)
        }
    }
}

/**
 * Handcrafted vector illustration depicting authentic Nigerian regional headwear and attire.
 * Zero AI stock imagery; pure procedural vector illustration using Nigerian earthy tones.
 */
@Composable
private fun ZoneIllustratedAvatarCanvas(
    zone: GeopoliticalZone,
    gender: String,
    farmerName: String
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Warm Earthy Background
        val bgColor = when (zone) {
            GeopoliticalZone.NW, GeopoliticalZone.NE -> Color(0xFFF7ECE1) // Warm Sahara ochre
            GeopoliticalZone.NC -> Color(0xFFEDE8DD)                     // Savanna limestone
            GeopoliticalZone.SW -> Color(0xFFF5EBE6)                     // Laterite clay
            GeopoliticalZone.SE -> Color(0xFFEBF3E8)                     // Palm green tint
            GeopoliticalZone.SS -> Color(0xFFE8EEF5)                     // Coastal river mist
        }
        drawCircle(color = bgColor, radius = w / 2f)

        // 2. Warm African Skin Tone
        val skinColor = Color(0xFF6B4226) // Rich bronze mahogany
        val neckColor = Color(0xFF5A361E) // Shadow tone

        // Neck
        drawRect(
            color = neckColor,
            topLeft = Offset(w * 0.42f, h * 0.55f),
            size = Size(w * 0.16f, h * 0.22f)
        )

        // Torso / Traditional Robe (Babban Riga, Agbada, Isiagu, Niger Delta Shirt)
        val attireColor = when (zone) {
            GeopoliticalZone.NW, GeopoliticalZone.NE -> LateriteRedPrimary      // Laterite embroidered kaftan
            GeopoliticalZone.NC -> ForestGreenSecondary                         // Forest green vest
            GeopoliticalZone.SW -> Color(0xFF3E2723)                            // Deep cocoa aso-oke
            GeopoliticalZone.SE -> Color(0xFF880E4F)                            // Chieftain Isiagu burgundy
            GeopoliticalZone.SS -> Color(0xFF1A237E)                            // Royal navy ceremonial tunic
        }
        val attirePath = Path().apply {
            moveTo(w * 0.15f, h)
            cubicTo(w * 0.25f, h * 0.72f, w * 0.75f, h * 0.72f, w * 0.85f, h)
            close()
        }
        drawPath(attirePath, color = attireColor)

        // Head Base Oval
        drawOval(
            color = skinColor,
            topLeft = Offset(w * 0.28f, h * 0.25f),
            size = Size(w * 0.44f, h * 0.42f)
        )

        // 3. Cultural Headgear & Identity by Geopolitical Zone
        when (zone) {
            GeopoliticalZone.NW, GeopoliticalZone.NE -> {
                if (gender.uppercase() == "FEMALE") {
                    // Mayafi / Hijab headwrap draped gracefully
                    val mayafiPath = Path().apply {
                        moveTo(w * 0.22f, h * 0.38f)
                        cubicTo(w * 0.25f, h * 0.12f, w * 0.75f, h * 0.12f, w * 0.78f, h * 0.38f)
                        cubicTo(w * 0.85f, h * 0.75f, w * 0.15f, h * 0.75f, w * 0.22f, h * 0.38f)
                        close()
                    }
                    drawPath(mayafiPath, color = Color(0xFFD4A03C)) // Warm ochre mayafi
                } else {
                    // Traditional Hausa/Fulani Hula Cap with geometric stitch
                    val capPath = Path().apply {
                        moveTo(w * 0.27f, h * 0.30f)
                        cubicTo(w * 0.28f, h * 0.12f, w * 0.72f, h * 0.12f, w * 0.73f, h * 0.30f)
                        close()
                    }
                    drawPath(capPath, color = Color(0xFFFAF7F2))
                    drawPath(capPath, color = LateriteRedPrimary, style = Stroke(width = w * 0.03f))
                }
            }

            GeopoliticalZone.SW -> {
                if (gender.uppercase() == "FEMALE") {
                    // Yoruba Gele (Flared layered headtie)
                    val gelePath = Path().apply {
                        moveTo(w * 0.18f, h * 0.32f)
                        lineTo(w * 0.12f, h * 0.16f)
                        cubicTo(w * 0.35f, h * 0.08f, w * 0.65f, h * 0.08f, w * 0.88f, h * 0.16f)
                        lineTo(w * 0.82f, h * 0.32f)
                        close()
                    }
                    drawPath(gelePath, color = WarmOchreAccent)
                    drawPath(gelePath, color = LateriteRedPrimary, style = Stroke(width = w * 0.025f))
                } else {
                    // Yoruba Fila Cap (Gobi folded to the right)
                    val filaPath = Path().apply {
                        moveTo(w * 0.27f, h * 0.30f)
                        cubicTo(w * 0.28f, h * 0.15f, w * 0.65f, h * 0.10f, w * 0.75f, h * 0.20f)
                        lineTo(w * 0.72f, h * 0.30f)
                        close()
                    }
                    drawPath(filaPath, color = Color(0xFF3E2723))
                }
            }

            GeopoliticalZone.SE -> {
                if (gender.uppercase() == "FEMALE") {
                    // Igbo Ichafu headscarf
                    drawOval(
                        color = Color(0xFF1565C0),
                        topLeft = Offset(w * 0.22f, h * 0.14f),
                        size = Size(w * 0.56f, h * 0.24f)
                    )
                } else {
                    // Traditional Igbo Red Cap (Okpu Agu/Chieftaincy Cap)
                    val capPath = Path().apply {
                        moveTo(w * 0.28f, h * 0.28f)
                        lineTo(w * 0.28f, h * 0.15f)
                        lineTo(w * 0.72f, h * 0.15f)
                        lineTo(w * 0.72f, h * 0.28f)
                        close()
                    }
                    drawPath(capPath, color = Color(0xFFB71C1C)) // Chieftaincy scarlet red
                    // Eagle feather accent
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.70f, h * 0.22f),
                        end = Offset(w * 0.84f, h * 0.06f),
                        strokeWidth = w * 0.035f
                    )
                }
            }

            GeopoliticalZone.SS, GeopoliticalZone.NC -> {
                // Niger Delta / Middle Belt Fedoras & Coral Beads
                drawRoundRect(
                    color = Color(0xFF212121),
                    topLeft = Offset(w * 0.20f, h * 0.20f),
                    size = Size(w * 0.60f, h * 0.10f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(w * 0.30f, h * 0.10f),
                    size = Size(w * 0.40f, h * 0.15f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Coral Bead Necklace
                drawCircle(color = Color(0xFFFF5722), radius = w * 0.025f, center = Offset(w * 0.50f, h * 0.68f))
                drawCircle(color = Color(0xFFFF5722), radius = w * 0.025f, center = Offset(w * 0.44f, h * 0.69f))
                drawCircle(color = Color(0xFFFF5722), radius = w * 0.025f, center = Offset(w * 0.56f, h * 0.69f))
            }
        }
    }
}

/**
 * Human-centered photo consent dialog respecting farmer data sovereignty.
 * Translates into local geopolitical zone languages (Hausa, Yoruba, Igbo, Pidgin).
 */
@Composable
fun FarmerPhotoConsentDialog(
    zone: GeopoliticalZone,
    onConsentGiven: () -> Unit,
    onConsentDeclined: () -> Unit
) {
    val localPrompt = when (zone) {
        GeopoliticalZone.NW, GeopoliticalZone.NE ->
            "Shin manomin ya amince a ɗauki hotonsa don rajistar TraceHarvest?"
        GeopoliticalZone.SW ->
            "Ṣe àgbẹ̀ náà gbà kí a ya fọ́tò rẹ̀ fún ìforúkọsílẹ̀ TraceHarvest?"
        GeopoliticalZone.SE ->
            "Onye ọrụ ugbo a ọ kwenyere ka e sere ya foto maka ndekọ TraceHarvest?"
        GeopoliticalZone.NC, GeopoliticalZone.SS ->
            "The farmer agree make we snap photo for this TraceHarvest export record?"
    }

    AlertDialog(
        onDismissRequest = onConsentDeclined,
        icon = {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = LateriteRedPrimary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Farmer Photo Consent",
                fontWeight = FontWeight.Bold,
                color = CharcoalBrownText,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Do you have the farmer's explicit permission to take and store their photo for this export traceability record?",
                    fontSize = 14.sp,
                    color = CharcoalBrownText,
                    lineHeight = 20.sp
                )
                Surface(
                    color = SoftCreamSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineWarm)
                ) {
                    Text(
                        text = localPrompt,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreenSecondary,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 16.sp
                    )
                }
                Text(
                    text = "If no photo is taken, TraceHarvest will use the regional illustrated avatar instead.",
                    fontSize = 11.sp,
                    color = MutedBrownText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConsentGiven,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenSecondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Yes, farmer consents")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onConsentDeclined,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Use regional avatar")
            }
        },
        containerColor = WarmOffWhiteBackground,
        shape = RoundedCornerShape(16.dp)
    )
}
