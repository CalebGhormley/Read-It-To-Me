package com.example.readittome

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

class ConnectionManager {
    private var connectionStatus: Boolean = false
    constructor(context: Context){
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting  == true
        connectionStatus = isConnected
    }
    fun checkConnection(): Boolean {
        return connectionStatus
    }
}