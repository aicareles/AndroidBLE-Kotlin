package com.jerry.ble.request

import android.bluetooth.BluetoothDevice
import com.jerry.ble.*
import com.jerry.ble.callback.BleConnectCallback

class ConnectRequest<T: BleDevice> private constructor():
    BleConnectCallback<T> {

    private val TAG = "ConnectRequest"
    private var devices = mutableListOf<T>()
    private var connectedDevices = mutableListOf<T>()
    private lateinit var connectCallback: ListenerBuilder

    init {

    }

    companion object : Dependency<ConnectRequest<BleDevice>> by Provider({
            ConnectRequest<BleDevice>()
        })


    override fun onConnectionChanged(device: BluetoothDevice, status: BleStates) {
        val bleDevice: T = getBleDevice(device.address) ?: return
        bleDevice.connectState = status
        when(status){
            BleStates.CONNECTED ->{
                connectedDevices.add(bleDevice)
                L.e(TAG, "CONNECTED>>>> " + bleDevice.name)
            }
            BleStates.DISCONNECT ->{
                connectedDevices.remove(bleDevice)
                devices.remove(bleDevice)
                L.e(TAG, "DISCONNECT>>>> " + bleDevice.name)
            }
            else -> {}
        }
        connectCallback.connectionChangedAction?.invoke(bleDevice)
    }

    override fun onConnectException(device: BluetoothDevice) {
        val bleDevice: T = getBleDevice(device.address) ?: return
        val errorCode: BleStates
        when(bleDevice.connectState){
            BleStates.CONNECTED ->{
                errorCode = BleStates.ConnectException
            }
            BleStates.CONNECTING ->{
                errorCode = BleStates.ConnectFailed
            }
            else -> errorCode = BleStates.ConnectError
        }
        connectCallback.connectExceptionAction?.invoke(bleDevice, errorCode)
    }

    override fun onConnectTimeOut(device: BluetoothDevice) {
        val bleDevice: T = getBleDevice(device.address) ?: return
        connectCallback.connectTimeOutAction?.invoke(bleDevice)
        onConnectionChanged(device, BleStates.DISCONNECT)
    }

    override fun onServicesDiscovered(device: BluetoothDevice) {
        val bleDevice: T = getBleDevice(device.address) ?: return
        connectCallback.servicesDiscoveredAction?.invoke(bleDevice)
    }

    override fun onReady(device: BluetoothDevice) {
        val bleDevice: T = getBleDevice(device.address) ?: return
        connectCallback.readyAction?.invoke(bleDevice)
    }

    inner class ListenerBuilder {
        internal var connectionChangedAction: ((device: T) -> Unit)? = null
        internal var connectExceptionAction: ((device: T, errorCode: BleStates) -> Unit)? = null
        internal var connectTimeOutAction: ((device: T) -> Unit)? = null
        internal var servicesDiscoveredAction: ((device: T) -> Unit)? = null
        internal var readyAction: ((device: T) -> Unit)? = null

        fun onConnectionChanged(action: (device: T) -> Unit) {
            connectionChangedAction = action
        }

        fun onConnectException(action: (device: T, errorCode: BleStates) -> Unit) {
            connectExceptionAction = action
        }

        fun onConnectTimeOut(action: (device: T) -> Unit) {
            connectTimeOutAction = action
        }

        fun onReady(action: (device: T) -> Unit) {
            readyAction = action
        }

        fun onServicesDiscovered(action: (device: T) -> Unit) {
            servicesDiscoveredAction = action
        }

    }

    fun connect(device: T, listenerBuilder: (ListenerBuilder.() -> Unit)?=null): Boolean{
        addBleDevice(device)
        if (listenerBuilder != null){
            connectCallback = ListenerBuilder().also (listenerBuilder)
        }
        val bleService = BLE.instance.getBleService()
        return bleService.connect(device.address)
    }

    fun connect(address: String, listenerBuilder: (ListenerBuilder.() -> Unit)?=null): Boolean{
        val newDevice = BleDevice.newDevice(address) as T
        return connect(newDevice, listenerBuilder)
    }

    fun disconnect(device: T,listenerBuilder: (ListenerBuilder.() -> Unit)?=null){
        if (listenerBuilder != null){
            connectCallback = ListenerBuilder().also (listenerBuilder)
        }
        val bleService = BLE.instance.getBleService()
        bleService.disconnect(device.address)
    }

    fun getConnectedDevices(): List<T> {
        return connectedDevices
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

    private fun addBleDevice(device: T?): Boolean{
        if (device == null) throw IllegalArgumentException("device is not null")
        if (getBleDevice(device.address) != null){
            L.i(TAG, "addBleDevice>>>> Already contains the device")
            return true
        }
        L.i(TAG, "addBleDevice>>>> Added a device to the device pool")
        return devices.add(device)
    }


}