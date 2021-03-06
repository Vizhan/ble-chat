package com.lanars.pdpblechatapp.chat.vm

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.bluetooth.BluetoothDevice
import com.lanars.pdpblechatapp.ble.*
import com.lanars.pdpblechatapp.ble.client.BleClientCallback
import com.lanars.pdpblechatapp.ble.client.BleClientManager
import com.lanars.pdpblechatapp.ble.server.BleServerCallback
import com.lanars.pdpblechatapp.ble.server.BleServerManager

class ChatViewModel(application: Application) : AndroidViewModel(application),
    BleServerCallback, BleClientCallback {
    private lateinit var mode: BleMode

    val discoveredDevicesLiveData = MutableLiveData<BluetoothDevice>()
    val incomingMessageLiveData = MutableLiveData<String>()
    val outgoingMessageLiveData = MutableLiveData<String>()
    val isServerConnectedLiveData = MutableLiveData<Boolean>()
    val isClientConnectedLiveData = MutableLiveData<Boolean>()

    val serverLiveData = MutableLiveData<String>()

    fun advertise() {
        mode = BleMode.SERVER

        BleServerManager.init(getApplication(), this)
    }

    fun discover() {
        mode = BleMode.CLIENT

        BleClientManager.init(getApplication(), this)
    }

    fun onDeviceSelected(device: BluetoothDevice) {
        BleClientManager.connect(device)
    }

    fun onSendButtonClicked(input: String) {
        if (input.isNotEmpty()) {
            if (mode == BleMode.SERVER) {
                BleServerManager.sendMultiDataMessage(input)
            } else if (mode === BleMode.CLIENT) {
                BleClientManager.sendMultiDataMessage(input.toByteArray())
            }
            showOutgoingMessage(input)
        }
    }

    private fun showOutgoingMessage(message: String) {
        outgoingMessageLiveData.postValue(message)
    }

    // BleServerCallback
    override fun onInitServerSuccess() {
        BleServerManager.initService()
        BleServerManager.startAdvertising()
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        //show msg
        BleServerManager.stopAdvertising()
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        //show msg
    }

    override fun onConnectionError(error: String) {
        //show msg
    }

    override fun onIncomingMessage(msg: String) {
        incomingMessageLiveData.postValue(msg)
    }

    override fun isServerConnected(isConnected: Boolean) {
        if (mode == BleMode.CLIENT) return
        isServerConnectedLiveData.postValue(isConnected)
    }

    override fun serverStarted() {
        serverLiveData.postValue("")
    }

    override fun serverFailed(error: String) {
        serverLiveData.postValue(error)
    }

    // BleClientCallback
    override fun onInitClientSuccess() {
        BleClientManager.startScan()
    }

    override fun onScanResults(bluetoothDevice: BluetoothDevice) {
        discoveredDevicesLiveData.postValue(bluetoothDevice)
    }

    override fun incomingMessage(msg: String) {
        incomingMessageLiveData.postValue(msg)
    }

    override fun isClientConnected(isConnected: Boolean) {
        if (mode == BleMode.SERVER) return
        isClientConnectedLiveData.postValue(isConnected)
    }

    override fun onCleared() {
        BleClientManager.onCleared()
        BleServerManager.onCleared()
        super.onCleared()
    }
}