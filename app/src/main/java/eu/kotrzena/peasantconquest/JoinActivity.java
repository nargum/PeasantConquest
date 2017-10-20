package eu.kotrzena.peasantconquest;

import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class JoinActivity extends AppCompatActivity {
	class ServerEntry {
		InetAddress ipAddress;
		String map;
	}

	LinkedList<ServerEntry> servers = new LinkedList<ServerEntry>();

	ListView listServer = null;
	ArrayAdapter<ServerEntry> listAdapter;

	Thread scanThread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_join);

		listServer = (ListView) findViewById(R.id.listServers);
		listAdapter = new ArrayAdapter<ServerEntry>(this, R.layout.join_list_item_layout, servers){
			@Override
			public View getView(int position, View convertView, ViewGroup parent){
				View row = convertView;

				if(row == null)				{
					//LayoutInflater inflater = (LayoutInflater) JoinActivity.this.getSystemService( JoinActivity.this.LAYOUT_INFLATER_SERVICE );
					LayoutInflater inflater = getLayoutInflater();
					row = inflater.inflate(R.layout.join_list_item_layout, parent, false);
				}

				((TextView)row.findViewById(R.id.textIP)).setText("");
				((TextView)row.findViewById(R.id.textMap)).setText("");

				return row;
			}
		};
		listServer.setAdapter(listAdapter);

		try {
			scanThread = new Thread(){
				private final DatagramSocket server = new DatagramSocket(3074);

				@Override
				public void run(){
					try {
						DatagramSocket broadcast = new DatagramSocket(null);
						Parcel parcel = Parcel.obtain();
						parcel.writeByte(Networking.MessageType.SERVER_SCAN);
						byte data[] = parcel.marshall();
						DatagramPacket p = new DatagramPacket(data, data.length);
						p.setAddress(Networking.getBroadcastAddress(JoinActivity.this));
						broadcast.send(p);
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						while(!isInterrupted()) {
							DatagramPacket p = new DatagramPacket(new byte[512],512);
							server.receive(p);

							Parcel parcel = Parcel.obtain();
							parcel.unmarshall(p.getData(), 0, p.getLength());

							byte messageType = parcel.readByte();
							if(messageType == Networking.MessageType.SERVER_SCAN_RESPONSE){
								ServerEntry se = new ServerEntry();
								se.ipAddress = p.getAddress();
								se.map = parcel.readString();
								servers.add(se);
								listAdapter.notifyDataSetChanged();
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void interrupt(){
					super.interrupt();
					server.close();
				}
			};
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		scanThread.start();
	}

	@Override
	protected void onPause() {
		super.onPause();

		scanThread.interrupt();
	}
}
