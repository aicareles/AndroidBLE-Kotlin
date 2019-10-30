package com.jerry.androidble.request

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import com.jerry.androidble.BLE
import com.jerry.androidble.BleDevice
import com.jerry.androidble.Dependency
import com.jerry.androidble.Provider
import com.jerry.androidble.callback.BleReadCallback

class ReadRequest<T: BleDevice> private constructor(): BleReadCallback<T> {

    private val TAG = "ReadRequest"
    private lateinit var readCallback: ListenerBuilder
    private lateinit var readRssiCallback: RssiListenerBuilder

    init {

    }

    companion object : Dependency<ReadRequest<BleDevice>> by Provider({
            ReadRequest<BleDevice>()
        })

    override fun onReadSuccess(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic
    ) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        readCallback.readSuccessAction?.invoke(bleDevice, characteristic)
    }

    override fun onReadFailed(device: BluetoothDevice, states: Int) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        readCallback.readFailedAction?.invoke(bleDevice, states)
    }

    override fun onReadRssiSuccess(device: BluetoothDevice, rssi: Int) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        readRssiCallback.readRssiSuccessAction?.invoke(bleDevice, rssi)
    }

    override fun onReadRssiFailed(device: BluetoothDevice, states: Int) {
        val bleDevice = ConnectRequest.get().getBleDevice(device.address) as T
        readRssiCallback.readRssiFailedAction?.invoke(bleDevice, states)
    }

    inner class ListenerBuilder {
        internal var readSuccessAction: ((device: T, characteristic: BluetoothGattCharacteristic) -> Unit)? = null
        internal var readFailedAction: ((device: T, states: Int) -> Unit)? = null

        fun onReadFailed(action: (device: T, states: Int) -> Unit) {
            readFailedAction = action
        }

        fun onReadSuccess(action: (device: T, characteristic: BluetoothGattCharacteristic) -> Unit) {
            readSuccessAction = action
        }

    }

    inner class RssiListenerBuilder {
        internal var readRssiSuccessAction: ((device: T, rssi: Int) -> Unit)? = null
        internal var readRssiFailedAction: ((device: T, states: Int) -> Unit)? = null

        fun onReadRssiSuccess(action: (device: T, rssi: Int) -> Unit) {
            readRssiSuccessAction = action
        }

        fun onReadRssiFailed(action: (device: T, states: Int) -> Unit) {
            readRssiFailedAction = action
        }
    }

    fun read(device: T, listenerBuilder: (ListenerBuilder.() -> Unit)?=null): Boolean{
        if (listenerBuilder != null){
            readCallback = ListenerBuilder().also (listenerBuilder)
        }
        val bleService = BLE.instance.getBleService()
        return bleService.readCharacteristic(device.address)
    }

    fun readRssi(device: T, listenerBuilder: RssiListenerBuilder.() -> Unit): Boolean{
        readRssiCallback = RssiListenerBuilder().also(listenerBuilder)
        val bleService = BLE.instance.getBleService()
        return bleService.readRssi(device.address)
    }


}