package co.zzyun.wsocks.data;

import io.vertx.core.json.JsonObject;

public class Slave {
  private String host;
  private int port;
  private String name;
  public Slave(JsonObject json){
    this.host = json.getString("host");
    this.port = json.getInteger("port");
    this.name = json.getString("name");
  }

  public Slave(String host, int port,String name){
    this.host = host;
    this.port = port;
    this.name = name;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public String toString(){
    return this.name+"+"+this.host+"+"+this.port;
  }

  public JsonObject toJson(){
    return new JsonObject().put("host",host).put("port",port);
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof Slave)
      return this.hashCode()==obj.hashCode();
    else
      return false;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
