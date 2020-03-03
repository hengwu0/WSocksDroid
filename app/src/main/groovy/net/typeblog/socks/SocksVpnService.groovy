package net.typeblog.socks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.Log
import net.typeblog.socks.util.Routes
import net.typeblog.socks.util.Utility

import static net.typeblog.socks.BuildConfig.DEBUG
import static net.typeblog.socks.util.Constants.*

public class SocksVpnService extends VpnService {
    private static final String TAG = 'SocksVpnService'

    private ParcelFileDescriptor mInterface
    private boolean mRunning = false

    @Override
    int onStartCommand(Intent intent, int flags, int startId) {

        if (DEBUG) {
            Log.d(TAG, "starting")
        }

        if (!intent) {
            return 0
        }

        if (mRunning) {
            return 0
        }

        final String name = intent.getStringExtra(INTENT_NAME)
        final String route = intent.getStringExtra(INTENT_ROUTE)
        final String dns = intent.getStringExtra(INTENT_DNS)
        final int dnsPort = intent.getIntExtra(INTENT_DNS_PORT, 53)
        final boolean perApp = intent.getBooleanExtra(INTENT_PER_APP, false)
        final boolean appBypass = intent.getBooleanExtra(INTENT_APP_BYPASS, false)
        final String[] appList = intent.getStringArrayExtra(INTENT_APP_LIST)
        final boolean ipv6 = false

        // Create the notification
        String channelId = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel("wsocks_service", "Wsocks Service", NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)
            service.createNotificationChannel(chan)
            channelId = "wsocks_service"
        }
        startForeground(R.mipmap.ic_launcher,
                new Notification.Builder(this, channelId).with {
                    contentTitle = getString(R.string.notify_title)
                    contentText = String.format(getString(R.string.notify_msg), name)
                    priority = Notification.PRIORITY_MIN
                    smallIcon = android.R.color.transparent
                    build()
                })

        // Create an fd.981019
        configure(name, route, perApp, appBypass, appList, ipv6)

        if (DEBUG)
            Log.d(TAG, "fd: ${mInterface.fd}")

        if (mInterface) {
//            Launcher.start(server, port, username, passwd,offset,doZip)
            start(mInterface.getFd(), dns, dnsPort)
        }
        START_STICKY
    }

    @Override
    void onRevoke() {
        super.onRevoke()
        stopMe()
    }

    @Override
    IBinder onBind(Intent intent) {
        return new IVpnService.Stub() {
            @Override
            boolean isRunning() {
                mRunning
            }

            @Override
            void stop() {
                stopMe()
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy()

        stopMe()
    }

    private void stopMe() {
        stopForeground(true)

        Utility.killPidFile("${DIR}/tun2socks.pid")
        Utility.killPidFile("${DIR}/pdnsd.pid")

        try {
            System.jniclose(mInterface.fd)
            mInterface.close()
        } catch (any) {
        }
        Launcher.stop()
        stopSelf()
    }

    private void configure(String name, String route, boolean perApp, boolean bypass, String[] apps, boolean ipv6) {
        VpnService.Builder b = new VpnService.Builder(this)
        b.with {
            mtu = 1500
            session = name
            addAddress "26.26.26.1", 24
            addDnsServer "8.8.8.8"

            if (ipv6) {
                // Route all IPv6 traffic
                addAddress "fdfe:dcba:9876::1", 126
                addRoute "::", 0
            }

            Routes.addRoutes(this, b, route)

            // Add the default DNS
            // Note that this DNS is just a stub.
            // Actual DNS requests will be redirected through pdnsd.
            addRoute "8.8.8.8", 32
        }
        try {
            b.addDisallowedApplication("net.typeblog.socks")
        } catch (Exception e) {

        }
        if(perApp) {
            if (bypass) {
                apps.each { p ->
                    if (TextUtils.isEmpty(p))
                        return

                    try {
                        b.addDisallowedApplication(p.trim())
                    } catch (any) {

                    }
                }
            } else {
                apps.each { p ->
                    if (TextUtils.isEmpty(p) || p.trim().equals("net.typeblog.socks")) {
                        return
                    }

                    try {
                        b.addAllowedApplication(p.trim())
                    } catch (any) {

                    }
                }
            }
        }
        mInterface = b.establish()
    }

    private void start(int fd, String dns, int dnsPort) {

        // Start DNS daemon first
        Utility.makePdnsdConf(this, dns, dnsPort)
        Utility.exec("${DIR}/pdnsd -c ${DIR}/pdnsd.conf")

        String command = """${DIR}/tun2socks --netif-ipaddr 26.26.26.2
			--netif-netmask 255.255.255.0
			--socks-server-addr 127.0.0.1:1080
			--tunfd ${fd}
			--tunmtu 1500
			--loglevel 3
			--pid ${DIR}/tun2socks.pid
			--dnsgw 26.26.26.1:8091
			""".stripIndent().replace("\n", "")

        if (DEBUG) {
            Log.d(TAG, command)
        }

        if (Utility.exec(command) != 0) {
            stopMe()
            return
        }

        // Try to send the Fd through socket.
        int i = 5
        while (i-- > 0) {
            if (System.sendfd(fd) != -1) {
                mRunning = true
                return
            }

            try {
                Thread.sleep(1000 * i)
            } catch (any) {

            }
        }

        stopMe()
    }
}
