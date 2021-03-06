package eu.kotrzena.peasantconquest.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import eu.kotrzena.peasantconquest.GameActivity;
import eu.kotrzena.peasantconquest.Networking;
import eu.kotrzena.peasantconquest.R;

public class Game {
	private static final int[] playerColors = new int[]{
		0xffa6583c,
		0xff157da8,
		0xff7915a8
	};
	private static Game game = null;
	private GameActivity activity;
	private Tile[][] tiles;
	public LinkedList<Entity> entities = new LinkedList<Entity>();
	SparseArray<PlayerInfo> players = new SparseArray<>();
	public GameLogic gameLogic;

	public boolean pause = true;

	private int size_x;
	private int size_y;

	private int playAreaTop;
	private int playAreaBottom;
	private int playAreaLeft;
	private int playAreaRight;

	private float scale = 1.0f;
	private float minScale = 1.0f;
	private PointF offset = new PointF(0, 0);

	private int motionEventStartNode = -1;
	private float takeUnitsPct = 0;

	public boolean playMusic;
	public boolean playSoundEffects;

	public Game(GameActivity activity, Tile[][] tiles, List<Entity> entities){
		this.activity = activity;
		Game.game = this;
		size_x = tiles.length;
		size_y = tiles[0].length;

		this.tiles = tiles;
		this.entities.addAll(entities);

		playAreaRight = playAreaBottom = 0;
		playAreaLeft = size_x - 1;
		playAreaTop = size_y -1;

		for(int x = 0; x < size_x; x++){
			for(int y = 0; y < size_y; y++){
				if(tiles[x][y].getRoads() != 0){
					if(playAreaLeft > x)
						playAreaLeft = x;
					if(playAreaRight < x)
						playAreaRight = x;
					if(playAreaTop > y)
						playAreaTop = y;
					if(playAreaBottom < y)
						playAreaBottom = y;
				}
			}
		}

		playMusic = activity.prefs.getBoolean("soundMusic", true);
		playSoundEffects = activity.prefs.getBoolean("soundEffects", true);

		prepareLogic();
	}

	public void destroy(){
		game = null;
		activity = null;
		tiles = null;
		if(entities != null) {
			entities.clear();
			entities = null;
		}
		if(players != null) {
			players.clear();
			players = null;
		}
		gameLogic = null;
	}

	public static Game getGame(){
		return game;
	}

	public void fitDisplay(View view){
		float playAreaWidth = ((float)(playAreaRight-playAreaLeft+1) * Tile.TILE_SIZE);
		float playAreaHeight = ((float)(playAreaBottom-(playAreaTop-0.3f)+1) * Tile.TILE_SIZE);

		float scalex = ((float)view.getWidth()) / playAreaWidth;
		float scaley = ((float)view.getHeight()) / playAreaHeight;

		scale = (scalex > scaley)? scaley : scalex;

		playAreaWidth *= scale;
		playAreaHeight *= scale;

		offset.x = -(playAreaLeft * Tile.TILE_SIZE)*scale + Math.max((view.getWidth() - playAreaWidth)/2, 0);
		offset.y = -((playAreaTop-0.3f) * Tile.TILE_SIZE)*scale + Math.max((view.getHeight() - playAreaHeight)/2, 0);

		// Min scale
		scalex = ((float)view.getWidth()) / ((float)size_x*Tile.TILE_SIZE);
		scaley = ((float)view.getHeight()) / ((float)size_y*Tile.TILE_SIZE);

		minScale = (scalex < scaley)? scaley : scalex;
	}

