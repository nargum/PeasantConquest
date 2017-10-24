package eu.kotrzena.peasantconquest;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DialogActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dialog);

		Button button = ((Button)findViewById(R.id.button));
		TextView text = ((TextView) findViewById(R.id.text));

		Intent intent = getIntent();

		if(intent.hasExtra("title")){
			int str_id = intent.getIntExtra("title", 0);
			if(str_id > 0){
				setTitle(str_id);
			} else {
				String str = intent.getStringExtra("title");
				if(str != null) {
					setTitle(str);
				}
			}
		}

		if(intent.hasExtra("text")){
			int str_id = intent.getIntExtra("text", 0);
			if(str_id > 0){
				text.setText(str_id);
			} else {
				String str = intent.getStringExtra("text");
				if(str != null) {
					text.setText(str);
				}
			}
		}

		button.setText(getString(R.string.ok));
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});
	}
}
