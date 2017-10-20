package eu.kotrzena.peasantconquest;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.InetAddress;

class Networking {
	static class MessageType {
		static final byte SERVER_SCAN = 1;
		static final byte SERVER_SCAN_RESPONSE = 2;
		/* byte messageType
		*  String mapName
		* */
	}
	static InetAddress getBroadcastAddress(Context context) throws IOException {
		WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcp = wifi.getDhcpInfo();
		// handle null somehow

		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) (broadcast >> (k * 8));
		return InetAddress.getByAddress(quads);
	}
}
