### AndroidBLE-Kotlin

### 使用(暂时是测试版,未上传到jcenter或jitpack.io,可先依赖library)

### 初始化
```
    BLE.options().apply {
            logTAG = "BLE-KOTLIN" //全局tag
            logBleEnable = true //是否开启日志
            throwBleException = true //是否在错误时抛出异常
            autoConnect = true //是否自动连接,TODO
            connectFailedRetryCount = 3 //连接失败重试次数 TODO
            connectTimeout = 10000L //连接超时时间ms
            scanPeriod = 12000L //扫描周期
            filterByName = "" //通过名称过滤
            filterRepeat = true //是否过滤重复设备
            uuidService = UUID_SERVICE //主服务uuid
            uuidWriteCha = UUID_WRITE_CHA //写特征uuid
            uuidReadCha = UUID_READ_CHA //读特征uuid
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
```

### 1.注册全局蓝牙状态监听
```
        ble.setBluetoothCallback(applicationContext){
                onBluetoothOn {
                    loge(TAG, "蓝牙已打开")
                }
                onBluetoothOff {
                    loge(TAG, "蓝牙已关闭")
                }
            }
```
### 2.扫描
```
        ble.startScan({
                /*onStart {
                    loge(TAG,"开始扫描")
                }
                onStop {
                    loge(TAG,"停止扫描")
                }*/
                onLeScan { device, _, _ ->
                    loge(TAG, "扫描到的设备>>>>${device.name}:${device.address}")
                    /*for (d in listDatas) {
                        if (d.address == device.address)return@onLeScan
                    }*/
                    device.let {
                        listDatas.add(it)
                        adapter.notifyDataSetChanged()
                    }
                }
                onScanFailed {
                    launch {
                        loge(TAG, "扫描失败$it")
                        if (it == -1)ble.openBluetooth(this@MainActivity, REQUEST_ENABLE_BT)
                    }
                }
            })
```
### 3.连接
```
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
                onReady {
                    //开启(使能)通知,不然收不到硬件发送过来的数据
                    enableNotify(it)
                }
            }
```
### 4.断开连接
```
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
```
### 5.使能通知
```
        ble.enableNotify(device){
                onChanged { device, characteristic ->
                    loge(TAG, "收到设备数据:${characteristic.value.bytesToHexString()}")
                }
                onNotifySuccess {
                    loge(TAG, "设置通知成功>>>>")
                    launch(Dispatchers.Main) {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
```
### 6.通过UUID使能/除能通知
```
        ble.enableNotifyByUUID(device.address, UUID_SERVICE, UUID_NOTIFY_CHA, enable){
                    onChanged { device, characteristic ->
                        loge(TAG, "收到数据${characteristic.value.bytesToHexString()}")
                    }
                    onNotifySuccess {
                        loge(TAG, "打开通知成功")
                        launch(Dispatchers.Main) {
                            adapter.notifyDataSetChanged()
                        }
                    }
                    onNotifyCanceled {
                        loge(TAG, "通知已关闭")
                        launch(Dispatchers.Main) {
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
```
### 7.读取数据
```
        推荐->需在初始化时配置uuid
        ble.read(ble.getConnectedDevices()[0]){
                    onReadSuccess { device, characteristic ->
                        loge(TAG, "读取数据成功:${characteristic.value.bytesToHexString()}")
                    }
                    onReadFailed { device, states ->
                        loge(TAG, "读取数据失败")
                    }
                }
        根据uuid读取数据
        /*ble.readByUUID(ble.getConnectedDevices()[0].address, UUID_SERVICE, UUID_READ_CHA){
                    onReadSuccess { device, characteristic ->
                        loge(TAG, "读取数据成功:${characteristic.value.bytesToHexString()}")
                    }
                    onReadFailed { device, states ->
                        loge(TAG, "读取数据失败")
                    }
                }*/

```
### 8.读取rssi
```
        ble.readRssi(ble.getConnectedDevices()[0]){
                   onReadRssiSuccess { device, rssi ->
                       loge(TAG, rssi.toString())
                   }
               }
```
### 9.写入数据
```
        推荐->需要在初始化中配置uuid
        ble.write(ble.getConnectedDevices()[0], value = data){
                    onWriteSuccess { device, characteristic ->
                        loge(TAG, "写入数据成功")
                    }
                    onWriteFailed { device, states ->
                        loge(TAG, "写入数据失败")
                    }
                }
        根据uuid发送数据
        /*ble.writeByUUID(ble.getConnectedDevices()[0].address, UUID_SERVICE, UUID_WRITE_CHA, value = data){
                    onWriteSuccess { device, characteristic ->
                        loge(TAG, "写入数据成功")
                    }
                    onWriteFailed { device, states ->
                        loge(TAG, "写入数据失败")
                    }
                }*/
```
### 10.设置mtu
```
        supportsLollipop({
                    //此处第二个参数  不是特定的   比如你也可以设置500   但是如果设备不支持500个字节则会返回最大支持数
                    ble.setMtu(ble.getConnectedDevices()[0], 300){
                        onMtuChanged { device, mtu, status ->
                            launch(Dispatchers.Main){
                                toast("支持MTU：$mtu")
                            }
                        }
                    }
                },{
                    toast("设备不支持MTU")
                })
```