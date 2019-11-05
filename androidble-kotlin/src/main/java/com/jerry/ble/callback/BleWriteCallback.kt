package com.jerry.ble.callback

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

interface BleWriteCallback<T> {
    fun onWriteSuccess(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic)
    fun onWriteFailed(device: BluetoothDevice?, states: Int)
}