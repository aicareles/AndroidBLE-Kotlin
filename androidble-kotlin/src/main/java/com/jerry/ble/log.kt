package com.jerry.ble

import android.util.Log

var TAG = "AndroidBLE"

fun logv(tag: String = TAG, message: String) = Log.v(tag, message)

fun logd(tag: String = TAG, message: String) = Log.d(tag, message)

fun logi(tag: String = TAG, message: String) = Log.i(tag, message)

fun logw(tag: String = TAG, message: String) = Log.w(tag, message)

fun loge(tag: String = TAG, message: String) = Log.e(tag, message)
