package eu.kotrzena.peasantconquest;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import eu.kotrzena.peasantconquest.game.Assets;
import eu.kotrzena.peasantconquest.game.Game;
import eu.kotrzena.peasantconquest.game.PlayerInfo;

public class GameActivity extends AppCompatActivity {
	public SurfaceView gameView = null;
	public SeekBar unitSlider = null;
	public View overlay = null;
	public Game game = null;
	public DrawThread drawThread = null;

	private Thread scanResponseThread = null;
	private Thread serverThread = null;
	private int serverMap = 0;
	private Thread clientThread = null;

	// Connection to server if in client mode
	public ClientConnection clientConnection = null;

	// Servers logic thread if in server mode
	public ServerLogicThread serverLogicThread = null;

	class LoadRunnable implements Runnable {
		@Override
		public void run() {
			Assets.init(GameActivity.this);

			Intent intent = getIntent();
			String type = intent.getStringExtra("type");
			if(type.equals("server")){
				serverMap = R.xml.mapa;
				XmlPullParser xml = getResources().getXml(serverMap);
				startGame(xml, true);

				PlayerInfo p = game.getPlayers().get(0);
				p.isHost = true;
				p.ready = true;

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

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}

		// If the Android version is lower than Jellybean, use this call to hide
		// the status bar.
		if (Build.VERSION.SDK_INT < 16) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}


		// -----------------

		gameView = (SurfaceView) findViewById(R.id.gameView);
		unitSlider = (SeekBar) findViewById(R.id.unitSlider);
		overlay = findViewById(R.id.overlay);

		/*gameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);*/

		/*Intent intent = getIntent();
		String type = intent.getStringExtra("type");
		if(type.equals("server")){
			serverMap = R.xml.mapa;
			XmlPullParser xml = getResources().getXml(serverMap);
			startGame(xml);
			//game.getPlayers().get(0).isHost = true;

			startScanResponseThread();
			startServerSocket();
		} else if(type.equals("client")){
			byte address[] = intent.getByteArrayExtra("address");
			try {
				InetAddress ipAddress = InetAddress.getByAddress(address);
				startClientSocket(ipAddress);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}*/
    }

	@Override
	protected void onStart() {
		super.onStart();

		new Thread(new LoadRunnable()).start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(scanResponseThread != null)
			scanResponseThread.interrupt();
		if(clientThread != null)
			clientThread.interrupt();
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
		if(game != null && game.getPlayers() != null){
			for(PlayerInfo p : game.getPlayers()){
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
	}

	private void startScanResponseThread(){
		if(scanResponseThread != null && !scanResponseThread.isInterrupted())
			scanResponseThread.interrupt();
		scanResponseThread = new Thread(){
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
										if (messageType == Networking.MessageType.SERVER_SCAN) {
											DatagramSocket response = null;
											try {
												response = new DatagramSocket(null);
												ByteArrayOutputStream baos = new ByteArrayOutputStream();
												DataOutputStream dos = new DataOutputStream(baos);
												dos.writeShort(Networking.IDENTIFIER);
												new Networking.ServerScanResponse(getResources().getResourceEntryName(serverMap)).write(dos);
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
											PlayerInfo player = null;
											ArrayList<PlayerInfo> players = game.getPlayers();
											synchronized (players) {
												for (PlayerInfo p : game.getPlayers()) {
													if (!p.isHost && p.clientConnection == null) {
														player = p;
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
													new ServerThread(player.clientConnection, GameActivity.this).start();
													//dos.flush();
												}
											}
										}
									}
								} catch (IOException e) {
									Log.e(this.getClass().getName(), "IOException", e);
								}
							}
						}.start();
					}
				} catch (IOException e) {
					Log.e(this.getClass().getName(), "IOException", e);
				}
			}

			@Override
			public void interrupt() {
				super.interrupt();
				if(server != null) {
					try {
						server.close();
					} catch (IOException e) {
						Log.e(this.getClass().getName(), "IOException", e);
					}
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
					dos.writeByte(Networking.MessageType.JOIN);
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
							new ClientThread(clientConnection, GameActivity.this).start();
							startGame(getResources().getXml(msg.mapId), false);
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

		gameView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
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
		} else {
			if (clientConnection != null)
				clientConnection.send(new Networking.SimpleMessage(Networking.MessageType.READY));
			Log.i("Networking", "Sending READY.");
		}
	}

	public void connectionLost(int reason){
		finish();
		Intent intent = new Intent(this, DialogActivity.class);
		intent.putExtra("title", R.string.error);
		intent.putExtra("text", reason);
		startActivity(intent);
	}
}
