package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import cn.com.superLei.aoparms.annotation.Permission
import cn.com.superLei.aoparms.annotation.PermissionDenied
import cn.com.superLei.aoparms.annotation.PermissionNoAskDenied
import cn.com.superLei.aoparms.common.permission.AopPermissionUtils
import com.jerry.androidble.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private val TAG = "MainActivity"
    private val UUID_SERVICE = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb")
    private val UUID_WRITE_CHA = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600")
    private val REQUEST_ENABLE_BT = 1
    private lateinit var ble: BLE<BleDevice>
    private var listDatas = mutableListOf<BleDevice>()
    private val adapter = DeviceAdapter(listDatas)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        initView()
        initLinsenter()
    }

    private fun initView(){
        recyclerview.layoutManager = LinearLayoutManager(this)
        recyclerview.adapter = adapter
        adapter.setOnItemClickListener {
            val device = adapter.items[it]
            device.apply {
                if (connected){
                    disconnect(this)
                }else if (!connectting){
                    connect(this)
                }
            }
        }
    }

    private fun initLinsenter() {
        readRssi.setOnClickListener{

        }
        sendData.setOnClickListener {

        }
        requestMtu.setOnClickListener {
            if (ble.getConnectedDevices().isNotEmpty()){
                supportsLollipop({
                    //此处第二个参数  不是特定的   比如你也可以设置500   但是如果设备不支持500个字节则会返回最大支持数
                    ble.setMtu(ble.getConnectedDevices()[0], 300){
                        onMtuChanged { device, mtu, status ->
                            toast("支持MTU：$mtu")
                        }
                    }
                },{
                    toast("设备不支持MTU")
                })
            }
        }

        scan.setOnClickListener {
            listDatas.clear()
            listDatas.addAll(ble.getConnectedDevices())
            adapter.notifyDataSetChanged()
            scan()
        }
    }

    private fun scan() {
        BLE.instance.startScan({
            onLeScan { device, _, _ ->
                for (d in listDatas) {
                    if (d.address == device.address)return@onLeScan
                }
                device.let {
                    listDatas.add(it)
                    adapter.notifyDataSetChanged()
                }
            }
        }, 12000L)
    }
    
    private fun connect(device: BleDevice) {
        ble.connect(device) {
            onConnectionChanged {device ->
                if (device.connected){
                    loge(TAG, "设备连接成功")
                }
                launch(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }
            }
            onConnectTimeOut {
                loge(TAG, "设备连接超时")
            }
        }
    }
    
    private fun disconnect(device: BleDevice){
        ble.disconnect(device){
            onConnectionChanged {device ->
                if (device.disconnected){
                    loge(TAG, "设备连接断开")
                }
                launch(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    //请求权限
    @Permission(
        value = [Manifest.permission.ACCESS_COARSE_LOCATION],
        requestCode = 2,
        rationale = "需要蓝牙相关权限"
    )
     fun requestPermission() {
        ble = BLE.options().apply {
            logTAG = "BLE-KOTLIN"
            logBleEnable = true
            throwBleException = true
            autoConnect = true
            connectFailedRetryCount = 3
            connectTimeout = 10000L
            scanPeriod = 12000L
            uuidService = UUID_SERVICE
            uuidWriteCha = UUID_WRITE_CHA
        }.create(applicationContext).apply {
            if (!isSupportBle()){
                toast("BLE is not supported")
                finish()
            }
            if (!isBleEnable()){
                openBluetooth(this@MainActivity, REQUEST_ENABLE_BT)
            }else {
                scan()
            }
        }
    }

    @PermissionDenied
    fun permissionDenied(requestCode: Int, denyList: List<String>) {
        if (requestCode == 2) {
            Log.e(TAG, "permissionDenied>>>:定位权限被拒 $denyList")
        }
    }

    @PermissionNoAskDenied
    fun permissionNoAskDenied(requestCode: Int, denyNoAskList: List<String>) {
        if (requestCode == 2) {
            Log.e(TAG, "permissionNoAskDenied 定位权限被拒>>>: $denyNoAskList")
        }
        AopPermissionUtils.showGoSetting(this, "为了更好的体验，建议前往设置页面打开权限")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish()
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            //6、若打开，则进行扫描
            scan()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