	private void prepareLogic(){
		gameLogic = new GameLogic();

		// Find nodes
		{
			ArrayList<GameLogic.Node> nodes = new ArrayList<>();

			for (int x = 0; x < size_x; x++) {
				for (int y = 0; y < size_y; y++) {
					Tile tile = tiles[x][y];
					byte roads = tile.getRoads();
					byte roadsCount = (byte) (((roads & Tile.ROAD_N) != 0) ? 1 : 0);
					roadsCount += (byte) (((roads & Tile.ROAD_S) != 0) ? 1 : 0);
					roadsCount += (byte) (((roads & Tile.ROAD_W) != 0) ? 1 : 0);
					roadsCount += (byte) (((roads & Tile.ROAD_E) != 0) ? 1 : 0);
					if ((roadsCount != 2 && roadsCount != 0) || tile.castle != null) {
						GameLogic.Node node = gameLogic.new Node();
						node.position = new Point(x, y);
						node.roads = new int[roadsCount];
						for(int i = 0; i < node.roads.length; i++)
							node.roads[i] = -1;
						node.playerId = tiles[x][y].ownerOnStart;
						node.unitsCount = tiles[x][y].unitsOnStart;
						if(node.playerId > 0 && players.get(node.playerId) == null){
							PlayerInfo pi = new PlayerInfo(node.playerId, playerColors[node.playerId-1]);
							players.append(node.playerId, pi);
						}
						nodes.add(node);
						tile.nodeId = nodes.size() - 1;
					}
				}
			}

			gameLogic.nodes = new GameLogic.Node[nodes.size()];
			gameLogic.nodes = nodes.toArray(gameLogic.nodes);
		}

		// Find roads
		boolean visited[][] = new boolean[size_x][size_y];

		ArrayList<GameLogic.Road> roads = new ArrayList<GameLogic.Road>();

		for(int i = 0; i < gameLogic.nodes.length; i++){
			GameLogic.Node node = gameLogic.nodes[i];
			Point nodePoint = node.position;
			Tile tile = tiles[nodePoint.x][nodePoint.y];

			byte[] directions = new byte[]{Tile.ROAD_N, Tile.ROAD_S, Tile.ROAD_W, Tile.ROAD_E};
			for(byte dir : directions){
				if((tile.getRoads() & dir) == 0){
					continue;
				}
				ArrayList<Point> path = new ArrayList<Point>();
				GameLogic.Road road = gameLogic.new Road();
				road.fromNode = i;

				byte dirTo = dir;
				Point currentPoint = new Point(nodePoint.x, nodePoint.y);
				while(dirTo != 0) {
					Point roadVec = Tile.getRoadDirection(dirTo);
					currentPoint = new Point(currentPoint.x + roadVec.x, currentPoint.y + roadVec.y);
					if (tiles[currentPoint.x][currentPoint.y].nodeId != -1) {
						road.toNode = tiles[currentPoint.x][currentPoint.y].nodeId;
						break;
					}
					if(visited[currentPoint.x][currentPoint.y]){
						break;
					}
					path.add(currentPoint);
					visited[currentPoint.x][currentPoint.y] = true;
					dirTo = (byte)(tiles[currentPoint.x][currentPoint.y].getRoads() & (~Tile.getRoadOppositeDirection(dirTo)));
				}

				if(road.toNode != -1){
					road.path = new Point[path.size()];
					road.path = path.toArray(road.path);

					GameLogic.Node n = gameLogic.nodes[road.fromNode];
					for(int ri = 0; ri < n.roads.length; ri++){
						if(n.roads[ri] == -1){
							n.roads[ri] = roads.size();
							break;
						}
					}
					n = gameLogic.nodes[road.toNode];
					for(int ri = 0; ri < n.roads.length; ri++){
						if(n.roads[ri] == -1){
							n.roads[ri] = roads.size();
							break;
						}
					}
					roads.add(road);
				}
			}
		}

		gameLogic.roads = new GameLogic.Road[roads.size()];
		gameLogic.roads = roads.toArray(gameLogic.roads);
	}

	public Point getTouchTile(double x, double y){
		x -= offset.x;
		y -= offset.y;
		x /= scale;
		y /= scale;
		return new Point((int)(x/Tile.TILE_SIZE), (int)(y/Tile.TILE_SIZE));
	}

	public int getCurrentPlayerId(){
		if(activity.serverLogicThread != null) {
			SparseArray<PlayerInfo> players = game.getPlayers();
			for(int i = 0; i < players.size(); i++){
				PlayerInfo player = players.valueAt(i);
				if(player.isHost){
					return player.id;
				}
			}
		} else if(activity.clientConnection != null) {
			return activity.clientConnection.playerId;
		}
		return -1;
	}

	public void onTouch(MotionEvent motionEvent){
		Point p = getTouchTile(motionEvent.getX(), motionEvent.getY());
		Log.i("Game", "Touch point "+p.toString());
		switch(motionEvent.getAction()){
			case MotionEvent.ACTION_DOWN:
				if(p.x >= 0 && p.y >= 0 && p.x < size_x && p.y < size_y) {
					int ni = tiles[p.x][p.y].nodeId;
					if(ni != -1)
						motionEventStartNode = ni;
				}
				break;
			case MotionEvent.ACTION_UP:
				if(motionEventStartNode != -1){
					if(p.x >= 0 && p.y >= 0 && p.x < size_x && p.y < size_y) {
						int ni = tiles[p.x][p.y].nodeId;
						if(ni != -1)
							if(gameLogic.nodes[motionEventStartNode].playerId == getCurrentPlayerId()) {
								if (activity.serverLogicThread != null) {
									gameLogic.sendArmy(motionEventStartNode, ni, takeUnitsPct);
								} else if (activity.clientConnection != null) {
									activity.clientConnection.send(new Networking.ArmyCommand(motionEventStartNode, ni, takeUnitsPct));
								}
							}
					}
				}
				motionEventStartNode = -1;
				break;
		}
	}

