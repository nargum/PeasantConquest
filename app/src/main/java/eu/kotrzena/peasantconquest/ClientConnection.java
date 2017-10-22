package eu.kotrzena.peasantconquest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ClientConnection {
	public final int playerId;
	public InetAddress address = null;
	public Socket socket = null;
	public DataInputStream in = null;
	public DataOutputStream out = null;
	public Thread thread = null;

	public ClientConnection(int playerId){
		this.playerId = playerId;
	}

	public void send(Networking.Message message) throws IOException {
		message.write(out);
	}
}
