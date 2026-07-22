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
import app.lumadocs.kmp.ui.formatExpiry
import app.lumadocs.kmp.viewmodels.DocumentsViewModel

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
    val groups = listOf(
        Triple("Within 7 days", c.err, withExpiry.filter { it.second in 0..7 }),
        Triple("Within 30 days", c.warn, withExpiry.filter { it.second in 8..30 }),
        Triple("Within 90 days", c.accent, withExpiry.filter { it.second in 31..90 }),
        Triple("Later", c.textDim, withExpiry.filter { it.second > 90 }),
    )
    val under30 = withExpiry.count { it.second in 0..30 }
    val totalExpiry = withExpiry.size

    Column(modifier.fillMaxSize().background(c.bg).padding(top = 8.dp)) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
            Text("REMINDERS", fontFamily = LumaMono, fontSize = 10.5.sp, color = c.textMute, letterSpacing = 1.6.sp)
            Text("What's expiring.", fontFamily = LumaDisplay, fontSize = 34.sp, letterSpacing = (-1).sp, color = c.text, modifier = Modifier.padding(top = 4.dp))
        }

        // Summary
        Row(
            Modifier
                .fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(c.err.copy(alpha = 0.1f), c.err.copy(alpha = 0.02f))))
                .border(1.dp, c.err.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryStat("$under30", "EXPIRE IN < 30 DAYS", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(48.dp).background(c.hairline))
            SummaryStat("$totalExpiry", "HAVE EXPIRY SET", Modifier.weight(1f))
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
                                Text(formatExpiry(f.expiryDate), fontFamily = LumaMono, fontSize = 11.5.sp, color = c.textMute, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text("${days}d", fontFamily = LumaMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, letterSpacing = 0.3.sp)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
            if (withExpiry.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("No documents have an expiry date yet.", color = c.textMute, fontFamily = LumaUi, fontSize = 14.sp) } }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = LocalLumaColors.current
    Column(modifier) {
        Text(value, fontFamily = LumaDisplay, fontSize = 42.sp, lineHeight = 42.sp, color = c.text)
        Text(label, fontFamily = LumaMono, fontSize = 11.5.sp, color = c.textDim, letterSpacing = 0.4.sp, modifier = Modifier.padding(top = 6.dp))
    }
}
