package com.lanars.pdpblechatapp.ble.client

import android.bluetooth.BluetoothDevice

interface BleClientCallback {
    fun onInitClientSuccess()
    fun onScanResults(bluetoothDevice: BluetoothDevice)
    fun incomingMessage(msg: String)
    fun outgoingMessage(message: String)
    fun isClientConnected(isConnected: Boolean)
}