package co.zzyun.wsocks.client.core.client.impl

import co.zzyun.wsocks.client.core.KCP
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

interface IClientImpl {
  fun stop()
  fun start(name:String,remoteHost:String,remotePort:Int,headers:JsonObject):Future<Void>
  fun connected(kcp:KCP)
  fun write(buffer: Buffer)
}
