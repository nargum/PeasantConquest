package eu.kotrzena.peasantconquest;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;

import junit.runner.Version;

import eu.kotrzena.peasantconquest.game.Assets;

public class DrawThread extends Thread {
	private static final long FRAME_PERIOD = 1000/30;

	private SurfaceHolder surfaceHolder;
	private GameActivity activity;
	private boolean hwAccelerated;

	public DrawThread(SurfaceHolder surfaceHolder, GameActivity activity){
		setName("_DrawThread");
		this.surfaceHolder = surfaceHolder;
		this.activity = activity;
		hwAccelerated = activity.prefs.getBoolean("graphicsHwAcc", true);
	}

	@Override
	public void run() {
		Paint background = new Paint();
		background.setARGB(0, 0, 0, 0);
		long beginTime, sleepTime;
		MediaPlayer mp = Assets.getBackgroundMusic();
		while(true){
			beginTime = System.currentTimeMillis();
			if(activity != null && activity.game != null && activity.game.playMusic) {
				if (activity.game.pause && mp.isPlaying()) {
					mp.pause();
				} else if (!activity.game.pause && !mp.isPlaying()) {
					mp.start();
				}
			}
			Canvas canvas;
			if(hwAccelerated && Build.VERSION.SDK_INT >= 26)
				canvas = surfaceHolder.lockHardwareCanvas();
			else
				canvas = surfaceHolder.lockCanvas();

			if(canvas == null || !surfaceHolder.getSurface().isValid())
				continue;

			synchronized(surfaceHolder){
				activity.game.draw(canvas);
			}

			surfaceHolder.unlockCanvasAndPost(canvas);

			sleepTime = FRAME_PERIOD - (System.currentTimeMillis() - beginTime);

			if(sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
