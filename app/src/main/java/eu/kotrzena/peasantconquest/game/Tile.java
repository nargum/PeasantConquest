package eu.kotrzena.peasantconquest.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import java.util.HashMap;
import java.util.Map;

public class Tile {
	public final static int TILE_SIZE = 64;

	public final static byte ROAD_N = 8;
	public final static byte ROAD_S = 4;
	public final static byte ROAD_W = 2;
	public final static byte ROAD_E = 1;

	Bitmap texture = null;
	private byte roads = 0;
	private Point position;
	int nodeId = -1;

	PlayerCity castle = null;
	int ownerOnStart = -1;

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

	public static Point getRoadDirection(byte road){
		switch(road){
			case ROAD_N: return new Point(0, -1);
			case ROAD_S: return new Point(0, 1);
			case ROAD_W: return new Point(-1, 0);
			case ROAD_E: return new Point(1, 0);
			default: return null;
		}
	}

	public static byte getRoadOppositeDirection(byte road){
		switch(road){
			case ROAD_N: return ROAD_S;
			case ROAD_S: return ROAD_N;
			case ROAD_W: return ROAD_E;
			case ROAD_E: return ROAD_W;
			default: return 0;
		}
	}
}
