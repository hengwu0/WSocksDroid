package me.wooy.proxy.client

import android.util.Log
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import me.wooy.proxy.common.UserInfo
import me.wooy.proxy.data.*
import org.apache.commons.lang3.RandomStringUtils
import java.net.Inet4Address
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ClientWebSocket : AbstractVerticle() {
    private lateinit var netServer: NetServer
    private val connectMap = ConcurrentHashMap<String, NetSocket>()
    private val address = Inet4Address.getByName("127.0.0.1").address
    private var port = 1080
    private var remotePort = 1888
    private lateinit var remoteIp: String
    private lateinit var userInfo: UserInfo
    private val httpClient: HttpClient by lazy { vertx.createHttpClient() }
    private lateinit var ws: WebSocket
    override fun start(future: Future<Void>) {
        port = config().getInteger("local.port") ?: 1080
        remotePort = config().getInteger("remote.port") ?: 1888
        remoteIp = config().getString("remote.ip")
        userInfo = UserInfo.fromJson(config())
        login()
        vertx.createNetServer().connectHandler {socket->
            socket.handler{ buf->
                handleQuery(socket,buf)
            }
        }.listen(5553){
            println("Listen at 5553")
        }
        initSocksServer(port)
        vertx.setPeriodic(5000) {
            if (this::ws.isInitialized)
                ws.writePing(Buffer.buffer())
        }
        future.complete()
    }

    override fun stop() {
        netServer.close()
        ws.close()
        connectMap.clear()
        super.stop()
    }

    private fun login() {
        if (!this::remoteIp.isInitialized)
            return
        httpClient.websocket(remotePort
                , remoteIp
                , "/"+RandomStringUtils.randomAlphabetic(5)
                , MultiMap.caseInsensitiveMultiMap()
                .add(RandomStringUtils.randomAlphanumeric(Random().nextInt(10)+1)
                        , userInfo.secret())) { webSocket ->
            webSocket.writePing(Buffer.buffer())
            webSocket.binaryMessageHandler { _buffer ->
                if (_buffer.length() < 4) {
                    return@binaryMessageHandler
                }
                val buffer = if (userInfo.offset != 0) _buffer.getBuffer(userInfo.offset, _buffer.length()) else _buffer
                when (buffer.getIntLE(0)) {
                    Flag.CONNECT_SUCCESS.ordinal -> wsConnectedHandler(ConnectSuccess(userInfo, buffer).uuid)
                    Flag.EXCEPTION.ordinal -> wsExceptionHandler(Exception(userInfo, buffer))
                    Flag.RAW.ordinal -> {
                        wsReceivedRawHandler(RawData(userInfo, buffer))
                    }
                    Flag.DNS.ordinal->{
                        wsReceivedDNSHandler(DnsQuery(userInfo,buffer))
                    }
                    else -> Log.w("ClientWebSocket", buffer.getIntLE(0).toString())
                }
            }.exceptionHandler { t ->
                Log.w("ClientWebSocket", t)
                ws.close()
                login()
            }
            this.ws = webSocket
            Log.w("ClientWebSocket", "Connected to remote server")
        }
    }

    private fun initSocksServer(port: Int) {
        if (this::netServer.isInitialized) {
            this.netServer.close()
        }
        this.netServer = vertx.createNetServer().connectHandler { socket ->
            if (!this::ws.isInitialized) {
                socket.close()
                return@connectHandler
            }
            val uuid = UUID.randomUUID().toString()
            socket.handler {
                bufferHandler(uuid, socket, it)
            }.closeHandler {
                connectMap.remove(uuid)
            }
        }.listen(port) {
            Log.w("ClientWebSocket", "Listen at $port")
        }
    }


    private fun bufferHandler(uuid: String, netSocket: NetSocket, buffer: Buffer) {
        val version = buffer.getByte(0)
        if (version != 0x05.toByte()) {
            netSocket.close()
        }
        when {
            buffer.getByte(1).toInt() + 2 == buffer.length() -> {
                handshakeHandler(netSocket)
            }
            else -> requestHandler(uuid, netSocket, buffer)
        }
    }

    private fun handshakeHandler(netSocket: NetSocket) {
        netSocket.write(Buffer.buffer().appendByte(0x05.toByte()).appendByte(0x00.toByte()))
    }

    private fun requestHandler(uuid: String, netSocket: NetSocket, buffer: Buffer) {
        /*
        * |VER|CMD|RSV|ATYP|DST.ADDR|DST.PORT|
        * -----------------------------------------
        * | 1 | 1 |0x0| 1  |Variable|   2    |
        * -----------------------------------------
        * */
        val cmd = buffer.getByte(1)
        val addressType = buffer.getByte(3)
        Log.w("ClientWebSocket", "UUID:$uuid,$cmd,$addressType")
        val (host, port) = when (addressType) {
            0x01.toByte() -> {
                val host = Inet4Address.getByAddress(buffer.getBytes(4, 8)).toString().removePrefix("/")
                val port = buffer.getShort(8).toInt()
                Log.w("ClientWebSocket", "UUID:$uuid,Connect to $host:$port")
                host to port
            }
            0x03.toByte() -> {
                val hostLen = buffer.getByte(4).toInt()
                val host = buffer.getString(5, 5 + hostLen)
                val port = buffer.getShort(5 + hostLen).toInt()
                host to port
            }
            else -> {
                netSocket.write(Buffer.buffer()
                        .appendByte(0x05.toByte())
                        .appendByte(0x08.toByte()))
                return
            }
        }
        when (cmd) {
            0x01.toByte() -> {
                tryConnect(uuid, netSocket, host, port)
            }
            0x03.toByte() -> {
                netSocket.write(Buffer.buffer()
                        .appendByte(0x05.toByte())
                        .appendByte(0x00.toByte())
                        .appendByte(0x00.toByte())
                        .appendByte(0x01.toByte())
                        .appendBytes(address).appendUnsignedShortLE(29799))
            }
            else -> {
                netSocket.write(Buffer.buffer()
                        .appendByte(0x05.toByte())
                        .appendByte(0x07.toByte()))
                return
            }
        }
    }

    private fun WebSocket.writeBinaryMessageWithOffset(data: Buffer) {
        if (userInfo.offset == 0) {
            this.writeBinaryMessage(data)
        } else {
            val bytes = ByteArray(userInfo.offset)
            Random().nextBytes(bytes)
            this.writeBinaryMessage(Buffer.buffer(bytes).appendBuffer(data))
        }
    }


    private fun tryConnect(uuid: String, netSocket: NetSocket, host: String, port: Int) {
        connectMap[uuid] = netSocket
        ws.writeBinaryMessageWithOffset(ClientConnect.create(userInfo, uuid, host, port))
    }

    private fun wsConnectedHandler(uuid: String) {
        val netSocket = connectMap[uuid] ?: return
        //建立连接后修改handler
        netSocket.handler {
            ws.writeBinaryMessageWithOffset(RawData.create(userInfo, uuid, it))
        }
        val buffer = Buffer.buffer()
                .appendByte(0x05.toByte())
                .appendByte(0x00.toByte())
                .appendByte(0x00.toByte())
                .appendByte(0x01.toByte())
                .appendBytes(ByteArray(6) { 0x0 })
        netSocket.write(buffer)
    }

    private fun wsReceivedRawHandler(data: RawData) {
        val netSocket = connectMap[data.uuid] ?: return
        netSocket.write(data.data)
    }

    private fun wsExceptionHandler(e: Exception) {
        connectMap.remove(e.uuid)?.close()
        Log.w("ClientWebSocket", e.message)
    }

    private val dnsQueryMap = ConcurrentHashMap<String,Pair<NetSocket,Buffer>>()
    private fun handleQuery(socket: NetSocket, buffer: Buffer){
        val sb = StringBuilder()
        fun getName(buffer: Buffer, offset:Int):Int{
            val length = buffer.getByte(offset)

            if(length==0.toByte()){
                return offset + 1
            }
            val bytes = buffer.getString(offset+1,offset+1+length.toInt())
            sb.append(bytes).append(".")
            return getName(buffer,offset+1+length.toInt())
        }
        getName(buffer,14)
        val host = sb.toString().removeSuffix(".")
        val uuid = UUID.randomUUID().toString()
        dnsQueryMap[uuid] = socket to buffer
        ws.writeBinaryMessageWithOffset(DnsQuery.create(userInfo,uuid,host))
    }

    private fun response(id:Short,ip:String,query: Buffer): Buffer {
        val address = Inet4Address.getByName(ip).address
        val buffer = Buffer.buffer()
        buffer.appendShort(id)
        buffer.appendUnsignedShort(0x8180)
        buffer.appendUnsignedShort(1)
        buffer.appendUnsignedShort(1)
        buffer.appendUnsignedShort(0)
        buffer.appendUnsignedShort(0)
        buffer.appendBuffer(query)
        buffer.appendUnsignedShort(0xC00C)
        buffer.appendUnsignedShort(1)
        buffer.appendUnsignedShort(1)
        buffer.appendUnsignedInt(86400)
        buffer.appendUnsignedShort(4)
        buffer.appendBytes(address)
        return buffer
    }

    private fun wsReceivedDNSHandler(data: DnsQuery){
        val (sock,buffer) = dnsQueryMap.remove(data.uuid)?:return
        val id = buffer.getShort(2)
        val buf = response(id,data.host,buffer.getBuffer(14,buffer.length()))
        sock.write(Buffer.buffer().appendShort(buf.length().toShort()).appendBuffer(buf)).end()
    }

}

