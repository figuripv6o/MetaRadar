package f.cking.software.domain.interactor

import android.bluetooth.BluetoothClass
import f.cking.software.domain.model.DeviceClass

object BuildDeviceClassFromSystemInfo {

    private const val MAJOR_BIT_MASK = 0x1F00

    /**
     * Build a [DeviceClass] from the [android.bluetooth.BluetoothClass.Device].
     */
    fun execute(systemClass: Int): DeviceClass {
        val major = systemClass and MAJOR_BIT_MASK
        return when (major) {
            BluetoothClass.Device.Major.PHONE -> {
                when (systemClass) {
                    BluetoothClass.Device.PHONE_CELLULAR -> DeviceClass.Phone.Cellular
                    BluetoothClass.Device.PHONE_CORDLESS -> DeviceClass.Phone.Cordless
                    BluetoothClass.Device.PHONE_ISDN -> DeviceClass.Phone.Isdn
                    BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY -> DeviceClass.Phone.ModemOrGateway
                    BluetoothClass.Device.PHONE_SMART -> DeviceClass.Phone.Smartphone
                    else -> DeviceClass.Phone.Uncategorised
                }
            }
            BluetoothClass.Device.Major.COMPUTER -> {
                when (systemClass) {
                    BluetoothClass.Device.COMPUTER_DESKTOP -> DeviceClass.Computer.Desktop
                    BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA -> DeviceClass.Computer.Laptop
                    BluetoothClass.Device.COMPUTER_LAPTOP -> DeviceClass.Computer.Laptop
                    BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA -> DeviceClass.Computer.Laptop
                    BluetoothClass.Device.COMPUTER_SERVER -> DeviceClass.Computer.Server
                    BluetoothClass.Device.COMPUTER_WEARABLE -> DeviceClass.Computer.Wearable
                    else -> DeviceClass.Computer.Uncategorised
                }
            }
            BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                when (systemClass) {
                    BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER -> DeviceClass.AudioVideo.Camcorder
                    BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> DeviceClass.AudioVideo.CarAudio
                    BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> DeviceClass.AudioVideo.HandsFree
                    BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> DeviceClass.AudioVideo.Headphones
                    BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO -> DeviceClass.AudioVideo.HifiAudio
                    BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> DeviceClass.AudioVideo.Loudspeaker
                    BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE -> DeviceClass.AudioVideo.Microphone
                    BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> DeviceClass.AudioVideo.PortableAudio
                    BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> DeviceClass.AudioVideo.SetTopBox
                    BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED -> DeviceClass.AudioVideo.Uncategorised
                    BluetoothClass.Device.AUDIO_VIDEO_VCR -> DeviceClass.AudioVideo.Vcr
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA -> DeviceClass.AudioVideo.VideoCamera
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING -> DeviceClass.AudioVideo.VideoConferencing
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER -> DeviceClass.AudioVideo.VideoDisplayAndLoudspeaker
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> DeviceClass.AudioVideo.VideoGamingToy
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR -> DeviceClass.AudioVideo.VideoMonitor
                    BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceClass.AudioVideo.WearableHeadset
                    else -> DeviceClass.AudioVideo.Uncategorised
                }
            }
            BluetoothClass.Device.Major.WEARABLE -> {
                when (systemClass) {
                    BluetoothClass.Device.WEARABLE_GLASSES -> DeviceClass.Wearable.Glasses
                    BluetoothClass.Device.WEARABLE_HELMET -> DeviceClass.Wearable.Helmet
                    BluetoothClass.Device.WEARABLE_JACKET -> DeviceClass.Wearable.Jacket
                    BluetoothClass.Device.WEARABLE_PAGER -> DeviceClass.Wearable.Pager
                    BluetoothClass.Device.WEARABLE_WRIST_WATCH -> DeviceClass.Wearable.WristWatch
                    else -> DeviceClass.Wearable.Uncategorised
                }
            }
            BluetoothClass.Device.Major.TOY -> {
                when (systemClass) {
                    BluetoothClass.Device.TOY_CONTROLLER -> DeviceClass.Toy.Controller
                    BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE -> DeviceClass.Toy.Doll
                    BluetoothClass.Device.TOY_GAME -> DeviceClass.Toy.Game
                    BluetoothClass.Device.TOY_ROBOT -> DeviceClass.Toy.Robot
                    BluetoothClass.Device.TOY_VEHICLE -> DeviceClass.Toy.Vehicle
                    else -> DeviceClass.Toy.Uncategorised
                }
            }
            BluetoothClass.Device.Major.HEALTH -> {
                when (systemClass) {
                    BluetoothClass.Device.HEALTH_BLOOD_PRESSURE -> DeviceClass.Health.BloodPressure
                    BluetoothClass.Device.HEALTH_THERMOMETER -> DeviceClass.Health.Thermometer
                    BluetoothClass.Device.HEALTH_WEIGHING -> DeviceClass.Health.Weighing
                    else -> DeviceClass.Health.Uncategorised
                }
            }

            else -> DeviceClass.Unknown
        }
    }
}