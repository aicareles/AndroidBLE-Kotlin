package com.jerry.ble.callback

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

interface BleNotifyCallback<T> {

    fun onChanged(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic)

    fun onNotifySuccess(device: BluetoothDevice)

    fun onNotifyCanceled(device: BluetoothDevice)

}