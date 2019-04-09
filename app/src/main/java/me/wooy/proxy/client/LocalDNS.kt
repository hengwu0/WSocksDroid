package me.wooy.proxy.client

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocket
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import me.wooy.proxy.common.UserInfo
import me.wooy.proxy.data.DnsQuery
import java.net.Inet4Address
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LocalDNS(private val vertx: Vertx,private val ws: WebSocket,private val userInfo: UserInfo) {
    private fun WebSocket.writeBinaryMessageWithOffset(data: Buffer) {
        if (userInfo.offset == 0) {
            this.writeBinaryMessage(data)
        } else {
            val bytes = ByteArray(userInfo.offset)
            Random().nextBytes(bytes)
            this.writeBinaryMessage(Buffer.buffer(bytes).appendBuffer(data))
        }
    }

}