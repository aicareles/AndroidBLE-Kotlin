package com.jerry.ble.callback

interface BleScanCallback<T> {
    fun onStart(){}
    fun onStop(){}
    fun onLeScan(device: T, rssi: Int, scanRecord: ByteArray?)
    fun onScanFailed(errorCode: Int){}
}