	public void onUnitSliderChange(SeekBar seekBar){
		takeUnitsPct = ((float)seekBar.getProgress()) / ((float)seekBar.getMax());
	}

	public PointF getArmyPosition(GameLogic.Army army){
		GameLogic.Road road = gameLogic.roads[army.roadId];
		int pathI = (int)(army.position);
		Point tileA;
		Point tileB;
		if(road.path.length == 0){
			tileA = gameLogic.nodes[road.fromNode].position;
			tileB = gameLogic.nodes[road.toNode].position;
		} else if(pathI <= 0) {
			tileA = gameLogic.nodes[road.fromNode].position;
			tileB = road.path[0];
		} else if(pathI >= road.path.length+1){
			tileB = gameLogic.nodes[road.toNode].position;
			return new PointF(tileB.x + 0.5f, tileB.y + 0.5f);
		} else if(pathI >= road.path.length) {
			tileA = road.path[road.path.length - 1];
			tileB = gameLogic.nodes[road.toNode].position;
		} else {
			tileA = road.path[pathI-1];
			tileB = road.path[pathI];
		}
		float posInTile;
		if(army.position >= road.path.length + 1)
			posInTile = 1;
		else
		posInTile = army.position - (float)((int)army.position);
		return new PointF(
				tileA.x + (tileB.x - tileA.x)*posInTile + 0.5f,
				tileA.y + (tileB.y - tileA.y)*posInTile + 0.5f
			);
	}

	public void update(){
		gameLogic.update();
	}

	public void draw(Canvas c){
		c.drawRect(0, 0, c.getWidth(), c.getHeight(), Assets.getBackgroundPaint());
		c.save();
		c.translate(offset.x, offset.y);
		c.scale(scale, scale);
		for(Tile[] tRow : tiles){
			for(Tile tile : tRow){
				if(tile != null)
					tile.draw(c);
			}
		}
		ArmyEntity.updateAll();
		InsertionSort.sort(entities, new Comparator<Entity>() {
			@Override
			public int compare(Entity o, Entity t1) {
			PointF opos = o.getPosition();
			PointF tpos = t1.getPosition();
			if(opos == null || tpos == null)
				return 0;
			if(opos.y > tpos.y){
				return 1;
			} else {
				return -1;
			}
			}
		});
		for(Entity e : entities){
			e.draw(c);
		}
		//debugDraw(c);
		c.restore();
	}

	public void debugDraw(Canvas c){
		Paint paint = new Paint();

		paint.setARGB(255, 255, 255, 0);
		paint.setStrokeWidth(2);
		paint.setStyle(Paint.Style.STROKE);
		paint.setTextAlign(Paint.Align.LEFT);
		paint.setTextSize(20);

		// Nodes
		paint.setTextAlign(Paint.Align.LEFT);
		for(GameLogic.Node n : gameLogic.nodes) {
			c.drawText(Integer.toString(n.playerId), n.position.x * Tile.TILE_SIZE, n.position.y * Tile.TILE_SIZE, paint);
			c.drawCircle(
				n.position.x * Tile.TILE_SIZE + Tile.TILE_SIZE / 2, n.position.y * Tile.TILE_SIZE + Tile.TILE_SIZE / 2,
				Tile.TILE_SIZE / 2,
				paint
			);
			c.drawText(Integer.toString((int)n.unitsCount), n.position.x * Tile.TILE_SIZE + Tile.TILE_SIZE / 2, n.position.y * Tile.TILE_SIZE + Tile.TILE_SIZE / 2, paint);
		}

		// Roads
		if(gameLogic.roads != null && gameLogic.roads.length > 0)
			for(GameLogic.Road r : gameLogic.roads){
				if(r.path != null) {
					Point prevP = gameLogic.nodes[r.fromNode].position;
					for (Point p : r.path) {
						c.drawLine(
								prevP.x * Tile.TILE_SIZE + Tile.TILE_SIZE/2, prevP.y * Tile.TILE_SIZE + Tile.TILE_SIZE/2,
								p.x * Tile.TILE_SIZE + Tile.TILE_SIZE/2, p.y * Tile.TILE_SIZE + Tile.TILE_SIZE/2,
								paint
						);
						prevP = p;
					}
					Point toP = gameLogic.nodes[r.toNode].position;
					c.drawLine(
							prevP.x * Tile.TILE_SIZE + Tile.TILE_SIZE/2, prevP.y * Tile.TILE_SIZE + Tile.TILE_SIZE/2,
							toP.x * Tile.TILE_SIZE + Tile.TILE_SIZE/2, toP.y * Tile.TILE_SIZE + Tile.TILE_SIZE/2,
							paint
					);
				}
			}

		// Armies
		paint.setTextAlign(Paint.Align.RIGHT);
		for(int i = 0; i < gameLogic.armies.size(); i++) {
			GameLogic.Army army = gameLogic.armies.valueAt(i);
			PointF pos = getArmyPosition(army);
			pos.x *= Tile.TILE_SIZE;
			pos.y *= Tile.TILE_SIZE;
			c.drawCircle(pos.x, pos.y, 5, paint);
			c.drawText(Integer.toString((int)army.unitsCount), pos.x, pos.y, paint);
		}
	}

