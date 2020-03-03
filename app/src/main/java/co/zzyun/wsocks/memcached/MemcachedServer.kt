package co.zzyun.wsocks.memcached

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import net.spy.memcached.MemcachedClient
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class MemcachedServer(private val vertx: Vertx) {
  class ConnectInfo(private val vertx: Vertx,private val nextIp:String,private val id:String,val info:JsonObject){
    companion object {
      val clientMap = ConcurrentHashMap<String,MemcachedClient>()
    }
    private lateinit var connection: MemcachedConnection
    fun reject(){
      (clientMap[nextIp]?: MemcachedClient(InetSocketAddress(nextIp,11211)).also {
        clientMap[nextIp]=it
      }).set(id,1000,"reject")
    }

    fun connect():MemcachedConnection{
      connection = MemcachedConnection(id,vertx,(clientMap[nextIp]?: MemcachedClient(InetSocketAddress(nextIp,11211)).also { clientMap[nextIp]=it }),info)
      connection.start()
      return connection
    }
    fun stop(){
      this.connection.stop()
    }
    fun write(data:Buffer){
      connection.write(data)
    }
  }
  private lateinit var centerServer: String
  private lateinit var client: MemcachedClient
  private var timerId: Long = 0
  private lateinit var onConnect:Handler<ConnectInfo>
  fun start(name: String, centerServer: String) {
    this.centerServer = centerServer
    client = MemcachedClient(InetSocketAddress(centerServer, 11211))
    client.delete(name)
    Thread.sleep(500)
    timerId = vertx.setPeriodic(200) {
      client.asyncGet(name, MyTranscoder.instance).addListener {
        val data = it.get() as DataNode? ?: return@addListener
        client.delete(name)
        val json = data.buffer.toJsonObject()
        val nextIp = json.getString("next")
        val id = json.getString("id")
        onConnect.handle(ConnectInfo(vertx,nextIp,id,json.getJsonObject("info")))
      }
    }
  }

  fun onConnect(handler: Handler<ConnectInfo>):MemcachedServer{
    this.onConnect = handler
    return this
  }

  fun stop() {
    vertx.cancelTimer(timerId)
    this.client.shutdown()
  }
}
