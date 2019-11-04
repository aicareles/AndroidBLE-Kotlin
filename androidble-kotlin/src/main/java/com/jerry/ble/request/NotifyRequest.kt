package com.jerry.ble.request

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import com.jerry.ble.BLE
import com.jerry.ble.BleDevice
import com.jerry.ble.Dependency
import com.jerry.ble.Provider
import com.jerry.ble.callback.BleNotifyCallback


class NotifyRequest<T: BleDevice> private constructor():
    BleNotifyCallback<T> {

    private val TAG = "NotifyRequest"
    private lateinit var notifyCallback: ListenerBuilder

    init {

    }

    override fun onChanged(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        notifyCallback.changedAction?.invoke(bleDevice, characteristic)
    }

    override fun onNotifySuccess(device: BluetoothDevice) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        notifyCallback.notifySuccessAction?.invoke(bleDevice)
    }

    companion object : Dependency<NotifyRequest<BleDevice>> by Provider({
            NotifyRequest<BleDevice>()
        })

    inner class ListenerBuilder {
        internal var changedAction: ((device: T, characteristic: BluetoothGattCharacteristic) -> Unit)? = null
        internal var notifySuccessAction: ((device: T) -> Unit)? = null

        fun onChanged(action: (device: T, characteristic: BluetoothGattCharacteristic) -> Unit) {
            changedAction = action
        }

        fun onNotifySuccess(action: (device: T) -> Unit) {
            notifySuccessAction = action
        }

    }

    fun enableNotify(device: T, listenerBuilder: (ListenerBuilder.() -> Unit)?=null){
        if (listenerBuilder != null){
            notifyCallback = ListenerBuilder().also (listenerBuilder)
        }
        val bleService = BLE.instance.getBleService()
        bleService.setCharacteristicNotification(device.address, true)
    }
}