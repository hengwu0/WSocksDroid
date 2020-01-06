package co.zzyun.wsocks.data

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

class Exception(key:ByteArray,buffer: Buffer) {
  private val json = Buffer.buffer(Aes.decrypt(buffer.getBytes(Int.SIZE_BYTES,buffer.length()),key,true)).toJsonObject()
  val message get() = json.getString("message")
  val uuid get() = json.getString("uuid")
  companion object {
    fun create(key:ByteArray,uuid:String,message:String):ByteArray {
      val encryptedBuffer = Aes.encrypt(JsonObject()
          .put("message", message)
          .put("uuid", uuid)
          .toBuffer().bytes,key,true)
      return Buffer.buffer().appendIntLE(Flag.EXCEPTION.ordinal).appendBytes(encryptedBuffer).bytes
    }
  }
}
