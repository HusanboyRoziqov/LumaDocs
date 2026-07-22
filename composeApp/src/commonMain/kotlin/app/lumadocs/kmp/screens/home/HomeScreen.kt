package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumadocs.kmp.CurrentUserState
import app.lumadocs.kmp.VaultEvents
import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaUi
import org.koin.compose.viewmodel.koinViewModel
import app.lumadocs.kmp.viewmodels.DocumentsViewModel

private enum class HomeTab { VAULT, SEARCH, REMINDERS, SETTINGS }

@Composable
internal fun HomeScreen(
    user: FirebaseUser?,
    modifier: Modifier = Modifier,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToScan: () -> Unit = {},
) {
    remember {
        if (CurrentUserState.user.value == null && !user?.userEmail.isNullOrEmpty()) {
            CurrentUserState.set(user)
        }
    }
    val currentUser by CurrentUserState.user.collectAsState()
    val c = LocalLumaColors.current
    var tab by remember { mutableStateOf(HomeTab.VAULT) }
    val docsVm: DocumentsViewModel = koinViewModel()

    // Refresh the vault after a scan-save (which happens on a different nav entry).
    val refreshTick by VaultEvents.refreshTick.collectAsState()
    LaunchedEffect(refreshTick) { if (refreshTick > 0) docsVm.refreshFiles() }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (tab) {
                HomeTab.VAULT -> VaultScreen(
                    vm = docsVm, user = currentUser,
                    onOpenDoc = { onNavigateToDetail(it.id) },
                    onOpenReminders = { tab = HomeTab.REMINDERS },
                )
                HomeTab.SEARCH -> VaultSearchScreen(
                    vm = docsVm,
                    onOpenDoc = { onNavigateToDetail(it.id) },
                    onBack = { tab = HomeTab.VAULT },
                )
                HomeTab.REMINDERS -> RemindersScreen(vm = docsVm, onOpenDoc = { onNavigateToDetail(it.id) })
                HomeTab.SETTINGS -> SettingsScreen(paddingValues = PaddingValues(0.dp), modifier = Modifier.fillMaxSize())
            }
        }

        LumaTabBar(
            active = tab,
            onSelect = { tab = it },
            onScan = onNavigateToScan,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LumaTabBar(
    active: HomeTab,
    onSelect: (HomeTab) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLumaColors.current
    Row(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Color(0xD90B0D12))
            .padding(start = 12.dp, end = 12.dp, bottom = 20.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem(LumaIcons.Vault, "Vault", active == HomeTab.VAULT, Modifier.weight(1f)) { onSelect(HomeTab.VAULT) }
        TabItem(LumaIcons.Search, "Search", active == HomeTab.SEARCH, Modifier.weight(1f)) { onSelect(HomeTab.SEARCH) }
        // Center scan FAB
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .offset(y = (-18).dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(c.accentHi, c.accent)))
                    .clickable(onClick = onScan),
                contentAlignment = Alignment.Center,
            ) { Icon(LumaIcons.Scan, null, tint = Color(0xFF1A1408), modifier = Modifier.size(26.dp)) }
        }
        TabItem(LumaIcons.Bell, "Reminders", active == HomeTab.REMINDERS, Modifier.weight(1f)) { onSelect(HomeTab.REMINDERS) }
        TabItem(LumaIcons.Settings, "Settings", active == HomeTab.SETTINGS, Modifier.weight(1f)) { onSelect(HomeTab.SETTINGS) }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalLumaColors.current
    Column(
        modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, label, tint = if (on) c.accent else c.textMute, modifier = Modifier.size(22.dp))
        Text(label, fontFamily = LumaUi, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (on) c.accent else c.textMute)
    }
}
