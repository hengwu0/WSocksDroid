package co.zzyun.wsocks.memcached

import io.vertx.core.buffer.Buffer
import net.spy.memcached.CachedData
import net.spy.memcached.transcoders.Transcoder

class MyTranscoder: Transcoder<DataNode> {
  override fun asyncDecode(d: CachedData): Boolean {
    return false
  }

  override fun encode(o: DataNode): CachedData {
    return CachedData(0,o.buffer.bytes,o.buffer.length())
  }

  override fun decode(d: CachedData): DataNode {
    val buffer = Buffer.buffer(d.data)
    return DataNode(buffer)
  }

  override fun getMaxSize(): Int {
    return 1500
  }
  companion object{
    val instance = MyTranscoder()
  }
}
