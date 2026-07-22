package app.lumadocs.kmp.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaDisplay
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.ui.DocCategory
import app.lumadocs.kmp.ui.DocThumb
import app.lumadocs.kmp.ui.PillButton
import app.lumadocs.kmp.ui.ThumbSize

private data class OnbSlide(
    val kicker: String,
    val title: String,
    val body: String,
    val art: String,
)

private val slides = listOf(
    OnbSlide("LUMADOCS", "Your\ndigital safe.", "Scan, encrypt, and store your most important documents — passports, IDs, records — in one private vault.", "vault"),
    OnbSlide("FEATURE · 01", "Scan any\ndocument.", "Auto edge detection and enhancement. Stack multi-page contracts, IDs, and reports into a single file.", "scan"),
    OnbSlide("FEATURE · 02", "End-to-end\nencrypted.", "AES-256 locally. Face ID to unlock. Everything syncs to your Google Drive encrypted — only you hold the key.", "lock"),
    OnbSlide("FEATURE · 03", "Never miss\na renewal.", "Set expiry dates on any document. Get alerts 90, 30, 7 and 1 day before a passport or visa expires.", "bell"),
)

@Composable
fun OnboardingPager(
    onGetStarted: () -> Unit,
    onSkipIntro: () -> Unit = onGetStarted,
) {
    val c = LocalLumaColors.current
    var slide by remember { mutableStateOf(0) }
    val s = slides[slide]
    val isLast = slide == slides.lastIndex

    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.End) {
            Text(
                "Skip →", color = c.textMute, fontFamily = LumaUi, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onSkipIntro),
            )
        }

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = s.art,
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                label = "art",
            ) { art ->
                when (art) {
                    "vault" -> VaultArt()
                    "scan" -> ScanArt()
                    "lock" -> LockArt()
                    else -> BellArt()
                }
            }
        }

        Column(Modifier.padding(horizontal = 28.dp)) {
            Text(s.kicker, fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = c.accent, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(14.dp))
            Text(s.title, fontFamily = LumaDisplay, fontStyle = FontStyle.Italic, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-1.5).sp, color = c.text)
            Spacer(Modifier.height(16.dp))
            Text(s.body, fontFamily = LumaUi, fontSize = 15.sp, lineHeight = 22.sp, color = c.textDim)
            Spacer(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    slides.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(width = if (i == slide) 22.dp else 6.dp, height = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (i == slide) c.accent else Color(0x26FFFFFF))
                                .clickable { slide = i }
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                PillButton(
                    text = if (isLast) "Get Started" else "Continue",
                    trailingIcon = LumaIcons.Forward,
                    onClick = { if (isLast) onGetStarted() else slide++ },
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun VaultArt() {
    val c = LocalLumaColors.current
    Box(Modifier.size(320.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(300.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.13f), Color.Transparent)))
        )
        val cats = listOf(DocCategory.IDENTITY, DocCategory.TRAVEL, DocCategory.MEDICAL)
        cats.forEachIndexed { i, cat ->
            Box(
                Modifier
                    .padding(start = (i * 12).dp, top = (i * 12).dp)
                    .rotate((i - 1) * 6f)
            ) {
                DocThumb(category = cat, code = cat.key.take(3).uppercase(), pages = 3, hasPhoto = i == 0, size = ThumbSize.LG)
            }
        }
    }
}

@Composable
private fun ScanArt() {
    val c = LocalLumaColors.current
    val transition = rememberInfiniteTransition(label = "scan")
    val y by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse), label = "y",
    )
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 168.dp, height = 220.dp)) {
            DocThumb(category = DocCategory.TRAVEL, code = "PP-USA", hasPhoto = true, size = ThumbSize.LG)
            Box(
                Modifier
                    .fillMaxWidth()
                    .offset(y = (y * 214).dp)
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, c.accent, Color.Transparent)))
            )
        }
    }
}

@Composable
private fun LockArt() {
    val c = LocalLumaColors.current
    val t = rememberInfiniteTransition(label = "spin")
    val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "a")
    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(280.dp).rotate(a).clip(CircleShape).border(1.dp, c.accent.copy(alpha = 0.27f), CircleShape))
        Box(Modifier.size(200.dp).rotate(-a).clip(CircleShape).border(1.dp, c.accent.copy(alpha = 0.2f), CircleShape))
        Box(
            Modifier.size(140.dp).clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(c.bg3, c.bg2)))
                .border(1.dp, c.accent.copy(alpha = 0.2f), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(LumaIcons.Shield, null, tint = c.accent, modifier = Modifier.size(64.dp)) }
    }
}

@Composable
private fun BellArt() {
    val c = LocalLumaColors.current
    data class R(val doc: String, val label: String, val color: Color)
    val rows = listOf(
        R("U.S. Passport", "90 days", c.textDim),
        R("Apartment Lease", "30 days", c.accent),
        R("Schengen Visa", "7 days", c.warn),
        R("Boarding Pass", "1 day", c.err),
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { r ->
            Row(
                Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x08FFFFFF))
                    .border(1.dp, c.hairline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(r.color))
                Column(Modifier.weight(1f)) {
                    Text(r.doc, fontFamily = LumaUi, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.text)
                    Text("expires in ${r.label}", fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
