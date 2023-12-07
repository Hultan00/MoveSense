package com.example.accmon.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface MoveSenseViewModel {
    val recordWithBluetoothDevice: StateFlow<Boolean>

    fun setRecordWithBluetoothDevice(boolean: Boolean)
}

class MoveSenseVM (
    private val context: Context
):MoveSenseViewModel,ViewModel() {

    private var _recordWithBluetoothDevice = MutableStateFlow(false)
    override val recordWithBluetoothDevice: StateFlow<Boolean>
        get() = _recordWithBluetoothDevice

    override fun setRecordWithBluetoothDevice(boolean: Boolean){
        _recordWithBluetoothDevice.value = boolean
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