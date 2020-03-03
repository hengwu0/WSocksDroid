package net.typeblog.socks

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import co.zzyun.wsocks.client.core.Client
import io.vertx.core.json.JsonObject
import net.typeblog.socks.util.Profile
import net.typeblog.socks.util.ProfileManager
import net.typeblog.socks.util.Utility

import java.util.function.Consumer

public class MainActivity extends Activity {
    private static String CHANNEL_ID = "wsocks.channel"
    private ProfileManager mManager
    private Profile mProfile
    public static MainActivity instance
    private boolean mRunning = false
    private IVpnService mBinder
    private ServiceConnection mConnection = [
            onServiceConnected   : { p1, binder ->
                mBinder = IVpnService.Stub.asInterface(binder)
                try {
                    mRunning = mBinder.isRunning()
                } catch (Exception e) {

                }
            },
            onServiceDisconnected: {
                mBinder = null
            }
    ] as ServiceConnection

    @Override
    void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        contentView = R.layout.main
        Utility.extractFile(this)
        mManager = ProfileManager.getInstance(this.applicationContext)
        mProfile = mManager.default
        WebView webView = (WebView) findViewById(R.id.webview)
        webView.getSettings().setUserAgentString(CustomWebViewClient.randomUserAgent())
        webView.getSettings().setSupportMultipleWindows(true)
        webView.setWebViewClient(new CustomWebViewClient(this,webView.getSettings().getUserAgentString()))
        WebSettings webSettings = webView.getSettings()
        webSettings.setJavaScriptEnabled(true)
        WebEngineUtils utils = new WebEngineUtils(this,webView)
        webView.addJavascriptInterface(utils, "engine")
        if (mProfile.username.length() >= 6) {
            webView.loadUrl("http://www.zzyun.co/client/mobile/index.html?version=211&user=${mProfile.username}&pass=${mProfile.password}")
        } else {
            webView.loadUrl("http://www.zzyun.co/client/mobile/index.html?version=211")
        }
        new Thread(new Runnable() {
            @Override
            void run() {
                Client.start()
            }
        }).start()
        instance = this
        if (mBinder == null) {
            this.bindService(new Intent(this, SocksVpnService.class), mConnection, 0)
        }
        startVpn()
        Client.onOffline = new Runnable() {
            @Override
            void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    void run() {
                        createNotification("连接中断", "远程连接断开，请尝试重新连接")
                    }
                })

            }
        }
        Client.onSave = new Consumer<JsonObject>() {
            @Override
            void accept(JsonObject entries) {
                String user = entries.getString("user")
                String pass = entries.getString("pass")
                mProfile.username = user
                mProfile.password = pass
            }
        }

//		fragmentManager.beginTransaction().replace(R.id.frame, new ProfileFragment()).commit()
    }

    private void createNotification(String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(0, builder.build())
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = "wsocks"
        String description = "wsocks"
        int importance = NotificationManager.IMPORTANCE_DEFAULT
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance)
        channel.setDescription(description)
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class)
        notificationManager.createNotificationChannel(channel)
    }


    @Override
    void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Utility.startVpn(this, mProfile)
            checkState()
        }
    }

    private void checkState() {
        mRunning = false
        if (mBinder == null) {
            this.bindService(new Intent(this, SocksVpnService.class), mConnection, 0)
        }
    }

    private void startVpn() {
        Intent i = VpnService.prepare(this)
        if (i != null) {
            startActivityForResult(i, 0)
        } else {
            onActivityResult(0, Activity.RESULT_OK, null)
        }
    }

    private void stopVpn() {
        if (mBinder == null)
            return
        try {
            mBinder.stop()
        } catch (any) {
        }
        mBinder = null
        this.unbindService(mConnection)
        this.bindService(new Intent(this, SocksVpnService.class), mConnection, 0)
    }

}
