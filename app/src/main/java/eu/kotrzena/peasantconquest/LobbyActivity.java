package eu.kotrzena.peasantconquest;

import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class LobbyActivity extends AppCompatActivity {
	Thread serverThread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);
	}

	@Override
	protected void onResume() {
		super.onResume();


	}

	@Override
	protected void onPause() {
		super.onPause();

		serverThread.interrupt();
	}
}
