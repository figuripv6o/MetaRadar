package f.cking.software.domain.interactor

import f.cking.software.data.helpers.BluetoothSIG
import f.cking.software.domain.model.ExtendedAddressInfo
import f.cking.software.domain.model.ExtendedAddressInfo.BleAddressType
import f.cking.software.domain.model.ManufacturerInfo
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object BuildExtendedAddressInfoInteractor {

    fun execute(address: String, lifetime: Long, manufacturerInfo: ManufacturerInfo?): ExtendedAddressInfo {
        val type = getBleAddressType(address, lifetime, manufacturerInfo)
        return ExtendedAddressInfo(address, type)
    }

    private fun getBleAddressType(address: String, lifetime: Long, manufacturerInfo: ManufacturerInfo?): BleAddressType {
        val bytes = address.split(":").mapNotNull { it.toIntOrNull(16) }
        if (bytes.size != 6) return BleAddressType.INVALID

        val msb = bytes[0] // BLE addresses are big-endian, so MSB is the first byte

        return when ((msb shr 6) and 0b11) {
            0b00 -> {
                // PUBLIC and NON_RESOLVABLE_PRIVATE share the same pattern, but:
                // PUBLIC: Assigned by the manufacturer (first 3 bytes form an OUI)
                // NON_RESOLVABLE_PRIVATE: Random and changes periodically
                if (isPublicAddress(msb, lifetime, manufacturerInfo)) BleAddressType.PUBLIC else BleAddressType.NON_RESOLVABLE_PRIVATE
            }
            0b01 -> BleAddressType.RESOLVABLE_PRIVATE
            0b11 -> BleAddressType.STATIC_RANDOM
            else -> BleAddressType.INVALID
        }
    }

    private fun isPublicAddress(msb: Int, lifetime: Long, manufacturerInfo: ManufacturerInfo?): Boolean {
        return lifetime > HOURS_TO_BE_CONSIDERED_STATIC.toDuration(DurationUnit.HOURS).inWholeMilliseconds
                || (!MANUFACTURERS_WITH_PRIVATE_ADDRESSES.contains(manufacturerInfo?.id) && (msb and 0b110000) == 0)
    }

    private const val HOURS_TO_BE_CONSIDERED_STATIC = 12
    private val MANUFACTURERS_WITH_PRIVATE_ADDRESSES = BluetoothSIG.bluetoothSIG.entries.filter {
        it.value.contains("apple", ignoreCase = true)
                || it.value.contains("microsoft", ignoreCase = true)
    }
        .map { it.key }
}