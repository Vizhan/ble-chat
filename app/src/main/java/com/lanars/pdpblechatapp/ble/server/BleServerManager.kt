package com.lanars.pdpblechatapp.ble.server

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import com.lanars.pdpblechatapp.ble.BleChatGattProfile
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset


object BleServerManager {

    private lateinit var bleServerManagerCallback: BleServerCallback

    private var context: Application? = null
    private var connectedDevice: BluetoothDevice? = null

    private var bleAdapter: BluetoothAdapter? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null

    private lateinit var bleGattServer: BluetoothGattServer

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private val bleManager: BluetoothManager by lazy { context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

    fun init(context: Application?, bleServerCallback: BleServerCallback) {
        context ?: throw RuntimeException("Context cannot be null")

        bleAdapter = BluetoothAdapter.getDefaultAdapter()
        bleAdapter ?: throw RuntimeException("Bluetooth not supported in this device")

        bleAdvertiser = bleAdapter?.bluetoothLeAdvertiser
        bleAdvertiser ?: throw RuntimeException("Error initializing BLE Advertiser")

        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw RuntimeException("Bluetooth LE is not supported in this devices")
        }

        BleServerManager.context = context
        bleServerManagerCallback = bleServerCallback

        if (bleAdapter?.isDisabled == true) {
            bleServerManagerCallback.enableBluetooth()
        } else {
            bleServerManagerCallback.onInitServerSuccess()
        }
    }

    private val advertiserCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i("+++ SERVER", "LE Advertise Server Started.")
            bleServerManagerCallback.serverStarted()
        }

        override fun onStartFailure(errorCode: Int) {
            Log.i("+++ SERVER", "LE Advertise Server Failed: $errorCode")
            bleServerManagerCallback.serverFailed("LE Advertise Server Failed: $errorCode")
        }
    }

    fun startAdvertising() {
        bleAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .build()

            val data = AdvertiseData.Builder()
                    .setIncludeDeviceName(false)
//                    .setIncludeTxPowerLevel(false)
                    .addServiceUuid(ParcelUuid(BleChatGattProfile.SERVICE_UUID))
                    .build()

            it.startAdvertising(settings, data, advertiserCallback)
        }
                ?: Log.i("+++ SERVER", "Failed to create advertiser")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.i("+++ SERVER", "onConnectionStateChange $status $newState")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    connectedDevice = device

                    bleServerManagerCallback.onDeviceConnected(device)
                    bleServerManagerCallback.isServerConnected(true)
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    connectedDevice = null

                    bleServerManagerCallback.onDeviceDisconnected(device)
                    bleServerManagerCallback.isServerConnected(false)
                }
            } else {
                val error = "Error: $status"
                bleServerManagerCallback.onConnectionError(error)
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.i("+++ SERVER", "onCharacteristicReadRequest " + characteristic.uuid.toString())

            val value = ByteArray(0)
            bleGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            Log.i("+++ SERVER", "onMtuChanged")
            super.onMtuChanged(device, mtu)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            Log.i("+++ SERVER", "onCharacteristicWriteRequest " + characteristic.uuid.toString())
            var gatResult = BluetoothGatt.GATT_SUCCESS
            try {
                if (BleChatGattProfile.CHARACTERISTIC_MESSAGE_UUID == characteristic.uuid) {
                    val msg = String(value, Charset.forName("UTF-8"))

                    bleServerManagerCallback.onIncomingMessage(msg)
                }
            } catch (ex: UnsupportedEncodingException) {
                bleServerManagerCallback.onConnectionError(ex.toString())
                gatResult = BluetoothGatt.GATT_FAILURE
            } finally {
                if (responseNeeded) {
                    bleGattServer.sendResponse(device, requestId, gatResult, offset, value)
                }
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            Log.i("+++ SERVER", "onDescriptorWriteRequest")
            if (responseNeeded) {
                bleGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    fun initService() {
        bleGattServer = bleManager.openGattServer(context, gattServerCallback)
        bleGattServer.addService(BleChatGattProfile.createService())
    }

    fun stopAdvertising() {
        bleAdvertiser?.stopAdvertising(advertiserCallback)
    }

    fun sendMessage(message: String) {
        val msgCharacteristic = bleGattServer
                .getService(BleChatGattProfile.SERVICE_UUID)
                .getCharacteristic(BleChatGattProfile.CHARACTERISTIC_MESSAGE_UUID)

        msgCharacteristic.setValue(message)

        bleGattServer.notifyCharacteristicChanged(connectedDevice, msgCharacteristic, false)

        bleServerManagerCallback.onOutgoingMessage(message)

        //

    }

    fun onCleared() {
        context = null
        connectedDevice = null
        bleAdapter = null
        bleAdvertiser = null

        bleGattServer.cancelConnection(connectedDevice)
        bleGattServer.close()
//        bleGattServer.clearServices()
    }
}