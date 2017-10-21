package eu.kotrzena.peasantconquest.game;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import java.net.InetAddress;
import java.net.Socket;

public class PlayerInfo {
	public final int id;
	public final int color;
	public final Paint colorFilter;
	public boolean isHost = false;
	public InetAddress address = null;
	public Socket socket = null;

	public PlayerInfo(int id, int color){
		this.id = id;
		this.color = color;
		colorFilter = new Paint();
		//colorFilter.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.OVERLAY));
		//colorFilter.setColorFilter(new LightingColorFilter(0xFFFFFFFF - color, color));

		float r = Color.red(color) / 256f, g = Color.green(color) / 256f, b = Color.blue(color) / 256f;
		colorFilter.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]
			{
					2*r, 0, 0, 0, r, // r
					0, 2*g, 0, 0, g, // g
					0, 0, 2*b, 0, b, // b
					0, 0, 0, 1, 0, // a
			}))
		);
	}
}
