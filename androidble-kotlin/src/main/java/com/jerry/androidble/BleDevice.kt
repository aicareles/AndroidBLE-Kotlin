package com.jerry.androidble

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
open class BleDevice private constructor(
    var connectState: BleStates = BleStates.DISCONNECT, var address: String,
    var name: String?, var alias: String = "", var autoConnect: Boolean = false, var autoConnectting: Boolean = false
) : Parcelable {

    val connected
        get() = connectState == BleStates.CONNECTED
    val connectting
        get() = connectState == BleStates.CONNECTING
    val disconnected
        get() = connectState == BleStates.DISCONNECT

    constructor(device: BluetoothDevice) : this(BleStates.DISCONNECT, device.address, device.name)

    constructor(address: String) : this(BleStates.DISCONNECT, address, "")

    companion object {
        fun newDevice(address: String) {
            BleDevice(address)
        }
    }


    override fun toString(): String {
        return "BleDevice(connectState=$connectState, address=$address, name=$name, alias='$alias', autoConnect=$autoConnect, autoConnectting=$autoConnectting, connected=$connected, connectting=$connectting)"
    }


}