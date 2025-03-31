package f.cking.software.domain.interactor

import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.SavedDeviceHandle
import f.cking.software.domain.model.isNullOrEmpty
import f.cking.software.mapParallel
import f.cking.software.splitToBatchesEqual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DeviceServicesFetchingPlanner(
    private val fetchDeviceServiceInfo: FetchDeviceServiceInfo,
    private val bleScannerHelper: BleScannerHelper,
) {

    private var currentJob: Job? = null

    suspend fun scheduleFetchServiceInfo(devices: List<SavedDeviceHandle>) = coroutineScope {
        currentJob?.cancel()
        currentJob = launch {
            withContext(Dispatchers.IO) {
                val metadataNeeded = devices
                    .filter { checkIfMetadataUpdateNeeded(it) }
                    .map { it.device }
                    .sortedBy { it.rssi }
                    .reversed()

                Timber.tag(TAG).i("Scheduling fetch service info for ${metadataNeeded.size} devices, out of ${devices.size} total")

                fetchAllDevices(metadataNeeded)
                Timber.tag(TAG).i("All devices processed")
            }
        }
    }

    private suspend fun fetchAllDevices(metadataNeeded: List<DeviceData>) {
        try {
            withTimeout(TOTAL_FETCH_TIMEOUT_SEC.seconds) {
                Timber.tag(TAG).i("Fetching total devices")
                metadataNeeded.splitToBatchesEqual(PARALLEL_BATCH_COUNT)
                    .filter { it.isNotEmpty() }
                    .mapParallel { batch ->
                        Timber.tag(TAG).i("Processing batch of ${batch.size} devices")
                        batch.forEach { device ->
                            fetchDevice(device)
                        }
                    }
            }

        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).e(e, "Timeout fetching total devices")
            bleScannerHelper.closeAllConnections()
        }
    }

    private suspend fun fetchDevice(device: DeviceData) {
        try {
            Timber.tag(TAG).i("Fetching device info for ${device.address}")
            val result = withTimeout(DEVICE_FETCH_TIMEOUT_SEC.seconds) { fetchDeviceServiceInfo.execute(device) }
            Timber.tag(TAG).i("Fetching complete for ${device.address}. Result: $result")
        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).e(e, "Timeout fetching device info for ${device.address}")
            bleScannerHelper.closeDeviceConnection(device.address)
        } catch (e: FetchDeviceServiceInfo.MaxConnectionsReached) {
            Timber.tag(TAG).e(e, "Max connections reached")
            bleScannerHelper.closeAllConnections()
        }
    }

    private fun checkIfMetadataUpdateNeeded(device: SavedDeviceHandle): Boolean {
        if (!device.device.isConnectable) {
            return false
        }

        val lastSeenSecAgo = (System.currentTimeMillis() - device.previouslySeenAtTime)
        val recentlyChecked = lastSeenSecAgo < CHECK_INTERVAL_PER_DEVICE_MIN.minutes.inWholeMilliseconds

        val isNewDevice = device.device.detectCount == 1

        return isNewDevice
                || device.device.metadata.isNullOrEmpty()
                || !recentlyChecked
    }

    companion object {
        private const val PARALLEL_BATCH_COUNT = 6
        private const val CHECK_INTERVAL_PER_DEVICE_MIN = 10
        private const val DEVICE_FETCH_TIMEOUT_SEC = 5
        private const val TOTAL_FETCH_TIMEOUT_SEC = 30
        private const val TAG = "DeviceServicesFetchingPlanner"
    }
}