package eu.kotrzena.peasantconquest.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.MediaPlayer;

import eu.kotrzena.peasantconquest.R;

public class PlayerCity extends Entity {
	public Tile tile = null;
	public Bitmap colorLayer = null;

	private int lastPlayerId = -1;

	@Override
	public void draw(Canvas c){
		int playerId = Game.getGame().gameLogic.nodes[tile.nodeId].playerId;
		if(Game.getGame().playSoundEffects) {
			if (lastPlayerId != playerId && lastPlayerId != -1) {
				int currentPlayerId = Game.getGame().getCurrentPlayerId();
				if (playerId == currentPlayerId) {
					MediaPlayer mp = Assets.getCityConqueredSound();
					mp.seekTo(0);
					mp.start();
				} else if (lastPlayerId == currentPlayerId) {
					MediaPlayer mp = Assets.getCityLostSound();
					mp.seekTo(0);
					mp.start();
				}
			}
		}
		lastPlayerId = playerId;
		super.draw(c);
		if(colorLayer != null && tile != null && tile.nodeId >= 0){
			PointF position = getPosition();
			c.drawBitmap(
				colorLayer,
				Tile.TILE_SIZE * position.x - ((float) texture.getWidth()) / 2,
				Tile.TILE_SIZE * position.y - ((float) texture.getHeight()),
				(playerId > 0)?
					Game.getGame().players.get(playerId).colorFilter :
					null
			);
			if(tile.nodeId >= 0) {
				Bitmap cityFlag = Assets.getBitmap(R.drawable.city_flag);
				c.drawBitmap(
					cityFlag,
					position.x * Tile.TILE_SIZE - cityFlag.getWidth(), (position.y - 1f) * Tile.TILE_SIZE - cityFlag.getHeight(),
					(playerId > 0)?
						Game.getGame().players.get(playerId).colorFilter :
						null
				);
				int unitsCount = (int)Game.getGame().gameLogic.nodes[tile.nodeId].unitsCount;
				Paint textPaint = Assets.getTextPaint();
				textPaint.setTextAlign(Paint.Align.CENTER);
				c.drawText(
					Integer.toString(unitsCount),
					(position.x - 0.18f) * Tile.TILE_SIZE, (position.y - 1.05f) * Tile.TILE_SIZE,
					textPaint
				);
			}
		}
	}
}
