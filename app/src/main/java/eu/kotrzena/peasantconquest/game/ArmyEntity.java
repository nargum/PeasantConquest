package eu.kotrzena.peasantconquest.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import android.util.SparseArray;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import eu.kotrzena.peasantconquest.R;

public class ArmyEntity extends Entity {
	public class Unit extends Entity {
		public Bitmap colorLayer;
		private float jump;
		private boolean jumpDir;
		private final float JUMP_HEIGHT = 0.1f;

		public Unit(){
			Random rand = new Random();
			jump = rand.nextFloat();
			jumpDir = rand.nextBoolean();
		}

		@Override
		public PointF getPosition() {
			PointF position = super.getPosition();
			PointF armyPosition = ArmyEntity.this.getPosition();
			if(armyPosition == null)
				return null;
			return new PointF(
				position.x + armyPosition.x,
				position.y + armyPosition.y - 0.51f
			);
		}

		public void draw(Canvas c){
			PointF position = getPosition();
			if(position == null)
				return;
			if(jumpDir){
				jump += 0.3;
				if(jump >= 1)
					jumpDir = false;
			} else {
				jump -= 0.3;
				if(jump <= 0)
					jumpDir = true;
			}
			PointF unitPos = new PointF(
				position.x*Tile.TILE_SIZE - texture.getWidth()/2,
				position.y*Tile.TILE_SIZE + jump*JUMP_HEIGHT*Tile.TILE_SIZE - texture.getHeight()
			);
			c.drawBitmap(
				texture,
				unitPos.x, unitPos.y,
				null
			);
			c.drawBitmap(
				colorLayer,
				unitPos.x, unitPos.y,
				ArmyEntity.this.playerFilter
			);
		}
	}

	private static SparseArray<ArmyEntity> armyEntities = new SparseArray<ArmyEntity>();

	public static void updateAll(){
		synchronized (Game.getGame().gameLogic.armies) {
			for (int i = 0; i < armyEntities.size(); i++) {
				if (armyEntities.valueAt(i).update()) {
					//Log.i("ArmyEntity", "ArmyEntity remove, armyId = "+armyEntities.valueAt(i).armyId);
					armyEntities.remove(armyEntities.keyAt(i--));
				}
			}
			SparseArray<GameLogic.Army> armies = Game.getGame().gameLogic.armies;
			for (int i = 0; i < armies.size(); i++) {
				int armyId = armies.keyAt(i);
				if (armyEntities.get(armyId) == null) {
					ArmyEntity armyEntity = new ArmyEntity(armyId);
					Game.getGame().entities.add(armyEntity);
					armyEntities.append(armyId, armyEntity);
				}
			}
		}
	}

	private final int MAX_UNITS = 100;
	private int armyId;
	private Paint playerFilter;
	private List<Entity> entities = new LinkedList<Entity>();

	private ArmyEntity(int armyId){
		this.armyId = armyId;

		GameLogic.Army army = Game.getGame().gameLogic.armies.get(armyId);

		playerFilter = Game.getGame().getPlayers().get(army.playerId).colorFilter;

		Random rand = new Random();
		int unitsToCreate = Math.min(MAX_UNITS, (int)army.unitsCount);
		for (int ui = 0; ui < unitsToCreate; ui++) {
			Unit u = new Unit();
			if (ui == 0)
				u.setPosition(0, 0);
			else
				u.setPosition(rand.nextFloat() - 0.5f, rand.nextFloat() - 0.5f);
			u.texture = Assets.getBitmap(R.drawable.unit1);
			u.colorLayer = Assets.getBitmap(R.drawable.unit1_color);
			entities.add(u);
			Game.getGame().entities.add(u);
		}
	}

	@Override
	public PointF getPosition() {
		GameLogic.Army a = Game.getGame().gameLogic.armies.get(armyId);
		if(a == null)
			return null;
		PointF pos =Game.getGame().getArmyPosition(a);
		pos.y += 0.51;
		return pos;
	}

	@Override
	public void draw(Canvas c) {
		PointF point = getPosition();
		if(point == null)
			return;
		point.y -= 0.51;

		int flagOffset = 1;
		for(int aIdx = 0; aIdx < armyEntities.size(); aIdx++){
			if(armyEntities.keyAt(aIdx) == armyId){
				break;
			}
			PointF point2 = armyEntities.valueAt(aIdx).getPosition();
			if(point2 == null)
				continue;
			float distanceX = (point.x - point2.x);
			distanceX *= distanceX;
			float distanceY = (point.y - point2.y);
			distanceY *= distanceY;
			if(distanceX+distanceY < 0.1)
				flagOffset++;
		}

		Bitmap armyFlag = Assets.getBitmap(R.drawable.army_flag);
		c.drawBitmap(
			armyFlag,
			point.x * Tile.TILE_SIZE, (point.y - 0.5f) * Tile.TILE_SIZE - armyFlag.getHeight()*flagOffset,
			playerFilter
		);
		int unitsCount = (int)Game.getGame().gameLogic.armies.get(armyId).unitsCount;
		Paint textPaint = Assets.getTextPaint();
		textPaint.setTextAlign(Paint.Align.CENTER);
		c.drawText(
			Integer.toString(unitsCount),
			(point.x + 0.18f) * Tile.TILE_SIZE, (point.y - 0.55f) * Tile.TILE_SIZE - armyFlag.getHeight()*(flagOffset-1),
			textPaint
		);
	}

	private boolean update(){
		GameLogic.Army army = Game.getGame().gameLogic.armies.get(armyId);
		if(army == null){
			for(Entity e : entities){
				Game.getGame().entities.remove(e);
			}
			entities.clear();
			return true;
		} else if((int)army.unitsCount > entities.size()){
			int unitsToAdd = Math.min((int)army.unitsCount - entities.size(), MAX_UNITS-entities.size());
			Random rand = new Random();
			for(int i = 0; i < unitsToAdd; i++){
				Unit u = new Unit();
				u.setPosition(rand.nextFloat() - 0.5f, rand.nextFloat() - 0.5f);
				u.texture = Assets.getBitmap(R.drawable.unit1);
				u.colorLayer = Assets.getBitmap(R.drawable.unit1_color);
				entities.add(u);
				Game.getGame().entities.add(u);
			}
		} else if(army.unitsCount < entities.size()){
			int unitsToRemove = entities.size() - (int)army.unitsCount;
			for(int i = 0; i < unitsToRemove; i++){
				Entity u = entities.remove(entities.size()-1);
				Game.getGame().entities.remove(u);
			}
		}
		return false;
	}
}