	public SparseArray<PlayerInfo> getPlayers() {
		return players;
	}

	public void onScale(float focusX, float focusY, float scaleFactor) {
		offset.x *= scaleFactor;
		offset.y *= scaleFactor;
		offset.x -= (focusX*scaleFactor-focusX);
		offset.y -= (focusY*scaleFactor-focusY);
		//offset.x = (offset.x/scale - (focusX - offset.x)*(scaleFactor) - (focusX - offset.x))*scaleFactor;
		//offset.y = (offset.y/scale - (focusY - offset.y)*(scaleFactor) - (focusY - offset.y))*scaleFactor;
		//offset.x = -(focusX*scaleFactor + offset.x*scale);
		//offset.y = -(focusY*scaleFactor + offset.y*scale);
		scale *= scaleFactor;
		checkViewBounds();
	}

	public void onScroll(float distanceX, float distanceY) {
		offset.x -= distanceX;
		offset.y -= distanceY;
		checkViewBounds();
	}

	private void checkViewBounds(){
		if(scale < minScale)
			scale = minScale;
		if(offset.x > 0)
			offset.x = 0;
		if(offset.y > 0)
			offset.y = 0;
		if(size_x*Tile.TILE_SIZE*scale + offset.x < activity.gameView.getWidth())
			offset.x = activity.gameView.getWidth()-size_x*Tile.TILE_SIZE*scale;
		if(size_y*Tile.TILE_SIZE*scale + offset.y < activity.gameView.getHeight())
			offset.y = activity.gameView.getHeight()-size_y*Tile.TILE_SIZE*scale;
	}

	public void load(DataInputStream dis){
		try {
			int nodesLength = dis.readInt();	// Number of nodes
			for(int ni = 0; ni < nodesLength; ni++){
				GameLogic.Node node = gameLogic.nodes[ni];
				node.playerId = dis.readInt();
				node.unitsCount = dis.readFloat();
			}

			int armiesLength = dis.readInt();	// Number of armies
			for(int ai = 0; ai < armiesLength; ai++){
				GameLogic.Army army = gameLogic.new Army();
				int armyId = dis.readInt();
				army.playerId = dis.readInt();
				army.position = dis.readFloat();
				army.roadId = dis.readInt();
				army.backDirection = dis.readBoolean();
				army.unitsCount = dis.readFloat();
				int roadsLength = dis.readInt();
				for(int ri = 0; ri < roadsLength; ri++){
					army.nextRoadId.add(dis.readInt());
				}
				gameLogic.armies.append(armyId, army);
			}

			gameLogic.nextArmyId = dis.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void save(String saveName){
		try{
			FileOutputStream outputStream = activity.openFileOutput(saveName, Context.MODE_PRIVATE);
			DataOutputStream dos = new DataOutputStream(outputStream);

			dos.writeInt(activity.serverMap);

			dos.writeInt(gameLogic.nodes.length);	// Number of nodes
			for(int ni = 0; ni < gameLogic.nodes.length; ni++){
				GameLogic.Node node = gameLogic.nodes[ni];
				dos.writeInt(node.playerId);
				dos.writeFloat(node.unitsCount);
			}

			dos.writeInt(gameLogic.armies.size());	// Number of armies
			for(int ai = 0; ai < gameLogic.armies.size(); ai++){
				GameLogic.Army army = gameLogic.armies.valueAt(ai);
				dos.writeInt(gameLogic.armies.keyAt(ai));
				dos.writeInt(army.playerId);
				dos.writeFloat(army.position);
				dos.writeInt(army.roadId);
				dos.writeBoolean(army.backDirection);
				dos.writeFloat(army.unitsCount);
				dos.writeInt(army.nextRoadId.size());
				for(int ri = 0; ri < army.nextRoadId.size(); ri++){
					dos.writeInt(army.nextRoadId.get(ri));
				}
			}

			dos.writeInt(gameLogic.nextArmyId);

			dos.close();
		} catch (FileNotFoundException e) {
			Log.e(getClass().getName(), "FileNotFoundException", e);
		} catch (IOException e) {
			Log.e(getClass().getName(), "IOException", e);
		}
	}
}
