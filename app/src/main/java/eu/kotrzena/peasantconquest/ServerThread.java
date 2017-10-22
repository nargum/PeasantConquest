package eu.kotrzena.peasantconquest;

import android.util.Log;
import java.io.IOException;

public class ServerThread extends Thread {
	private ClientConnection connection;
	private GameActivity activity;
	public ServerThread(ClientConnection connection, GameActivity activity){
		setName("_ServerThread");
		this.connection = connection;
		this.activity = activity;
		connection.thread = this;
	}

	@Override
	public void run() {
		try {
			while(!isInterrupted() && !connection.socket.isClosed()) {
				byte messageType = connection.in.readByte();
				switch (messageType) {
					case Networking.MessageType.READY:
						Log.i("Networking", "Client ready "+connection.address.toString());
						activity.game.getPlayers().get(1+connection.playerId).ready = true;
						break;
				}
			}
		} catch (IOException e) {
			Log.e(getClass().getName(), "IOException", e);
		}

		Log.i("Networking", "Connection to client "+connection.address.toString()+" lost or closed.");

		if(!connection.socket.isClosed()){
			try {
				connection.socket.close();
			} catch (IOException e) {}
		}
		connection.socket = null;
		connection.in = null;
		connection.out = null;
		connection.thread = null;
	}

	@Override
	public void interrupt() {
		super.interrupt();
		try {
			connection.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
