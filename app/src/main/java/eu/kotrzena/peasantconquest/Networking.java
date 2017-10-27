package eu.kotrzena.peasantconquest;

import android.util.Log;

import java.io.ByteArrayOutputStream;
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
import java.util.LinkedList;
import java.util.List;

import eu.kotrzena.peasantconquest.game.GameLogic;

public class Networking {
	public static int PORT = 3074;
	public static short IDENTIFIER = (short)0xEC01;
	public static class MessageType {
		static final byte SERVER_SCAN = 1;
		static final byte SERVER_SCAN_RESPONSE = 2;
		/* byte messageType
		*  String mapName
		* */
		static final byte JOIN = 3;
		static final byte JOIN_ACCEPT = 4;
		static final byte JOIN_REFUSE = 5;
		static final byte READY = 6;
		static final byte UNPAUSE = 7;
		static final byte MINIMAL_UPDATE = 8;
		static final byte READY_FOR_UPDATE = 9;
		static final byte ARMY_COMMAND = 10;
	}
	public interface Message {
		void write(DataOutputStream out) throws IOException;
	}
	public static class SimpleMessage implements Message {
		public byte messageType;
		public SimpleMessage(byte messageType){
			this.messageType = messageType;
		}
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			out.writeByte(0);
			out.flush();
		}
	}
	public static class ServerScanResponse implements Message {
		public static final byte messageType = MessageType.SERVER_SCAN_RESPONSE;
		public String mapName;
		public String phoneName;
		public ServerScanResponse(String mapName, String phoneName){
			this.mapName = mapName;
			this.phoneName = phoneName;
		}
		public ServerScanResponse(DataInputStream in) throws IOException {
			mapName = readString(in);
			phoneName = readString(in);
		}
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			writeString(out, mapName);
			writeString(out, phoneName);
			out.flush();
		}
	}
	public static class JoinAccept implements Message {
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
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			out.writeInt(mapId);
			out.writeInt(playerId);
			out.flush();
		}
	}
	public static class JoinRefuse implements Message {
		public static final byte messageType = MessageType.JOIN_REFUSE;
		public int reasonStringId;
		public JoinRefuse(int reasonStringId){
			this.reasonStringId = reasonStringId;
		}
		public JoinRefuse(DataInputStream in) throws IOException {
			reasonStringId = in.readInt();
		}
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			out.writeInt(reasonStringId);
			out.flush();
		}
	}
	public static class MinimalUpdate implements Message {
		public static final byte messageType = MessageType.MINIMAL_UPDATE;
		public GameLogic gameLogic;
		private ByteArrayOutputStream bout;
		public MinimalUpdate(GameLogic gameLogic){
			this.gameLogic = gameLogic;
			bout = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bout);
			try {
				out.writeInt(gameLogic.nodes.length);	// Number of nodes
				for(int ni = 0; ni < gameLogic.nodes.length; ni++){
					GameLogic.Node node = gameLogic.nodes[ni];
					out.writeInt(node.playerId);
					out.writeFloat(node.unitsCount);
				}

				out.writeInt(gameLogic.armies.size());	// Number of armies
				for(int ai = 0; ai < gameLogic.armies.size(); ai++){
					GameLogic.Army army = gameLogic.armies.valueAt(ai);
					out.writeInt(gameLogic.armies.keyAt(ai));
					out.writeInt(army.playerId);
					out.writeFloat(army.position);
					out.writeInt(army.roadId);
					out.writeFloat(army.unitsCount);
				}
				out.flush();
			} catch (IOException e) {
				Log.e("Networking", "MinimalUpdate write to byte buffer failed.", e);
			}
		}
		public MinimalUpdate(DataInputStream in, GameLogic gameLogic) throws IOException {
			this.gameLogic = gameLogic;
			int nodesLenght = in.readInt();
			for(int ni = 0; ni < nodesLenght; ni++){
				GameLogic.Node node = gameLogic.nodes[ni];
				node.playerId = in.readInt();
				node.unitsCount = in.readFloat();
			}

			int armiesLenght = in.readInt();
			List<Integer> updatedArmies = new LinkedList<Integer>();
			for(int ai = 0; ai < armiesLenght; ai++){
				int aId = in.readInt();		// Army id
				updatedArmies.add(aId);
				GameLogic.Army army = gameLogic.armies.get(aId);
				if(army == null){
					army = gameLogic.new Army();
					gameLogic.armies.append(aId, army);
				}
				army.playerId = in.readInt();
				army.position = in.readFloat();
				army.roadId = in.readInt();
				army.unitsCount = in.readFloat();
			}
			// Remove unupdated armies
			for(int ai = 0; ai < gameLogic.armies.size(); ai++){
				int aId = gameLogic.armies.keyAt(ai);
				if(!updatedArmies.contains(aId)){
					gameLogic.armies.remove(aId);
					ai--;
				}
			}
		}
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			out.write(bout.toByteArray());
			out.flush();
		}
	}
	public static class ArmyCommand implements Message {
		public static final byte messageType = MessageType.ARMY_COMMAND;
		public int fromNode;
		public int toNode;
		public float unitsPct;
		public ArmyCommand(int fromNode, int toNode, float unitsPct){
			this.fromNode = fromNode;
			this.toNode = toNode;
			this.unitsPct = unitsPct;
		}
		public ArmyCommand(DataInputStream in) throws IOException {
			fromNode = in.readInt();
			toNode = in.readInt();
			unitsPct = in.readFloat();
		}
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeByte(messageType);
			out.writeInt(fromNode);
			out.writeInt(toNode);
			out.writeFloat(unitsPct);
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
