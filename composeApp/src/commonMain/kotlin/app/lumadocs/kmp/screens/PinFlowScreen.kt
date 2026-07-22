package app.lumadocs.kmp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lumadocs.kmp.Authenticator
import app.lumadocs.kmp.PinFlowRequest
import app.lumadocs.kmp.PinViewModel
import app.lumadocs.kmp.data.Response
import app.lumadocs.kmp.data_store.rememberDataStore
import app.lumadocs.kmp.icons.GoogleIcon
import app.lumadocs.kmp.platform.showToast
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.theme.nWhite100
import app.lumadocs.kmp.theme.nWhite600
import app.lumadocs.kmp.utils.ErrorMessages
import kotlinx.coroutines.launch
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.error_sign_in_failed
import lumadocs.composeapp.generated.resources.pin_changed
import lumadocs.composeapp.generated.resources.pin_created
import lumadocs.composeapp.generated.resources.pin_removed
import lumadocs.composeapp.generated.resources.pin_verify_change
import lumadocs.composeapp.generated.resources.pin_verify_remove
import lumadocs.composeapp.generated.resources.pin_verify_set
import lumadocs.composeapp.generated.resources.pin_verify_title
import lumadocs.composeapp.generated.resources.verify_with_google
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private enum class FlowStep { VERIFY, ENTER_PIN }

/**
 * Full-screen flow (covers the bottom nav) that confirms the user with Google
 * re-authentication before setting, changing, or removing the app PIN.
 */
@Composable
fun PinFlowScreen(request: PinFlowRequest, onDone: () -> Unit) {
    val dataStore = rememberDataStore()
    val pinViewModel = viewModel { PinViewModel(dataStore) }
    val authenticator = koinInject<Authenticator>()
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(FlowStep.VERIFY) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val signInFailed = stringResource(Res.string.error_sign_in_failed)

    // Confirmation toasts shown only on a successful action.
    val createdMsg = stringResource(Res.string.pin_created)
    val changedMsg = stringResource(Res.string.pin_changed)
    val removedMsg = stringResource(Res.string.pin_removed)

    when (step) {
        FlowStep.VERIFY -> ReauthPrompt(
            request = request,
            loading = loading,
            error = error,
            onVerify = {
                scope.launch {
                    loading = true
                    error = null
                    val ok = runCatching { authenticator.login() }
                        .getOrNull() is Response.Success<*>
                    loading = false
                    if (ok) {
                        if (request == PinFlowRequest.DISABLE) {
                            pinViewModel.disableLock()
                            showToast(removedMsg)
                            onDone()
                        } else {
                            step = FlowStep.ENTER_PIN
                        }
                    } else {
                        error = ErrorMessages.forOperation(Res.string.error_sign_in_failed)
                            .ifBlank { signInFailed }
                    }
                }
            },
            onCancel = onDone,
        )

        FlowStep.ENTER_PIN -> PinScreen(
            mode = PinMode.SETUP,
            onPinCreated = {
                showToast(if (request == PinFlowRequest.CREATE) createdMsg else changedMsg)
                onDone()
            },
            onCancel = onDone,
        )
    }
}

@Composable
private fun ReauthPrompt(
    request: PinFlowRequest,
    loading: Boolean,
    error: String?,
    onVerify: () -> Unit,
    onCancel: () -> Unit,
) {
    val subtitle = when (request) {
        PinFlowRequest.CREATE -> stringResource(Res.string.pin_verify_set)
        PinFlowRequest.CHANGE -> stringResource(Res.string.pin_verify_change)
        PinFlowRequest.DISABLE -> stringResource(Res.string.pin_verify_remove)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(nBlack100)
            // Swallow every pointer event so taps can't reach the screen behind this overlay.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = nWhite600)
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(nBrand100.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Shield, null, tint = nBrand100, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(Res.string.pin_verify_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = nWhite100,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            fontSize = 14.sp,
            color = nWhite600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(visible = error != null) {
            Text(error ?: "", color = Color(0xFFFF6B6B), fontSize = 13.sp)
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable(enabled = !loading, onClick = onVerify),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF1A1A1A),
                    strokeWidth = 3.dp,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(GoogleIcon, null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(Res.string.verify_with_google),
                        color = Color(0xFF1A1A1A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
