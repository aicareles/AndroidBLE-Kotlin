package com.jerry.androidble

enum class BleStates {

    CONNECTED,
    CONNECTING,
    DISCONNECT,
    ConnectionChanged,
    ServicesDiscovered,
    Read,
    Write,
    Changed,
    DescriptorWriter,
    DescriptorRead,
    Start,
    Stop,
    ConnectTimeOut,
    OnReady,
    ConnectFailed,
    ConnectError,
    ConnectException,
    ReadRssi,
    NotifySuccess,
    MTUCHANGED,
    BlutoothStatusOff,
    BlutoothStatusOn

}