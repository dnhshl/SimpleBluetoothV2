package com.example.simplebluetoothv2.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplebluetoothv2.bluetooth.*
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.checkConnect
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.connectEsp32
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.disconnectEsp32
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.readBtMessage
import com.example.simplebluetoothv2.bluetooth.Esp32Bt.sendBtMessage

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

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


    private lateinit var dataLoadJob: Job

    fun connect() {
        val MAC = getMAC()
        if (!MAC.isEmpty()) viewModelScope.launch { connectEsp32(MAC) }
    }

    fun disconnect() {
        viewModelScope.launch { disconnectEsp32() }
    }

    fun sendLedData() {
         viewModelScope.launch {
             try {
                 // wrap JSON Encoded Data with ! and ?
                 val msg = "!${jsonEncodeLedData(ledData)}?"
                 // send this string
                 sendBtMessage(msg)
             } catch (e: Exception) {
                 Log.i(TAG, "Error sending ledData ${e.message}")
             }
         }
    }

    // Prüfe alle 100 ms, ob sich der Status geändert hat
    // Prüfe, bis der Status auf CONNECTED ist
    // längstens aber bis zum timeOut
    fun checkConnectionState(timeOut: Long) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + timeOut
            while (_isConnected.value != ConnectionState.CONNECTED
                     && System.currentTimeMillis()<endTime) {
                _isConnected.value = checkConnect()
                Log.i(TAG, "checking Connection ${_isConnected.value}")
                delay(100)
            }
        }
    }


    fun startDataLoadJob() {
        dataLoadJob = viewModelScope.launch {
            while (isActive){
                try {
                    val jsonStrings = readBtMessage().split("!")
                    jsonStrings.forEach { jsonstring ->
                        // Endet der String mit ?
                        if (jsonstring.endsWith("?"))
                            // dann entferne das Fragezeichen und Werte den JSON String aus
                            _esp32Data.value = jsonParseEsp32Data(jsonstring.dropLast(1))
                    }
                } catch (e: Exception) {
                    Log.i(TAG, "Error sending ledData ${e.message}")
                }
                delay(250)
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
            Log.i(TAG, "Error decoding JSON ${e.message}")
            return Esp32Data()
        }
    }
}