package app.lumadocs.kmp.platform

import coil3.ComponentRegistry

// Coil already handles the content:// URIs that loadGalleryImages returns on Android.
actual fun ComponentRegistry.Builder.addPlatformComponents(): ComponentRegistry.Builder = this
