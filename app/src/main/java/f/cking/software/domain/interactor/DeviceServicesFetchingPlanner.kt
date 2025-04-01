package f.cking.software.domain.interactor

import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.JournalEntry
import f.cking.software.domain.model.SavedDeviceHandle
import f.cking.software.domain.model.isNullOrEmpty
import f.cking.software.mapParallel
import f.cking.software.splitToBatchesEqual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
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
    private val saveReportInteractor: SaveReportInteractor,
) {

    private var parallelProcessingBatches = PARALLEL_BATCH_COUNT
    private var maxPossibleConnections = PARALLEL_BATCH_COUNT
    private var cooldown: Long? = null

    suspend fun scheduleFetchServiceInfo(devices: List<SavedDeviceHandle>): List<SavedDeviceHandle> = coroutineScope {

        val cooldown = this@DeviceServicesFetchingPlanner.cooldown
        if (cooldown != null && System.currentTimeMillis() - cooldown < MIN_COOLDOWN_DURATION_MINS.seconds.inWholeMilliseconds) {
            Timber.tag(TAG).i("Device services fetching is on cooldown due to a high errors rate, current batch will be skipped")
            return@coroutineScope devices
        }

        val result = devices
            .map { it }
            .associateBy { it.device.address }
            .toMutableMap()

        var updatedCount = 0
        var errors = 0
        var timeouts = 0

        withContext(Dispatchers.IO) {
            val metadataNeeded = devices
                .filter { checkIfMetadataUpdateNeeded(it) }
                .map { it.device }
                .sortedBy { it.rssi }
                .reversed()

            Timber.tag(TAG).i("Scheduling fetch service info for ${metadataNeeded.size} devices, out of ${devices.size} total")
            val updated = fetchAllDevices(metadataNeeded)
            updated.forEach { fetchFeedback ->
                when (fetchFeedback.feedback) {
                    FetchFeedback.SUCCESS -> {
                        fetchFeedback.device?.let { device ->
                            result[device.address]?.let { previousHandle ->
                                result[device.address] = previousHandle.copy(device = device)
                            }
                            updatedCount++
                        }
                    }
                    FetchFeedback.TIMEOUT -> {
                        timeouts++
                    }
                    FetchFeedback.ERROR -> {
                        errors++
                    }
                }
            }

            analyzeFeedback(metadataNeeded.size, updatedCount, timeouts, errors, devices.size)
            result.values.toList()
        }
    }

    private suspend fun fetchAllDevices(metadataNeeded: List<DeviceData>): List<DeviceFetchFeedback> = coroutineScope {
        val result = mutableListOf<DeviceFetchFeedback>()

        softTimeout(TOTAL_FETCH_TIMEOUT_SEC.seconds, onTimeout = {
            Timber.tag(TAG).e("Timeout fetching total devices")
        }) {
            metadataNeeded.splitToBatchesEqual(parallelProcessingBatches)
                .filter { it.isNotEmpty() }
                .mapParallel { batch ->
                    Timber.tag(TAG).i("Processing batch of ${batch.size} devices ($parallelProcessingBatches parallel)")
                    batch.map { device ->
                        result += fetchDevice(device)
                    }
                }
        }

        result
    }

    private suspend fun fetchDevice(device: DeviceData): DeviceFetchFeedback {
        return softTimeout(DEVICE_FETCH_TIMEOUT_SEC.seconds, onTimeout = {
            Timber.tag(TAG).e("Timeout fetching device info for ${device.address}")
            DeviceFetchFeedback(null, FetchFeedback.TIMEOUT)
        }) {
            Timber.tag(TAG).i("Fetching device info for ${device.address}, distance: ${device.distance()}")
            try {
                val result = fetchDeviceServiceInfo.execute(device)
                Timber.tag(TAG).i("Fetching complete for ${device.address}. Result: $result")
                DeviceFetchFeedback(result?.let { device.copy(metadata = it) }, FetchFeedback.SUCCESS)
            } catch (e: FetchDeviceServiceInfo.BluetoothConnectionException) {
                Timber.tag(TAG).e(e, "Error when connecting to device ${device.address}")
                DeviceFetchFeedback(null, FetchFeedback.ERROR)
            }
        }
    }

    private data class DeviceFetchFeedback(
        val device: DeviceData?,
        val feedback: FetchFeedback,
    )

    private enum class FetchFeedback {
        TIMEOUT,
        ERROR,
        SUCCESS,
    }

    private suspend fun analyzeFeedback(
        updateNeeded: Int,
        updated: Int,
        timeouts: Int,
        errors: Int,
        total: Int,
    ) {
        Timber.tag(TAG).i("Deep analysis finished. Candidates: $updateNeeded (updated: $updated, timeouts: $timeouts, errors: $errors), total $total devices")
        if (updateNeeded > 5 && errors / updateNeeded > 0.7) {
            val report = JournalEntry.Report.Error(
                title = "Too many errors during deep analysis. Restart bluetooth or disable deep analysis in settings",
                stackTrace = "Errors: $errors, timeouts: $timeouts, updated: $updated, in total: $updateNeeded"
            )
            saveReportInteractor.execute(report)
            cooldown = System.currentTimeMillis()
        }
    }

    private suspend fun <T> softTimeout(timeout: Duration, onTimeout: suspend () -> T, block: suspend () -> T): T = coroutineScope {
        channelFlow<T> {
            var timeoutJob: Job? = null
            val primaryJob = async {
                val result = block.invoke()
                timeoutJob?.cancel()
                send(result)
            }

            timeoutJob = async {
                delay(timeout)
                primaryJob.cancel()
                send(onTimeout.invoke())
            }
        }.first()
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

    private fun tooMachConnections() {
        maxPossibleConnections = parallelProcessingBatches - 1
        parallelProcessingBatches = max(1, (parallelProcessingBatches * 0.5).toInt())
        bleScannerHelper.closeAllConnections()
    }

    private fun increaseConnections() {
        parallelProcessingBatches = min(max(1, (parallelProcessingBatches * 1.2).toInt()), maxPossibleConnections)
    }

    companion object {
        private const val PARALLEL_BATCH_COUNT = 10
        private const val CHECK_INTERVAL_PER_DEVICE_MIN = 10
        private const val DEVICE_FETCH_TIMEOUT_SEC = 5
        private const val TOTAL_FETCH_TIMEOUT_SEC = 30
        private const val MIN_COOLDOWN_DURATION_MINS = 5
        private const val TAG = "DeviceServicesFetchingPlanner"
    }
}