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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.ui.DocFileThumb
import app.lumadocs.kmp.ui.SectionLabel
import app.lumadocs.kmp.ui.ThumbSize
import app.lumadocs.kmp.ui.categoryOf
import app.lumadocs.kmp.ui.expiryDaysOf
import app.lumadocs.kmp.viewmodels.DocumentsViewModel

private enum class Smart(val label: String) {
    EXPIRING("Expiring this month"),
    PHOTOS("Photos"),
    ENCRYPTED("Encrypted"),
    HAS_EXPIRY("Has expiry set"),
}

@Composable
internal fun VaultSearchScreen(
    vm: DocumentsViewModel,
    onOpenDoc: (DriveFile) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLumaColors.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val files = state.files ?: emptyList()

    var query by remember { mutableStateOf("") }
    var smart by remember { mutableStateOf<Smart?>(null) }
    val recents by vm.recentSearches.collectAsStateWithLifecycle()

    val results: List<DriveFile> = when {
        query.isNotBlank() -> files.filter {
            it.name.contains(query, true) ||
                categoryOf(it).label.contains(query, true) ||
                (it.description ?: "").contains(query, true)
        }
        smart != null -> files.filter {
            when (smart) {
                Smart.EXPIRING -> (expiryDaysOf(it) ?: 999) in 0..30
                Smart.PHOTOS -> it.mimeType.startsWith("image/")
                Smart.ENCRYPTED -> it.encrypted
                Smart.HAS_EXPIRY -> !it.expiryDate.isNullOrBlank()
                null -> true
            }
        }
        else -> emptyList()
    }
    val showResults = query.isNotBlank() || smart != null

    Column(modifier.fillMaxSize().background(c.bg).padding(top = 8.dp)) {
        // Search bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.hairline2, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(LumaIcons.Search, null, tint = c.textMute, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Search name, tag, or text inside…", color = c.textMute, fontFamily = LumaUi, fontSize = 15.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it; smart = null },
                        singleLine = true, textStyle = TextStyle(color = c.text, fontFamily = LumaUi, fontSize = 15.sp),
                        cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.addRecentSearch(query) }),
                    )
                }
                if (query.isNotEmpty()) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0x1AFFFFFF)).clickable { query = "" }, contentAlignment = Alignment.Center) {
                        Icon(LumaIcons.Close, null, tint = c.text, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Text("Cancel", color = c.accent, fontFamily = LumaUi, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { query = ""; smart = null; onBack() })
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            if (!showResults) {
                item {
                    SectionLabel("Recent", right = {
                        if (recents.isNotEmpty()) {
                            Text("Clear", color = c.accent, fontFamily = LumaUi, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { vm.clearRecentSearches() })
                        }
                    })
                    Spacer(Modifier.height(8.dp))
                    if (recents.isEmpty()) {
                        Text(
                            "Your recent searches will show up here.",
                            color = c.textMute, fontFamily = LumaUi, fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    } else {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            recents.forEach { r ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { query = r; vm.addRecentSearch(r) }.padding(vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(LumaIcons.Search, null, tint = c.textMute, modifier = Modifier.size(16.dp))
                                    Text(r, Modifier.weight(1f), color = c.text, fontFamily = LumaUi, fontSize = 15.sp)
                                    Icon(LumaIcons.Forward, null, tint = c.textMute, modifier = Modifier.size(14.dp))
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Smart Suggestions"); Spacer(Modifier.height(12.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Smart.entries) { s ->
                            Row(
                                Modifier.clip(RoundedCornerShape(999.dp)).background(c.accentDim).border(1.dp, c.accent.copy(alpha = 0.2f), RoundedCornerShape(999.dp)).clickable { smart = s; query = "" }.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(LumaIcons.Sparkle, null, tint = c.accentHi, modifier = Modifier.size(12.dp))
                                Text(s.label, color = c.accentHi, fontFamily = LumaUi, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            } else {
                item { SectionLabel("${results.size} match${if (results.size != 1) "es" else ""}"); Spacer(Modifier.height(12.dp)) }
                items(results) { f ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(14.dp))
                            .clickable { vm.addRecentSearch(query); onOpenDoc(f) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DocFileThumb(file = f, size = ThumbSize.SM)
                        Column(Modifier.weight(1f)) {
                            Text(highlight(f.name, query, c.accent), fontFamily = LumaUi, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.text)
                            Text("${categoryOf(f).label} · match", fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute, modifier = Modifier.padding(top = 3.dp))
                        }
                        Icon(LumaIcons.Chevron, null, tint = c.textMute, modifier = Modifier.size(14.dp))
                    }
                }
                if (results.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("No documents match", color = c.textMute, fontFamily = LumaUi, fontSize = 14.sp) } }
                }
            }
        }
    }
}

private fun highlight(text: String, q: String, accent: Color) = buildAnnotatedString {
    val i = if (q.isBlank()) -1 else text.indexOf(q, ignoreCase = true)
    if (i < 0) { append(text); return@buildAnnotatedString }
    append(text.substring(0, i))
    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) { append(text.substring(i, i + q.length)) }
    append(text.substring(i + q.length))
}
