package com.jerry.ble.callback

import com.jerry.ble.request.ConnectRequest

interface Request<T> {
    fun getBleDevice(address: String): T{
        return ConnectRequest.get().getBleDevice(address) as T
    }
}