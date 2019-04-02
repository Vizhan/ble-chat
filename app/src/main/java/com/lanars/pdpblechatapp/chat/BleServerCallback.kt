package com.lanars.pdpblechatapp.chat

import android.bluetooth.BluetoothDevice

interface BleServerCallback {
    fun enableBluetooth()
    fun onInitServerSuccess()
    fun onDeviceConnected(device: BluetoothDevice)
    fun onDeviceDisconnected(device: BluetoothDevice)
    fun onConnectionError(error: String)
    fun onOutgoingMessage(msg: String)
    fun onIncomingMessage(msg: String)
    fun isServerConnected(isConnected: Boolean)
    fun serverStarted()
    fun serverFailed(error: String)
}