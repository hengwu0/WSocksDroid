package co.zzyun.wsocks.data

import io.vertx.core.buffer.Buffer

class ConnectSuccess(key:ByteArray,buffer: Buffer) {
  val uuid = String(Aes.decrypt(buffer.getBytes(Int.SIZE_BYTES,buffer.length()),key,true))
  companion object {
    fun create(key:ByteArray,uuid:String):ByteArray {
      val encryptedUUID = Aes.encrypt(uuid.toByteArray(),key,true)
      return Buffer.buffer()
        .appendIntLE(Flag.CONNECT_SUCCESS.ordinal)
        .appendBytes(encryptedUUID).bytes
    }
  }
}
