package com.jerry.ble.callback

import android.bluetooth.BluetoothDevice
import com.jerry.ble.BleStates

interface BleConnectCallback<T> {
    fun onConnectionChanged(device: BluetoothDevice, status: BleStates)
    fun onConnectException(device: BluetoothDevice)
    fun onConnectTimeOut(device: BluetoothDevice)
    fun onServicesDiscovered(device: BluetoothDevice)
    fun onReady(device: BluetoothDevice)
}