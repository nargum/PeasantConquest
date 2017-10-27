package eu.kotrzena.peasantconquest;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;

public class JoinActivity extends AppCompatActivity {
	private class ServerEntry {
		InetAddress ipAddress;
		String map;
		String phoneName;
	}

	LinkedList<ServerEntry> servers = new LinkedList<ServerEntry>();

	ListView listServer = null;
	ArrayAdapter<ServerEntry> listAdapter;

	Thread scanThread = null;
	Thread broadcastThread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_join);

		listServer = (ListView) findViewById(R.id.listServers);
		listAdapter = new ArrayAdapter<ServerEntry>(this, R.layout.join_list_item_layout, servers){
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent){
				View row = convertView;

				if(row == null)				{
					//LayoutInflater inflater = (LayoutInflater) JoinActivity.this.getSystemService( JoinActivity.this.LAYOUT_INFLATER_SERVICE );
					LayoutInflater inflater = getLayoutInflater();
					row = inflater.inflate(R.layout.join_list_item_layout, parent, false);
				}

				final ServerEntry se = servers.get(position);
				((TextView)row.findViewById(R.id.textIP)).setText(se.ipAddress.toString());
				((TextView)row.findViewById(R.id.textMap)).setText(se.map);
				((TextView)row.findViewById(R.id.textName)).setText(se.phoneName);

				return row;
			}
		};
		listServer.setAdapter(listAdapter);
		listServer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				ServerEntry server = servers.get(i);
				Intent intent = new Intent(JoinActivity.this, GameActivity.class);
				intent.putExtra("type", "client");
				intent.putExtra("address", server.ipAddress.getAddress());
				startActivity(intent);
			}
		});
	}

	private void startScan(){
		if(broadcastThread != null && !broadcastThread.isInterrupted())
			broadcastThread.interrupt();
		broadcastThread = new Thread(){
			@Override
			public void run() {
				DatagramSocket broadcast = null;
				try {
					broadcast = new DatagramSocket(null);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);

					dos.writeShort(Networking.IDENTIFIER);
					dos.writeByte(Networking.MessageType.SERVER_SCAN);
					dos.flush();

					byte data[] = baos.toByteArray();
					DatagramPacket p = new DatagramPacket(data, data.length);
					p.setAddress(Networking.getBroadcastAddress());
					p.setPort(Networking.PORT);
					broadcast.send(p);
				} catch (SocketException e) {
					Log.e(this.getClass().getName(), "SocketException", e);
				} catch (IOException e) {
					Log.e(this.getClass().getName(), "IOException", e);
				} finally {
					if(broadcast != null)
						broadcast.close();
				}
			}
		};
		broadcastThread.start();
	}

	@Override
	protected void onResume() {
		super.onResume();

		servers.clear();

		scanThread = new Thread(){
			private DatagramSocket server = null;

			@Override
			public void run(){
				try {
					server = new DatagramSocket(Networking.PORT);
					while(!isInterrupted()) {
						DatagramPacket p = new DatagramPacket(new byte[512],512);
						server.receive(p);

						new Networking.UdpResponseThread(p){
							@Override
							public void run() {
								ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
								DataInputStream dis = new DataInputStream(bais);
								try {
									if(dis.readShort() == Networking.IDENTIFIER) {
										byte messageType = dis.readByte();
										if (messageType == Networking.MessageType.SERVER_SCAN_RESPONSE) {
											Networking.ServerScanResponse ssr = new Networking.ServerScanResponse(dis);
											ServerEntry se = new ServerEntry();
											se.ipAddress = packet.getAddress();
											se.map = ssr.mapName;
											se.phoneName = ssr.phoneName;
											servers.add(se);
											JoinActivity.this.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													listAdapter.notifyDataSetChanged();
												}
											});
										}
									}
								} catch (IOException e) {
									//Log.e(this.getClass().getName(), "IOException", e);
								}
							}
						}.start();
					}
				} catch (IOException e) {
					//Log.e(this.getClass().getName(), "IOException", e);
				} finally {
					if(server != null)
						server.close();
				}
			}

			@Override
			public void interrupt(){
				super.interrupt();
				if(server != null)
					server.close();
			}
		};

		scanThread.start();
		startScan();
	}

	@Override
	protected void onPause() {
		scanThread.interrupt();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.join_activity_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		servers.clear();
		listAdapter.notifyDataSetChanged();
		startScan();
		return true;
	}
}
