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
        val devices = devices.map { it.device.copy(lastDetectTimeMs = it.previouslySeenAtTime) }
        currentJob?.cancel()
        currentJob = launch {
            withContext(Dispatchers.IO) {
                val metadataNeeded = devices.filter {
                    val lastSeenSecAgo = (System.currentTimeMillis() - it.lastDetectTimeMs)
                    it.metadata == null && (it.detectCount == 1 || lastSeenSecAgo >= 10.minutes.inWholeMilliseconds)
                }
                Timber.tag(TAG).i("Scheduling fetch service info for ${metadataNeeded.size} devices, out of ${devices.size} total")
                metadataNeeded.splitToBatches(10).mapParallel { batch ->
                    batch.forEach { device ->

                        if (device.metadata == null) {
                            try {
                                withTimeout(2.seconds) {
                                    Timber.tag(TAG).i("Fetching device info for ${device.address}")
                                    val result = fetchDeviceServiceInfo.execute(device)
                                    Timber.tag(TAG).i("Fetching complete for ${device.address}. Result: $result")
                                }
                            } catch (e: TimeoutCancellationException) {
                                Timber.tag(TAG).e(e, "Timeout fetching device info for ${device.address}")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "DeviceServicesFetchingPlanner"
    }
}