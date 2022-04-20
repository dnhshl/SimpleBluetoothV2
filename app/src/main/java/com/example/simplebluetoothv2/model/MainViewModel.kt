package com.example.simplebluetoothv2.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplebluetoothv2.bluetooth.*
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.resetSocket
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel : ViewModel() {
    val TAG = "MainViewModel"
    var selectedDevice = ""
    var socket: BluetoothSocket? = null
    private var _isConnected = MutableLiveData<Boolean>()
    val isConnected : LiveData<Boolean>
        get() = _isConnected

    init {
        _isConnected.value = false
    }

    var ledData = LedData()

    fun getMAC(): String {
        if (selectedDevice.isEmpty()) return ""
        return selectedDevice.substring(selectedDevice.length-17)
    }

    @SuppressLint("MissingPermission")
    fun connectEsp32() {
        viewModelScope.launch {
            socket?.let { resetSocket(it) }
            try {
                socket = Esp32Bt.getSocket(getMAC())
                socket?.connect()
            } catch (e: IOException) {
                Log.i(TAG, "Error Connection ${e.message}")
            }
        }
    }
    
    fun checkConnect(timeInterval: Long) {
        if (socket != null) {
            viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + timeInterval
                while (!(_isConnected.value!!) && System.currentTimeMillis()<endTime) {
                    delay(10)
                    _isConnected.value = socket?.let {it.isConnected} ?: false
                }
            }
        }
    }

    fun sendLedData() {
         viewModelScope.launch {
             socket?.let {
                 try {
                     Esp32Bt.sendBtMessage(it, Esp32Bt.jsonEncodeLedData(ledData))
                 } catch (e: Exception) {
                     Log.i(TAG, "Error sending ledData ${e.message}")
                 }
             }
         }
    }


    fun getEsp32Data() {
        viewModelScope.launch {
            socket?.let {
                try {
                    Esp32Bt.readBtMessage(it)
                } catch (e: Exception) {
                    Log.i(TAG, "Error sending ledData ${e.message}")
                }
            }
        }
    }
}