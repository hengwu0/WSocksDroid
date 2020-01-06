package co.zzyun.wsocks.data

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import java.io.DataInputStream
import java.nio.ByteBuffer

class DnsQuery(key:ByteArray,buffer: ByteArray) {
  constructor(key:ByteArray,buffer:Buffer) : this(key,buffer.bytes)
  val host:String
  val uuid:String
  init {
    val json = JsonObject(String(Aes.decrypt(buffer.sliceArray(Int.SIZE_BYTES..buffer.size),key,true)))
    host = json.getString("host")
    uuid = json.getString("uuid")
  }
  companion object {
    fun create(key:ByteArray,uuid:String,host: String): ByteArray {
      val buffer = JsonObject().put("host", host).put("uuid", uuid).toString().toByteArray()
      val encryptedBuffer = Aes.encrypt(buffer,key,true)
      return Buffer.buffer().appendIntLE(Flag.DNS.ordinal).appendBytes(encryptedBuffer).bytes
    }
  }
}
