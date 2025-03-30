package f.cking.software.domain.interactor

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.DeviceMetadata
import f.cking.software.domain.model.DeviceMetadata.CharacteristicType
import f.cking.software.domain.model.DeviceMetadata.ServiceTypes
import f.cking.software.fromBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class FetchDeviceServiceInfo(
    private val bleScannerHelper: BleScannerHelper,
    private val devicesRepository: DevicesRepository
) {

    suspend fun execute(device: DeviceData) {
        withContext(Dispatchers.IO) {
            Timber.tag(TAG).i("Fetching device info for ${device.address}")
            val originalMetadata = device.metadata
            val updatedMetadata = connectAndFetchServices(device).firstOrNull()
            if (originalMetadata != updatedMetadata) {
                devicesRepository.saveDevice(device.copy(metadata = updatedMetadata))
            }
            Timber.tag(TAG).i("Finished fetching device info for ${device.address}, metadata: $updatedMetadata")
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun connectAndFetchServices(device: DeviceData): Flow<DeviceMetadata> = coroutineScope {
        val connectionStream = bleScannerHelper.connectToDevice(device.address)
        val pendingCharacteristics = mutableMapOf<String, BluetoothGattCharacteristic>()

        var metadata = device.metadata ?: DeviceMetadata()

        flow<DeviceMetadata> {
            Timber.tag(TAG).i("Connecting to ${device.address}")
            connectionStream.collect { event ->
                when (event) {
                    is BleScannerHelper.DeviceConnectResult.Connected -> {
                        Timber.tag(TAG).i("Connected to ${device.address}. Discovering services...")
                        bleScannerHelper.discoverServices(event.gatt)
                    }
                    is BleScannerHelper.DeviceConnectResult.AvailableServices -> {
                        if (pendingCharacteristics.isEmpty()) {
                            val relevantCharacteristics = findRelevantCharacteristics(event.services)
                            Timber.tag(TAG).i("Services discovered for ${device.address}. Relevant characteristics: ${relevantCharacteristics.map { it.uuid.toString() }}")
                            if (relevantCharacteristics.isNotEmpty()) {
                                pendingCharacteristics.putAll(relevantCharacteristics.associateBy { it.uuid.toString() })
                                requestCharacteristic(event.gatt, relevantCharacteristics.first())
                            } else {
                                Timber.tag(TAG).i("No relevant characteristics found for ${device.address}")
                                emit(metadata)
                            }
                        } else {
                            Timber.tag(TAG).i("No services to request for ${device.address}")
                            emit(metadata)
                        }
                    }
                    is BleScannerHelper.DeviceConnectResult.CharacteristicRead -> {
                        val characteristic = event.characteristic
                        val value = event.valueEncoded64.fromBase64()
                        val uuid = characteristic.uuid.toString()
                        Timber.tag(TAG).i("Characteristic read for ${device.address}: Characteristic data $uuid: ${value.decodeToString()}")

                        metadata = when (CharacteristicType.findByUuid(uuid)) {
                            CharacteristicType.DEVICE_NAME -> {
                                metadata.copy(deviceName = value.decodeToString())
                            }
                            CharacteristicType.MANUFACTURER_NAME -> {
                                metadata.copy(manufacturerName = value.decodeToString())
                            }
                            CharacteristicType.MODEL_NUMBER -> {
                                metadata.copy(moderNumber = value.decodeToString())
                            }
                            CharacteristicType.SERIAL_NUMBER -> {
                                metadata.copy(serialNumber = value.decodeToString())
                            }
                            CharacteristicType.BATTERY_LEVEL -> {
                                metadata.copy(batteryLevel = value.getOrNull(0)?.toInt())
                            }
                            else -> metadata
                        }
                        pendingCharacteristics.remove(uuid)

                        if (pendingCharacteristics.isEmpty()) {
                            Timber.tag(TAG).i("All characteristics read for ${device.address}, finishing fetching...")
                            emit(metadata)
                        } else {
                            Timber.tag(TAG).i("Still pending characteristics for ${device.address}: ${pendingCharacteristics.keys}")
                            requestCharacteristic(event.gatt, pendingCharacteristics.values.first())
                        }
                    }
                    is BleScannerHelper.DeviceConnectResult.FailedReadCharacteristic -> {
                        val uuid = event.characteristic.uuid.toString()
                        Timber.tag(TAG).e("Failed to read characteristic ${event.characteristic.uuid} for ${device.address}")
                        pendingCharacteristics.remove(uuid)

                        if (pendingCharacteristics.isEmpty()) {
                            Timber.tag(TAG).i("All characteristics read for ${device.address}, finishing fetching...")
                            emit(metadata)
                        } else {
                            Timber.tag(TAG).i("Still pending characteristics for ${device.address}: $pendingCharacteristics.keys")
                            requestCharacteristic(event.gatt, pendingCharacteristics.values.first())
                        }
                    }
                    is BleScannerHelper.DeviceConnectResult.Disconnected -> {
                        Timber.tag(TAG).i("Disconnected from ${device.address}")
                        emit(metadata)
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }//.timeout(5.seconds)
    }

    private fun requestCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        Timber.tag(TAG).i("Requesting characteristic ${characteristic.uuid}")
        bleScannerHelper.readCharacteristic(gatt, characteristic)
    }

    private fun findRelevantCharacteristics(services: List<BluetoothGattService>): List<BluetoothGattCharacteristic> {
        return services.flatMap { service ->
            val isRelevant = ServiceTypes.findByUuid(service.uuid.toString()) != null
            if (isRelevant) {
                filterRelevantCharacteristics(service.characteristics)
            } else {
                emptyList()
            }
        }
    }

    private fun filterRelevantCharacteristics(characteristics: List<BluetoothGattCharacteristic>): List<BluetoothGattCharacteristic> {
        return characteristics.filter { CharacteristicType.findByUuid(it.uuid.toString()) != null }
    }

    companion object {
        private const val TAG = "FetchDeviceServiceInfo"
    }
}