package app.lumadocs.kmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.utils.PreviewCache
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.all
import lumadocs.composeapp.generated.resources.items_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath

// ─────────────────────────────────────────────────────────────
// Doc thumbnail — stylized "paper" card (ports primitives.jsx DocThumb)
// ─────────────────────────────────────────────────────────────

enum class ThumbSize(val w: Dp, val h: Dp, val bandH: Dp) {
    SM(56.dp, 74.dp, 20.dp),
    MD(134.dp, 176.dp, 28.dp),
    LG(168.dp, 220.dp, 36.dp),
}

@Composable
fun DocThumb(
    category: DocCategory,
    modifier: Modifier = Modifier,
    code: String? = null,
    pages: Int = 1,
    hasPhoto: Boolean = false,
    expiring: Boolean = false,
    size: ThumbSize = ThumbSize.MD,
) {
    val paper = Brush.linearGradient(listOf(Color(0xFFF5F1E8), Color(0xFFE8E2D4)))
    Box(modifier = modifier.size(width = size.w, height = size.h)) {
        // Stacked-pages shadow effect for multi-page docs.
        if (pages > 1) {
            Box(
                Modifier.size(size.w, size.h).offset(6.dp, 6.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Color(0xFF2A2520))
            )
            Box(
                Modifier.size(size.w, size.h).offset(3.dp, 3.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A3530))
            )
        }
        // Front paper.
        Box(
            Modifier.size(size.w, size.h).clip(RoundedCornerShape(10.dp)).background(paper)
        ) {
            // Header band with the category code.
            Box(
                Modifier.fillMaxWidth().height(size.bandH)
                    .background(category.hue.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (size != ThumbSize.SM) {
                    Text(
                        text = (code ?: category.key.take(3)).uppercase(),
                        fontFamily = LumaMono,
                        fontSize = if (size == ThumbSize.LG) 9.sp else 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Photo box.
            if (hasPhoto) {
                val pw = if (size == ThumbSize.LG) 50.dp else 38.dp
                val ph = if (size == ThumbSize.LG) 60.dp else 46.dp
                Box(
                    Modifier
                        .offset(10.dp, if (size == ThumbSize.LG) 52.dp else 40.dp)
                        .size(pw, ph)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF8B7D6B), Color(0xFF5C4E3C))))
                )
            }
            // Content lines.
            if (size != ThumbSize.SM) {
                val startX = if (hasPhoto) (if (size == ThumbSize.LG) 70.dp else 56.dp) else 10.dp
                val topY = if (size == ThumbSize.LG) 56.dp else 44.dp
                val widths = listOf(0.7f, 0.9f, 0.5f, 0.8f, 0.6f).take(if (size == ThumbSize.LG) 5 else 3)
                Column(
                    Modifier.offset(startX, topY).width(size.w - startX - 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    widths.forEach { w ->
                        Box(
                            Modifier.fillMaxWidth(w).height(3.dp)
                                .clip(RoundedCornerShape(1.dp)).background(Color.Black.copy(alpha = 0.18f))
                        )
                    }
                }
            }
        }
        // Page-count badge.
        if (pages > 1 && size != ThumbSize.SM) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .clip(RoundedCornerShape(999.dp)).background(Color(0xE60B0D12))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "$pages", fontFamily = LumaMono, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFFF5F3EE),
                )
            }
        }
        // Expiring dot.
        if (expiring && size != ThumbSize.SM) {
            Box(
                Modifier.align(Alignment.TopStart).padding(8.dp)
                    .size(10.dp).clip(CircleShape).background(Color(0xFFE89B5D))
            )
        }
    }
}

/**
 * Thumbnail for a real [DriveFile]: shows the document's actual photo (Drive thumbnail, cropped to
 * fill) with the category-colored band pinned across the top. Falls back to the stylized paper look
 * when there's no image thumbnail. Use this in every list; use [DocThumb] only for mock/no-file art.
 */
