package com.lanars.pdpblechatapp.ble.server

import android.bluetooth.BluetoothDevice

interface BleServerCallback {
    fun onInitServerSuccess()
    fun onDeviceConnected(device: BluetoothDevice)
    fun onDeviceDisconnected(device: BluetoothDevice)
    fun onConnectionError(error: String)
    fun onIncomingMessage(msg: String)
    fun isServerConnected(isConnected: Boolean)
    fun serverStarted()
    fun serverFailed(error: String)
}