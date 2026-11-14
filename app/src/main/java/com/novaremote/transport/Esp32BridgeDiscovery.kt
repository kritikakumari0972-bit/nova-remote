package com.novaremote.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class BridgeCandidate(val name: String, val address: String)

/** Foreground mDNS discovery. Manual private-IP entry remains available as fallback. */
class Esp32BridgeDiscovery(context: Context) {
    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    suspend fun discover(timeoutMillis: Long = 6000): List<BridgeCandidate> = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val finished = AtomicBoolean(false)
            val found = linkedMapOf<String, BridgeCandidate>()
            lateinit var listener: NsdManager.DiscoveryListener
            lateinit var timeout: Runnable

            fun stopSafely() {
                runCatching { manager.stopServiceDiscovery(listener) }
            }
            fun finish(error: Throwable?) {
                if (!finished.compareAndSet(false, true)) return
                handler.removeCallbacks(timeout)
                stopSafely()
                if (!continuation.isActive) return
                if (error == null) continuation.resume(found.values.toList())
                else continuation.resumeWithException(error)
            }
            timeout = Runnable { finish(null) }

            listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    finish(IllegalStateException("mDNS discovery failed with code " + errorCode))
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (!serviceInfo.serviceType.contains("_novaremote._tcp")) return
                    runCatching {
                        manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                            override fun onServiceResolved(info: NsdServiceInfo) {
                                val host = info.host?.hostAddress ?: return
                                val candidate = BridgeCandidate(info.serviceName, "http://" + host + ":" + info.port)
                                found[candidate.address] = candidate
                            }
                        })
                    }
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    found.entries.removeAll { it.value.name == serviceInfo.serviceName }
                }
            }

            continuation.invokeOnCancellation {
                handler.removeCallbacks(timeout)
                if (finished.compareAndSet(false, true)) stopSafely()
            }
            try {
                manager.discoverServices("_novaremote._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
                handler.postDelayed(timeout, timeoutMillis.coerceIn(1500, 15000))
            } catch (exception: Exception) {
                finish(exception)
            }
        }
    }
}
