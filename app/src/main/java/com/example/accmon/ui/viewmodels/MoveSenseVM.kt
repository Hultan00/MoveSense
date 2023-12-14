package com.example.accmon.ui.viewmodels

import Acc
import BluetoothDeviceScanner
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.accmon.data.ConnectedDevice
import com.example.accmon.data.Fusion
import com.example.accmon.data.Gyro
import com.example.accmon.data.InternalAccelerometer
import com.example.accmon.data.InternalGyroscope
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

interface MoveSenseViewModel {
    val recordWithBluetoothDevice: StateFlow<Boolean>
    val isRecording: StateFlow<Boolean>

    val foundBluetoothDevices: StateFlow<ArrayList<BluetoothDevice>>
    val isBluetoothSearching: StateFlow<Boolean>
    val numOfDiscoveredDevices: StateFlow<Int>
    val connectedDevice: StateFlow<ConnectedDevice>

    val foundPolarDevices: StateFlow<ArrayList<PolarDeviceInfo>>

    val polarAccValues: StateFlow<ArrayList<Acc>>
    val polarGyroValues: StateFlow<ArrayList<Gyro>>
    val polarFusionValues: StateFlow<ArrayList<Fusion>>

    fun setRecordWithBluetoothDevice(boolean: Boolean)
    fun setIsRecording(boolean: Boolean)
    fun getAccelerometer(): InternalAccelerometer
    fun getGyroscope(): InternalGyroscope
    fun getBluetoothScanner(): BluetoothDeviceScanner
    fun getFoundBluetoothDevices()
    fun getPolarDevices()
    fun getContext(): Context
}

