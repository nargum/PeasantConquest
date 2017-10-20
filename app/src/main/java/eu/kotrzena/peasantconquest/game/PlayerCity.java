package eu.kotrzena.peasantconquest.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class PlayerCity extends Entity {
	public Tile tile = null;
	public Bitmap colorLayer = null;

	@Override
	public void draw(Canvas c){
		super.draw(c);
		if(colorLayer != null && tile != null && tile.nodeId >= 0){
			int playerId = Game.getGame().gameLogic.nodes[tile.nodeId].playerId;
			c.drawBitmap(
				colorLayer,
				Tile.TILE_SIZE * position.x - ((float) texture.getWidth()) / 2,
				Tile.TILE_SIZE * position.y - ((float) texture.getHeight()),
				(playerId > 0)?
					Game.getGame().players.get(-1 + playerId).colorFilter :
					null
			);
		}
	}
}