@Composable
fun DocFileThumb(
    file: DriveFile,
    modifier: Modifier = Modifier,
    size: ThumbSize = ThumbSize.MD,
    fillWidth: Boolean = false,
) {
    // Derived values are memoized so scrolling doesn't re-parse dates / rebuild brushes per frame.
    val category = remember(file.category, file.name) { categoryOf(file) }
    val expiring = remember(file.expiryDate) { (expiryDaysOf(file) ?: Int.MAX_VALUE) in 0..30 }
    val isImage = file.mimeType.startsWith("image/")
    val isPdf = file.mimeType.contains("pdf", ignoreCase = true)
    val paper = remember { Brush.linearGradient(listOf(Color(0xFFF5F1E8), Color(0xFFE8E2D4))) }

    // The locally cached original, resolved off the main thread. Preferred over Drive's thumbnail:
    // full quality, survives Drive's short-lived thumbnail links, and works offline. Coil
    // downsamples the decode to the cell, so a multi-MB photo stays cheap. Encrypted files have no
    // usable Drive thumbnail at all — this is the only way they ever show their real picture.
    val cachedPath by produceState<String?>(null, file.id, isImage) {
        value = if (isImage) withContext(Dispatchers.Default) { PreviewCache.localPath(file.id) } else null
    }
    // Drive renders a first-page preview for PDFs and office files too, so anything with a
    // thumbnail link gets a real picture in the list — not just photos.
    val hasThumb = cachedPath != null || !file.thumbnailLink.isNullOrBlank()

    val outer = if (fillWidth) modifier.fillMaxWidth().aspectRatio(size.w.value / size.h.value)
    else modifier.size(width = size.w, height = size.h)
    Box(modifier = outer) {
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(10.dp)).background(paper)) {
            if (hasThumb) {
                // No isOnline() gate here: it made a blocking ConnectivityManager call per item per
                // frame while scrolling (the flicker). Coil serves cached thumbnails offline anyway.
                // Stable cache keys + placeholder-from-cache mean a re-scrolled item reuses its
                // cached bitmap instantly, with no blank frame / reload flicker. The key tracks the
                // source so the small thumbnail never masks the full-quality upgrade.
                val ctx = LocalPlatformContext.current
                val request = remember(file.id, file.thumbnailLink, cachedPath) {
                    val key = if (cachedPath != null) "full_${file.id}" else file.id
                    ImageRequest.Builder(ctx)
                        .data(cachedPath?.toPath() ?: sizedThumb(file.thumbnailLink, 400))
                        .memoryCacheKey(key)
                        .diskCacheKey(key)
                        // Placeholder always points at the thumbnail entry, so the upgrade to the
                        // full-quality decode swaps in over the small image instead of a blank cell.
                        .placeholderMemoryCacheKey(file.id)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else if (!isImage) {
                // A document with no preview yet (offline, or Drive hasn't rendered one): show what
                // kind of file it is instead of blank paper, at every thumbnail size.
                Column(
                    Modifier.matchParentSize().padding(top = size.bandH),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        if (isPdf) LumaIcons.Pdf else LumaIcons.Page,
                        null,
                        tint = Color(0xFF8A7F6B),
                        modifier = Modifier.size(if (size == ThumbSize.LG) 44.dp else if (size == ThumbSize.SM) 20.dp else 34.dp),
                    )
                    if (size != ThumbSize.SM) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            localizedMimeLabel(file.mimeType).uppercase(),
                            fontFamily = LumaMono,
                            fontSize = if (size == ThumbSize.LG) 9.sp else 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8A7F6B),
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else if (size != ThumbSize.SM) {
                // Paper "content lines" fallback for a photo whose thumbnail hasn't arrived.
                Column(
                    Modifier.matchParentSize().padding(start = 10.dp, end = 10.dp, top = size.bandH + 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(0.7f, 0.9f, 0.5f, 0.8f).take(if (size == ThumbSize.LG) 4 else 2).forEach { w ->
                        Box(Modifier.fillMaxWidth(w).height(3.dp).clip(RoundedCornerShape(1.dp)).background(Color.Black.copy(alpha = 0.18f)))
                    }
                }
            }
            // Category band pinned on top of the picture.
            Box(
                Modifier.fillMaxWidth().height(size.bandH)
                    .background(category.hue.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (size != ThumbSize.SM) {
                    Text(
                        category.localizedLabel().uppercase(),
                        fontFamily = LumaMono,
                        fontSize = if (size == ThumbSize.LG) 9.sp else 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f),
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (expiring && size != ThumbSize.SM) {
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).clip(CircleShape).background(Color(0xFFE89B5D)))
        }
    }
}

/**
 * A lightweight **folder** tile — just an icon and item count, no image loading. Used for
 * folder documents in lists so scrolling stays fast (folders don't decode/fetch photos).
 */
@Composable
fun DocFolderThumb(
    count: Int,
    modifier: Modifier = Modifier,
    size: ThumbSize = ThumbSize.MD,
    fillWidth: Boolean = false,
) {
    val c = LocalLumaColors.current
    val outer = if (fillWidth) modifier.fillMaxWidth().aspectRatio(size.w.value / size.h.value)
    else modifier.size(width = size.w, height = size.h)
    Box(
        outer.clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(c.bg3, c.bg2)))
            .border(1.dp, c.accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                LumaIcons.Folder, null, tint = c.accent,
                modifier = Modifier.size(if (size == ThumbSize.LG) 56.dp else if (size == ThumbSize.SM) 24.dp else 44.dp),
            )
            if (size != ThumbSize.SM) {
                Spacer(Modifier.height(8.dp))
                Text(pluralStringResource(Res.plurals.items_count, count, count), fontFamily = LumaMono, fontSize = 10.sp, color = c.textMute, letterSpacing = 0.3.sp)
            }
        }
        if (size == ThumbSize.SM && count > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).clip(RoundedCornerShape(999.dp))
                    .background(c.accentDim).padding(horizontal = 6.dp, vertical = 1.dp),
            ) { Text("$count", fontFamily = LumaMono, fontSize = 9.sp, color = c.accentHi) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Category chip
// ─────────────────────────────────────────────────────────────

@Composable
fun CategoryChip(
    category: DocCategory,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val c = LocalLumaColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) category.tint else Color(0x0AFFFFFF))
            .border(
                1.dp,
                if (active) category.hue.copy(alpha = 0.33f) else c.hairline,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(category.hue))
        Text(
            category.localizedLabel(),
            fontFamily = LumaUi, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (active) c.text else c.textDim,
        )
        if (count != null) {
            Text("$count", fontFamily = LumaMono, fontSize = 12.sp, color = c.textMute)
        }
    }
}