class MoveSenseVM (
    private val context: Context
):MoveSenseViewModel,ViewModel() {

    private val onlyShowPolarDevices = false

    private val _recordWithBluetoothDevice = MutableStateFlow(false)
    override val recordWithBluetoothDevice: StateFlow<Boolean>
        get() = _recordWithBluetoothDevice

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean>
        get() = _isRecording

    private val accelerometer = InternalAccelerometer(context)

    private val gyroscope = InternalGyroscope(context)

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

    private val _polarAccValues = MutableStateFlow<ArrayList<Acc>>(ArrayList())
    override val polarAccValues: StateFlow<ArrayList<Acc>>
        get() = _polarAccValues

    private val _polarGyroValues = MutableStateFlow<ArrayList<Gyro>>(ArrayList())
    override val polarGyroValues: StateFlow<ArrayList<Gyro>>
        get() = _polarGyroValues

    private val _polarFusionValues = MutableStateFlow<ArrayList<Fusion>>(ArrayList())
    override val polarFusionValues: StateFlow<ArrayList<Fusion>>
        get() = _polarFusionValues

    private var job: Job? = null  // coroutine job for the game event

    override fun setRecordWithBluetoothDevice(boolean: Boolean){
        _recordWithBluetoothDevice.value = boolean
    }

    override fun setIsRecording(boolean: Boolean){
        _isRecording.value = boolean

        if (!recordWithBluetoothDevice.value) {
            if (boolean) {
                accelerometer.startListening()
                gyroscope.startListening()
            } else {
                accelerometer.stopListening()
                gyroscope.stopListening()
            }
        }
    }

    override fun getAccelerometer(): InternalAccelerometer {
        return accelerometer
    }

    override fun getGyroscope(): InternalGyroscope {
        return gyroscope
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

            if (onlyShowPolarDevices) {
                for (bDevice in hashList) {
                    for (pDevice in _foundPolarDevices.value) {
                        if (bDevice.name == pDevice.name) {
                            if (!foundBluetoothDevices.value.contains(bDevice)) {
                                foundBluetoothDevices.value.add(bDevice)
                            }
                        }
                    }
                }
            }else{
                foundBluetoothDevices.value.addAll(hashList)
            }
            _numOfDiscoveredDevices.value = _foundBluetoothDevices.value.size
            Log.d("Bluetooth", "Size ${_numOfDiscoveredDevices.value}")

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
    private var gyroDisposable: Disposable? = null

    fun prepareGraphs(){
        _polarAccValues.value.clear()
        _polarGyroValues.value.clear()
        _polarFusionValues.value.clear()
    }

    fun getCurrentNanoTime(): Long{
        // Get the current LocalDateTime
        val localDateTime = LocalDateTime.now()

        // Set the epoch date to 2000-01-01
        val epochDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0)

        // Calculate the nanoseconds since the epoch
        val nanoseconds = ChronoUnit.NANOS.between(epochDateTime, localDateTime)

        return nanoseconds
    }


    fun findClosestGyroValue(timestamp: Long, gyroValues: List<Gyro>): Gyro? {
        var closestGyroValue: Gyro? = null
        var minTimeDifference = Long.MAX_VALUE

        for (gyroValue in gyroValues) {
            val timeDifference = Math.abs(gyroValue.ms - timestamp)

            if (timeDifference < minTimeDifference) {
                closestGyroValue = gyroValue
                minTimeDifference = timeDifference
            }
        }

        return closestGyroValue
    }


    fun startAccStreaming(settings: Map<PolarSensorSetting.SettingType, Int>, settingsGyro: Map<PolarSensorSetting.SettingType, Int>) {
        accDisposable?.dispose() // Dispose of the previous disposable if it exists
        gyroDisposable?.dispose() // Dispose of the previous disposable if it exists

        var firstSampleTimestamp: Long = 0
        var firstSampleRead = false

        if (_recordWithBluetoothDevice.value && _isRecording.value) {
            _connectedDevice.value.polarVariant?.let { polarVariant ->

                val polarSensorSetting = PolarSensorSetting(settings)

                accDisposable = bluetoothDeviceScanner.api
                    .startAccStreaming(polarVariant.deviceId, polarSensorSetting)
                    .subscribe(
                        { accelerometerData ->
                            Log.d("Accelerometer", "Sample $accelerometerData")
                            // This block is executed for each emitted accelerometer data
                            // You can access accelerometer values from 'accelerometerData' here
                            accelerometerData.samples.forEachIndexed { index, sample ->
                                if ((index == 0) && !firstSampleRead) {
                                    // Save the timestamp of the first sample
                                    firstSampleTimestamp = sample.timeStamp
                                    firstSampleRead = true
                                }

                                val epochStart = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                                val dateTime = epochStart.plusNanos(sample.timeStamp)
                                val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(dateTime)
                                val timeDifferenceNanos = sample.timeStamp - firstSampleTimestamp
                                val timeDifferenceMillis = timeDifferenceNanos / 1_000_000

                                Log.d(
                                    "Accelerometer",
                                    "Sample $index: Time: $formattedDate X=${sample.x} Y=${sample.y} Z=${sample.z}"
                                )
                                Log.d("Accelerometer", "Sample $index: Time: $timeDifferenceMillis ms")

                                synchronized(_polarAccValues.value) {
                                    if (_polarAccValues.value.isEmpty()){
                                        _polarAccValues.value.add(
                                            Acc(
                                                sample.x,
                                                sample.y,
                                                sample.z,
                                                timeDifferenceMillis
                                            )
                                        )
                                    }else{
                                        val lastAcc = _polarAccValues.value.last()
                                        _polarAccValues.value.add(
                                            Acc(
                                                sample.x,
                                                sample.y,
                                                sample.z,
                                                timeDifferenceMillis,
                                                lastAcc
                                            )
                                        )
                                    }
                                }
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

                val polarSensorSettingGyro = PolarSensorSetting(settingsGyro)

                gyroDisposable = bluetoothDeviceScanner.api
                    .startGyroStreaming(polarVariant.deviceId, polarSensorSettingGyro)
                    .subscribe(
                        { gyroData ->
                            // This block is executed for each emitted accelerometer data
                            // You can access accelerometer values from 'accelerometerData' here
                            gyroData.samples.forEachIndexed { index, sample ->
                                if ((index == 0) && !firstSampleRead) {
                                    // Save the timestamp of the first sample
                                    firstSampleTimestamp = sample.timeStamp
                                    firstSampleRead = true
                                }

                                val epochStart = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                                val dateTime = epochStart.plusNanos(sample.timeStamp)
                                val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(dateTime)
                                val timeDifferenceNanos = sample.timeStamp - firstSampleTimestamp
                                val timeDifferenceMillis = timeDifferenceNanos / 1_000_000

                                Log.d(
                                    "Gyro", "Sample $index: Time: $formattedDate X=${sample.x} Y=${sample.y} Z=${sample.z}"
                                )
                                Log.d("Gyro", "Sample $index: Time: $timeDifferenceMillis ms")

                                synchronized(_polarGyroValues.value) {
                                    if (_polarGyroValues.value.isEmpty()) {
                                        _polarGyroValues.value.add(
                                            Gyro(
                                                sample.x,
                                                sample.y,
                                                sample.z,
                                                _polarAccValues.value.get(0).p,
                                                0F,
                                                0F,
                                                timeDifferenceMillis
                                            )
                                        )
                                    }else{
                                        val lastGyro = _polarGyroValues.value.last()
                                        _polarGyroValues.value.add(
                                            Gyro(
                                                sample.x,
                                                sample.y,
                                                sample.z,
                                                timeDifferenceMillis,
                                                lastGyro
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        { error ->
                            // Handle error if any
                            Log.e("Gyro", "Error receiving gyro data", error)
                        },
                        {
                            // This block is executed when the Flowable completes
                            Log.d("Gyro", "Gyro data stream completed")
                        }
                    )
            }
        }

    }

    fun stopAccStreaming() {
        accDisposable?.dispose() // Dispose of the disposable to stop the streaming
        gyroDisposable?.dispose()
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