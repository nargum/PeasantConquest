package eu.kotrzena.peasantconquest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class Tile {
	public static int TILE_SIZE = 64;

	Bitmap texture = null;

	private int x;
	private int y;

	public Tile(int x, int y, Bitmap texture){
		this.x = x;
		this.y = y;
		this.texture = texture;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void draw(Canvas c){
		if(texture != null)
			c.drawBitmap(texture, x*TILE_SIZE, y*TILE_SIZE, null);
	}
}
