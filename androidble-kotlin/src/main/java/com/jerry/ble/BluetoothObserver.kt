package com.jerry.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.jerry.ble.request.ScanRequest
import java.lang.ref.WeakReference

class BluetoothObserver constructor(private val context: Context){
    private var bleReceiver: BleReceiver? = null
    private lateinit var bluetoothCallback: ListenerBuilder

    inner class ListenerBuilder {
        internal var bluetoothOnAction: (() -> Unit)? = null
        internal var bluetoothOffAction: (() -> Unit)? = null

        fun onBluetoothOn(action: () -> Unit) {
            bluetoothOnAction = action
        }

        fun onBluetoothOff(action: () -> Unit) {
            bluetoothOffAction = action
        }

    }

    fun setBluetoothCallback(listenerBuilder: ListenerBuilder.() -> Unit) {
        bluetoothCallback = ListenerBuilder().also(listenerBuilder)
    }

    fun registerReceiver() {
        bleReceiver = BleReceiver(this)
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bleReceiver, filter)
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(bleReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal inner class BleReceiver(bluetoothObserver: BluetoothObserver) : BroadcastReceiver() {
        private val observerWeakReference: WeakReference<BluetoothObserver> =
            WeakReference(bluetoothObserver)

        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
                val observer = observerWeakReference.get()
                val status = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (status == BluetoothAdapter.STATE_ON) {
                    observer?.bluetoothCallback?.bluetoothOnAction?.invoke()
                } else if (status == BluetoothAdapter.STATE_OFF) {
                    ScanRequest.get().apply {
                        if (isScanning()){
                            stopScan()
                        }
                    }
                    observer?.bluetoothCallback?.bluetoothOffAction?.invoke()
                }
            }
        }
    }
}