package app.lumadocs.kmp.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue
import kotlin.concurrent.Volatile

/**
 * NWPathMonitor only reports reachability asynchronously, so the latest status is cached
 * to satisfy [isOnline]'s synchronous contract. It starts optimistic: the first path
 * update lands within milliseconds of the monitor starting, and reporting offline during
 * that gap would wrongly block the first request after launch.
 */
@OptIn(ExperimentalForeignApi::class)
private object Reachability {
    @Volatile
    var online: Boolean = true
        private set

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            online = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }
}

actual fun isOnline(): Boolean = Reachability.online
