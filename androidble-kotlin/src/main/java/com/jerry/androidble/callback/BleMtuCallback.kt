package com.jerry.androidble.callback

import android.bluetooth.BluetoothDevice

interface BleMtuCallback<T> {
    fun onMtuChanged(device: BluetoothDevice, mtu: Int, status: Int)
}