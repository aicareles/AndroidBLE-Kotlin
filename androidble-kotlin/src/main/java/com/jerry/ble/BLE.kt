package com.jerry.ble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.jerry.ble.request.*
import com.jerry.ble.request.MtuRequest
import java.util.*

class BLE<T: BleDevice> private constructor(){
    val TAG = "BLE"
    private lateinit var context: Context
    private lateinit var options: Options
    private lateinit var bluetoothObserver: BluetoothObserver

    companion object{
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            BLE<BleDevice>()
        }

        fun create(context: Context, options: Options= options()): BLE<BleDevice> {
            instance.init(context, options)
            return instance
        }

        fun options(): Options {
            return Options.options
        }
    }

    private fun init(context: Context, options: Options){
        this.context = context
        this.options = options
        L.init(options)
        BleRequestImpl.get().init(options, context)
    }

    fun setBluetoothCallback(context: Context, listenerBuilder: BluetoothObserver.ListenerBuilder.() -> Unit) {
        this.bluetoothObserver = BluetoothObserver(context)
        this.bluetoothObserver.setBluetoothCallback(listenerBuilder)
        this.bluetoothObserver.registerReceiver()
    }

    fun destory(){
        if (this::context.isInitialized){
            bluetoothObserver.unregisterReceiver()
            BleRequestImpl.get().close()
        }else {
            throw BleException("Please initialize, like this 'BLE.options()...create()' ")
        }
    }

    fun isSupportBle(): Boolean {
        return BluetoothAdapter.getDefaultAdapter() != null && context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBleEnable(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

    fun openBluetooth(activity: Activity, requestCode: Int){
        if (!isBleEnable()){
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    fun getContext(): Context{
        return context
    }

    fun getConnectedDevices(): List<T> {
        return ConnectRequest.get().getConnectedDevices() as List<T>
    }

    fun getBleDevice(address: String): T?{
        return ConnectRequest.get().getBleDevice(address) as T
    }

    fun scanning(): Boolean {
        return ScanRequest.get().isScanning()
    }

    fun startScan(listenerBuilder: ScanRequest<BleDevice>.ListenerBuilder.() -> Unit, scanPeriod: Long=options.scanPeriod){
        ScanRequest.get().startScan(listenerBuilder, scanPeriod)
    }

    fun stopScan(){
        ScanRequest.get().stopScan()
    }

    @Synchronized
    fun connect(device: T, listenerBuilder: (ConnectRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        ConnectRequest.get().connect(device, listenerBuilder)
    }

    @Synchronized
    fun connect(address: String, listenerBuilder: (ConnectRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        ConnectRequest.get().connect(address, listenerBuilder)
    }

    fun disconnect(device: T, listenerBuilder: (ConnectRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        ConnectRequest.get().disconnect(device, listenerBuilder)
    }

    fun enableNotify(device: T, listenerBuilder: (NotifyRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        NotifyRequest.get().enableNotify(device, listenerBuilder)
    }

    fun enableNotifyByUUID(address: String, serviceUUID: UUID, characteristicUUID: UUID, enable: Boolean=true, listenerBuilder: (NotifyRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        NotifyRequest.get().enableNotifyByUUID(address, serviceUUID, characteristicUUID, enable, listenerBuilder)
    }

    fun read(device: T, listenerBuilder: (ReadRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        ReadRequest.get().read(device, listenerBuilder)
    }

    fun readByUUID(address: String, serviceUUID: UUID, characteristicUUID: UUID,listenerBuilder: (ReadRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        ReadRequest.get().readByUUID(address, serviceUUID, characteristicUUID, listenerBuilder)
    }

    fun readRssi(device: T, listenerBuilder: (ReadRequest<BleDevice>.RssiListenerBuilder.() -> Unit)?=null){
        ReadRequest.get().readRssi(device, listenerBuilder)
    }

    fun write(device: T, value: ByteArray, listenerBuilder: (WriteRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        WriteRequest.get().write(device, value, listenerBuilder)
    }

    fun writeByUUID(address: String, serviceUUID: UUID, characteristicUUID: UUID, value: ByteArray, listenerBuilder: (WriteRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        WriteRequest.get().writeByUUID(address, serviceUUID, characteristicUUID, value, listenerBuilder)
    }

    fun setMtu(device: T, mtu: Int, listenerBuilder: (MtuRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null) {
        MtuRequest.get().setMtu(device, mtu, listenerBuilder)
    }

    /**
     * 蓝牙相关参数配置类
     */
    class Options {

        companion object {
            val options by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
                Options()
            }
        }
        /**
         * 是否打印蓝牙日志
         */
        var logBleEnable = true
        /**
         * 日志TAG，用于过滤日志信息
         */
        var logTAG = "AndroidBLE"
        /**
         * 是否抛出蓝牙异常
         */
        var throwBleException = true
        /**
         * 是否在蓝牙异常断开时自动连接
         */
        var autoConnect = false
        /**
         * 蓝牙连接超时时长
         */
        var connectTimeout = 10 * 1000L
        /**
         * 蓝牙扫描周期时长
         */
        var scanPeriod = 12 * 1000L
        /**
         * 蓝牙连接失败重试次数
         */
        var connectFailedRetryCount = 3
        /**
         * 是否过滤重复设备
         */
        var filterRepeat = false
        /**
         * 根据设备名称过滤指定设备
         */
        var filterByName = ""

        var uuidServicesExtra = arrayOf<UUID>()
        var uuidService = UUID.fromString("0000fd00-0000-1000-8000-00805f9b34fb")
        var uuidWriteCha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601")
        var uuidReadCha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129602")
//        var uuidNotify = UUID.fromString("d44bc439-abfd-45a2-b575-925416129603")
//        var uuidNotifyDesc = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        fun scanPeriod(scanPeriod: Long) = apply {
            this.scanPeriod = scanPeriod
        }

        fun logTAG(logTAG: String)= apply  {
            this.logTAG = logTAG
        }

        fun logBleEnable(logBleEnable: Boolean)= apply  {
            this.logBleEnable = logBleEnable
        }

        fun throwBleException(throwBleException: Boolean)= apply  {
            this.throwBleException = throwBleException
        }

        fun autoConnect(autoConnect: Boolean)= apply  {
            this.autoConnect = autoConnect
        }

        fun connectTimeout(connectTimeout: Long)= apply  {
            this.connectTimeout = connectTimeout
        }

        fun connectFailedRetryCount(connectFailedRetryCount: Int)= apply  {
            this.connectFailedRetryCount = connectFailedRetryCount
        }

        fun filterRepeat(filterRepeat: Boolean)= apply  {
            this.filterRepeat = filterRepeat
        }

        fun filterByName(name: String)= apply  {
            this.filterByName = name
        }

        fun uuidServicesExtra(uuid_services_extra: Array<UUID>)= apply  {
            this.uuidServicesExtra = uuid_services_extra
        }

        fun uuidService(uuid_service: UUID)= apply  {
            this.uuidService = uuid_service
        }

        fun uuidWriteCha(uuid_write_cha: UUID)= apply {
            this.uuidWriteCha = uuid_write_cha
        }

        fun uuidReadCha(uuid_read_cha: UUID)= apply  {
            this.uuidReadCha = uuid_read_cha
        }

        fun create(context: Context): BLE<BleDevice> {
            return BLE.create(context)
        }

    }

}