package f.cking.software.domain.interactor

object GetServiceNameFromBluetoothService {

    fun execute(serviceId: String): String? {
        return KNOWN_SERVICE_ID_TO_NAME[serviceId]
    }

    private val KNOWN_SERVICE_ID_TO_NAME = mapOf(
        "00001800-0000-1000-8000-00805f9b34fb" to "Generic Access",
        "00001801-0000-1000-8000-00805f9b34fb" to "Generic Attribute",
        "00001802-0000-1000-8000-00805f9b34fb" to "Immediate Alert",
    )
}