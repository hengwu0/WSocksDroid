package co.zzyun.wsocks.client.core.client.impl

import co.zzyun.wsocks.client.core.KCP
import co.zzyun.wsocks.memcached.MemcachedClient
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

class MemcachedClientImpl(private val vertx: Vertx):IClientImpl {
  private lateinit var memcachedClient: MemcachedClient
  override fun stop() {
    memcachedClient.stop()
  }

  override fun start(name: String, remoteHost: String, remotePort: Int, headers: JsonObject):Future<Void> {
    val fut = Future.future<Void>()
    memcachedClient  = MemcachedClient(vertx,remoteHost)
    memcachedClient.successHandler {
      println("Connection success")
      fut.complete()
    }.shutdownHandler {
      if(!fut.isComplete){
        fut.fail("Reject connection")
      }
    }.start(remotePort.toString(),headers.getString("next"),headers)
    return fut
  }

  override fun connected(kcp: KCP) {
    memcachedClient.handler(Handler {
      kcp.InputAsync(it)
    })
  }

  override fun write(buffer: Buffer) {
    memcachedClient.write(buffer)
  }
}
