package net.typeblog.socks

import android.app.Activity
import android.os.Bundle
import me.wooy.proxy.client.ClientSocks5
import me.wooy.proxy.client.Launcher
import net.typeblog.socks.util.Utility

public class MainActivity extends Activity {
    @Override void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState)
		contentView = R.layout.main
		Utility.extractFile(this)
		
		fragmentManager.beginTransaction().replace(R.id.frame, new ProfileFragment()).commit()
	}
}
