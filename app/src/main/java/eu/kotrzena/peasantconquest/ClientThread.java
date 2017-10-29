package eu.kotrzena.peasantconquest;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import java.io.IOException;

import eu.kotrzena.peasantconquest.game.PlayerInfo;

public class ClientThread extends Thread {
	private final ClientConnection connection;
	private final GameActivity activity;
	public ClientThread(ClientConnection connection, GameActivity activity){
		setName("_ClientThread");
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
					case Networking.MessageType.PAUSE:
						Log.i("Networking", "PAUSE");
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								activity.game.pause = true;
								activity.overlay.setVisibility(View.VISIBLE);
							}
						});
						break;
					case Networking.MessageType.UNPAUSE:
						Log.i("Networking", "UNPAUSE");
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								activity.game.pause = false;
								activity.overlay.setVisibility(View.GONE);
							}
						});
						break;
					case Networking.MessageType.MINIMAL_UPDATE:
						new Networking.MinimalUpdate(connection.in, activity.game.gameLogic);
						connection.send(new Networking.SimpleMessage(Networking.MessageType.READY_FOR_UPDATE));
						break;
					case Networking.MessageType.PLAYERS_INFO:
						Log.i("Networking", "PLAYERS_INFO");
						final Networking.PlayersInfo playersInfo = new Networking.PlayersInfo(connection.in);
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								SparseArray<PlayerInfo> players = activity.game.getPlayers();
								synchronized (players) {
									playersInfo.mergeInto(players);
								}
								if(activity.playerListAdapter != null)
								activity.playerListAdapter.notifyDataSetChanged();
							}
						});
						break;
				}
			}
		} catch (IOException e) {
			Log.e(getClass().getName(), "IOException", e);
		}

		Log.i("Networking", "Connection to server lost or closed.");

		if(!connection.socket.isClosed()){
			try {
				connection.socket.close();
			} catch (IOException e) {}
		}
		connection.socket = null;
		connection.in = null;
		connection.out = null;
		connection.thread = null;

		if(!isInterrupted())
			activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				activity.connectionLost(R.string.connection_lost);
			}
		});
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
