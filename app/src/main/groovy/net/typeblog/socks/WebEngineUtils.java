package net.typeblog.socks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.vertx.core.json.JsonObject;

public class WebEngineUtils {
    private WebView view;
    private Activity activity;
    public WebEngineUtils(Activity activity, WebView view){
        this.view = view;
        this.activity = activity;
    }
    @JavascriptInterface
    public void intercept(String url){
        CustomWebViewClient.addInterceptUrl(url);
    }

    @JavascriptInterface
    public void ajax(String method, String url, String headers, String mediaType, String requestBody, String callback) {
        new Thread(() -> {
            OkHttpClient httpClient = new OkHttpClient();
            httpClient.setConnectTimeout(10, TimeUnit.SECONDS);
            httpClient.setReadTimeout(10, TimeUnit.SECONDS);
            Request.Builder requestBuilder = new Request.Builder();
            new JsonObject(headers).forEach(v -> requestBuilder.addHeader(v.getKey(), (String) v.getValue()));
            requestBuilder.url(url);
            switch (method) {
                case "GET":
                    requestBuilder.get();
                    break;
                case "POST":
                    requestBuilder.post(RequestBody.create(MediaType.parse(mediaType), requestBody));
                    break;
                case "PUT":
                    requestBuilder.put(RequestBody.create(MediaType.parse(mediaType), requestBody));
                    break;
                case "DELETE":
                    requestBuilder.delete(RequestBody.create(MediaType.parse(mediaType), requestBody));
                    break;
            }
            Request request = requestBuilder.build();
            Response response = null;
            try {
                response = httpClient.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (response != null) {
                Map responseHeaders = new HashMap<>();
                if (response.headers() != null) {
                    if (response.headers().size() > 0) {
                        for (int i = 0; i < response.headers().size(); i++) {
                            String key = response.headers().name(i);
                            String value = response.headers().value(i);
                            responseHeaders.put(key, value);
                            Log.i("HEADER", key + ":" + value);
                        }
                    }
                }
                String body = null;
                try {
                    body = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                JsonObject res = new JsonObject().put("headers",new JsonObject(responseHeaders)).put("body",body);
                activity.runOnUiThread(()-> view.evaluateJavascript(callback+"("+res.toString()+")",null));
            }
        }).start();
    }

    @JavascriptInterface
    public void openIntent(String url){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        activity.startActivity(i);
    }

}
