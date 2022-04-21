package com.example.simplebluetoothv2.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

val TAG = "Esp32Bt"


object Esp32Bt {
    @SuppressLint("MissingPermission")
    suspend fun getSocket(address: String): BluetoothSocket? {
        try {
            val mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
            val socket = device.createInsecureRfcommSocketToServiceRecord(mUUID)
            return socket
        } catch (e: IOException) {
            Log.d(TAG, "Unable to connect ${e.message}")
            return null
        }
    }

    suspend fun resetSocket(socket: BluetoothSocket) {
        socket.inputStream.close()
        socket.outputStream.close()
        socket.close()
    }


    suspend fun sendBtMessage(socket: BluetoothSocket, msg: String) {
        try {
            val data = "!${msg}?"
            socket.outputStream.write(data.toByteArray())
            Log.i(TAG, "Sending ${data}")
        } catch (e: IOException) {
            Log.i(TAG, "Error writing buffer ${e.message}")
        }
    }

    fun jsonEncodeLedData(ledData: LedData): String {
        val obj = JSONObject()
        obj.put("LED", ledData.led)
        obj.put("LEDBlinken", ledData.ledBlinken)
        return obj.toString()
    }


    suspend fun readBtMessage(socket: BluetoothSocket): String {
        try {
            val buffer = ByteArray(2048)
            val bytes = socket.inputStream.read(buffer)
            return String(buffer, 0, bytes)
        } catch (e: IOException) {
            Log.i(TAG, "Error reading buffer ${e.message}")
            return ""
        }
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