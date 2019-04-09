package me.wooy.proxy;

import android.util.Log;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import me.wooy.proxy.client.ClientWebSocket;

public class Launcher {
    private static Vertx vertx;
    private static String ID;

    public static void init() {
        VertxOptions options = new VertxOptions();
        options.getFileSystemOptions().setFileCachingEnabled(false);
        options.getFileSystemOptions().setClassPathResolvingEnabled(false);
        vertx = Vertx.vertx(options);
    }

    public static String start(String server, int port, String user, String pass,int offset,boolean doZip) {
        Log.i("ClientWebSocket", "Start");
        if(vertx==null){
            init();
        }
        vertx.deployVerticle(new ClientWebSocket(), new DeploymentOptions().setConfig(
                new JsonObject().put("remote.ip", server)
                        .put("remote.port", port)
                        .put("user", user)
                        .put("pass", pass)
                        .put("offset",offset)
                        .put("zip",doZip)), it -> {
                            if (it.succeeded()) {
                                ID = it.result();
                                Log.i("ClientWebSocket", it.result());
                            } else {
                                Log.e("ClientWebSocket", it.cause().getLocalizedMessage());
                            }
                        });
        return ID;
    }

    public static boolean stop(){
        if(vertx==null) return false;
        vertx.undeploy(ID);
        ID=null;
        return true;
    }
}
