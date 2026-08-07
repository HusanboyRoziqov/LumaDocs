package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumadocs.kmp.navigation.AppBackHandler
import app.lumadocs.kmp.platform.PickedFile
import app.lumadocs.kmp.platform.cropImageBytes
import app.lumadocs.kmp.theme.nBlack400
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.utils.decodeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.cancel
import lumadocs.composeapp.generated.resources.done
import lumadocs.composeapp.generated.resources.close
import lumadocs.composeapp.generated.resources.crop
import lumadocs.composeapp.generated.resources.send_count
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Telegram-style media editor: shows the selected images as a bottom filmstrip, lets the
 * user crop each one, then sends the (cropped) set for upload.
 */
@Composable
internal fun MediaEditorScreen(
    initial: List<PickedFile>,
    onSend: (List<PickedFile>) -> Unit,
    onCancel: () -> Unit,
) {
    var items by remember { mutableStateOf(initial) }
    var index by remember { mutableStateOf(0) }
    var cropMode by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val current = items.getOrNull(index)
    if (current == null) {
        onCancel()
        return
    }

    val bitmap by produceState<ImageBitmap?>(null, current.bytes) {
        value = withContext(Dispatchers.Default) { decodeImageBitmap(current.bytes) }
    }

    // Normalized crop rect; reset when entering crop mode or switching image.
    var crop by remember(index, cropMode) { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }

    // Back leaves crop mode first, then leaves the editor.
    AppBackHandler { if (cropMode) cropMode = false else onCancel() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cropMode) {
                    TextButton(onClick = { cropMode = false }) {
                        Text(stringResource(Res.string.cancel), color = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            val bmp = bitmap ?: return@TextButton
                            val c = crop
                            val file = current
                            scope.launch {
                                busy = true
                                val newBytes = cropImageBytes(
                                    file.bytes, c.left, c.top, c.right, c.bottom, file.mimeType
                                )
                                busy = false
                                if (newBytes != null) {
                                    items = items.toMutableList().also {
                                        it[index] = file.copy(bytes = newBytes)
                                    }
                                }
                                cropMode = false
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.done), color = nBrand100, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.close), tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { cropMode = true }) {
                        Icon(Icons.Filled.Crop, contentDescription = stringResource(Res.string.crop), tint = Color.White)
                    }
                }
            }

            // Main preview / crop area
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val bmp = bitmap
                when {
                    bmp == null -> CircularProgressIndicator(color = nBrand100)
                    cropMode -> CropArea(bmp, crop) { crop = it }
                    else -> Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (!cropMode) {
                // Bottom filmstrip of all picked images
                if (items.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(items, key = { i, _ -> i }) { i, item ->
                            ThumbImage(
                                bytes = item.bytes,
                                selected = i == index,
                                onClick = { index = i }
                            )
                        }
                    }
                }

                Button(
                    onClick = { onSend(items) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = nBrand100,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.send_count, items.size),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (busy) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = nBrand100)
            }
        }
    }
}

@Composable
private fun ThumbImage(bytes: ByteArray, selected: Boolean, onClick: () -> Unit) {
    val bmp by produceState<ImageBitmap?>(null, bytes) {
        value = withContext(Dispatchers.Default) { decodeImageBitmap(bytes) }
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(nBlack400)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) nBrand100 else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        bmp?.let {
            Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun CropArea(bitmap: ImageBitmap, crop: Rect, onCropChange: (Rect) -> Unit) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val ar = bitmap.width.toFloat() / bitmap.height.toFloat()
        val dispW: Float
        val dispH: Float
        if (cw / ch > ar) {
            dispH = ch
            dispW = ch * ar
        } else {
            dispW = cw
            dispH = cw / ar
        }
        Box(
            modifier = Modifier.size(
                width = with(density) { dispW.toDp() },
                height = with(density) { dispH.toDp() }
            )
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            CropRectOverlay(dispW, dispH, crop, onCropChange)
        }
    }
}

@Composable
private fun CropRectOverlay(dispW: Float, dispH: Float, crop: Rect, onChange: (Rect) -> Unit) {
    val density = LocalDensity.current
    val cropState = rememberUpdatedState(crop)
    val minNorm = 0.12f

    val l = crop.left * dispW
    val t = crop.top * dispH
    val r = crop.right * dispW
    val b = crop.bottom * dispH

    Canvas(modifier = Modifier.fillMaxSize()) {
        val dim = Color.Black.copy(alpha = 0.5f)
        drawRect(dim, Offset(0f, 0f), Size(dispW, t))
        drawRect(dim, Offset(0f, b), Size(dispW, dispH - b))
        drawRect(dim, Offset(0f, t), Size(l, b - t))
        drawRect(dim, Offset(r, t), Size(dispW - r, b - t))
        drawRect(
            color = Color.White,
            topLeft = Offset(l, t),
            size = Size(r - l, b - t),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Interior move
    Box(
        modifier = Modifier
            .offset { IntOffset(l.roundToInt(), t.roundToInt()) }
            .size(with(density) { (r - l).toDp() }, with(density) { (b - t).toDp() })
            .pointerInput(dispW, dispH) {
                detectDragGestures { _, drag ->
                    val c = cropState.value
                    val dx = (drag.x / dispW).coerceIn(-c.left, 1f - c.right)
                    val dy = (drag.y / dispH).coerceIn(-c.top, 1f - c.bottom)
                    onChange(Rect(c.left + dx, c.top + dy, c.right + dx, c.bottom + dy))
                }
            }
    )

    CornerHandle(l, t, density) { dx, dy ->
        val c = cropState.value
        onChange(
            Rect(
                (c.left + dx / dispW).coerceIn(0f, c.right - minNorm),
                (c.top + dy / dispH).coerceIn(0f, c.bottom - minNorm),
                c.right,
                c.bottom
            )
        )
    }
    CornerHandle(r, t, density) { dx, dy ->
        val c = cropState.value
        onChange(
            Rect(
                c.left,
                (c.top + dy / dispH).coerceIn(0f, c.bottom - minNorm),
                (c.right + dx / dispW).coerceIn(c.left + minNorm, 1f),
                c.bottom
            )
        )
    }
    CornerHandle(l, b, density) { dx, dy ->
        val c = cropState.value
        onChange(
            Rect(
                (c.left + dx / dispW).coerceIn(0f, c.right - minNorm),
                c.top,
                c.right,
                (c.bottom + dy / dispH).coerceIn(c.top + minNorm, 1f)
            )
        )
    }
    CornerHandle(r, b, density) { dx, dy ->
        val c = cropState.value
        onChange(
            Rect(
                c.left,
                c.top,
                (c.right + dx / dispW).coerceIn(c.left + minNorm, 1f),
                (c.bottom + dy / dispH).coerceIn(c.top + minNorm, 1f)
            )
        )
    }
}

@Composable
private fun CornerHandle(cx: Float, cy: Float, density: Density, onDrag: (dx: Float, dy: Float) -> Unit) {
    val halfPx = with(density) { 14.dp.toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((cx - halfPx).roundToInt(), (cy - halfPx).roundToInt()) }
            .size(28.dp)
            .pointerInput(Unit) {
                detectDragGestures { _, drag -> onDrag(drag.x, drag.y) }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(14.dp).background(Color.White, CircleShape))
    }
}
