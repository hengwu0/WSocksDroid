package co.zzyun.wsocks.memcached

import io.vertx.core.buffer.Buffer

class DataNode(val buffer: Buffer){
  fun isSuccess():Boolean{
    return buffer.length()==Short.SIZE_BYTES && buffer.getShort(0) == ZERO
  }
  fun isShutdown():Boolean{
    return buffer.length()==Short.SIZE_BYTES && buffer.getShort(0) == ONE
  }
  companion object {
    private val ZERO = 0.toShort()
    private val ONE = 1.toShort()
    val shutdown = DataNode(Buffer.buffer().appendShort(1))
    val success = DataNode(Buffer.buffer().appendShort(0))
  }
}
