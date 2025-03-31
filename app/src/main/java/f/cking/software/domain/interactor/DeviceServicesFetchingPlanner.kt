package f.cking.software.domain.interactor

import f.cking.software.domain.model.SavedDeviceHandle
import f.cking.software.mapParallel
import f.cking.software.splitToBatches
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
    private val fetchDeviceServiceInfo: FetchDeviceServiceInfo
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
                metadataNeeded.splitToBatches(metadataNeeded.size / PARALLEL_BATCHES).mapParallel { batch ->
                    Timber.tag(TAG).i("Processing batch of ${batch.size} devices")
                    batch.forEach { device ->
                        try {
                            withTimeout(5.seconds) {
                                Timber.tag(TAG).i("Fetching device info for ${device.address}")
                                val result = fetchDeviceServiceInfo.execute(device)
                                Timber.tag(TAG).i("Fetching complete for ${device.address}. Result: $result")
                            }
                        } catch (e: TimeoutCancellationException) {
                            Timber.tag(TAG).e(e, "Timeout fetching device info for ${device.address}")
                        }
                    }
                }
                Timber.tag(TAG).i("All devices processed")
            }
        }
    }

    private fun checkIfMetadataUpdateNeeded(device: SavedDeviceHandle): Boolean {
        val lastSeenSecAgo = (System.currentTimeMillis() - device.previouslySeenAtTime)
        return device.device.metadata == null && (device.device.detectCount == 1 || lastSeenSecAgo >= 10.minutes.inWholeMilliseconds)
    }

    companion object {
        private const val PARALLEL_BATCHES = 10
        private const val TAG = "DeviceServicesFetchingPlanner"
    }
}