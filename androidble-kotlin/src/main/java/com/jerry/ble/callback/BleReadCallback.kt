package com.jerry.ble.callback

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

interface BleReadCallback<T> {
    fun onReadSuccess(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic)
    fun onReadFailed(device: BluetoothDevice, states: Int)
    fun onReadRssiSuccess(device: BluetoothDevice, rssi: Int)
    fun onReadRssiFailed(device: BluetoothDevice, states: Int)
}