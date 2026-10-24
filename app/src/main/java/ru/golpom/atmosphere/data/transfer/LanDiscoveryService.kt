/**
 * Поиск устройств учителей в локальной сети через NSD (mDNS) и реклама своего устройства.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.transfer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Singleton
class LanDiscoveryService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _devices = MutableStateFlow<Map<String, LanDevice>>(emptyMap())
    val devices: StateFlow<Map<String, LanDevice>> = _devices.asStateFlow()

    private val lock = Any()
    private val resolvedNames = mutableSetOf<String>()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var registeredName: String? = null

    fun discoverDeputies(): Flow<List<LanDevice>> =
        devices.map { all -> all.values.filter { it.isDeputy }.sortedBy { it.displayName } }
            .distinctUntilChanged()

    fun startDiscovery() {
        synchronized(lock) {
            if (discoveryListener != null) return
        }
        acquireMulticastLock()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i(TAG, "discovery: started for $serviceType")
            }

            override fun onServiceFound(found: NsdServiceInfo) {
                Log.i(TAG, "discovery: found name=${found.serviceName} type=${found.serviceType}")
                if (!found.serviceType.orEmpty().startsWith(TransferProtocol.SERVICE_TYPE)) return
                synchronized(lock) {
                    if (found.serviceName in resolvedNames) return
                    resolvedNames += found.serviceName
                }
                resolve(found)
            }

            override fun onServiceLost(lost: NsdServiceInfo) {
                Log.i(TAG, "discovery: lost ${lost.serviceName}")
                synchronized(lock) { resolvedNames.remove(lost.serviceName) }
                _devices.update { it - lost.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(TAG, "discovery: stopped for $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "discovery: start failed err=$errorCode")
                synchronized(lock) {
                    if (discoveryListener === this) discoveryListener = null
                }
                releaseMulticastLock()
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "discovery: stop failed err=$errorCode")
            }
        }

        synchronized(lock) { discoveryListener = listener }
        runCatching {
            nsdManager.discoverServices(TransferProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            Log.e(TAG, "discovery: discoverServices threw", it)
            synchronized(lock) { discoveryListener = null }
            releaseMulticastLock()
        }
    }

    fun stopDiscovery() {
        val listener = synchronized(lock) {
            discoveryListener.also { discoveryListener = null }
        } ?: return
        runCatching { nsdManager.stopServiceDiscovery(listener) }
        synchronized(lock) { resolvedNames.clear() }
        _devices.update { emptyMap() }
        releaseMulticastLock()
    }

    fun startAdvertising(displayName: String, shortId: String, port: Int, isDeputy: Boolean) {
        val safeShortId = shortId.ifBlank { "atmo" }
        if (port <= 0) {
            Log.e(TAG, "register: skipped, port=$port")
            return
        }
        synchronized(lock) {
            if (registrationListener != null) return
        }
        if (registeredName == null) registeredName = safeShortId

        val info = NsdServiceInfo().apply {
            serviceName = safeShortId
            serviceType = TransferProtocol.SERVICE_TYPE
            this.port = port
            setAttribute(TransferProtocol.ATTR_NAME, displayName.takeIf { it.isNotBlank() } ?: safeShortId)
            setAttribute(TransferProtocol.ATTR_SHORT_ID, safeShortId)
            setAttribute(TransferProtocol.ATTR_ROLE, if (isDeputy) TransferProtocol.ROLE_DEPUTY else TransferProtocol.ROLE_TEACHER)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "register: ok name=${serviceInfo.serviceName}")
                registeredName = serviceInfo.serviceName
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "register: failed err=$errorCode name=${serviceInfo.serviceName}")
                synchronized(lock) {
                    if (registrationListener === this) registrationListener = null
                }
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "register: unregistered ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "register: unregister failed err=$errorCode")
            }
        }

        synchronized(lock) { registrationListener = listener }
        Log.i(TAG, "register: calling registerService name=$safeShortId type=${TransferProtocol.SERVICE_TYPE}")
        runCatching {
            nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            Log.e(TAG, "register: registerService threw", it)
            synchronized(lock) { registrationListener = null }
        }
    }

    fun stopAdvertising() {
        val listener = synchronized(lock) {
            registrationListener.also { registrationListener = null }
        } ?: return
        registeredName = null
        runCatching { nsdManager.unregisterService(listener) }
    }

    @Suppress("DEPRECATION")
    private fun resolve(found: NsdServiceInfo) {
        runCatching {
            nsdManager.resolveService(found, object : NsdManager.ResolveListener {
                override fun onResolveFailed(failed: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "resolve: failed err=$errorCode name=${failed.serviceName}")
                    synchronized(lock) { resolvedNames.remove(failed.serviceName) }
                }

                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    Log.i(TAG, "resolve: ok name=${resolved.serviceName} host=${resolved.host?.hostAddress} port=${resolved.port}")
                    val host = resolved.host?.hostAddress ?: return
                    val port = resolved.port
                    if (port <= 0) return

                    val shortId = textAttr(resolved, TransferProtocol.ATTR_SHORT_ID)
                        ?: resolved.serviceName
                    val displayName = textAttr(resolved, TransferProtocol.ATTR_NAME) ?: shortId
                    val isDeputy = textAttr(resolved, TransferProtocol.ATTR_ROLE) ==
                        TransferProtocol.ROLE_DEPUTY

                    _devices.update { current ->
                        current + (resolved.serviceName to LanDevice(
                            serviceId = resolved.serviceName,
                            displayName = displayName,
                            shortId = shortId,
                            host = host,
                            port = port,
                            isDeputy = isDeputy,
                        ))
                    }
                }
            })
        }.onFailure {
            synchronized(lock) { resolvedNames.remove(found.serviceName) }
        }
    }

    @Suppress("DEPRECATION")
    private fun textAttr(info: NsdServiceInfo, key: String): String? =
        info.attributes[key]?.let { bytes -> String(bytes, Charsets.UTF_8) }

    private fun acquireMulticastLock() {
        if (multicastLock?.isHeld == true) return
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        val lock = wifiManager.createMulticastLock(MULTICAST_TAG).apply {
            setReferenceCounted(false)
            @Suppress("DEPRECATION")
            acquire()
        }
        multicastLock = lock
    }

    private fun releaseMulticastLock() {
        val lock = multicastLock ?: return
        multicastLock = null
        if (lock.isHeld) lock.release()
    }

    private companion object {
        const val TAG = "AtmosphereXfer"
        const val MULTICAST_TAG = "atmosphere_nsd"
    }
}
