package eu.kotrzena.peasantconquest;

import android.view.View;

import eu.kotrzena.peasantconquest.game.PlayerInfo;

public class ServerLogicThread extends Thread {
	public static final long UPDATE_PERIOD = 1000/60;
	private GameActivity activity;
	public ServerLogicThread(GameActivity activity){
		setName("_ServerLogicThread");
		this.activity = activity;
	}

	@Override
	public void run() {
		long beginTime, sleepTime;
		while(!isInterrupted()){
			beginTime = System.currentTimeMillis();

			if(activity.game.pause){
				boolean allReady = true;
				for(PlayerInfo p : activity.game.getPlayers()){
					if(!p.ready){
						allReady = false;
						break;
					}
				}
				if(allReady){
					activity.game.pause = false;
					activity.overlay.setVisibility(View.GONE);
				}
			} else {
				activity.game.update();
			}

			sleepTime = UPDATE_PERIOD - (System.currentTimeMillis() - beginTime);

			if(sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
					continue;
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
