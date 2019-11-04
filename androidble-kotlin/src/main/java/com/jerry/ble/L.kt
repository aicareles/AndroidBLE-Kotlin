package com.jerry.ble

import android.text.TextUtils
import android.util.Log
import java.util.*

object L {

    var TAG = "AndroidBLE"
    var isDebug: Boolean = false

    fun init(opts: BLE.Options) {
        isDebug = opts.logBleEnable
        if (!TextUtils.isEmpty(opts.logTAG))
            TAG = opts.logTAG
    }

    private fun getSubTag(o: Any): String {
        var tag = ""
        if (o is String) {
            tag = o
        } else if (o is Number) {
            tag = o.toString()
        } else {
            tag = o.javaClass.simpleName
        }
        return tag
    }

    fun e(o: Any, msg: String) {
        if (isDebug) {
            Log.e(TAG, buildMessge(getSubTag(o), msg))
        }
    }

    fun i(o: Any, msg: String) {
        if (isDebug) {
            Log.i(TAG, buildMessge(getSubTag(o), msg))
        }
    }

    fun w(o: Any, msg: String) {
        if (isDebug) {
            Log.w(TAG, buildMessge(getSubTag(o), msg))
        }
    }

    fun d(o: Any, msg: String) {
        if (isDebug) {
            Log.d(TAG, buildMessge(getSubTag(o), msg))
        }
    }

    private fun buildMessge(subTag: String, msg: String): String {
        return String.format(
            Locale.CHINA, "[%d] %s: %s",
            Thread.currentThread().id, subTag, msg
        )
    }

    /**
     * Formats the caller's provided message and prepends useful info like
     * calling thread ID and method name.
     */
    private fun buildMessage(format: String, vararg args: Any): String {
        val msg = if (args == null) format else String.format(Locale.CHINA, format, *args)
        val trace = Throwable().fillInStackTrace().stackTrace

        var caller = "<unknown>"
        // Walk up the stack looking for the first caller outside of VolleyLog.
        // It will be at least two frames up, so start there.
        for (i in 2 until trace.size) {
            val clazz = trace[i].javaClass
            if (clazz != L::class.java) {
                var callingClass = trace[i].className
                callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1)
                callingClass = callingClass.substring(callingClass.lastIndexOf('$') + 1)

                caller = callingClass + "." + trace[i].methodName
                break
            }
        }
        return String.format(
            Locale.CHINA, "[%d] %s: %s",
            Thread.currentThread().id, caller, msg
        )
    }

}