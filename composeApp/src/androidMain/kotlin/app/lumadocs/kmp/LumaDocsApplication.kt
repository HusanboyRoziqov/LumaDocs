package app.lumadocs.kmp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.lumadocs.kmp.data_store.createDataStore
import app.lumadocs.kmp.di.initKoin
import org.koin.android.ext.koin.androidContext
import java.lang.ref.WeakReference

class LumaDocsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        dataStore = createDataStore(this)
        registerActivityLifecycleCallbacks(ActivityTracker)
        initKoin {
            androidContext(this@LumaDocsApplication)
        }
    }

    companion object {
        lateinit var instance: LumaDocsApplication
            private set

        lateinit var dataStore: DataStore<Preferences>
            private set

        /** The currently resumed Activity, needed for Credential Manager UI. */
        val currentActivity: Activity?
            get() = ActivityTracker.current

        private object ActivityTracker : ActivityLifecycleCallbacks {
            private var ref: WeakReference<Activity>? = null
            val current: Activity? get() = ref?.get()

            override fun onActivityResumed(activity: Activity) {
                ref = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (ref?.get() === activity) ref = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
    }
}
