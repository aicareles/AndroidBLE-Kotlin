package com.example.myapplication

import com.jerry.ble.BleDevice

class MyDevice(address: String) : BleDevice(address) {
    val isEnableNotify: Boolean = false

}