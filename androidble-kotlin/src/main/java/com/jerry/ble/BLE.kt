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
    private lateinit var bleService: BleService
    private lateinit var options: Options

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

    private fun init(context: Context, options: Options) : Boolean{
        this.context = context
        this.options = options
        L.init(options)
        return startService(context)
    }

    private fun startService(context: Context): Boolean{
        val serviceIntent = Intent(context, BleService::class.java)
        val result: Boolean
        result = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (result) L.i(TAG, "service bind succseed!!!") else{
            if (options.throwBleException){
                throw BleException("Bluetooth service binding failed, Please check whether the service is registered in the manifest file!")
            }
        }
        return result
    }

    private fun destory(){
        if (this::context.isInitialized){
            context.unbindService(serviceConnection)
        }else {
            throw BleException("Please initialize, like this 'BLE.options()...create()' ")
        }
    }

    fun getBleService(): BleService{
        return bleService
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

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bleService = (service as BleService.LocalBinder).service
            bleService.initialize(options)
            L.e(TAG, "Service connection successful")
            if (!bleService.initBLE()) {
                L.e(TAG, "Unable to initBLE Bluetooth")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
//            bleService = null
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


    fun startScan(listenerBuilder: ScanRequest<BleDevice>.ListenerBuilder.() -> Unit, scanPeriod: Long){
        ScanRequest.get().startScan(listenerBuilder, scanPeriod)
    }

    fun stopScan(){
        ScanRequest.get().stopScan()
    }

    @Synchronized
    fun connect(device: T, listenerBuilder: (ConnectRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        ConnectRequest.get().connect(device, listenerBuilder)
    }

    fun disconnect(device: T, listenerBuilder: (ConnectRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        ConnectRequest.get().disconnect(device, listenerBuilder)
    }

    fun enableNotify(device: T, listenerBuilder: (NotifyRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        NotifyRequest.get().enableNotify(device, listenerBuilder)
    }

    fun read(device: T, listenerBuilder: (ReadRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        ReadRequest.get().read(device, listenerBuilder)
    }

    fun readRssi(device: T, listenerBuilder: ReadRequest<BleDevice>.RssiListenerBuilder.() -> Unit){
        ReadRequest.get().readRssi(device, listenerBuilder)
    }

    fun write(device: T, value: ByteArray, listenerBuilder: (WriteRequest<BleDevice>.ListenerBuilder.() -> Unit)?=null){
        WriteRequest.get().write(device, value, listenerBuilder)
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
        var scanPeriod = 10 * 1000L
        /**
         * 服务绑定失败重试次数
         */
        var serviceBindFailedRetryCount = 3
        /**
         * 蓝牙连接失败重试次数
         */
        var connectFailedRetryCount = 3
        /**
         * 是否过滤扫描设备
         */
        var isFilterScan = false

        var uuidServicesExtra = arrayOf<UUID>()
        var uuidService = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb")
        var uuidWriteCha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600")
        var uuidReadCha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600")
        var uuidNotify = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601")
        var uuidNotifyDesc = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        var uuidOtaService = UUID.fromString("0000fee8-0000-1000-8000-00805f9b34fb")
        var uuidOtaNotifyCha = UUID.fromString("003784cf-f7e3-55b4-6c4c-9fd140100a16")
        var uuidOtaWriteCha = UUID.fromString("013784cf-f7e3-55b4-6c4c-9fd140100a16")

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

        fun serviceBindFailedRetryCount(serviceBindFailedRetryCount: Int)= apply  {
            this.serviceBindFailedRetryCount = serviceBindFailedRetryCount
        }

        fun connectFailedRetryCount(connectFailedRetryCount: Int)= apply  {
            this.connectFailedRetryCount = connectFailedRetryCount
        }

        fun filterScan(filterScan: Boolean)= apply  {
            isFilterScan = filterScan
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

        fun uuidNotify(uuid_notify: UUID)= apply  {
            this.uuidNotify = uuid_notify
        }

        fun uuidNotifyDesc(uuid_notify_desc: UUID)= apply  {
            this.uuidNotifyDesc = uuid_notify_desc
        }

        fun uuidOtaService(uuid_ota_service: UUID)= apply  {
            this.uuidOtaService = uuid_ota_service
        }

        fun uuidOtaNotifyCha(uuid_ota_notify_cha: UUID)= apply  {
            this.uuidOtaNotifyCha = uuid_ota_notify_cha
        }

        fun uuidOtaWriteCha(uuid_ota_write_cha: UUID)= apply  {
            this.uuidOtaWriteCha = uuid_ota_write_cha
        }

        fun create(context: Context): BLE<BleDevice> {
            return BLE.create(context)
        }

    }

}