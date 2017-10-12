package eu.kotrzena.peasantconquest;

import android.graphics.Canvas;

public class Game {
	private Tile[][] tiles;

	public Game(){
		int size_x = 10;
		int size_y = 10;

		tiles = new Tile[size_x][size_y];

		for(int x = 0; x < size_x; x++){
			for(int y = 0; y < size_y; y++){
				tiles[x][y] = new Tile(x, y, Bitmaps.getBitmap(R.drawable.tile_grass1));
			}
		}
	}

	public void update(){

	}

	public void draw(Canvas c){
		for(Tile[] tRow : tiles){
			for(Tile tile : tRow){
				tile.draw(c);
			}
		}
	}
}
