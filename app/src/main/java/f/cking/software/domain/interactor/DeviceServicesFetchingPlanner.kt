package f.cking.software.domain.interactor

import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.SavedDeviceHandle
import f.cking.software.domain.model.isNullOrEmpty
import f.cking.software.mapParallel
import f.cking.software.splitToBatchesEqual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DeviceServicesFetchingPlanner(
    private val fetchDeviceServiceInfo: FetchDeviceServiceInfo,
    private val bleScannerHelper: BleScannerHelper,
) {

    private var currentJob: Job? = null
    private var parallelProcessingBatches = PARALLEL_BATCH_COUNT
    private var maxPossibleConnections = PARALLEL_BATCH_COUNT

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
                try {
                    fetchAllDevices(metadataNeeded)
//                    increaseConnections()
                } catch (e: FetchDeviceServiceInfo.BluetoothConnectionException.UnspecifiedConnectionError) {
                    Timber.tag(TAG).e(e, "Max connections reached")
//                    currentJob?.cancel()
//                    tooMachConnections()
                }
            }
        }
    }

    private fun tooMachConnections() {
        maxPossibleConnections = parallelProcessingBatches - 1
        parallelProcessingBatches = max(1, (parallelProcessingBatches * 0.5).toInt())
        bleScannerHelper.closeAllConnections()
    }

    private fun increaseConnections() {
        parallelProcessingBatches = min(max(1, (parallelProcessingBatches * 1.2).toInt()), maxPossibleConnections)
    }

    private suspend fun fetchAllDevices(metadataNeeded: List<DeviceData>) = coroutineScope {
        softTimeout(TOTAL_FETCH_TIMEOUT_SEC.seconds, onTimeout = {
            Timber.tag(TAG).e("Timeout fetching total devices")
        }) {
            metadataNeeded.splitToBatchesEqual(parallelProcessingBatches)
                .filter { it.isNotEmpty() }
                .mapParallel { batch ->
                    Timber.tag(TAG).i("Processing batch of ${batch.size} devices ($parallelProcessingBatches parallel)")
                    batch.forEach { device ->
                        fetchDevice(device)
                    }
                }
            Timber.tag(TAG).i("All devices processed")
        }
    }

    private suspend fun fetchDevice(device: DeviceData) {
        softTimeout(DEVICE_FETCH_TIMEOUT_SEC.seconds, onTimeout = {
            Timber.tag(TAG).e("Timeout fetching device info for ${device.address}")
        }) {
            Timber.tag(TAG).i("Fetching device info for ${device.address}, distance: ${device.distance()}")
            try {
                val result = fetchDeviceServiceInfo.execute(device)
                Timber.tag(TAG).i("Fetching complete for ${device.address}. Result: $result")
            } catch (e: FetchDeviceServiceInfo.BluetoothConnectionException) {
                Timber.tag(TAG).e(e, "Error when connecting to device ${device.address}")
            }
        }
    }

    private suspend fun softTimeout(timeout: Duration, onTimeout: suspend () -> Unit, block: suspend () -> Unit) = coroutineScope {
        var timeoutJob: Job? = null
        val primaryJob = async {
            block.invoke()
            timeoutJob?.cancel()
        }

        timeoutJob = async {
            delay(timeout)
            primaryJob.cancel()
            onTimeout.invoke()
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