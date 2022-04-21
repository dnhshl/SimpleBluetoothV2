package com.example.simplebluetoothv2.bluetooth

import org.json.JSONArray

data class LedData(var led: String = "L", var ledBlinken: Boolean = false)
data class Esp32Data(val ledstatus: String = "", val potiArray: JSONArray = JSONArray())