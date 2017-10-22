package eu.kotrzena.peasantconquest;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.IOException;

public class ClientThread extends Thread {
	private ClientConnection connection;
	private GameActivity activity;
	public ClientThread(ClientConnection connection, GameActivity activity){
		setName("_ClientThread");
		this.connection = connection;
		this.activity = activity;
		connection.thread = this;
	}

	@Override
	public void run() {
		try {
			while(!isInterrupted() && !connection.socket.isClosed()) {
				byte messageType = connection.in.readByte();
				switch (messageType) {

				}
			}
		} catch (IOException e) {
			Log.e(getClass().getName(), "IOException", e);
		}

		Log.i("Networking", "Connection to server lost or closed.");

		if(!connection.socket.isClosed()){
			try {
				connection.socket.close();
			} catch (IOException e) {}
		}
		connection.socket = null;
		connection.in = null;
		connection.out = null;
		connection.thread = null;

		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setTitle(R.string.error);
		alertDialog.setMessage(activity.getString(R.string.connection_lost));
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alertDialog.show();
		activity.finish();
	}

	@Override
	public void interrupt() {
		super.interrupt();
		try {
			connection.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
