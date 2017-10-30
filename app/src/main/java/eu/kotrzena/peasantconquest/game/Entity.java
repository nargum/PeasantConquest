package eu.kotrzena.peasantconquest.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;

public class Entity {
	Bitmap texture;

	protected final PointF position = new PointF();

	public Entity(){
	}

	public void draw(Canvas c){
		if(texture != null){
			c.drawBitmap(
				texture,
				Tile.TILE_SIZE * position.x - ((float)texture.getWidth())/2,
				Tile.TILE_SIZE * position.y - ((float)texture.getHeight()),
				null
			);
		}
	}

	public PointF getPosition() {
		return position;
	}

	public void setPosition(float x, float y){
		position.set(x, y);
	}
}
