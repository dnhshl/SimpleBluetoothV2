package com.example.simplebluetoothv2.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

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
            socket.outputStream.write(msg.toByteArray())
            Log.i(TAG, "Sending ${msg}")
        } catch (e: IOException) {
            Log.i(TAG, "Error writing buffer ${e.message}")
        }
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
}