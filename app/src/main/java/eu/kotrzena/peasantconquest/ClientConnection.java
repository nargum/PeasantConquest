package eu.kotrzena.peasantconquest;

import android.util.Log;

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

	public synchronized void send(Networking.Message message) {
		try {
			message.write(out);
		} catch (IOException e){
			Log.e("Networking", "IOException", e);
			if(socket.isClosed())
				Log.w("Networking", "Socket is closed!");
		}
	}
}
