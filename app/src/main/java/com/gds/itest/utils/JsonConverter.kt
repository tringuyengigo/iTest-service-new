package com.gds.itest.utils

import com.gds.itest.model.Request
import com.gds.itest.model.WifiData
import com.google.gson.Gson


object JsonConverter{

    @Throws(IllegalArgumentException::class)
     fun convertRequest(jsonRequest: String): Request {
        return Request.create(jsonRequest)
    }

    @Throws(IllegalArgumentException::class)
    fun convertRequestAutomation(jsonRequest: String): Array<Request> {
        return Gson().fromJson(jsonRequest, Array<Request>::class.java)
    }

    @Throws(IllegalArgumentException::class,
            IllegalStateException::class)
    fun getWifiListFromOption(request: Request): Array<WifiData>? {
        val element = request.options?.get(Constants.OPT_WIFI_INFO)?.asJsonObject?.get(Constants.OPT_WIFI_INFO_LIST)
        return Gson().fromJson(element, Array<WifiData>::class.java)
    }

    @Throws(IllegalArgumentException::class,
            IllegalStateException::class)
    fun getBluetoothListFromOption(request: Request): Array<WifiData>? {
        val element = request.options?.get(Constants.OPT_BLUETOOTH_INFO)?.asJsonObject?.get(Constants.OPT_BLUETOOTH_INFO_LIST)
        return Gson().fromJson(element, Array<WifiData>::class.java)
    }
}