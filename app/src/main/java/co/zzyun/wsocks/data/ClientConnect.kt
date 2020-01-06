package co.zzyun.wsocks.data

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

class ClientConnect(key:ByteArray, buffer: Buffer) {
  private val json = Buffer.buffer(Aes.decrypt(buffer.getBytes(Int.SIZE_BYTES,buffer.length()),key,true)).toJsonObject()
  val host get() = json.getString("host")
  val port get() = json.getInteger("port")
  val uuid get() = json.getString("uuid")

  companion object {
    fun create(key: ByteArray,uuid:String,host: String, port: Int): ByteArray {
      val buffer = Buffer.buffer()
        .appendBuffer(JsonObject().put("host", host).put("port", port).put("uuid", uuid).toBuffer())
      return Buffer.buffer()
          .appendIntLE(Flag.CONNECT.ordinal).appendBytes(Aes.encrypt(buffer.bytes,key,true)).bytes
    }
  }
}
