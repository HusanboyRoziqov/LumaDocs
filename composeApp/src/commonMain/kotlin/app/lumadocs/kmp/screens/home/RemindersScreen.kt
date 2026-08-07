package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaDisplay
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.ui.DocFileThumb
import app.lumadocs.kmp.ui.ThumbSize
import app.lumadocs.kmp.ui.expiryDaysOf
import app.lumadocs.kmp.ui.formatExpiryLocalized
import app.lumadocs.kmp.viewmodels.DocumentsViewModel
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.days_ago_short
import lumadocs.composeapp.generated.resources.days_short
import lumadocs.composeapp.generated.resources.nav_reminders
import lumadocs.composeapp.generated.resources.rem_expired
import lumadocs.composeapp.generated.resources.rem_later
import lumadocs.composeapp.generated.resources.rem_within_30
import lumadocs.composeapp.generated.resources.rem_within_7
import lumadocs.composeapp.generated.resources.rem_within_90
import lumadocs.composeapp.generated.resources.reminders_empty
import lumadocs.composeapp.generated.resources.reminders_headline
import lumadocs.composeapp.generated.resources.stat_due_30
import lumadocs.composeapp.generated.resources.stat_expired
import lumadocs.composeapp.generated.resources.stat_tracked
import lumadocs.composeapp.generated.resources.today_short
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RemindersScreen(
    vm: DocumentsViewModel,
    onOpenDoc: (DriveFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLumaColors.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val files = state.files ?: emptyList()

    val withExpiry = files.mapNotNull { f -> expiryDaysOf(f)?.let { f to it } }
    val expired = withExpiry.filter { it.second < 0 }.sortedByDescending { it.second }

    fun upcoming(range: IntRange) = withExpiry.filter { it.second in range }.sortedBy { it.second }
    val groups = listOf(
        Triple(stringResource(Res.string.rem_within_7), c.warn, upcoming(0..7)),
        Triple(stringResource(Res.string.rem_within_30), c.accent, upcoming(8..30)),
        Triple(stringResource(Res.string.rem_within_90), c.textDim, upcoming(31..90)),
        Triple(stringResource(Res.string.rem_later), c.textMute, withExpiry.filter { it.second > 90 }.sortedBy { it.second }),
        Triple(stringResource(Res.string.rem_expired), c.err, expired),
    )
    val expiredCount = expired.size
    val under30 = withExpiry.count { it.second in 0..30 }   
    val totalExpiry = withExpiry.size

    Column(modifier.fillMaxSize().background(c.bg).padding(top = 8.dp)) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
            Text(stringResource(Res.string.nav_reminders).uppercase(), fontFamily = LumaMono, fontSize = 10.5.sp, color = c.textMute, letterSpacing = 1.6.sp)
            Text(stringResource(Res.string.reminders_headline), fontFamily = LumaDisplay, fontSize = 34.sp, letterSpacing = (-1).sp, color = c.text, modifier = Modifier.padding(top = 4.dp))
        }

        Row(
            Modifier
                .fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(c.err.copy(alpha = 0.1f), c.err.copy(alpha = 0.02f))))
                .border(1.dp, c.err.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryStat("$expiredCount", stringResource(Res.string.stat_expired), Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(48.dp).background(c.hairline))
            SummaryStat("$under30", stringResource(Res.string.stat_due_30), Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(48.dp).background(c.hairline))
            SummaryStat("$totalExpiry", stringResource(Res.string.stat_tracked), Modifier.weight(1f))
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            groups.forEach { (label, color, docs) ->
                if (docs.isNotEmpty()) {
                    item {
                        Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                            Text("${label.uppercase()} · ${docs.size}", fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = c.textDim, letterSpacing = 1.4.sp)
                        }
                    }
                    items(docs) { (f, days) ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(14.dp))
                                .clickable { onOpenDoc(f) }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            DocFileThumb(file = f, size = ThumbSize.SM)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontFamily = LumaUi, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatExpiryLocalized(f.expiryDate), fontFamily = LumaMono, fontSize = 11.5.sp, color = c.textMute, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            val badge = when {
                                days < 0 -> stringResource(Res.string.days_ago_short, -days)
                                days == 0 -> stringResource(Res.string.today_short)
                                else -> stringResource(Res.string.days_short, days)
                            }
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(badge, fontFamily = LumaMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, letterSpacing = 0.3.sp, maxLines = 1)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
            if (withExpiry.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text(stringResource(Res.string.reminders_empty), color = c.textMute, fontFamily = LumaUi, fontSize = 14.sp) } }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = LocalLumaColors.current
    Column(modifier) {
        Text(value, fontFamily = LumaDisplay, fontSize = 34.sp, lineHeight = 34.sp, color = c.text)
        Text(label, fontFamily = LumaMono, fontSize = 10.5.sp, color = c.textDim, letterSpacing = 0.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}
