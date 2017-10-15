package eu.kotrzena.peasantconquest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import java.util.HashMap;
import java.util.Map;

public class Tile {
	public static int TILE_SIZE = 64;

	public static byte ROAD_N = 8;
	public static byte ROAD_S = 4;
	public static byte ROAD_W = 2;
	public static byte ROAD_E = 1;

	Bitmap texture = null;
	byte roads = 0;

	private Point position;

	public Tile(int x, int y, Bitmap texture){
		this.position = new Point(x, y);
		this.texture = texture;
	}

	public Point getPosition() {
		return position;
	}

	public void draw(Canvas c){

		if(texture != null)
			c.drawBitmap(texture, position.x*TILE_SIZE, position.y*TILE_SIZE, null);
	}

	public byte getRoads() {
		return roads;
	}

	public void setRoads(byte roads) {
		this.roads = roads;
	}
}
