package com.jerry.androidble.request

import android.bluetooth.BluetoothDevice
import com.jerry.androidble.BLE
import com.jerry.androidble.BleDevice
import com.jerry.androidble.Dependency
import com.jerry.androidble.Provider
import com.jerry.androidble.callback.BleMtuCallback

class MtuRequest<T : BleDevice> private constructor() : BleMtuCallback<T> {

    private val TAG = "MtuRequest"
    private lateinit var mtuCallback: ListenerBuilder

    init {

    }

    companion object : Dependency<MtuRequest<BleDevice>> by Provider({
            MtuRequest<BleDevice>()
        })

    fun setMtu(device: T, mtu: Int, listenerBuilder: (ListenerBuilder.() -> Unit)?=null) {
        val bleService = BLE.instance.getBleService()
        bleService.setMTU(device.address, mtu)
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