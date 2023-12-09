import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.UUID

class BluetoothDeviceScanner(
    private val context: Context
) {
    // Get the default BluetoothAdapter directly
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private var isConnected = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("BluetoothScanner", "OnReceive")
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d("BluetoothScanner", "Device found: ${it.name}")
                        discoveredDevices.add(it)
                    }
                }
            }
        }
    }

    val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(context,
        setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
            PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
            PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO)
    )

    init {
        registerDiscoveryReceiver()
        api.setApiCallback(object : PolarBleApiCallback() {

            override fun blePowerStateChanged(powered: Boolean) {
                Log.d("MyApp", "BLE power: $powered")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "CONNECTED: ${polarDeviceInfo.deviceId}")
                isConnected = true
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("MyApp", "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                isConnected = false
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d("MyApp", "Polar BLE SDK feature $feature is ready")
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d("MyApp", "DIS INFO uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d("MyApp", "BATTERY LEVEL: $level")
            }
        })
    }


    private fun registerDiscoveryReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(discoveryReceiver, filter)
    }

    fun startDiscovery() {
        if (bluetoothAdapter?.isEnabled == true) {
            Log.d("BluetoothScanner", "Bluetooth is enabled!")
            discoveredDevices.clear() // Clear previous results
            Log.d("BluetoothScanner", "Cleared list!")

            // Use context as an activity if it implements the Activity interface
            val activityContext = context as? Activity
            val bluetoothPermissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN
            )

            if (activityContext != null &&
                !checkPermissionsGranted(activityContext, bluetoothPermissions)
            ) {
                Log.d("BluetoothScanner", "No permission!")
                ActivityCompat.requestPermissions(
                    activityContext,
                    bluetoothPermissions,
                    REQUEST_BLUETOOTH_PERMISSION
                )
                return
            }
            Log.d("BluetoothScanner", "Started adapter!")
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("BluetoothScanner", "Permission not granted!")
                return
            }
            bluetoothAdapter.startDiscovery()
        }
    }

    private fun checkPermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun stopDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            // Use context as an activity if it implements the Activity interface
            val activityContext = context as? Activity
            val bluetoothPermissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN
            )

            if (activityContext != null &&
                !checkPermissionsGranted(activityContext, bluetoothPermissions)
            ) {
                Log.d("BluetoothScanner", "No permission!")
                ActivityCompat.requestPermissions(
                    activityContext,
                    bluetoothPermissions,
                    REQUEST_BLUETOOTH_PERMISSION
                )
                return
            }
            Log.d("BluetoothScanner", "Stopped adapter!")
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("BluetoothScanner", "Permission not granted!")
                return
            }
            bluetoothAdapter.cancelDiscovery()
        }
    }

    fun getDiscoveredDevices(): List<BluetoothDevice> {
        return discoveredDevices.toList()
    }

    fun clearDiscoveredDevices() {
        discoveredDevices.clear()
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(discoveryReceiver)
    }

    fun isConnected(): Boolean{
        return isConnected
    }

    companion object {
        const val BLUETOOTH_SCAN = Manifest.permission.BLUETOOTH_SCAN
        const val BLUETOOTH = Manifest.permission.BLUETOOTH
        const val BLUETOOTH_CONNECT = Manifest.permission.BLUETOOTH_CONNECT
        const val BLUETOOTH_ADMIN = Manifest.permission.BLUETOOTH_ADMIN
        const val ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
        const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val REQUEST_BLUETOOTH_PERMISSION = 123 // You can use any unique request code
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
