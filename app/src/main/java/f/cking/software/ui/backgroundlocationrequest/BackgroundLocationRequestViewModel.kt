package f.cking.software.ui.backgroundlocationrequest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router

class BackgroundLocationRequestViewModel(
    private val permissionHelper: PermissionHelper,
    private val router: Router,
) : ViewModel() {

    var grantButtonEnabled by mutableStateOf(true)

    fun onBack() {
        router.navigate(BackCommand)
    }

    fun grantPermission() {
        permissionHelper.checkBlePermissions(permissions = PermissionHelper.BACKGROUND_LOCATION) {
            onBack()
        }
    }

    private fun checkPermission() {
        grantButtonEnabled = !permissionHelper.checkBackgroundLocationPermition()
    }
}