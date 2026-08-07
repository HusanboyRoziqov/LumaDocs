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
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.action_continue
import lumadocs.composeapp.generated.resources.days_count
import lumadocs.composeapp.generated.resources.get_started
import lumadocs.composeapp.generated.resources.onb1_body
import lumadocs.composeapp.generated.resources.onb1_title
import lumadocs.composeapp.generated.resources.onb2_body
import lumadocs.composeapp.generated.resources.onb2_kicker
import lumadocs.composeapp.generated.resources.onb2_title
import lumadocs.composeapp.generated.resources.onb3_body
import lumadocs.composeapp.generated.resources.onb3_kicker
import lumadocs.composeapp.generated.resources.onb3_title
import lumadocs.composeapp.generated.resources.onb4_body
import lumadocs.composeapp.generated.resources.onb4_kicker
import lumadocs.composeapp.generated.resources.onb4_title
import lumadocs.composeapp.generated.resources.onb_brand
import lumadocs.composeapp.generated.resources.onb_expires_in
import lumadocs.composeapp.generated.resources.onb_skip
import lumadocs.composeapp.generated.resources.sample_boarding
import lumadocs.composeapp.generated.resources.sample_lease
import lumadocs.composeapp.generated.resources.sample_passport
import lumadocs.composeapp.generated.resources.sample_visa
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private data class OnbSlide(
    val kicker: StringResource,
    val title: StringResource,
    val body: StringResource,
    val art: String,
)

/** Slide copy lives in string resources so the intro speaks the user's language. */
private val slides = listOf(
    OnbSlide(Res.string.onb_brand, Res.string.onb1_title, Res.string.onb1_body, "vault"),
    OnbSlide(Res.string.onb2_kicker, Res.string.onb2_title, Res.string.onb2_body, "scan"),
    OnbSlide(Res.string.onb3_kicker, Res.string.onb3_title, Res.string.onb3_body, "lock"),
    OnbSlide(Res.string.onb4_kicker, Res.string.onb4_title, Res.string.onb4_body, "bell"),
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
                stringResource(Res.string.onb_skip), color = c.textMute, fontFamily = LumaUi, fontSize = 14.sp,
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
            Text(stringResource(s.kicker), fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = c.accent, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(s.title), fontFamily = LumaDisplay, fontStyle = FontStyle.Italic, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-1.5).sp, color = c.text)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(s.body), fontFamily = LumaUi, fontSize = 15.sp, lineHeight = 22.sp, color = c.textDim)
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
                    text = stringResource(if (isLast) Res.string.get_started else Res.string.action_continue),
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
        R(stringResource(Res.string.sample_passport), pluralStringResource(Res.plurals.days_count, 90, 90), c.textDim),
        R(stringResource(Res.string.sample_lease), pluralStringResource(Res.plurals.days_count, 30, 30), c.accent),
        R(stringResource(Res.string.sample_visa), pluralStringResource(Res.plurals.days_count, 7, 7), c.warn),
        R(stringResource(Res.string.sample_boarding), pluralStringResource(Res.plurals.days_count, 1, 1), c.err),
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
                    Text(stringResource(Res.string.onb_expires_in, r.label), fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
