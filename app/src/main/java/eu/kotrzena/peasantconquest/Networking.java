package eu.kotrzena.peasantconquest;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Parcel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

class Networking {
	static int PORT = 3074;
	static short INDENTIFIER = (short)0xEC01;
	static class MessageType {
		static final byte SERVER_SCAN = 1;
		static final byte SERVER_SCAN_RESPONSE = 2;
		/* byte messageType
		*  String mapName
		* */
		static final byte JOIN = 3;
	}

	/*static InetAddress getBroadcastAddress(Context context) throws IOException {
		WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcp = wifi.getDhcpInfo();
		// handle null somehow

		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) (broadcast >> (k * 8));
		return InetAddress.getByAddress(quads);
	}*/

	static InetAddress getBroadcastAddress(){
		InetAddress broadcastAddress = null;
		try {
			Enumeration<NetworkInterface> networkInterface = NetworkInterface.getNetworkInterfaces();

			while (broadcastAddress == null
					&& networkInterface.hasMoreElements()) {
				NetworkInterface singleInterface = networkInterface
						.nextElement();
				String interfaceName = singleInterface.getName();
				if (interfaceName.contains("wlan0")
						|| interfaceName.contains("eth0")) {
					for (InterfaceAddress infaceAddress : singleInterface
							.getInterfaceAddresses()) {
						broadcastAddress = infaceAddress.getBroadcast();
						if (broadcastAddress != null) {
							break;
						}
					}
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		}

		return broadcastAddress;
	}

	static class UdpResponseThread extends Thread {
		DatagramPacket packet;
		public UdpResponseThread(DatagramPacket packet){
			super();
			this.packet = packet;
		}
	}
	static class TcpResponseThread extends Thread {
		Socket socket;
		public TcpResponseThread(Socket socket){
			super();
			this.socket = socket;
		}

		@Override
		public void interrupt() {
			super.interrupt();
			if(socket != null & !socket.isClosed()) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
