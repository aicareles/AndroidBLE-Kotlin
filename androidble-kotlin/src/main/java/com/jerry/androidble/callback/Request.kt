package com.jerry.androidble.callback

import com.jerry.androidble.request.ConnectRequest

interface Request<T> {
    fun getBleDevice(address: String): T{
        return ConnectRequest.get().getBleDevice(address) as T
    }
}