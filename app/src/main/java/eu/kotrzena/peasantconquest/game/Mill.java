package eu.kotrzena.peasantconquest.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import eu.kotrzena.peasantconquest.R;

public class Mill extends Entity {
	static private Bitmap mill_sails = null;
	private float angle = 360f;
	static private float ROTATING_SPEED = 0.4f;
	public void draw(Canvas c){
		if(mill_sails == null)
			mill_sails = Assets.getBitmap(R.drawable.mill_sails);
		super.draw(c);
		c.save();
		c.translate(
			Tile.TILE_SIZE * position.x,
			Tile.TILE_SIZE * position.y - 45f
		);
		c.rotate(angle);
		angle -= ROTATING_SPEED;
		if(angle < 0)
			angle = 360f;
		c.drawBitmap(
			mill_sails,
			-mill_sails.getWidth()/2,
			-mill_sails.getHeight()/2,
			null
		);
		c.restore();
	}
}
