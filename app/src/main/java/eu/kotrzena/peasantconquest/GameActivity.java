package eu.kotrzena.peasantconquest;

import android.graphics.Point;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;

public class GameActivity extends AppCompatActivity {
	public SurfaceView gameView = null;
	public SeekBar unitSlider = null;
	public Game game = null;
	public DrawThread drawThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

		Assets.init(this);

		game = Assets.loadMap(R.xml.mapa);

		drawThread = new DrawThread(((SurfaceView)findViewById(R.id.gameView)).getHolder(), this);
		drawThread.start();

		/*ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}*/

		// If the Android version is lower than Jellybean, use this call to hide
		// the status bar.
		if (Build.VERSION.SDK_INT < 16) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}


		// -----------------

		gameView = (SurfaceView) findViewById(R.id.gameView);
		unitSlider = (SeekBar) findViewById(R.id.unitSlider);

		/*gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);*/

		gameView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				game.onTouch(motionEvent);
				return true;
			}
		});

		unitSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				game.onUnitSliderChange(seekBar);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		game.onUnitSliderChange(unitSlider);

		gameView.post(new Runnable() {
			@Override
			public void run() {
				game.fitDisplay(gameView);
			}
		});
    }
}
