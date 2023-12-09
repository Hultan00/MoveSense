package com.example.accmon.ui.viewmodels

import BluetoothDeviceScanner
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.accmon.data.Accelerometer
import com.example.accmon.data.ConnectedDevice
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

interface MoveSenseViewModel {
    val recordWithBluetoothDevice: StateFlow<Boolean>
    val isRecording: StateFlow<Boolean>

    val foundBluetoothDevices: StateFlow<ArrayList<BluetoothDevice>>
    val isBluetoothSearching: StateFlow<Boolean>
    val numOfDiscoveredDevices: StateFlow<Int>
    val connectedDevice: StateFlow<ConnectedDevice>

    val foundPolarDevices: StateFlow<ArrayList<PolarDeviceInfo>>

    fun setRecordWithBluetoothDevice(boolean: Boolean)
    fun setIsRecording(boolean: Boolean)
    fun getAccelerometer(): Accelerometer
    fun getBluetoothScanner(): BluetoothDeviceScanner
    fun getFoundBluetoothDevices()
    fun getPolarDevices()
    fun getContext(): Context
}

class MoveSenseVM (
    private val context: Context
):MoveSenseViewModel,ViewModel() {

    private val _recordWithBluetoothDevice = MutableStateFlow(false)
    override val recordWithBluetoothDevice: StateFlow<Boolean>
        get() = _recordWithBluetoothDevice

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean>
        get() = _isRecording

    private val accelerometer = Accelerometer(context)

    private val bluetoothDeviceScanner = BluetoothDeviceScanner(context)

    private val _foundBluetoothDevices = MutableStateFlow<ArrayList<BluetoothDevice>>(ArrayList())
    override val foundBluetoothDevices: StateFlow<ArrayList<BluetoothDevice>>
        get() = _foundBluetoothDevices

    private val _foundPolarDevices = MutableStateFlow<ArrayList<PolarDeviceInfo>>(ArrayList())
    override val foundPolarDevices: StateFlow<ArrayList<PolarDeviceInfo>>
        get() = _foundPolarDevices

    private val _isBluetoothSearching = MutableStateFlow(false)
    override val isBluetoothSearching: StateFlow<Boolean>
        get() = _isBluetoothSearching

    private val _numOfDiscoveredDevices = MutableStateFlow(0)
    override val numOfDiscoveredDevices: StateFlow<Int>
        get() = _numOfDiscoveredDevices

    private val _connectedDevice = MutableStateFlow<ConnectedDevice>(ConnectedDevice())
    override val connectedDevice: StateFlow<ConnectedDevice>
        get() = _connectedDevice

    private var job: Job? = null  // coroutine job for the game event

    override fun setRecordWithBluetoothDevice(boolean: Boolean){
        _recordWithBluetoothDevice.value = boolean
    }

    override fun setIsRecording(boolean: Boolean){
        _isRecording.value = boolean

        if (boolean){
            accelerometer.startListening()
        }else{
            accelerometer.stopListening()
        }
    }

    override fun getAccelerometer(): Accelerometer {
        return accelerometer
    }

    override fun getBluetoothScanner(): BluetoothDeviceScanner {
        return bluetoothDeviceScanner
    }

    override fun getPolarDevices(){

        val polarDevices = bluetoothDeviceScanner.api.searchForDevice()
        polarDevices.subscribe(
            { device ->
                // This block is executed for each emitted device
                // You can process the device here
                Log.d("BluetoothPolar", "Device found: $device")
                _foundPolarDevices.value.add(device)
                // You can update UI or perform other actions for each device here
                // Note: This code is executed asynchronously for each emitted device
            },
            { error ->
                // Handle error if any
                Log.e("BluetoothPolar", "Error finding devices", error)
            },
            {
                // This block is executed when the Flowable completes
                // You can update UI or perform any finalization here
                Log.d("BluetoothPolar", "Device discovery completed")
            }
        )
    }

    override fun getFoundBluetoothDevices() {
        job?.cancel()
        _numOfDiscoveredDevices.value = 0
        job = viewModelScope.launch {
            _foundBluetoothDevices.value.clear()
            _foundPolarDevices.value.clear()
            _isBluetoothSearching.value = true
            getPolarDevices()
            var hashList = HashSet<BluetoothDevice>()
            hashList.addAll(findBluetoothDevices())
            var hashListRemove = HashSet<BluetoothDevice>()
            for (device in hashList){
                if (device.name == null){
                    hashListRemove.add(device)
                }
            }
            hashList.removeAll(hashListRemove)
            _numOfDiscoveredDevices.value = hashList.size
            Log.d("Bluetooth", "Size ${_numOfDiscoveredDevices.value}")
            foundBluetoothDevices.value.addAll(hashList)
            Log.d("Bluetooth", "List ${foundBluetoothDevices.value}")
        }
    }

    private suspend fun findBluetoothDevices(): List<BluetoothDevice>{
        job?.cancel()
        Log.d("Bluetooth", "Starting discovery")
        bluetoothDeviceScanner.startDiscovery()

        Log.d("Bluetooth", "Scanning bluetooth world..")
        delay(5000)

        Log.d("Bluetooth", "Getting devices..")
        val deviceList = bluetoothDeviceScanner.getDiscoveredDevices()

        Log.d("Bluetooth", "Devices: ${deviceList}")

        Log.d("Bluetooth", "Stopping discovery")
        bluetoothDeviceScanner.stopDiscovery()
        _isBluetoothSearching.value = false

        return deviceList
    }

    fun connectToDevice(bluetoothDevice: BluetoothDevice){
        Log.d("BluetoothConnect", "foundBluetoothDevices size: ${_foundBluetoothDevices.value.size}")
        Log.d("BluetoothConnect", "foundPolarDevices size: ${_foundPolarDevices.value.size}")
        for (bDevice in _foundBluetoothDevices.value){
            for (pDevice in _foundPolarDevices.value){
                Log.d("BluetoothConnect", "Is ${pDevice} == ${bluetoothDevice.name}")
                if(pDevice.name == bluetoothDevice.name){
                    bluetoothDeviceScanner.api.connectToDevice(pDevice.deviceId)
                    _connectedDevice.value.setConnectedDevice(bDevice, pDevice)
                    Log.d("BluetoothConnect", "Connected to ${pDevice}")
                    break
                }
            }
        }
    }

    fun disconnectDevice(){
        if (_connectedDevice.value != null) {
            _connectedDevice.value.polarVariant?.let {
                bluetoothDeviceScanner.api.disconnectFromDevice(
                    it.deviceId)
            }
            _connectedDevice.value.setConnectedDevice(null, null)
        }
    }

    override fun getContext(): Context{
        return context
    }

    private var accDisposable: Disposable? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun startAccStreaming(settings: Map<PolarSensorSetting.SettingType, Int>) {
        accDisposable?.dispose() // Dispose of the previous disposable if it exists

        if (_recordWithBluetoothDevice.value && _isRecording.value) {
            _connectedDevice.value.polarVariant?.let { polarVariant ->
                val polarSensorSetting = PolarSensorSetting(settings)
                accDisposable = bluetoothDeviceScanner.api
                    .startAccStreaming(polarVariant.deviceId, polarSensorSetting)
                    .subscribe(
                        { accelerometerData ->
                            // This block is executed for each emitted accelerometer data
                            // You can access accelerometer values from 'accelerometerData' here
                            accelerometerData.samples.forEachIndexed { index, sample ->
                                val epochStart = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

                                val dateTime = epochStart.plusNanos(sample.timeStamp)
                                val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(dateTime)

                                Log.d("Accelerometer", "Sample $index: Time: $formattedDate X=${sample.x} Y=${sample.y} Z=${sample.z}")
                            }

                        },
                        { error ->
                            // Handle error if any
                            Log.e("Accelerometer", "Error receiving accelerometer data", error)
                        },
                        {
                            // This block is executed when the Flowable completes
                            Log.d("Accelerometer", "Accelerometer data stream completed")
                        }
                    )
            }
        }
    }

    fun stopAccStreaming() {
        accDisposable?.dispose() // Dispose of the disposable to stop the streaming
    }



    companion object {
        // Factory outside the companion object
        fun createFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MoveSenseVM::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return MoveSenseVM(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }

    init {

    }
}