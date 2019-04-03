package com.lanars.pdpblechatapp.ble.client

import android.app.Application
import android.bluetooth.*
import android.os.ParcelUuid
import android.util.Log
import com.lanars.pdpblechatapp.ble.BleChatGattProfile
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*

object BleClientManager {

    private var context: Application? = null
    private var bleAdapter: BluetoothAdapter? = null
    private var bleConnectedGatt: BluetoothGatt? = null
    private lateinit var bleClientManagerCallback: BleClientCallback
    private lateinit var rxBleClient: RxBleClient
    private lateinit var rxBleDevice: RxBleDevice
    private var connectedDevice: BluetoothDevice? = null

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    fun init(context: Application?, bleClientCallback: BleClientCallback) {
        context ?: throw RuntimeException("Context cannot be null")

        bleAdapter = BluetoothAdapter.getDefaultAdapter()
        bleAdapter ?: throw RuntimeException("Bluetooth not supported on this device")

        BleClientManager.context = context
        bleClientManagerCallback = bleClientCallback
        rxBleClient = RxBleClient.create(context)

        bleClientManagerCallback.onInitClientSuccess()
    }

    private lateinit var searchDisposable: Disposable
    fun startScan() {
        searchDisposable = rxBleClient
                .scanBleDevices(
                        ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .build(),
                        ScanFilter.Builder()
                                .setServiceUuid(ParcelUuid(BleChatGattProfile.SERVICE_UUID))
                                .build()
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ scanResult ->
                    Log.d("+++", "bleClientRx.scanBleDevices - SUCCESS")
                    rxBleDevice = scanResult.bleDevice
                    bleClientManagerCallback.onScanResults(scanResult.bleDevice.bluetoothDevice)
                }, {
                    Log.e("+++", "bleClientRx.scanBleDevices - ERROR")
                })

    }

    private var gattClientCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.i("+++ CLIENT", "onConnectionStateChange $status $$newState")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                    bleClientManagerCallback.isClientConnected(true)

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    bleClientManagerCallback.isClientConnected(false)

                }
            } else {
                Log.i("+++ CLIENT", "Connection state error! : Error = $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.i("+++ CLIENT", "onServicesDiscovered")

            for (service in gatt.services) {
                Log.i("+++", "Service: " + service.uuid)
                if (BleChatGattProfile.SERVICE_UUID == service.uuid) {
                    gatt.setCharacteristicNotification(
                            service.getCharacteristic(BleChatGattProfile.CHARACTERISTIC_MESSAGE_UUID),
                            true
                    )
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.i("+++ CLIENT", "onCharacteristicRead")
            if (BleChatGattProfile.CHARACTERISTIC_MESSAGE_UUID == characteristic.uuid) {
                val msg = characteristic.getStringValue(0)

                bleClientManagerCallback.incomingMessage(msg)

                gatt.setCharacteristicNotification(characteristic, true)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i("+++ CLIENT", "onCharacteristicWrite")


            if (packetInteraction < packetSize) { // for sendMultiDataMessage function
                val characteristicData = bleConnectedGatt
                        ?.getService(BleChatGattProfile.SERVICE_UUID)
                        ?.getCharacteristic(BleChatGattProfile.CHARACTERISTIC_MESSAGE_UUID)

                characteristicData?.value = packets[packetInteraction]
                bleConnectedGatt?.writeCharacteristic(characteristicData)
                packetInteraction++
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i("+++ CLIENT", "onMtuChanged")
            Log.i("+++", "onMtuChanged mtu=$mtu")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.i("+++ CLIENT", "onCharacteristicChanged")
            Log.i("+++", "Notification of message characteristic changed on server.")
            if (BleChatGattProfile.CHARACTERISTIC_MESSAGE_UUID == characteristic.uuid) {
                bleClientManagerCallback.incomingMessage(characteristic.getStringValue(0))
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        searchDisposable.dispose()
        connectedDevice = device

        bleConnectedGatt = device.connectGatt(context, false, gattClientCallback)
//        bleConnectedGatt?.requestMtu(512) // max value 512 bytes - if supported by device.  Issue => close connection
    }

    fun onCleared() {
        if (!searchDisposable.isDisposed) searchDisposable.dispose()

        context = null
        bleAdapter = null
        bleConnectedGatt = null

        bleConnectedGatt?.disconnect()
        bleConnectedGatt?.close()
    }

    var packetSize = 0
    var packetInteraction = 0
    lateinit var packets: Array<ByteArray>

    fun sendMultiDataMessage(data: ByteArray) {
        val chunkSize = 20.0 //20 default byte chunk
        packetSize = Math.ceil(data.size / chunkSize).toInt()

        // this is use as header, so peripheral device know how much packet will be received.
        val characteristicData = bleConnectedGatt
                ?.getService(BleChatGattProfile.SERVICE_UUID)
                ?.getCharacteristic(BleChatGattProfile.CHARACTERISTIC_MESSAGE_UUID)

//        characteristicData?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // ??

        characteristicData?.value = packetSize.toString().toByteArray()

        bleConnectedGatt?.writeCharacteristic(characteristicData)
        bleConnectedGatt?.executeReliableWrite()

        packets = Array(packetSize) { ByteArray(chunkSize.toInt()) }
        packetInteraction = 0
        var start = 0

        packets.forEachIndexed { index, bytes ->
            var end = start + chunkSize
            if (end > data.size) {
                end = data.size.toDouble()
            }

            packets[index] = Arrays.copyOfRange(data, start, end.toInt())
            start += chunkSize.toInt()
        }
    }
}