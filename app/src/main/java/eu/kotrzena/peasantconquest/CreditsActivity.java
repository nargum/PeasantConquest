package eu.kotrzena.peasantconquest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class CreditsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credits);
		/*
		TODO: První link furt nejde
		TextView textView=(TextView) findViewById(R.id.link);
		textView.setClickable(true);
		String linkTxt=getResources().getString(R.string.link);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		textView.setText(Html.fromHtml( linkTxt));
		*/
	}
}
