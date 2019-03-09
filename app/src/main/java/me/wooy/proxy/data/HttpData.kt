package me.wooy.proxy.data

import io.vertx.core.buffer.Buffer
import me.wooy.proxy.encryption.Aes

class HttpData(private val buffer: Buffer) {
  private val decryptedBuffer = Buffer.buffer(Aes.decrypt(buffer.getBytes(IntSize,buffer.length())))
  private val uuidLen = decryptedBuffer.getIntLE(0)
  val uuid  = decryptedBuffer.getString(IntSize,IntSize+uuidLen)
  val port  = decryptedBuffer.getIntLE(IntSize+uuidLen)
  private val hostLen = decryptedBuffer.getIntLE(IntSize*2+uuidLen)
  val host  = decryptedBuffer.getString(IntSize*3+uuidLen, IntSize*3+hostLen+uuidLen)
  val data= decryptedBuffer.getBuffer(IntSize*3+hostLen+uuidLen,decryptedBuffer.length())
  fun toBuffer() = buffer
  companion object {
    fun create(uuid:String,port:Int,host:String,data:Buffer):HttpData {
      val encryptedBuffer = Aes.encrypt(Buffer.buffer()
        .appendIntLE(uuid.length)
        .appendString(uuid)
        .appendIntLE(port)
        .appendIntLE(host.length)
        .appendString(host)
        .appendBuffer(data).bytes)
      return HttpData(Buffer.buffer()
        .appendIntLE(Flag.HTTP.ordinal)
        .appendBytes(encryptedBuffer))
    }
  }
}
