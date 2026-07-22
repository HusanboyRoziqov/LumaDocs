package app.lumadocs.kmp.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import app.lumadocs.kmp.data.FirebaseUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Started : Route

    @Serializable
    data class Home(val user: FirebaseUser? = null) : Route

    @Serializable
    data class DocumentDetail(val fileId: String) : Route

    @Serializable
    data object Scan : Route
}

internal val NavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Onboarding::class)
            subclass(Route.Started::class)
            subclass(Route.Home::class)
            subclass(Route.DocumentDetail::class)
            subclass(Route.Scan::class)
        }
    }
}
