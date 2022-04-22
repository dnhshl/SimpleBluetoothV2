package com.example.simplebluetoothv2.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.simplebluetoothv2.model.MainViewModel
import java.io.IOException
import java.util.*

/**
 * Esp32Bt stellt Funktionen bereit, die im viewModelScope in Coroutinen genutzt werden können
 */

object Esp32Bt {

    private val TAG = "Esp32Bt"
    private var socket: BluetoothSocket? = null


    @SuppressLint("MissingPermission")
    suspend fun connectEsp32(MAC: String) {
        socket?.let { resetSocket() }
        getSocket(MAC)
        try {
            socket?.connect()
        }
        catch (e: IOException) { Log.i(TAG, "Error Connection ${e.message}") }
    }

    suspend fun disconnectEsp32() {
        socket?.let { resetSocket() }
    }

    suspend fun checkConnect(): MainViewModel.ConnectionState {
        socket?.let {
            if (it.isConnected) return MainViewModel.ConnectionState.CONNECTED
            else MainViewModel.ConnectionState.CONNECTING
        }
        return MainViewModel.ConnectionState.NOT_CONNECTED
    }

    suspend fun sendBtMessage(msg: String) {
        socket?.let {
            try {
                it.outputStream.write(msg.toByteArray())
                Log.i(TAG, "Sending ${msg}")
            } catch (e: IOException) {
                Log.i(TAG, "Error writing buffer ${e.message}")
            }
        }
    }

    suspend fun readBtMessage(): String {
        socket?.let {
            try {
                val buffer = ByteArray(2048)
                val bytes = it.inputStream.read(buffer)
                return String(buffer, 0, bytes)
            } catch (e: IOException) {
                Log.i(TAG, "Error reading buffer ${e.message}")
                return ""
            }
        }
        return ""
    }

    @SuppressLint("MissingPermission")
    private suspend fun getSocket(address: String) {
        try {
            val mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
            socket = device.createInsecureRfcommSocketToServiceRecord(mUUID)
        } catch (e: IOException) {
            Log.d(TAG, "Unable to connect ${e.message}")
        }
    }

    private suspend fun resetSocket() {
        socket?.let {
            it.inputStream.close()
            it.outputStream.close()
            it.close()
        }
    }
}