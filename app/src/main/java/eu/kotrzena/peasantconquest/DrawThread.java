package eu.kotrzena.peasantconquest;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.SurfaceHolder;

public class DrawThread extends Thread {
	public static final long FRAME_PERIOD = 1000/60;

	private SurfaceHolder surfaceHolder;
	private GameActivity activity;

	public DrawThread(SurfaceHolder surfaceHolder, GameActivity activity){
		setName("_DrawThread");
		this.surfaceHolder = surfaceHolder;
		this.activity = activity;
	}

	@Override
	public void run() {
		Paint background = new Paint();
		background.setARGB(0, 0, 0, 0);
		long beginTime, sleepTime;
		while(true){
			beginTime = System.currentTimeMillis();
			Canvas canvas = surfaceHolder.lockCanvas();

			if(canvas == null)
				continue;

			synchronized(surfaceHolder){
				activity.game.draw(canvas);
			}

			surfaceHolder.unlockCanvasAndPost(canvas);

			sleepTime = FRAME_PERIOD - (System.currentTimeMillis() - beginTime);

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
