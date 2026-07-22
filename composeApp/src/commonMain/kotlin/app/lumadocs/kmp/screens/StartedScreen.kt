package app.lumadocs.kmp.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.icons.GoogleIcon
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaDisplay
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.viewmodels.StartScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartedScreen(
    viewModel: StartScreenViewModel = koinViewModel(),
    gotoMainScreen: (FirebaseUser?) -> Unit,
) {
    val c = LocalLumaColors.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigateToHomeFlow.collect { user -> gotoMainScreen(user) }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = c.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        Column(
            Modifier.fillMaxSize().background(c.bg).safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Brand mark
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(c.bg3, c.bg2)))
                        .border(1.dp, c.accent.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("L", fontFamily = LumaDisplay, fontStyle = FontStyle.Italic, fontSize = 44.sp, color = c.accent)
                }
                Spacer(Modifier.height(28.dp))
                Text("CHOOSE HOW TO UNLOCK", fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = c.textMute, letterSpacing = 1.8.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    "Sign in, or go private.",
                    fontFamily = LumaDisplay, fontStyle = FontStyle.Italic, fontSize = 36.sp,
                    lineHeight = 38.sp, letterSpacing = (-1).sp, color = c.text, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Sign in to sync encrypted backups across your devices. Guest mode keeps everything on this phone only.",
                    fontFamily = LumaUi, fontSize = 14.sp, lineHeight = 21.sp, color = c.textDim, textAlign = TextAlign.Center,
                )
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Continue with Google
                Row(
                    Modifier
                        .fillMaxWidth().height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .clickable(enabled = !state.loading) { viewModel.checkCurrentUser() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Color(0xFF1A1408), strokeWidth = 3.dp)
                    } else {
                        Icon(GoogleIcon, null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Continue with Google", color = Color(0xFF1A1408), fontFamily = LumaUi, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                // Continue as Guest
                Row(
                    Modifier
                        .fillMaxWidth().height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, c.hairline2, RoundedCornerShape(16.dp))
                        .clickable { gotoMainScreen(null) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(LumaIcons.Lock, null, tint = c.textDim, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Continue as Guest", color = c.text, fontFamily = LumaUi, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Text(
                    "By continuing you agree to our Terms and Privacy Policy. Your documents are encrypted before they ever leave your device.",
                    fontFamily = LumaUi, fontSize = 11.5.sp, lineHeight = 17.sp, color = c.textMute,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
