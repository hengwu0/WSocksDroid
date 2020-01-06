package co.zzyun.wsocks.data

import io.vertx.core.buffer.Buffer

class RawData(key:ByteArray,buffer:Buffer) {
  val uuid:String
  val data:Buffer
  init{
    val decrypted = Buffer.buffer(Aes.decrypt(buffer.getBytes(Int.SIZE_BYTES,buffer.length()),key,true))
    val uuidLen = decrypted.getIntLE(0)
    uuid = decrypted.getString(Int.SIZE_BYTES,Int.SIZE_BYTES+uuidLen)
    data = decrypted.getBuffer(Int.SIZE_BYTES+uuidLen,decrypted.length())
  }
  companion object {
    fun create(key:ByteArray,uuid:String,data:Buffer):ByteArray {
      val buf = Buffer.buffer()
        .appendIntLE(uuid.length)
        .appendString(uuid)
        .appendBuffer(data)
      val encryptedBuffer = Aes.encrypt(buf.bytes,key,true)
      return Buffer.buffer()
        .appendIntLE(Flag.RAW.ordinal)
        .appendBytes(encryptedBuffer).bytes
    }
  }
}
