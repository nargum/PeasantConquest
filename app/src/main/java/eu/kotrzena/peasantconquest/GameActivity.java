package eu.kotrzena.peasantconquest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import eu.kotrzena.peasantconquest.game.Assets;
import eu.kotrzena.peasantconquest.game.Game;
import eu.kotrzena.peasantconquest.game.PlayerInfo;

import static eu.kotrzena.peasantconquest.R.string.players;

public class GameActivity extends AppCompatActivity {
	public SurfaceView gameView = null;
	public SeekBar unitSlider = null;
	public View overlay = null;
	public ListView playerListView = null;
	public PlayerListAdapter playerListAdapter = null;
	public View winnerOverlay = null;
	public TextView winnerName = null;

	ScaleGestureDetector scaleGestureDetector;
	GestureDetector gestureDetector;

	public Game game = null;
	public DrawThread drawThread = null;

	Thread scanResponseThread = null;
	private Thread serverThread = null;
	public int serverMap = 0;
	private Thread clientThread = null;

	// Connection to server if in client mode
	public ClientConnection clientConnection = null;

	// Servers logic thread if in server mode
	public ServerLogicThread serverLogicThread = null;

	public SharedPreferences prefs;

	private class LoadRunnable implements Runnable {
		@Override
		public void run() {
			Assets.init(GameActivity.this);

			Intent intent = getIntent();
			String type = intent.getStringExtra("type");
			if(type.equals("server")){
				serverMap = intent.getIntExtra("map", R.xml.mapa);
				String loadMap = intent.getStringExtra("load");
				DataInputStream dis = null;
				if(loadMap != null) {
					try {
						FileInputStream fileInputStream = openFileInput(loadMap);
						dis = new DataInputStream(fileInputStream);
						serverMap = dis.readInt();
					} catch (IOException e) {
						Log.e(getClass().getName(), "IOException", e);
					}
				}
				XmlPullParser xml = getResources().getXml(serverMap);
				startGame(xml, true);
				if(dis != null) {
					game.load(dis);
					try {
						dis.close();
					} catch (IOException e) {}
				}

				PlayerInfo p = game.getPlayers().valueAt(0);
				p.isHost = true;
				p.playerName = prefs.getString("gamePlayerName", android.os.Build.MODEL);
				startScanResponseThread();
				startServerSocket();
			} else if(type.equals("client")){
				byte address[] = intent.getByteArrayExtra("address");
				try {
					InetAddress ipAddress = InetAddress.getByAddress(address);
					startClientSocket(ipAddress);
				} catch (UnknownHostException e) {
					Log.e(this.getClass().getName(), "UnknownHostException", e);
				}
			}
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		/*ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}*/

		// If the Android version is lower than Jellybean, use this call to hide
		// the status bar.
		/*if (Build.VERSION.SDK_INT < 16) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}*/

		// -----------------

		gameView = (SurfaceView) findViewById(R.id.gameView);
		unitSlider = (SeekBar) findViewById(R.id.unitSlider);
		overlay = findViewById(R.id.overlay);
		playerListView = (ListView)findViewById(R.id.playerList);
		playerListView.setAdapter(null);
		winnerOverlay = findViewById(R.id.winnerOverlay);
		winnerName = (TextView)findViewById(R.id.winnerName);

		scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				if(game != null)
					game.onScale(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
				return true;
			}
		});
		gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if(game == null || (e1.getPointerCount() < 2 && e2.getPointerCount() < 2))
					return false;
				game.onScroll(distanceX, distanceY);
				return true;
			}
		});

		new Thread(new LoadRunnable()).start();
    }

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(drawThread != null){
			drawThread.interrupt();
			drawThread = null;
		}
		MediaPlayer mp = Assets.getBackgroundMusic();
		if (mp.isPlaying())
			mp.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(drawThread == null && game != null){
			drawThread = new DrawThread(((SurfaceView)findViewById(R.id.gameView)).getHolder(), GameActivity.this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(scanResponseThread != null)
			scanResponseThread.interrupt();
		if(clientThread != null)
			clientThread.interrupt();
		if(serverThread != null)
			serverThread.interrupt();
		if(drawThread != null)
			drawThread.interrupt();
		if(clientConnection != null){
			if(clientConnection.thread != null)
				clientConnection.thread.interrupt();
			if(clientConnection.socket != null)
				try {
					clientConnection.socket.close();
				} catch (IOException e){}
		}
		if(serverLogicThread != null)
			serverLogicThread.interrupt();

		SparseArray<PlayerInfo> players = null;
		if(game != null)
			players = game.getPlayers();
		if(game != null && players != null){
			for(int i = 0; i < players.size(); i++){
				PlayerInfo p = players.valueAt(i);
				if(p.clientConnection != null){
					if(p.clientConnection.thread != null)
						p.clientConnection.thread.interrupt();
					if(p.clientConnection.socket != null)
						try {
							p.clientConnection.socket.close();
						} catch (IOException e){}
				}
			}
		}
		try {
			if(scanResponseThread != null)
				scanResponseThread.join();
			if(clientThread != null)
				clientThread.join();
			if(serverThread != null)
				serverThread.join();
			if(drawThread != null)
				drawThread.join();
			if(clientConnection != null && clientConnection.thread != null)
				clientConnection.thread.join();
			if(serverLogicThread != null)
				serverLogicThread.join();
			if(players != null)
				for(int i = 0; i < players.size(); i++){
					PlayerInfo p = players.valueAt(i);
					if(p.clientConnection != null){
						if(p.clientConnection.thread != null)
							p.clientConnection.thread.join();
					}
				}
		} catch (InterruptedException e) {}
		if(game != null) {
			game.destroy();
			game = null;
		}
	}

	private void startScanResponseThread(){
		if(scanResponseThread != null && !scanResponseThread.isInterrupted())
			scanResponseThread.interrupt();
		scanResponseThread = new Thread(){
			private DatagramSocket server = null;

			@Override
			public void run(){
				String mapName = getResources().getResourceEntryName(serverMap);
				for(int i = 0; i < StartGameActivity.mapInfoList.length; i++){
					if(StartGameActivity.mapInfoList[i].resId == serverMap){
						mapName = getString(StartGameActivity.mapInfoList[i].mapName);
					}
				}
				final String finalMapName = mapName;
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
										if (messageType == Networking.MessageType.SERVER_SCAN) {
											DatagramSocket response = null;
											try {
												response = new DatagramSocket(null);
												ByteArrayOutputStream baos = new ByteArrayOutputStream();
												DataOutputStream dos = new DataOutputStream(baos);
												dos.writeShort(Networking.IDENTIFIER);
												new Networking.ServerScanResponse(
														finalMapName,
													prefs.getString("gamePlayerName", android.os.Build.MODEL)
												).write(dos);
												byte data[] = baos.toByteArray();
												DatagramPacket p = new DatagramPacket(data, data.length);
												p.setAddress(packet.getAddress());
												//p.setAddress(Networking.getBroadcastAddress());
												p.setPort(Networking.PORT);
												response.send(p);
											} catch (SocketException e) {
												Log.e(this.getClass().getName(), "SocketException", e);
											} catch (IOException e) {
												Log.e(this.getClass().getName(), "IOException", e);
											} finally {
												if (response != null)
													response.close();
											}
										}
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}.start();
					}
				} catch (IOException e) {
					Log.e(this.getClass().getName(), "IOException", e);
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
		scanResponseThread.start();
	}

	private void startServerSocket(){
		serverThread = new Thread(){
			private ServerSocket server = null;
			@Override
			public void run() {
				try {
					server = new ServerSocket(Networking.PORT);

					while(!isInterrupted()){
						Socket socket = server.accept();
						socket.setKeepAlive(true);
						socket.setTcpNoDelay(true);
						socket.setReceiveBufferSize(128);
						new Networking.TcpResponseThread(socket){
							@Override
							public void run() {
								Log.i("Networking", "Received connection from "+socket.getInetAddress());
								try {
									DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
									if(dis.readShort() == Networking.IDENTIFIER){
										if(dis.readByte() == Networking.MessageType.JOIN){
											Networking.Join msg = new Networking.Join(dis);
											PlayerInfo player = null;
											synchronized (game.getPlayers()) {
												SparseArray<PlayerInfo> players = game.getPlayers();
												for(int i = 0; i < players.size(); i++){
													PlayerInfo p = players.valueAt(i);
													if (!p.isHost && p.clientConnection == null) {
														player = p;
														player.playerName = msg.playerName;
														break;
													}
												}
												DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
												if (player == null) {
													Log.i("Networking", "Join refuse "+socket.getInetAddress());
													new Networking.JoinRefuse(R.string.join_refused_no_empty_slot).write(dos);
													dos.close();
													socket.close();
												} else {
													Log.i("Networking", "Join accept "+socket.getInetAddress());
													new Networking.JoinAccept(serverMap, player.id).write(dos);
													player.clientConnection = new ClientConnection(player.id);
													player.clientConnection.address = socket.getInetAddress();
													player.clientConnection.socket = socket;
													player.clientConnection.in = dis;
													player.clientConnection.out = dos;
													new Networking.PlayersInfo(game.getPlayers()).write(dos);
													new ServerThread(player.clientConnection, GameActivity.this).start();
													runOnUiThread(new Runnable() {
														@Override
														public void run() {
															playerListAdapter.notifyDataSetChanged();
														}
													});
												}
											}
										}
									}
								} catch (IOException e) {
									Log.e(this.getClass().getName(), "serverThread - TcpResponseThread", e);
								}
							}
						}.start();
					}
				} catch (IOException e) {
					Log.e(this.getClass().getName(), "serverThread", e);
				}
			}

			@Override
			public void interrupt() {
				super.interrupt();
				if(server != null) {
					try {
						server.close();
					} catch (IOException e) {}
				}
			}
		};
		serverThread.setName("_StartServerSocketThread");
		serverThread.start();
	}

	private void startClientSocket(final InetAddress serverAddr){
		clientThread = new Thread(){
			Socket socket = null;
			@Override
			public void run() {
				try {
					socket = new Socket(serverAddr, Networking.PORT);
					socket.setKeepAlive(true);
					socket.setTcpNoDelay(true);
					socket.setReceiveBufferSize(128);
					Log.i("Networking", "Connected to "+socket.getInetAddress());
					DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
					dos.writeShort(Networking.IDENTIFIER);
					new Networking.Join(prefs.getString("gamePlayerName", android.os.Build.MODEL)).write(dos);
					dos.flush();

					DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					byte messageType = dis.readByte();
					switch(messageType){
						case Networking.MessageType.JOIN_ACCEPT: {
							Log.i("Networking", "Join accepted "+socket.getInetAddress());
							Networking.JoinAccept msg = new Networking.JoinAccept(dis);
							clientConnection = new ClientConnection(msg.playerId);
							clientConnection.socket = socket;
							clientConnection.address = socket.getInetAddress();
							clientConnection.in = dis;
							clientConnection.out = dos;
							startGame(getResources().getXml(msg.mapId), false);
							serverMap = msg.mapId;
							new ClientThread(clientConnection, GameActivity.this).start();
							break;
						}
						case Networking.MessageType.JOIN_REFUSE: {
							Log.i("Networking", "Join refused "+socket.getInetAddress());
							final Networking.JoinRefuse msg = new Networking.JoinRefuse(dis);
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									GameActivity.this.connectionLost(msg.reasonStringId);
								}
							});
							socket.close();
							break;
						}
					}
				} catch (IOException e) {
					connectionLost(R.string.server_not_listening);
				}
			}
		};
		clientThread.setName("_StartClientSocketThread");
		clientThread.start();
	}

    private void startGame(XmlPullParser map, boolean server){
		game = Assets.loadMap(map);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				GameActivity.this.playerListAdapter = new PlayerListAdapter(GameActivity.this, game.getPlayers());
				playerListView.setAdapter(GameActivity.this.playerListAdapter);
			}
		});

		gameView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				scaleGestureDetector.onTouchEvent(motionEvent);
				gestureDetector.onTouchEvent(motionEvent);
				game.onTouch(motionEvent);
				return true;
			}
		});

		drawThread = new DrawThread(((SurfaceView)findViewById(R.id.gameView)).getHolder(), GameActivity.this);
		drawThread.start();

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

		GameActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				game.fitDisplay(gameView);
			}
		});

		if(server) {
			serverLogicThread = new ServerLogicThread(this);
			serverLogicThread.start();
		}
	}

	public void ready(boolean ready){
		game.getPlayers().get(game.getCurrentPlayerId()).ready = ready;
		if(clientConnection != null) {
			clientConnection.send(new Networking.SimpleMessage(Networking.MessageType.READY));
			Log.i("Networking", "Sending READY.");
		} else if(serverThread != null){
			SparseArray<PlayerInfo> players = game.getPlayers();
			for (int i = 0; i < players.size(); i++) {
				PlayerInfo p = players.valueAt(i);
				if (p.clientConnection != null) {
					try {
						p.clientConnection.send(new Networking.PlayersInfo(players));
					} catch (IOException e) {
						Log.e(this.getClass().getName(), "IOException", e);
					}
				}
			}
		}
	}

	public void connectionLost(int reason){
		finish();
		Intent intent = new Intent(this, DialogActivity.class);
		intent.putExtra("title", R.string.error);
		intent.putExtra("text", reason);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if(game != null && !game.pause) {
			getMenuInflater().inflate(R.menu.game_activity_menu, menu);
			return true;
		}

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.pauseGame:
				game.pause = true;
				overlay.setVisibility(View.VISIBLE);
				PlayerInfo p = game.getPlayers().get(game.getCurrentPlayerId());
				p.ready = false;
				new Thread(){
					@Override
					public void run() {
						final SparseArray<PlayerInfo> players = game.getPlayers();
						try {
							final Networking.PlayersInfo playersInfo = new Networking.PlayersInfo(players);
							for(int i = 0; i < players.size(); i++){
								final PlayerInfo p = players.valueAt(i);
								new Thread(){
									@Override
									public void run() {
										if(p.clientConnection != null) {
											p.clientConnection.send(new Networking.SimpleMessage(Networking.MessageType.PAUSE));
											p.clientConnection.send(playersInfo);
										}
									}
								}.start();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}.start();
				break;
			case R.id.saveGame:
				String map = "Map";
				for(int i = 0; i < StartGameActivity.mapInfoList.length; i++){
					if(StartGameActivity.mapInfoList[i].resId == serverMap){
						map = getString(StartGameActivity.mapInfoList[i].mapName);
					}
				}
				Locale locale;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					locale = getResources().getConfiguration().getLocales().get(0);
				} else {
					locale = getResources().getConfiguration().locale;
				}
				Calendar cal = GregorianCalendar.getInstance();
				map += " - "+DateFormat.getDateFormat(this).format(cal.getTime())+" "+DateFormat.getTimeFormat(this).format(cal.getTime());

				game.save(map);
				Toast toast = Toast.makeText(this, R.string.game_saved, Toast.LENGTH_LONG);
				toast.show();
				break;
		}
		return true;
	}
}
