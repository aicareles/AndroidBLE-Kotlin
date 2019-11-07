package com.jerry.ble.request

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import com.jerry.ble.*
import com.jerry.ble.BleRequestImpl
import com.jerry.ble.callback.BleWriteCallback
import java.util.*


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

    override fun onWriteFailed(device: BluetoothDevice?, states: Int) {
        val bleDevice = ConnectRequest.get().getBleDevice(device?.address) as T
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

    fun write(device: T, value: ByteArray, listenerBuilder: (ListenerBuilder.() -> Unit)?=null): Boolean{
        if (listenerBuilder != null){
            writeCallback = ListenerBuilder().also (listenerBuilder)
        }
        return BleRequestImpl.get().wirteCharacteristic(device.address, value)
    }

    fun writeByUUID(address: String, serviceUUID: UUID, characteristicUUID: UUID, value: ByteArray, listenerBuilder: (ListenerBuilder.() -> Unit)?=null): Boolean{
        if (listenerBuilder != null){
            writeCallback = ListenerBuilder().also (listenerBuilder)
        }
        return BleRequestImpl.get().wirteCharacteristicByUUID(address, serviceUUID, characteristicUUID, value)
    }

}