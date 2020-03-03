package co.zzyun.wsocks.client.core.client.impl

import co.zzyun.wsocks.client.core.KCP
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import java.util.*

class WebsocketClientImpl(private val vertx:Vertx) :IClientImpl{
  private val httpClient = vertx.createHttpClient(HttpClientOptions().setTcpNoDelay(true))
  private lateinit var websocketClient: WebSocket
  override fun stop() {

  }

  override fun start(name: String, remoteHost: String, remotePort: Int, headers: JsonObject): Future<Void> {
    val fut = Future.future<Void>()
    httpClient.websocket(remotePort, remoteHost, "/chat", MultiMap.caseInsensitiveMultiMap().apply {
      this["info"] = Base64.getEncoder().encodeToString(headers.toString().toByteArray())
    }, {
      this.websocketClient = it
      fut.complete()
    }) {
      fut.fail(it)
    }
    return fut
  }

  override fun connected(kcp: KCP) {
    websocketClient.binaryMessageHandler {
        kcp.Input(it.bytes)
      }
  }

  override fun write(buffer: Buffer) {
    if(this::websocketClient.isInitialized){
      websocketClient.writeBinaryMessage(buffer)
    }
  }

}
