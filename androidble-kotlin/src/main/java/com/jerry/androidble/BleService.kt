package com.jerry.androidble

import android.annotation.TargetApi
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import com.jerry.androidble.callback.BleConnectCallback
import com.jerry.androidble.callback.BleMtuCallback
import com.jerry.androidble.callback.BleNotifyCallback
import com.jerry.androidble.callback.BleReadCallback
import com.jerry.androidble.request.ConnectRequest
import com.jerry.androidble.request.MtuRequest
import com.jerry.androidble.request.NotifyRequest
import com.jerry.androidble.request.ReadRequest
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BleService : Service() {
    private val TAG = BleService::class.java.simpleName
    private lateinit var options: BLE.Options
    private var handler: Handler = Handler()
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val locker = Any()
    private val notifyCharacteristics = ArrayList<BluetoothGattCharacteristic>()//Notification attribute callback array
    private var notifyIndex = 0//Notification feature callback list
    private val writeCharacteristicMap = HashMap<String, BluetoothGattCharacteristic>()
    private val readCharacteristicMap = HashMap<String, BluetoothGattCharacteristic>()
    private val timeoutTasks = HashMap<String, Runnable>()

    /**
     * Multiple device connections must put the gatt object in the collection
     */
    private val bluetoothGattMap = HashMap<String, BluetoothGatt>()
    /**
     * The address of the connected device
     */
    private val connectedAddressList = ArrayList<String>()

    private var connectCallback: BleConnectCallback<BleDevice>? = null
    private var readCallback: BleReadCallback<BleDevice>? = null
    private var bleNotifyCallback: BleNotifyCallback<BleDevice>? = null
    private var bleMtuCallback: BleMtuCallback<BleDevice>? = null

    /**
     * 在各种状态回调中发现连接更改或服务
     */
    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt, status: Int,
            newState: Int
        ) {
            val device = gatt.device
            //remove timeout callback
            val timeoutRunnable = timeoutTasks[device.address]
            if (timeoutRunnable != null) {
                timeoutTasks.remove(device.address)
                handler.removeCallbacks(timeoutRunnable)
            }
            //There is a problem here Every time a new object is generated that causes the same device to be disconnected and the connection produces two objects
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedAddressList.add(device.address)
                    connectCallback?.onConnectionChanged(device, BleStates.CONNECTED)
                    L.i(TAG, "handleMessage:>>>>>>>>CONNECTED.")
                    // Attempts to discover services after successful connection.
                    logi(TAG, "Attempting to start service discovery")
                    bluetoothGattMap[device.address]?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    L.i(TAG, "Disconnected from GATT server.")
                    connectCallback?.onConnectionChanged(device, BleStates.DISCONNECT)
                    close(device.address)
                }
            } else {
                //Occurrence 133 or 257 19 Equal value is not 0: Connection establishment failed due to protocol stack
                L.e(TAG, "onConnectionStateChange>>>>>>>>: Connection status is abnormal:$status")
                close(device.address)
                connectCallback?.apply {
                    onConnectException(device)
                    onConnectionChanged(device, BleStates.DISCONNECT)
                }
            }

        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (gatt != null && gatt.device != null) {
                L.e(TAG, "onMtuChanged mtu=$mtu,status=$status")
                bleMtuCallback?.onMtuChanged(gatt.device, mtu, status)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bleNotifyCallback?.onServicesDiscovered(gatt)
                //Empty the notification attribute list
                notifyCharacteristics.clear()
                notifyIndex = 0
                //Start setting notification feature
                displayGattServices(gatt.device.address, getSupportedGattServices(gatt.device.address)
                )
            } else {
                L.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            L.d(TAG, "onCharacteristicRead:$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCallback?.onReadSuccess(gatt.device, characteristic)
            }else {
                readCallback?.onReadFailed(gatt.device, status)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            L.i(TAG, "--------write success----- status:$status")
            synchronized(locker) {
                L.i(TAG, gatt.device.address + " -- onCharacteristicWrite: " + status)
                if (status == BluetoothGatt.GATT_SUCCESS) {

                }
            }
        }

        /**
         * 当连接成功的时候会回调这个方法，这个方法可以处理发送密码或者数据分析
         * 当setnotify（true）被设置时，如果MCU（设备端）上的数据改变，则该方法被回调。
         * @param gatt 蓝牙gatt对象
         * @param characteristic 蓝牙通知特征对象
         */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            synchronized(locker) {
                val device = gatt.device ?: return
                L.i(TAG, gatt.device.address + " -- onCharacteristicChanged: "
                            + if (characteristic.value != null) Arrays.toString(characteristic.value) else "")
                bleNotifyCallback?.onChanged(device, characteristic)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor, status: Int
        ) {
            val uuid = descriptor.characteristic.uuid
            L.i(TAG, "onDescriptorWrite------descriptor_uuid:$uuid")
            synchronized(locker) {
                L.w(TAG, " -- onDescriptorWrite: $status")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (notifyCharacteristics.size > 0 && notifyIndex < notifyCharacteristics.size) {
                        setCharacteristicNotification(
                            gatt.device.address,
                            notifyCharacteristics[notifyIndex++],
                            true
                        )
                    } else {
                        L.i(TAG, "====setCharacteristicNotification is true,ready to sendData===")
                        bleNotifyCallback?.onNotifySuccess(gatt)
                    }
                }
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)
            val uuid = descriptor.characteristic.uuid
            L.i(TAG, "onDescriptorRead")
            L.i(TAG, "descriptor_uuid:$uuid")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            println("rssi = $rssi")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCallback?.onReadRssiSuccess(gatt.device, rssi)
            }else {
                readCallback?.onReadRssiFailed(gatt.device, status)
            }
        }
    }

    /**
     *
     * @return 已经连接的设备集合
     */
    val connectedDevices: List<BluetoothDevice>?
        get() =
            if (bluetoothManager == null) null else bluetoothManager?.getConnectedDevices(
                BluetoothProfile.GATT
            )

    private val mBinder = LocalBinder()


    inner class LocalBinder : Binder() {
        val service: BleService
            get() = this@BleService
    }

    override fun onBind(intent: Intent): IBinder? {
        L.e(TAG, "onBind>>>>")
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        close()
        L.e(TAG, "onUnbind>>>>")
        return super.onUnbind(intent)
    }

    fun initialize(options: BLE.Options) {
        this.connectCallback = ConnectRequest.get()
        this.readCallback = ReadRequest.get()
        this.bleNotifyCallback = NotifyRequest.get()
        this.bleMtuCallback = MtuRequest.get()
        this.options = options
    }

    /**
     * Initialize Bluetooth
     * For API level 18 and above, get a reference to BluetoothAdapter
     * Bluetooth 4.0, that API level> = 18, and supports Bluetooth 4.0 phone can use,
     * if the mobile phone system version API level <18, is not used Bluetooth 4
     * android system 4.3 above, the phone supports Bluetooth 4.0
     * @return
     */
    fun initBLE(): Boolean {
        if (bluetoothManager == null) {
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                L.e(TAG, "Unable to initBLE BluetoothManager.")
                return false
            }
        }
        bluetoothAdapter = bluetoothManager?.adapter
        return bluetoothAdapter != null
    }

    private fun checkTimeOutTask(device: BluetoothDevice): Runnable {
        return Runnable {
            connectCallback?.onConnectTimeOut(device)
            close(device.address)
        }
    }

    /**
     * 连接蓝牙
     *
     * @param address Bluetooth address
     * @return Connection result
     */
    fun connect(address: String): Boolean {
        if (connectedAddressList.contains(address)) {
            L.d(TAG, "This is device already connected.")
            return true
        }
        if (bluetoothAdapter == null) {
            L.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        // getRemoteDevice(address) will throw an exception if the device address is invalid,
        // so it's necessary to check the address
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            L.d(TAG, "the device address is invalid")
            return false
        }
        // Previously connected device. Try to reconnect. ()
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            L.d(TAG, "no device")
            return false
        }
        //10s after the timeout prompt
        val timeOutRunnable = checkTimeOutTask(device)
        timeoutTasks[device.address] = timeOutRunnable
        handler.postDelayed(timeOutRunnable, options.connectTimeout)
        connectCallback?.onConnectionChanged(device, BleStates.CONNECTING)
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false
        val bluetoothGatt = device.connectGatt(this, false, mGattCallback)
        if (bluetoothGatt != null) {
            bluetoothGattMap[address] = bluetoothGatt
            L.d(TAG, "Trying to create a new connection.")
            return true
        }
        return false
    }

    /**
     * 断开蓝牙
     *
     * @param address 蓝牙地址
     */
    fun disconnect(address: String) {
        if (bluetoothAdapter == null || bluetoothGattMap[address] == null) {
            L.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        notifyIndex = 0
        bluetoothGattMap[address]?.disconnect()
        notifyCharacteristics.clear()
        writeCharacteristicMap.remove(address)
        readCharacteristicMap.remove(address)
    }

    /**
     * 清除蓝牙蓝牙连接设备的指定蓝牙地址
     *
     * @param address 蓝牙地址
     */
    fun close(address: String) {
        connectedAddressList.remove(address)
        bluetoothGattMap[address]?.close()
        bluetoothGattMap.remove(address)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setMTU(address: String, mtu: Int): Boolean {
        L.d(TAG, "setMTU $mtu")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mtu > 20) {
                val result = bluetoothGattMap[address]?.requestMtu(mtu)
                L.d(TAG, "requestMTU $mtu result=$result")
                return result!!
            }
        }
        return false
    }

    /**
     * 清除所有可连接的设备
     */
    fun close() {
        connectedAddressList.forEach {address->
            bluetoothGattMap[address]?.close()
        }
        bluetoothGattMap.clear()
        connectedAddressList.clear()
    }

    /**
     * 清理蓝牙缓存
     */
    fun refreshDeviceCache(address: String): Boolean {
        val gatt = bluetoothGattMap[address]
        if (gatt != null) {
            try {
                val localMethod = gatt.javaClass.getMethod(
                    "refresh", *arrayOfNulls(0)
                )
                return (localMethod.invoke(
                    gatt, *arrayOfNulls(0)
                ) as Boolean)
            } catch (localException: Exception) {
                L.i(TAG, "An exception occured while refreshing device")
            }

        }
        return false
    }


    /**
     * 写入数据
     *
     * @param address 蓝牙地址
     * @param value   发送的字节数组
     * @return 写入是否成功(这个是客户端的主观认为)
     */
    fun wirteCharacteristic(address: String, value: ByteArray): Boolean? {
        if (bluetoothAdapter == null || bluetoothGattMap[address] == null) {
            L.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        val gattCharacteristic = writeCharacteristicMap[address]
        try {
            if (options.uuidWriteCha == gattCharacteristic?.uuid) {
                gattCharacteristic?.value = value
                val result = bluetoothGattMap[address]?.writeCharacteristic(gattCharacteristic)
                L.d(TAG, address + " -- write data:" + Arrays.toString(value))
                L.d(TAG, "$address -- write result:$result")
                return result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 读取数据
     *
     * @param address 蓝牙地址
     * @return 读取是否成功(这个是客户端的主观认为)
     */
    fun readCharacteristic(address: String): Boolean {
        if (bluetoothAdapter == null || bluetoothGattMap[address] == null) {
            L.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        val gattCharacteristic = readCharacteristicMap[address]
        try {
            if (options.uuidReadCha == gattCharacteristic?.uuid) {
                val result = bluetoothGattMap[address]?.readCharacteristic(gattCharacteristic)
                L.d(TAG, "$address -- read result:$result")
                return result!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false

    }

    /**
     * 读取远程RSSI
     * @param address 蓝牙地址
     * @return 是否读取RSSI成功(这个是客户端的主观认为)
     */
    fun readRssi(address: String): Boolean {
        if (bluetoothAdapter == null || bluetoothGattMap[address] == null) {
            L.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        val result = bluetoothGattMap[address]?.readRemoteRssi()
        L.d(TAG, "$address -- read result:$result")
        return result!!
    }

    /**
     * 读取数据
     * @param address   蓝牙地址
     * @param characteristic 蓝牙特征对象
     */
    fun readCharacteristic(address: String, characteristic: BluetoothGattCharacteristic) {
        L.d(TAG, "readCharacteristic: " + characteristic.properties)
        if (bluetoothAdapter == null || bluetoothGattMap[address] == null) {
            L.d(TAG, "BluetoothAdapter is null")
            return
        }
        bluetoothGattMap[address]?.readCharacteristic(characteristic)
    }

    /**
     * 启用或禁用给定特征的通知
     *
     * @param address        蓝牙地址
     * @param characteristic 通知特征对象
     * @param enabled   是否设置通知使能
     */
    fun setCharacteristicNotification(
        address: String,
        characteristic: BluetoothGattCharacteristic, enabled: Boolean
    ) {
        if (bluetoothAdapter == null || bluetoothGattMap[address] == null) {
            L.d(TAG, "BluetoothAdapter is null")
            return
        }
        bluetoothGattMap[address]?.setCharacteristicNotification(characteristic, enabled)
        //If the number of descriptors in the eigenvalue of the notification is greater than zero
        if (characteristic.descriptors.size > 0) {
            //Filter descriptors based on the uuid of the descriptor
            val descriptors = characteristic.descriptors
            for (descriptor in descriptors) {
                //Write the description value
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                bluetoothGattMap[address]?.writeDescriptor(descriptor)
            }
        }

    }

    /**
     * 设置通知数组
     * @param address 蓝牙地址
     * @param gattServices 蓝牙服务集合
     */
    private fun displayGattServices(address: String, gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        // Loops through available GATT Services.
        for (gattService in gattServices) {
            var uuid = gattService.uuid
            L.d(TAG, "displayGattServices: $uuid")
            if (uuid == options.uuidService || isContainUUID(uuid)) {
                L.d(TAG, "service_uuid: $uuid")
                val gattCharacteristics = gattService.characteristics
                for (gattCharacteristic in gattCharacteristics) {
                    uuid = gattCharacteristic.uuid
                    L.d(TAG, "Characteristic_uuid: $uuid")
                    when(uuid){
                        options.uuidWriteCha->writeCharacteristicMap[address] = gattCharacteristic
                        options.uuidReadCha->readCharacteristicMap[address] = gattCharacteristic
                    }
                    if (gattCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                        notifyCharacteristics.add(gattCharacteristic)
                        L.e("notifyCharacteristics", "PROPERTY_NOTIFY")
                    }
                    if (gattCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                        notifyCharacteristics.add(gattCharacteristic)
                        L.e("notifyCharacteristics", "PROPERTY_INDICATE")
                    }
                }
                //Really set up notifications
                if (notifyCharacteristics.size > 0) {
                    L.e("setCharaNotification", "setCharaNotification")
                    setCharacteristicNotification(
                        address,
                        notifyCharacteristics[notifyIndex++],
                        true
                    )
                }
            }
        }
    }

    //是否包含该uuid
    private fun isContainUUID(uuid: UUID): Boolean {
        for (u in options.uuidServicesExtra) {
            if (uuid == u) {
                return true
            }
        }
        return false
    }

    /**
     * 获取可写特征对象
     * @param address 蓝牙地址
     * @return  可写特征对象
     */
    fun getWriteCharacteristic(address: String): BluetoothGattCharacteristic? {
        synchronized(locker) {
            return writeCharacteristicMap[address]
        }
    }

    /**
     * 获取可读特征对象
     * @param address 蓝牙地址
     * @return  可读特征对象
     */
    fun getReadCharacteristic(address: String): BluetoothGattCharacteristic? {
        synchronized(locker) {
            return readCharacteristicMap[address]
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after `BluetoothGatt#discoverServices()`
     * completes successfully.
     *
     * @param address ble address
     * @return A `List` of supported services.
     */
    fun getSupportedGattServices(address: String): List<BluetoothGattService>? {
        return if (bluetoothGattMap[address] == null) null else bluetoothGattMap[address]?.services

    }

    /**
     * .读取连接的远程设备的RSSI
     *
     * @param address 蓝牙地址
     * @return 读取RSSI是否成功
     */
    fun getRssiVal(address: String): Boolean? {
        return if (bluetoothGattMap[address] == null) false else bluetoothGattMap[address]?.readRemoteRssi()

    }

    //The basic method of writing data
    fun writeCharacteristic(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ): Boolean {
        synchronized(locker) {
            return !(gatt == null || characteristic == null) && gatt.writeCharacteristic(
                characteristic
            )
        }
    }

}
