package com.xlzhen.sharefileserver.utils

import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

object NetWorkUtils {
    @JvmStatic
    fun getDeviceIp(): String {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val inetAddresses = networkInterfaces.nextElement().inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val nextElement = inetAddresses.nextElement()
                    if (nextElement is Inet4Address && !nextElement.isLoopbackAddress) {
                        return nextElement.hostAddress ?: "0.0.0.0"
                    }
                }
            }
            return "0.0.0.0"
        } catch (e: SocketException) {
            e.printStackTrace()
            return "0.0.0.0"
        }
    }
}
