package eu.kotrzena.peasantconquest;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

import eu.kotrzena.peasantconquest.game.PlayerInfo;

public class ServerLogicThread extends Thread {
	class PlayerThread extends Thread {
		PlayerInfo playerInfo;
		public PlayerThread(PlayerInfo playerInfo){
			this.playerInfo = playerInfo;
		}
	}

	public static final long UPDATE_PERIOD = 1000/60;
	private GameActivity activity;
	public ServerLogicThread(GameActivity activity) {
		setName("_ServerLogicThread");
		this.activity = activity;
	}

	@Override
	public void run() {
		long beginTime, sleepTime;
		try {
			while(!isInterrupted()){
				beginTime = System.currentTimeMillis();

				SparseArray<PlayerInfo> players = activity.game.getPlayers();
				if(activity.game.pause){
					boolean allReady = true;
					for(int i = 0; i < players.size(); i++){
						PlayerInfo p = players.valueAt(i);
						if(!p.ready && !p.isHost){
							allReady = false;
							break;
						}
					}
					if(allReady){
						for(int i = 0; i < players.size(); i++){
							PlayerInfo p = players.valueAt(i);
							if(p.clientConnection != null) {
								p.clientConnection.send(new Networking.SimpleMessage(Networking.MessageType.UNPAUSE));
								Log.i("Networking", "Sending UNPAUSE to " + p.clientConnection.address.toString());
							}
						}
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								activity.game.pause = false;
								activity.overlay.setVisibility(View.GONE);
							}
						});
					}
				} else {
					activity.game.update();

					final Networking.MinimalUpdate msg = new Networking.MinimalUpdate(activity.game.gameLogic);
					LinkedList<PlayerThread> playerThreads = new LinkedList<PlayerThread>();
					for(int i = 0; i < players.size(); i++){
						PlayerInfo p = players.valueAt(i);
						if(p.clientConnection != null) {
							PlayerThread pt = new PlayerThread(p) {
								@Override
								public void run() {
									while (!playerInfo.readyForUpdate) ;
									playerInfo.readyForUpdate = false;
									playerInfo.clientConnection.send(msg);
								}
							};
							playerThreads.add(pt);
							pt.start();
						}
					}
					for(PlayerThread pt : playerThreads)
						pt.join();
				}

				sleepTime = UPDATE_PERIOD - (System.currentTimeMillis() - beginTime);

				if(sleepTime > 0) {
					Thread.sleep(sleepTime);
					continue;
				}
			}
		} catch (InterruptedException e) {
			return;
		}
	}
}
