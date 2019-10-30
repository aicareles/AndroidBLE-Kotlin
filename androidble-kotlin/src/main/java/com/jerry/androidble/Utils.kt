package com.jerry.androidble

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

