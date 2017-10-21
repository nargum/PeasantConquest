package eu.kotrzena.peasantconquest;

import android.content.Intent;
import android.opengl.Visibility;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import eu.kotrzena.peasantconquest.game.Assets;
import eu.kotrzena.peasantconquest.game.Game;

public class GameActivity extends AppCompatActivity {
	public SurfaceView gameView = null;
	public SeekBar unitSlider = null;
	private View overlay = null;
	public Game game = null;
	public DrawThread drawThread = null;

	private Thread scanResponseThread = null;
	private Thread serverThread = null;
	private int serverMap = 0;
	private Thread clientThread = null;

	class LoadRunnable implements Runnable {
		@Override
		public void run() {
			Assets.init(GameActivity.this);

			Intent intent = getIntent();
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
					Log.e(this.getClass().getName(), "UnknownHostException", e);
				}
			}
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_game);

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
		new Thread(new LoadRunnable()).start();
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
									if(dis.readShort() == Networking.INDENTIFIER) {
										byte messageType = dis.readByte();
										if (messageType == Networking.MessageType.SERVER_SCAN) {
											DatagramSocket response = null;
											try {
												response = new DatagramSocket(null);
												ByteArrayOutputStream baos = new ByteArrayOutputStream();
												DataOutputStream dos = new DataOutputStream(baos);
												dos.writeShort(Networking.INDENTIFIER);
												dos.writeByte(Networking.MessageType.SERVER_SCAN_RESPONSE);
												String map = getResources().getResourceEntryName(serverMap);
												for(int i = 0; i < map.length(); i++){
													dos.writeChar(map.charAt(i));
												}
												dos.writeChar('\0');
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
						new Networking.TcpResponseThread(socket){
							@Override
							public void run() {
								try {
									InputStream is = socket.getInputStream();
									DataInputStream dis = new DataInputStream(is);
									if(dis.readShort() == Networking.INDENTIFIER){
										if(dis.readByte() == Networking.MessageType.JOIN){
											OutputStream os = socket.getOutputStream();
											DataOutputStream dos = new DataOutputStream(os);
											dos.writeInt(serverMap);
											dos.flush();
											os.flush();
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
					OutputStream os = socket.getOutputStream();
					DataOutputStream dos = new DataOutputStream(os);
					dos.writeShort(Networking.INDENTIFIER);
					dos.writeByte(Networking.MessageType.JOIN);
					dos.flush();
					os.flush();

					InputStream is = socket.getInputStream();
					DataInputStream dis = new DataInputStream(is);
					int map_id = dis.readInt();
					startGame(getResources().getXml(map_id));
				} catch (IOException e) {
					Log.e(this.getClass().getName(), "IOException", e);
				}
			}
		};
		clientThread.start();
	}

    private void startGame(XmlPullParser map){
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
				overlay.setVisibility(View.GONE);
			}
		});
	}
}
