package net.typeblog.socks;

import android.app.Activity;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.vertx.core.json.JsonObject;

public class CustomWebViewClient extends WebViewClient {
    private static String[] userAgents = new String[]{"Mozilla/5.0 (Linux; U; Android 6.0.1; zh-CN; F5121 Build/34.0.A.1.247) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/40.0.2214.89 UCBrowser/11.5.1.944 Mobile Safari/537.36"
            , "Mozilla/5.0 (Linux; U; Android 4.4.2; zh-CN; HUAWEI MT7-TL00 Build/HuaweiMT7-TL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/40.0.2214.89 UCBrowser/11.3.8.909 Mobile Safari/537.36"
            , "Mozilla/5.0 (Linux; U; Android 8.1.0; zh-CN; EML-AL00 Build/HUAWEIEML-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 baidu.sogo.uc.UCBrowser/11.9.4.974 UWS/2.13.1.48 Mobile Safari/537.36 AliApp(DingTalk/4.5.11) com.alibaba.android.rimet/10487439 Channel/227200 language/zh-CN"
            , "Mozilla/5.0 (Linux; Android 6.0.1; RedMi Note 5 Build/RB3N5C; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.91 Mobile Safari/537.36"};
    public static String randomUserAgent(){
        int index = new Random().nextInt(userAgents.length);
        return userAgents[index];
    }

    private static List<String> interceptUrlList = new ArrayList<>();

    public static void addInterceptUrl(String url){
        interceptUrlList.add(url);
    }

    private Activity activity;
    private String userAgent;
    private String currentUrl = "";

    CustomWebViewClient(Activity activity, String userAgent) {
        this.activity = activity;
        this.userAgent = userAgent;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        for (String s : interceptUrlList) {
            if(url.startsWith(s)){
                OkHttpClient httpClient = new OkHttpClient();
                Log.i("Current_URL", currentUrl);
                Request request = new Request.Builder()
                        .addHeader("User-Agent", userAgent)
                        .addHeader("Referer", currentUrl)
                        .url(url)
                        .build();
                try {
                    Response response = httpClient.newCall(request).execute();
                    if (response != null) {
                        int statusCode = response.code();
                        String encoding = "UTF-8";
                        String mimeType = response.header("Content-Type");
                        String reasonPhrase = response.message();
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
                        String body = response.body().string();
                        JsonObject res = new JsonObject().put("url",url).put("headers",new JsonObject(responseHeaders)).put("body",body);
                        activity.runOnUiThread(()-> view.evaluateJavascript("onResponse("+res.toString()+")",null));
                        InputStream data = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
                        return new WebResourceResponse(mimeType, encoding, statusCode, reasonPhrase, responseHeaders, data);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return super.shouldInterceptRequest(view, url);
    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        this.currentUrl = url;
        if (url.contains("exdynsrv")) {
            return super.shouldOverrideUrlLoading(view, url);
        }
        view.loadUrl(url);
        return true;
    }
}
