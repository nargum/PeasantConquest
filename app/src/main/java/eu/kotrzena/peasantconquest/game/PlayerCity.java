package eu.kotrzena.peasantconquest.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import eu.kotrzena.peasantconquest.R;

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
			if(tile.nodeId >= 0) {
				Bitmap cityFlag = Assets.getBitmap(R.drawable.city_flag);
				c.drawBitmap(
					cityFlag,
					position.x * Tile.TILE_SIZE - cityFlag.getWidth(), (position.y - 1f) * Tile.TILE_SIZE - cityFlag.getHeight(),
					(playerId > 0)?
						Game.getGame().players.get(-1 + playerId).colorFilter :
						null
				);
				int unitsCount = (int)Game.getGame().gameLogic.nodes[tile.nodeId].unitsCount;
				Paint textPaint = Assets.getTextPaint();
				textPaint.setTextAlign(Paint.Align.CENTER);
				c.drawText(
					Integer.toString((int) unitsCount),
					(position.x - 0.18f) * Tile.TILE_SIZE, (position.y - 1.05f) * Tile.TILE_SIZE,
					textPaint
				);
			}
		}
	}
}
