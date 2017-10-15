package eu.kotrzena.peasantconquest;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;

public class Game {
	private Tile[][] tiles;
	private GameLogic gameLogic;

	private int size_x;
	private int size_y;

	public Game(){
		size_x = 5;
		size_y = 5;

		tiles = new Tile[size_x][size_y];


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

		prepareLogic();
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
					if (roadsCount != 2 && roadsCount != 0) {
						GameLogic.Node node = gameLogic.new Node();
						node.position = new Point(x, y);
						node.roads = new int[roadsCount];
						for(int i = 0; i < node.roads.length; i++)
							node.roads[i] = -1;
						nodes.add(node);
					}
				}
			}

			gameLogic.nodes = new GameLogic.Node[nodes.size()];
			gameLogic.nodes = nodes.toArray(gameLogic.nodes);
		}

		if(1==1)
			return;

		boolean visited[][] = new boolean[size_x][size_y];



		Queue<Point> queue = new ArrayDeque<>();
		queue.add(gameLogic.nodes[0].position);

		while(!queue.isEmpty()){

		}
	}

	public void update(){

	}

	public void draw(Canvas c){
		for(Tile[] tRow : tiles){
			for(Tile tile : tRow){
				if(tile != null)
					tile.draw(c);
			}
		}
		debugDraw(c);
	}

	public void debugDraw(Canvas c){
		Paint p = new Paint();
		p.setARGB(255, 255, 0, 255);
		p.setStrokeWidth(2);
		p.setStyle(Paint.Style.STROKE);
		for(GameLogic.Node n : gameLogic.nodes)
			c.drawCircle(
				n.position.x*Tile.TILE_SIZE + Tile.TILE_SIZE/2, n.position.y*Tile.TILE_SIZE + Tile.TILE_SIZE/2,
				Tile.TILE_SIZE/2,
				p
			);
	}
}