/** Non-category "All" chip used in the vault filter row. */
@Composable
fun AllChip(active: Boolean, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalLumaColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) c.accent else Color(0x0AFFFFFF))
            .border(1.dp, if (active) c.accent else c.hairline, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(Res.string.all), fontFamily = LumaUi, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (active) Color(0xFF1A1408) else c.textDim,
        )
        Text(
            "$count", fontFamily = LumaUi, fontSize = 13.sp,
            color = if (active) Color(0xFF1A1408).copy(alpha = 0.6f) else c.textMute,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Pill button
// ─────────────────────────────────────────────────────────────

enum class PillVariant { PRIMARY, GHOST, QUIET, DARK }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillVariant = PillVariant.PRIMARY,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val c = LocalLumaColors.current
    val (bg, fg, border) = when (variant) {
        PillVariant.PRIMARY -> Triple(c.accent, Color(0xFF1A1408), null)
        PillVariant.GHOST -> Triple(Color(0x0FFFFFFF), c.text, c.hairline2)
        PillVariant.QUIET -> Triple(Color.Transparent, c.textDim, null)
        PillVariant.DARK -> Triple(Color(0x80000000), c.text, c.hairline2)
    }
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(999.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontFamily = LumaUi, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = fg)
        if (trailingIcon != null) {
            Spacer(Modifier.width(10.dp))
            Icon(trailingIcon, null, tint = fg, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Section label
// ─────────────────────────────────────────────────────────────

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    right: (@Composable () -> Unit)? = null,
) {
    val c = LocalLumaColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text.uppercase(), fontFamily = LumaMono, fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium, color = c.textMute, letterSpacing = 1.4.sp,
        )
        right?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────
// Toggle
// ─────────────────────────────────────────────────────────────

@Composable
fun LumaToggle(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalLumaColors.current
    Box(
        modifier = modifier
            .size(46.dp, 28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) c.accent else Color(0x26FFFFFF))
            .clickable { onChange(!on) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = if (on) 20.dp else 2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Meta row (detail screen)
// ─────────────────────────────────────────────────────────────

@Composable
fun MetaRow(label: String, isLast: Boolean = false, value: @Composable () -> Unit) {
    val c = LocalLumaColors.current
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label, fontFamily = LumaMono, fontSize = 10.5.sp,
                color = c.textMute, letterSpacing = 1.2.sp,
            )
            value()
        }
        if (!isLast) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

// ─────────────────────────────────────────────────────────────
// Settings group + row
// ─────────────────────────────────────────────────────────────

@Composable
fun SettingsGroup(label: String, content: @Composable () -> Unit) {
    val c = LocalLumaColors.current
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(
            label.uppercase(), fontFamily = LumaMono, fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium, color = c.textMute, letterSpacing = 1.4.sp,
            modifier = Modifier.padding(start = 36.dp, end = 36.dp, bottom = 10.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(c.bg2)
                .border(1.dp, c.hairline, RoundedCornerShape(18.dp)),
        ) { content() }
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    detail: String? = null,
    chevron: Boolean = false,
    isLast: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LocalLumaColors.current
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(c.accentDim),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = c.accent, modifier = Modifier.size(17.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, fontFamily = LumaUi, fontSize = 14.5.sp, color = c.text)
                if (sub != null) {
                    Text(
                        sub, fontFamily = LumaMono, fontSize = 11.5.sp,
                        color = c.textMute, letterSpacing = 0.2.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (detail != null && trailing == null) {
                Text(detail, fontFamily = LumaUi, fontSize = 13.sp, color = c.textDim)
            }
            trailing?.invoke()
            if (chevron) Icon(app.lumadocs.kmp.icons.LumaIcons.Chevron, null, tint = c.textMute, modifier = Modifier.size(16.dp))
        }
        if (!isLast) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

/**
 * Round account avatar: shows the signed-in user's photo when [photoUrl] is available, otherwise
 * falls back to their initial on the brand gradient.
 */
@Composable
fun InitialAvatar(
    letter: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 44.dp,
    fontSize: Int = 18,
    photoUrl: String? = null,
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFC9A870), Color(0xFF7A5F3A)))),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                letter.take(1).uppercase(),
                fontFamily = app.lumadocs.kmp.theme.LumaDisplay,
                fontSize = fontSize.sp,
                color = Color(0xFF1A1408),
            )
        }
    }
}
