package app.lumadocs.kmp.platform

import coil3.ComponentRegistry

/**
 * Registers platform-specific Coil fetchers. Android resolves its gallery `content://` URIs
 * natively, but iOS photos are PHAsset identifiers that Coil cannot load without help.
 */
expect fun ComponentRegistry.Builder.addPlatformComponents(): ComponentRegistry.Builder
