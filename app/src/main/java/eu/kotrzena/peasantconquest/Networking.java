package eu.kotrzena.peasantconquest;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Parcel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
		static final byte JOIN_ACCEPT = 4;
		static final byte JOIN_REFUSE = 5;
		static final byte READY = 6;
	}
	interface Message {
		void write(DataOutputStream out) throws IOException;
	}
	static class ServerScanResponse implements Message {
		public static final byte messageType = MessageType.SERVER_SCAN_RESPONSE;
		public String mapName;
		public ServerScanResponse(String mapName){
			this.mapName = mapName;
		}
		public ServerScanResponse(DataInputStream in) throws IOException {
			mapName = readString(in);
		}
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			writeString(out, mapName);
			out.writeByte(0);
			out.flush();
		}
	}
	static class JoinAccept implements Message {
		public static final byte messageType = MessageType.JOIN_ACCEPT;
		public int mapId;
		public int playerId;
		public JoinAccept(int mapId, int playerId){
			this.mapId = mapId;
			this.playerId = playerId;
		}
		public JoinAccept(DataInputStream in) throws IOException {
			mapId = in.readInt();
			playerId = in.readInt();
		}
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			out.writeInt(mapId);
			out.writeInt(playerId);
			out.writeByte(0);
			out.flush();
		}
	}
	static class JoinRefuse implements Message {
		public static final byte messageType = MessageType.JOIN_REFUSE;
		public int reasonStringId;
		public JoinRefuse(int reasonStringId){
			this.reasonStringId = reasonStringId;
		}
		public JoinRefuse(DataInputStream in) throws IOException {
			reasonStringId = in.readInt();
		}
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			out.writeInt(reasonStringId);
			out.writeByte(0);
			out.flush();
		}
	}

	public static void writeString(DataOutputStream out, String str) throws IOException {
		for(int i = 0; i < str.length(); i++){
			char c = str.charAt(i);
			if(c == '\0')
				c = ' ';
			out.writeChar(c);
		}
		out.writeChar('\0');
	}

	public static String readString(DataInputStream in) throws IOException {
		StringBuilder str = new StringBuilder();
		while(true){
			char c = in.readChar();
			if(c == '\0')
				break;
			str.append(c);
		}
		return str.toString();
	}

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
