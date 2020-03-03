package co.zzyun.wsocks.client.core;


import java.util.function.Consumer;

import co.zzyun.wsocks.client.core.client.BaseClient;
import co.zzyun.wsocks.data.UserInfo;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;


public class Client {
  private static String deployId = "";
  public static BaseClient client;
  public static Runnable onOffline;
  public static Consumer<JsonObject> onSave;
  public static Vertx start() {
    System.setProperty("java.net.preferIPv4Stack", "true");
    System.setProperty("vertx.disableDnsResolver", "true");
    System.setProperty("io.netty.noUnsafe", "true");
//    System.setProperty("ws.disableUdp","true");
    InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
    final Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1)
      .setWorkerPoolSize(1)
      .setInternalBlockingPoolSize(1)
      .setFileSystemOptions(new FileSystemOptions()
        .setFileCachingEnabled(false)
        .setClassPathResolvingEnabled(false)).setBlockedThreadCheckInterval(100000000000L));

    vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.OPTIONS) {
        req.response().putHeader("Access-Control-Allow-Origin", req.getHeader("origin"));
        req.response().putHeader("Access-Control-Allow-Credentials", "true");
        req.response().putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        req.response().putHeader("Access-Control-Allow-Headers", "DNT,X-Mx-ReqToken,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type");
        req.response().putHeader("Access-Control-Max-Age", "1728000");
        req.response().putHeader("Content-Type", "text/plain charset=UTF-8");
        req.response().setStatusCode(204).end();
        return;
      }
      req.response().putHeader("Access-Control-Allow-Origin", req.getHeader("origin"));
      req.response().putHeader("Access-Control-Allow-Credentials", "true");
      String s = req.path();
      if ("/start".equals(s)) {
        if (!deployId.isEmpty()) {
          req.response().end();
          return;
        }
        final String user = req.getParam("user");
        final String pass = req.getParam("pass");
        client = new BaseClient(new UserInfo(user, pass, -1, -1));
        vertx.deployVerticle(client, new DeploymentOptions().setConfig(new JsonObject()
                .put("user.info", new JsonObject().put("user", user).put("pass", pass))), result -> {
                  if (result.failed())
                    req.response().end(result.cause().getMessage());
                  else {
                    onSave.accept(new JsonObject().put("user", user).put("pass", pass));
                    deployId = result.result();
                    req.response().end();
                  }
                });
      } else if ("/stop".equals(s)) {
        if (!deployId.isEmpty()) {
          vertx.undeploy(deployId);
          req.response().end();
        }
      } else if ("/connect".equals(s)) {
        JsonObject headers = new JsonObject();
        req.params().entries().forEach(e->{
          if(e.getKey().startsWith("client-"))
            headers.put(e.getKey().substring(7),e.getValue());
        });
        client.reconnect(req.getParam("name")
                , req.getParam("token")
                , req.getParam("host")
                , Integer.parseInt(req.getParam("port"))
                , req.getParam("type"),headers).setHandler(e -> {
          if (e.succeeded()) {
            //Tray.setStatus(req.getParam("name"));
              req.response().end();
          }else{
              req.response().end(e.cause().getLocalizedMessage());
          }
        });
      } else if ("/status".equals(s)) {
        if (deployId.isEmpty()) {
          req.response().end("客户端未连接");
        } else {
          req.response().setStatusCode(200).end(client.getStatusMessage());
        }
      }
    }).listen(1078, r -> {
      if (r.failed()) r.cause().printStackTrace();
      else System.out.println("Listen at 1078");
    });
    return vertx;
  }
}
