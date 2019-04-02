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

    val enableBluetoothLiveData = MutableLiveData<Unit>()
    val discoveredDevicesLiveData = MutableLiveData<BluetoothDevice>()
    val incomingMessageLiveData = MutableLiveData<String>()
    val outgoingMessageLiveData = MutableLiveData<String>()
    val isConnectedLiveData = MutableLiveData<Boolean>()

    val serverLiveData = MutableLiveData<String>()
    val clientLiveData = MutableLiveData<String>()

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
                BleServerManager.sendMessage(input)
            } else if (mode === BleMode.CLIENT) {
//                BleClientManager.sendMessage(input)
                BleClientManager.sendMultiDataMessage(input.toByteArray())
            }
//            showOutgoingMessage(message)
        }
    }

    // BleServerCallback
    override fun enableBluetooth() {
        enableBluetoothLiveData.postValue(null)
    }

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

    override fun onOutgoingMessage(msg: String) {
        outgoingMessageLiveData.postValue(msg)
    }

    override fun onIncomingMessage(msg: String) {
        incomingMessageLiveData.postValue(msg)
    }

    override fun isServerConnected(isConnected: Boolean) {
        isConnectedLiveData.postValue(isConnected)
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

    override fun outgoingMessage(msg: String) {
        outgoingMessageLiveData.postValue(msg)
    }

    override fun isClientConnected(isConnected: Boolean) {
        isConnectedLiveData.postValue(isConnected)
    }

    override fun onCleared() {
        BleClientManager.onCleared()
        BleServerManager.onCleared()
        super.onCleared()
    }
}