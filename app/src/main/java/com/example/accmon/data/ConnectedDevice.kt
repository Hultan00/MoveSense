package com.example.accmon.data

import android.bluetooth.BluetoothDevice
import com.polar.sdk.api.model.PolarDeviceInfo

class ConnectedDevice {
    var bluetoothVariant: BluetoothDevice? = null
    var polarVariant: PolarDeviceInfo? = null

    fun setConnectedDevice(bluetoothDevice: BluetoothDevice?, polarDeviceInfo: PolarDeviceInfo?){
        bluetoothVariant = bluetoothDevice
        polarVariant = polarDeviceInfo
    }

    fun getBluetooth(): BluetoothDevice? {
        return bluetoothVariant
    }
    fun getPolar(): PolarDeviceInfo? {
        return polarVariant
    }
}