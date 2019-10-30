package com.jerry.androidble.request

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import com.jerry.androidble.*
import java.util.ArrayList

class ScanRequest<T : BleDevice> private constructor() {
    private val TAG = "ScanRequest"

    private var scanning: Boolean = false
    private var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var scanner: BluetoothLeScanner? = null
    private var scannerSetting: ScanSettings? = null
//    private var scanCallback: BleScanCallback<T>? = null
    private var scannerCallback: BLEScanCallback? = null
    private var filters: MutableList<ScanFilter>? = null
    private val scanDevices = ArrayList<T>()
    private lateinit var scanCallback: ListenerBuilder

    companion object : Dependency<ScanRequest<BleDevice>> by Provider({
            ScanRequest<BleDevice>()
        })

    inner class ListenerBuilder {
        internal var startAction: (() -> Unit)? = null
        internal var stopAction: (() -> Unit)? = null
        internal var scanAction: ((device: T, rssi: Int, scanRecord: ByteArray?) -> Unit)? = null
        internal var scanFailedAction: ((errorCode: Int) -> Unit)? = null

        fun onStart(action: () -> Unit) {
            startAction = action
        }

        fun onStop(action: () -> Unit) {
            stopAction = action
        }

        fun onLeScan(action: (device: T, rssi: Int, scanRecord: ByteArray?) -> Unit) {
            scanAction = action
        }

        fun onScanFailed(action: (errorCode: Int) -> Unit) {
            scanFailedAction = action
        }
    }

    init {
        isSupportsLollipop {
            scanner = bluetoothAdapter.bluetoothLeScanner
            scannerSetting = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scannerCallback = BLEScanCallback()
            filters = ArrayList()
        }
    }

    fun startScan(listenerBuilder: (ListenerBuilder.() -> Unit)?=null, scanPeriod: Long) {
        if (scanning) return
        if (listenerBuilder != null){
            scanCallback = ListenerBuilder().also(listenerBuilder)
        }
        scanning = true
        Handler().postDelayed({
            if (scanning)
                stopScan()
        }, scanPeriod)
        if (!bluetoothAdapter.isEnabled) {
            scanCallback.scanFailedAction?.invoke(-1)
        } else {
            supportsLollipop({
                setScanSettings()
                scanner?.startScan(filters, scannerSetting, scannerCallback)
            }, {
                bluetoothAdapter.startLeScan(mLeScanCallback)
            })
            scanCallback.startAction?.invoke()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun setScanSettings() {
        val background =
            isBackground(BLE.instance.getContext())
        L.i(TAG, "currently in the background:>>>>>$background")
        if (background) {
            val uuidService = BLE.options().uuidService
            filters?.add(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(uuidService.toString()))//8.0以上手机后台扫描，必须开启
                    .build()
            )
            scannerSetting = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()
        } else {
            filters = ArrayList()
            scannerSetting = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        }
    }

    fun stopScan() {
        if (!scanning) return
        scanning = false
        if (!bluetoothAdapter.isEnabled) {
            scanCallback.scanFailedAction?.invoke(-1)
        } else {
            supportsLollipop({
                scanner?.stopScan(scannerCallback)
            }, {
                bluetoothAdapter.stopLeScan(mLeScanCallback)
            })
        }
        scanDevices.clear()
        scanCallback.stopAction?.invoke()
    }

    fun isScanning(): Boolean {
        return scanning
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private inner class BLEScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val scanRecord = result.scanRecord?.bytes
            dispatcherScanResult(device, result.rssi, scanRecord)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (sr in results) {
                L.i("ScanResult - Results", sr.toString())
            }
        }

        override fun onScanFailed(errorCode: Int) {
            L.e("Scan Failed", "Error Code: $errorCode")
            scanCallback.scanFailedAction?.invoke(errorCode)
        }
    }

    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        dispatcherScanResult(
            device,
            rssi,
            scanRecord
        )
    }

    private fun dispatcherScanResult(
        device: BluetoothDevice?,
        rssi: Int,
        scanRecord: ByteArray?
    ) {
        if (device == null) return
        var bleDevice = getDevice(device.address)
        if (bleDevice == null) {
            bleDevice = BleDevice(device) as T
            scanCallback.scanAction?.invoke(bleDevice, rssi, scanRecord)
            scanDevices.add(bleDevice)
        } else {
            if (!BLE.options().isFilterScan) //无需过滤
                scanCallback.scanAction?.invoke(bleDevice, rssi, scanRecord)
        }
    }

    //获取已扫描到的设备（重复设备）
    private fun getDevice(address: String): T? {
        scanDevices.forEach {
            if (it.address == address) return@forEach
        }
        return null
    }
}