package com.jerry.androidble.callback

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

interface BleNotifyCallback<T> {

    fun onChanged(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic)

    /**
     * Set the notification feature to be successful and can send data
     * @param device ble device object
     */
    fun onReady(device: BluetoothDevice)

    /**
     * Set the notification here when the service finds a callback       setNotify
     * @param gatt
     */
    fun onServicesDiscovered(gatt: BluetoothGatt)

    fun onNotifySuccess(gatt: BluetoothGatt)
}