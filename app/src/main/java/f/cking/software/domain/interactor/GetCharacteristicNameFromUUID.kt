package f.cking.software.domain.interactor

object GetCharacteristicNameFromUUID {

    fun execute(characteristicId: String): String? {
        return KNOWN_CHARACTERISTIC_ID_TO_NAME[characteristicId]
    }

    private val KNOWN_CHARACTERISTIC_ID_TO_NAME = mapOf(
        "00002a00-0000-1000-8000-00805f9b34fb" to "Device Name",
        "00002a01-0000-1000-8000-00805f9b34fb" to "Appearance",
        "00002a02-0000-1000-8000-00805f9b34fb" to "Peripheral Privacy Flag",
    )
}