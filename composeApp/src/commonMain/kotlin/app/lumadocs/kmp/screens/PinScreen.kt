package app.lumadocs.kmp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lumadocs.kmp.PinViewModel
import app.lumadocs.kmp.data_store.rememberDataStore
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nBlack300
import app.lumadocs.kmp.theme.nBlack400
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.theme.nWhite100
import app.lumadocs.kmp.theme.nWhite600
import kotlinx.coroutines.launch
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.pin_confirm_subtitle
import lumadocs.composeapp.generated.resources.pin_confirm_title
import lumadocs.composeapp.generated.resources.pin_create_subtitle
import lumadocs.composeapp.generated.resources.pin_create_title
import lumadocs.composeapp.generated.resources.pin_enter_subtitle
import lumadocs.composeapp.generated.resources.pin_enter_title
import lumadocs.composeapp.generated.resources.pin_mismatch
import lumadocs.composeapp.generated.resources.pin_wrong
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val PIN_LENGTH = 4

enum class PinMode { UNLOCK, SETUP }

@Composable
fun PinScreen(
    mode: PinMode,
    onUnlock: () -> Unit = {},
    onPinCreated: () -> Unit = {},
    onCancel: (() -> Unit)? = null,
) {
    val dataStore = rememberDataStore()
    val viewModel = viewModel { PinViewModel(dataStore) }
    val scope = rememberCoroutineScope()

    var entered by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) } // SETUP: first entry to confirm
    var error by remember { mutableStateOf<String?>(null) }
    val shake = remember { Animatable(0f) }

    val wrongPin = stringResource(Res.string.pin_wrong)
    val mismatch = stringResource(Res.string.pin_mismatch)

    suspend fun doShake() {
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 380
                (-18f) at 60; 18f at 120; (-13f) at 200; 13f at 270; (-6f) at 330; 0f at 380
            },
        )
    }

    LaunchedEffect(entered) {
        if (entered.length < PIN_LENGTH) return@LaunchedEffect
        when (mode) {
            PinMode.UNLOCK -> {
                if (viewModel.verifyPin(entered)) {
                    onUnlock()
                } else {
                    error = wrongPin
                    doShake()
                    entered = ""
                }
            }

            PinMode.SETUP -> {
                val first = firstPin
                if (first == null) {
                    firstPin = entered
                    entered = ""
                } else if (entered == first) {
                    viewModel.setPin(entered)
                    onPinCreated()
                } else {
                    error = mismatch
                    firstPin = null
                    doShake()
                    entered = ""
                }
            }
        }
    }

    val title = when {
        mode == PinMode.UNLOCK -> stringResource(Res.string.pin_enter_title)
        firstPin == null -> stringResource(Res.string.pin_create_title)
        else -> stringResource(Res.string.pin_confirm_title)
    }
    val subtitle = when {
        mode == PinMode.UNLOCK -> stringResource(Res.string.pin_enter_subtitle)
        firstPin == null -> stringResource(Res.string.pin_create_subtitle)
        else -> stringResource(Res.string.pin_confirm_subtitle)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(nBlack100)
            // Swallow every pointer event so taps/scrolls can't reach the
            // Home/Documents screen rendered behind this overlay.
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
            if (onCancel != null) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = nWhite600)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Header lock badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(nBrand100.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = nBrand100,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = nWhite100)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            fontSize = 14.sp,
            color = nWhite600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )

        Spacer(Modifier.height(28.dp))

        // PIN dots
        Row(
            modifier = Modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            repeat(PIN_LENGTH) { index ->
                val filled = index < entered.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) nBrand100 else Color.Transparent)
                        .then(
                            if (!filled) Modifier.border(1.5.dp, nBlack300, CircleShape)
                            else Modifier
                        )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error ?: "",
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.weight(1f))

        // Numeric keypad
        val onDigit: (String) -> Unit = { d ->
            error = null
            if (entered.length < PIN_LENGTH) entered += d
        }
        val onBackspace: () -> Unit = {
            error = null
            if (entered.isNotEmpty()) entered = entered.dropLast(1)
        }

        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { d -> KeypadButton(label = d, onClick = { onDigit(d) }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Spacer(Modifier.size(74.dp))
                KeypadButton(label = "0", onClick = { onDigit("0") })
                KeypadButton(onClick = onBackspace) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = nWhite100,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String? = null,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(74.dp)
            .clip(CircleShape)
            .background(nBlack400)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = nBrand100),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else {
            Text(
                text = label ?: "",
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = nWhite100,
            )
        }
    }
}
