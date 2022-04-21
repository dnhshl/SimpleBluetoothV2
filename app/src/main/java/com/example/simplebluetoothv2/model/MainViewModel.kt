package com.example.simplebluetoothv2.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplebluetoothv2.bluetooth.*
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.readBtMessage
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.resetSocket
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.sendBtMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

class MainViewModel : ViewModel() {
    val TAG = "MainViewModel"

    private var _selectedDevice = MutableLiveData<String>("")
    val selectedDevice: LiveData<String>
        get() = _selectedDevice

    fun setSelectedDevice(device:String = "") {
        _selectedDevice.value = device
    }

    fun getMAC(): String {
        if (_selectedDevice.value!!.isEmpty()) return ""
        val device = _selectedDevice.value
        return device!!.substring(device.length-17)
    }


    enum class ConnectionState {NOT_CONNECTED, CONNECTING, CONNECTED}

    private var _isConnected = MutableLiveData<ConnectionState>(ConnectionState.NOT_CONNECTED)
    val isConnected : LiveData<ConnectionState>
        get() = _isConnected

    private var _esp32Data = MutableLiveData<Esp32Data>(Esp32Data())
    val esp32Data: LiveData<Esp32Data>
        get() = _esp32Data


    var ledData = LedData()

    private var socket: BluetoothSocket? = null
    private lateinit var dataLoadJob: Job



    @SuppressLint("MissingPermission")
    fun connectEsp32() {
        _isConnected.value = ConnectionState.CONNECTING
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

    fun disconnectEsp32() {
        _isConnected.value = ConnectionState.NOT_CONNECTED
        viewModelScope.launch { socket?.let { resetSocket(it) } }
    }

    fun checkConnect(timeOut: Long) {
        if (socket != null) {
            viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + timeOut
                while (!(_isConnected.value!! == ConnectionState.CONNECTED) && System.currentTimeMillis()<endTime) {
                    delay(10)
                    if (socket?.let {it.isConnected} ?: false)
                        _isConnected.value = ConnectionState.CONNECTED
                    Log.i(TAG, "connecting")
                }
            }
        }
    }

    fun sendLedData() {
         viewModelScope.launch {
             socket?.let {
                 try {
                     // wrap JSON Encoded Data with ! and ?
                     val msg = "!${jsonEncodeLedData(ledData)}?"
                     // send this string
                     sendBtMessage(it, msg)
                 } catch (e: Exception) {
                     Log.i(TAG, "Error sending ledData ${e.message}")
                 }
             }
         }
    }


    fun startDataLoadJob() {
        socket?.let {
            dataLoadJob = viewModelScope.launch {
                while (isActive){
                    try {
                        val jsonStrings = readBtMessage(it).split("!")
                        jsonStrings.forEach { jsonstring ->
                            // Endet der String mit ?
                            if (jsonstring.endsWith("?"))
                                // dann entferne das Fragezeichen und Werte den JSON String aus
                                _esp32Data.value = jsonParseEsp32Data(jsonstring.dropLast(1))
                        }
                        Log.i(TAG, "ESP32Data ${esp32Data}")
                    } catch (e: Exception) {
                        Log.i(TAG, "Error sending ledData ${e.message}")
                    }
                    delay(250)
                }
            }
        }
    }

    fun cancelDataLoadJob() {
        dataLoadJob.cancel()
    }

    fun jsonEncodeLedData(ledData: LedData): String {
        val obj = JSONObject()
        obj.put("LED", ledData.led)
        obj.put("LEDBlinken", ledData.ledBlinken)
        return obj.toString()
    }

    fun jsonParseEsp32Data(jsonString: String): Esp32Data {
        try {
            val obj = JSONObject(jsonString)
            return Esp32Data(
                ledstatus = obj.getString("ledstatus"),
                potiArray = obj.getJSONArray("potiArray")
            )
        } catch (e: Exception) {
            Log.i(com.example.simplebluetoothv2.bluetooth.TAG, "Error decoding JSON ${e.message}")
            return Esp32Data()
        }
    }
}