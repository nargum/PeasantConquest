package eu.kotrzena.peasantconquest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
	private Button buttonStartGame;
	private Button buttonJoinGame;
	private Button buttonSettings;
	private Button buttonCredits;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		buttonStartGame = (Button) findViewById(R.id.buttonStartGame);
		buttonJoinGame = (Button) findViewById(R.id.buttonJoinGame);
		buttonSettings = (Button) findViewById(R.id.buttonSettings);
		buttonCredits = (Button) findViewById(R.id.buttonCredits);

		buttonStartGame.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(MainActivity.this, StartGameActivity.class);
				startActivity(i);
			}
		});
		buttonJoinGame.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(MainActivity.this, JoinActivity.class);
				startActivity(i);
			}
		});
		buttonSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(MainActivity.this, SettingsActivity.class);
				startActivity(i);
			}
		});
		buttonCredits.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(MainActivity.this, CreditsActivity.class);
				startActivity(i);
			}
		});
	}
}
