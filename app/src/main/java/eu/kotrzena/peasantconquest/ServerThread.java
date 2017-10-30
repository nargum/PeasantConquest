package eu.kotrzena.peasantconquest;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import java.io.IOException;

import eu.kotrzena.peasantconquest.game.PlayerInfo;

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
					case Networking.MessageType.READY: {
						Log.i("Networking", "Client ready " + connection.address.toString());
						SparseArray<PlayerInfo> players = activity.game.getPlayers();
						synchronized (players) {
							players.get(connection.playerId).ready = true;
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									activity.playerListAdapter.notifyDataSetChanged();
								}
							});
							for (int i = 0; i < players.size(); i++) {
								PlayerInfo p = players.valueAt(i);
								if (p.clientConnection != null) {
									p.clientConnection.send(new Networking.PlayersInfo(players));
								}
							}
						}
						break;
					}
					case Networking.MessageType.UNREADY: {
						Log.i("Networking", "Client unready " + connection.address.toString());
						SparseArray<PlayerInfo> players = activity.game.getPlayers();
						synchronized (players) {
							players.get(connection.playerId).ready = false;
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									activity.playerListAdapter.notifyDataSetChanged();
								}
							});
							for (int i = 0; i < players.size(); i++) {
								PlayerInfo p = players.valueAt(i);
								if (p.clientConnection != null) {
									p.clientConnection.send(new Networking.PlayersInfo(players));
								}
							}
							break;
						}
					}
					case Networking.MessageType.READY_FOR_UPDATE:
						activity.game.getPlayers().get(connection.playerId).readyForUpdate = true;
						break;
					case Networking.MessageType.ARMY_COMMAND:
						Networking.ArmyCommand armyCommand = new Networking.ArmyCommand(connection.in);
						if(activity.game.gameLogic.nodes[armyCommand.fromNode].playerId == connection.playerId){
							activity.game.gameLogic.sendArmy(armyCommand.fromNode, armyCommand.toNode, armyCommand.unitsPct);
						}
						break;
				}
			}
		} catch (IOException e) {
			//Log.e(getClass().getName(), "IOException", e);
			//activity.connectionLost(R.string.connection_lost);
			activity.game.pause = true;
			SparseArray<PlayerInfo> players = activity.game.getPlayers();
			PlayerInfo p = players.get(connection.playerId);
			p.clientConnection = null;
			p.ready = false;
			p.playerName = "";
			if(activity.winnerOverlay.getVisibility() != View.VISIBLE)
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						activity.overlay.setVisibility(View.VISIBLE);
						activity.playerListAdapter.notifyDataSetChanged();
					}
				});
			try {
				Networking.SimpleMessage pauseMessage = new Networking.SimpleMessage(Networking.MessageType.PAUSE);
				Networking.PlayersInfo playersUpdateMessage = new Networking.PlayersInfo(activity.game.getPlayers());
				for(int i = 0; i < players.size(); i++){
					p = players.valueAt(i);
					if(p.clientConnection != null){
						p.clientConnection.send(pauseMessage);
						p.clientConnection.send(playersUpdateMessage);
					}
				}
			} catch (IOException e1) {
				Log.e(getClass().getName(), "IOException", e1);
			}
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
