package f.cking.software.domain.interactor

import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.JournalEntry
import f.cking.software.domain.model.RadarProfile
import f.cking.software.domain.model.SavedDeviceHandle
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

class CheckBatchForRadarMatchesInteractor(
    private val radarProfilesRepository: RadarProfilesRepository,
    private val filterChecker: FilterCheckerImpl,
    private val saveReportInteractor: SaveReportInteractor,
    private val locationProvider: LocationProvider,
) {

    suspend fun execute(batch: List<SavedDeviceHandle>): List<ProfileResult> {
        return withContext(Dispatchers.Default) {
            val checkStartTime = System.currentTimeMillis()

            // Use original previous detection time for device and it's airdrop info
            // This is needed to correctly process radar profiles that are based on last detection time
            val adjustedDevices = batch.map { handle ->
                handle.device.copy(
                    lastDetectTimeMs = handle.previouslySeenAtTime,
                    manufacturerInfo = handle.device.manufacturerInfo?.let { manufacturerInfo ->
                        manufacturerInfo.copy(
                            airdrop = manufacturerInfo.airdrop?.let { airdrop ->
                                airdrop.copy(
                                    contacts = airdrop.contacts.map { contact ->
                                        val originalLastDetectionTime = handle.airdrop?.contactShaToPreviouslySeenAtTime[contact.sha256]
                                        contact.copy(lastDetectionTimeMs = originalLastDetectionTime ?: handle.previouslySeenAtTime)
                                    }
                                )
                            }
                        )
                    }
                )
            }

            val allProfiles = radarProfilesRepository.getAllProfiles()

            val result = allProfiles.mapNotNull { profile ->
                checkProfile(profile, adjustedDevices)
            }

            result.forEach { saveReport(it) }

            val totalDuration = System.currentTimeMillis() - checkStartTime
            Timber.tag(TAG).i("Radar detection check: ${result.size} profiles detected. Duration $totalDuration ms")
            result
        }
    }

    private suspend fun checkProfile(profile: RadarProfile, devices: List<DeviceData>): ProfileResult? {
        val start = System.currentTimeMillis()

        val result = profile.takeIf { it.isActive }
            ?.let {
                devices.mapParallel { device ->
                    device.takeIf { profile.detectFilter?.let { filterChecker.check(device, it) } == true }
                }.filterNotNull()
            }
            ?.takeIf { matched -> matched.isNotEmpty() }
            ?.let { matched -> ProfileResult(profile, matched) }

        if (false) {
            logStatistic(profile, result, System.currentTimeMillis() - start)
        }

        return result
    }

    suspend fun <T> List<T>.mapParallel(transform: suspend (T) -> T?): List<T?> {
        return coroutineScope {
            map { async { transform(it) } }.awaitAll()
        }
    }

    private fun logStatistic(profile: RadarProfile, result: ProfileResult?, duration: Long) {
        val sb = StringBuilder()
        val statistics = filterChecker.captureStatistic()

        sb.append("Profile ${profile.name} detected ${result?.matched?.size ?: 0} devices. Total duration $duration:")
        statistics.sortedBy { it.total }.reversed().forEach { stat ->
            sb.append("\n      ${stat.name}: count: ${stat.count}; total: (${stat.total} ms; avg: ${stat.avg} ms)")
        }
        sb.append("\n")
        Timber.tag(TAG).d(sb.toString())
    }

    private suspend fun saveReport(result: ProfileResult) {
        val locationModel = locationProvider.getFreshLocation()

        val report = JournalEntry.Report.ProfileReport(
            profileId = result.profile.id ?: return,
            deviceAddresses = result.matched.map { it.address },
            locationModel = locationModel?.toDomain(System.currentTimeMillis()),
        )

        saveReportInteractor.execute(report)
    }

    data class ProfileResult(
        val profile: RadarProfile,
        val matched: List<DeviceData>,
    )

    companion object {
        private const val TAG = "CheckBatchForRadarMatchesInteractor"
    }
}