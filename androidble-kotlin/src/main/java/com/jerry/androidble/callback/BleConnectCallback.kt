package com.jerry.androidble.callback

import android.bluetooth.BluetoothDevice
import com.jerry.androidble.BleStates

interface BleConnectCallback<T> {
    fun onConnectionChanged(device: BluetoothDevice, status: BleStates)
    fun onConnectException(device: BluetoothDevice)
    fun onConnectTimeOut(device: BluetoothDevice)
}