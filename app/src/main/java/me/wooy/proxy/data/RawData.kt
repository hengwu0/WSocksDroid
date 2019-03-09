package me.wooy.proxy.data

import io.vertx.core.buffer.Buffer
import me.wooy.proxy.encryption.Aes

class RawData(private val buffer:Buffer) {
  private val decryptedBuffer = Buffer.buffer(Aes.decrypt(buffer.getBytes(IntSize,buffer.length())))
  private val uuidLength = decryptedBuffer.getIntLE(0)
  val uuid = decryptedBuffer.getString(IntSize,IntSize+uuidLength)
  val data = decryptedBuffer.getBuffer(IntSize+uuidLength,decryptedBuffer.length())
  fun toBuffer() = buffer
  companion object {
    fun create(uuid:String,data:Buffer):RawData {
      val encryptedBuffer = Aes.encrypt(Buffer.buffer()
        .appendIntLE(uuid.length)
        .appendString(uuid)
        .appendBuffer(data).bytes)

      return RawData(Buffer.buffer()
        .appendIntLE(Flag.RAW.ordinal)
        .appendBytes(encryptedBuffer))
    }
  }
}
