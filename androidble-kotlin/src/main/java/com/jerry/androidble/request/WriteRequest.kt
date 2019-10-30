package com.jerry.androidble.request

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import com.jerry.androidble.BLE
import com.jerry.androidble.BleDevice
import com.jerry.androidble.Dependency
import com.jerry.androidble.Provider
import com.jerry.androidble.callback.BleWriteCallback


class WriteRequest<T: BleDevice> private constructor(): BleWriteCallback<T> {

    private val TAG = "WriteRequest"
    private lateinit var writeCallback: ListenerBuilder

    init {

    }

    companion object : Dependency<WriteRequest<BleDevice>> by Provider({
            WriteRequest<BleDevice>()
        })


    override fun onWriteSuccess(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        writeCallback.writeSuccessAction?.invoke(bleDevice, characteristic)
    }

    override fun onWriteFailed(device: BluetoothDevice, states: Int) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        writeCallback.writeFailedAction?.invoke(bleDevice, states)
    }

    inner class ListenerBuilder {
        internal var writeSuccessAction: ((device: T, characteristic: BluetoothGattCharacteristic) -> Unit)? = null
        internal var writeFailedAction: ((device: T, states: Int) -> Unit)? = null

        fun onWriteFailed(action: (device: T, states: Int) -> Unit) {
            writeFailedAction = action
        }

        fun onWriteSuccess(action: (device: T, characteristic: BluetoothGattCharacteristic) -> Unit) {
            writeSuccessAction = action
        }

    }

    fun write(device: T, listenerBuilder: (ListenerBuilder.() -> Unit)?=null): Boolean{
        if (listenerBuilder != null){
            writeCallback = ListenerBuilder().also (listenerBuilder)
        }
        val bleService = BLE.instance.getBleService()
        return bleService.readCharacteristic(device.address)
    }

}