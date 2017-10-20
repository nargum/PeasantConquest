package eu.kotrzena.peasantconquest;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class Game {
	private Tile[][] tiles;
	private LinkedList<Entity> entities = new LinkedList<Entity>();
	private GameLogic gameLogic;

	private int size_x;
	private int size_y;

	private int playAreaTop;
	private int playAreaBottom;
	private int playAreaLeft;
	private int playAreaRight;

	private float scale = 1.2f;
	private PointF offset = new PointF(-20, 50);

	private int motionEventStartNode = -1;
	private float takeUnitsPct = 0;

	private HashMap<Point, Integer> tileOwner = null;

	public Game(){
		size_x = 5;
		size_y = 5;

		tiles = new Tile[size_x][size_y];

		playAreaLeft = playAreaTop = 0;
		playAreaRight = size_x - 1;
		playAreaBottom = size_y -1;

		for(int x = 0; x < size_x; x++){
			for(int y = 0; y < size_y; y++){
				tiles[x][y] = new Tile(x, y, Assets.getBitmap(R.drawable.tile_grass1));
			}
		}

		prepareLogic();
	}

	public Game(Tile[][] tiles){
		size_x = tiles.length;
		size_y = tiles[0].length;

		this.tiles = tiles;

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

		prepareLogic();
	}

	public void fitDisplay(View view){
		offset.x = -playAreaLeft * Tile.TILE_SIZE;
		offset.y = -playAreaTop * Tile.TILE_SIZE;

		float scalex = ((float)view.getWidth()) / ((float)(playAreaRight-playAreaLeft+1) * Tile.TILE_SIZE);
		float scaley = ((float)view.getHeight()) / ((float)(playAreaBottom-playAreaTop+1) * Tile.TILE_SIZE);

		scale = (scalex > scaley)? scaley : scalex;
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
					if ((roadsCount != 2 && roadsCount != 0) || tile.castle) {
						GameLogic.Node node = gameLogic.new Node();
						node.position = new Point(x, y);
						node.roads = new int[roadsCount];
						for(int i = 0; i < node.roads.length; i++)
							node.roads[i] = -1;
						node.playerId = tiles[x][y].ownerOnStart;
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

		Stack<Point> stack = new Stack<Point>();
		for(GameLogic.Node node : gameLogic.nodes)
			stack.push(node.position);

		ArrayList<GameLogic.Road> roads = new ArrayList<GameLogic.Road>();

		for(int i = 0; i < gameLogic.nodes.length; i++){
		//while(!stack.isEmpty()){
			//Point nodePoint = stack.pop();
			GameLogic.Node node = gameLogic.nodes[i];
			Point nodePoint = node.position;
			Tile tile = tiles[nodePoint.x][nodePoint.y];

			byte[] directions = new byte[]{Tile.ROAD_N, Tile.ROAD_S, Tile.ROAD_W, Tile.ROAD_E};
			for(byte dir : directions){
			//if((tile.getRoads() & Tile.ROAD_N) != 0){
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

	public Point getTouchTile(float x, float y){
		x /= scale;
		y /= scale;
		x -= offset.x;
		y -= offset.y;
		return new Point((int)(x/Tile.TILE_SIZE), (int)(y/Tile.TILE_SIZE));
	}

	public void onTouch(MotionEvent motionEvent){
		Point p = getTouchTile(motionEvent.getX(), motionEvent.getY());
		switch(motionEvent.getAction()){
			case MotionEvent.ACTION_DOWN:
				if(p.x < size_x && p.y < size_y) {
					int ni = tiles[p.x][p.y].nodeId;
					if(ni != -1)
						motionEventStartNode = ni;
				}
				break;
			case MotionEvent.ACTION_UP:
				if(motionEventStartNode != -1){
					if(p.x < size_x && p.y < size_y) {
						int ni = tiles[p.x][p.y].nodeId;
						if(ni != -1) {
							gameLogic.sendArmy(motionEventStartNode, ni, takeUnitsPct);
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
		c.scale(scale, scale);
		c.translate(offset.x, offset.y);
		for(Tile[] tRow : tiles){
			for(Tile tile : tRow){
				if(tile != null)
					tile.draw(c);
			}
		}
		InsertionSort.sort(entities, new Comparator<Entity>() {
			@Override
			public int compare(Entity o, Entity t1) {
				if(o.position.y > t1.position.y){
					return 1;
				} else {
					return -1;
				}
			}
		});
		for(Entity e : entities){
			e.draw(c);
		}
		debugDraw(c);
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
}
