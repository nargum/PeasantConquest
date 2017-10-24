package eu.kotrzena.peasantconquest.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.SparseArray;

import eu.kotrzena.peasantconquest.R;

public class ArmyEntity {
	public static void drawAll(Canvas c){
		SparseArray<GameLogic.Army> armies = Game.getGame().gameLogic.armies;
		for(int i = 0; i < armies.size(); i++){
			draw(c, armies.keyAt(i), armies.valueAt(i));
		}
	}

	private static void draw(Canvas c, int armyId, GameLogic.Army army){
		Paint colorFilter = Game.getGame().getPlayers().get(army.playerId-1).colorFilter;
		PointF point = Game.getGame().getArmyPosition(army);
		Bitmap unit1 = Assets.getBitmap(R.drawable.unit1);
		Bitmap unit1_color = Assets.getBitmap(R.drawable.unit1_color);

		Bitmap cityFlag = Assets.getBitmap(R.drawable.army_flag);
		c.drawBitmap(
			cityFlag,
			point.x * Tile.TILE_SIZE, (point.y - 0.5f) * Tile.TILE_SIZE - cityFlag.getHeight(),
			colorFilter
		);
		int unitsCount = (int)army.unitsCount;
		Paint textPaint = Assets.getTextPaint();
		textPaint.setTextAlign(Paint.Align.CENTER);
		c.drawText(
			Integer.toString((int) unitsCount),
			(point.x + 0.18f) * Tile.TILE_SIZE, (point.y - 0.55f) * Tile.TILE_SIZE,
			textPaint
		);

		point.x *= Tile.TILE_SIZE;
		point.y *= Tile.TILE_SIZE;

		point.x -= unit1.getWidth()/2;
		point.y -= unit1.getHeight();
		c.drawBitmap(
			unit1,
			point.x, point.y,
			null
		);
		c.drawBitmap(
			unit1_color,
			point.x, point.y,
			colorFilter
		);
	}
}
