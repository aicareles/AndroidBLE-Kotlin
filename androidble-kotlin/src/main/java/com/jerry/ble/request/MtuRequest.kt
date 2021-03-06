package com.jerry.ble.request

import android.bluetooth.BluetoothDevice
import com.jerry.ble.*
import com.jerry.ble.BleRequestImpl
import com.jerry.ble.callback.BleMtuCallback

class MtuRequest<T : BleDevice> private constructor() : BleMtuCallback<T> {

    private val TAG = "MtuRequest"
    private lateinit var mtuCallback: ListenerBuilder

    init {

    }

    companion object : Dependency<MtuRequest<BleDevice>> by Provider({
            MtuRequest<BleDevice>()
        })

    fun setMtu(device: T, mtu: Int, listenerBuilder: (ListenerBuilder.() -> Unit)?=null) {
        if (listenerBuilder != null){
            mtuCallback = ListenerBuilder().also (listenerBuilder)
        }
        BleRequestImpl.get().setMtu(device.address, mtu)
    }

    override fun onMtuChanged(device: BluetoothDevice, mtu: Int, status: Int) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        mtuCallback.mtuChangedAction?.invoke(bleDevice, mtu, status)
    }


    inner class ListenerBuilder {
        internal var mtuChangedAction: ((device: T, mtu: Int, status: Int) -> Unit)? = null

        fun onMtuChanged(action: (device: T, mtu: Int, status: Int) -> Unit) {
            mtuChangedAction = action
        }
    }

}