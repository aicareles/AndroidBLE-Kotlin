package com.jerry.androidble.request

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.jerry.androidble.BleDevice
import com.jerry.androidble.Dependency
import com.jerry.androidble.Provider
import com.jerry.androidble.callback.BleNotifyCallback


class NotifyRequest<T: BleDevice> private constructor():
    BleNotifyCallback<T> {
    override fun onChanged(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {

    }

    override fun onReady(device: BluetoothDevice) {

    }

    override fun onServicesDiscovered(gatt: BluetoothGatt) {

    }

    override fun onNotifySuccess(gatt: BluetoothGatt) {

    }


    private val TAG = "ConnectRequest"
    private var devices = mutableListOf<T>()
    private var connectDevices = mutableListOf<T>()
    private lateinit var connectCallback: ListenerBuilder

    init {

    }

    companion object : Dependency<NotifyRequest<BleDevice>> by Provider({
            NotifyRequest<BleDevice>()
        })

    inner class ListenerBuilder {
        internal var changedAction: ((device: T, characteristic: BluetoothGattCharacteristic) -> Unit)? = null
        internal var readyAction: ((device: T) -> Unit)? = null
        internal var servicesDiscoveredAction: ((gatt: BluetoothGatt) -> Unit)? = null
        internal var notifySuccessAction: ((gatt: BluetoothGatt) -> Unit)? = null

        fun onChanged(action: (device: T, characteristic: BluetoothGattCharacteristic) -> Unit) {
            changedAction = action
        }

        fun onReady(action: (device: T) -> Unit) {
            readyAction = action
        }

        fun onServicesDiscovered(action: (gatt: BluetoothGatt) -> Unit) {
            servicesDiscoveredAction = action
        }

        fun onNotifySuccess(action: (gatt: BluetoothGatt) -> Unit) {
            notifySuccessAction = action
        }

    }

    fun connect(device: T, listenerBuilder: (ListenerBuilder.() -> Unit)?=null){
        addBleDevice(device)
        if (listenerBuilder != null){
            connectCallback = ListenerBuilder().also (listenerBuilder)
        }
        var result = false


    }

    fun getBleDevice(address: String?) : T?{
        if (address == null)return null
        devices.forEach { d->
            if (address == d.address){
                return d
            }
        }
        return null
    }

    private fun addBleDevice(device: T?) {
        if (device == null) throw IllegalArgumentException("device is not null")

    }


}