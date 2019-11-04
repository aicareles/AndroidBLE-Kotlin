package com.jerry.ble

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun isBackground(context: Context): Boolean {
    val activityManager = context
        .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val appProcesses = activityManager
        .runningAppProcesses
    for (appProcess in appProcesses) {
        if (appProcess.processName == context.packageName) {
            return appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }
    return false
}

fun BluetoothAdapter?.isSupportBle(context: Context): Boolean {
    return this != null && context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
}

fun BluetoothAdapter?.isBleEnable():Boolean {
    return this != null &&  this.isEnabled
}

inline fun debug(code: () -> Unit){
    if (BuildConfig.DEBUG){
        code()
    }
}


fun supportsLollipop(code: () -> Unit, not: (() -> Unit)? = null){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
        code()
        return
    }else{
        not?.invoke()
    }
}

inline fun isSupportsLollipop(code: () -> Unit){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
        code()
    }
}

inline fun handleException(code: () -> Unit){
    try {
        code()
    }catch (e: Exception){
        e.printStackTrace()
    }
}

fun ByteArray?.bytesToHexString(): String? {
    val stringBuilder = StringBuilder("")
    if (this == null || this.isEmpty()) {
        return null
    }
    for (i in 0 until this.size) {
        val v = this[i].toInt() and 0xFF
        val hv = Integer.toHexString(v)
        if (hv.length < 2) {
            stringBuilder.append(0)
        }
        stringBuilder.append(hv)
    }
    return stringBuilder.toString()
}

fun String?.hexStringToBytes(): ByteArray? {
    var hexString = this
    if (hexString == null || hexString == "") {
        return null
    }
    hexString = hexString.toUpperCase()
    val length = hexString.length / 2
    val hexChars = hexString.toCharArray()
    val d = ByteArray(length)
    for (i in 0 until length) {
        val pos = i * 2
        d[i] = (charToByte(hexChars[pos]).toInt() shl 4 or charToByte(hexChars[pos + 1]).toInt()).toByte()
    }
    return d
}

private fun charToByte(c: Char): Byte {
    return "0123456789ABCDEF".indexOf(c).toByte()
}


