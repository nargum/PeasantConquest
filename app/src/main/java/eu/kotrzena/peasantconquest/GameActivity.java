package eu.kotrzena.peasantconquest;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

public class GameActivity extends AppCompatActivity {
	public SurfaceView gameView = null;
	public Game game = null;
	public DrawThread drawThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

		Bitmaps.init(this);

		game = new Game();

		drawThread = new DrawThread(((SurfaceView)findViewById(R.id.gameView)).getHolder(), this);
		drawThread.start();

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}

		// -----------------

		game = new Game();

		gameView = (SurfaceView) findViewById(R.id.gameView);
		gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
}
