package com.lanars.pdpblechatapp.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.*

object BleChatGattProfile {

    val SERVICE_UUID: UUID = UUID.fromString("1706BBC0-88AB-4B8D-877E-2237916EE929")
    val CHARACTERISTIC_MESSAGE_UUID: UUID = UUID.fromString("275348FB-C14D-4FD5-B434-7C3F351DEA5F")
    private val DESCRIPTOR_MESSAGE_UUID: UUID = UUID.fromString("45bda094-ff40-4cb8-835d-0da8742bb1eb")

    fun createService(): BluetoothGattService {
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // Current Time characteristic
        val msgCharacteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_MESSAGE_UUID,
            //Read/write characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val configDescriptor = BluetoothGattDescriptor(
            DESCRIPTOR_MESSAGE_UUID,
            //Read/write descriptor
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        msgCharacteristic.addDescriptor(configDescriptor)

        service.addCharacteristic(msgCharacteristic)

        return service
    }
}