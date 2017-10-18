package eu.kotrzena.peasantconquest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;

public class Assets {
	private static Context context;
	private static SparseArray<Bitmap> bitmaps = new SparseArray<Bitmap>();

	// Map tileset ids to resource ids
	private static SparseArray<Integer> tilesetIds = new SparseArray<Integer>();

	private static SparseArray<Byte> tilesRoads = new SparseArray<Byte>();

	public static void init(Context context){
		if(bitmaps.size() > 0)
			return;

		Assets.context = context;
		Field[] fields = R.drawable.class.getDeclaredFields();
		final R.drawable drawableResources = new R.drawable();
		for (Field f : fields) {
			try {
				int res_id = f.getInt(drawableResources);
				bitmaps.append(res_id, BitmapFactory.decodeResource(context.getResources(), res_id));
			} catch (IllegalAccessException e) {
				Log.w("WARN", "Assets: Not found " + f.toString());
			} catch (IllegalArgumentException e){
				Log.w("WARN", "Assets: Not primitive field " + f.toString());
			}
		}

		XmlPullParser xml = context.getResources().getXml(R.xml.tiles);

		try {
			int eventType = xml.getEventType();

			int tileId = -1;
			while(eventType != XmlPullParser.END_DOCUMENT){
				switch(eventType){
					case XmlPullParser.START_TAG:
						if(xml.getName().equals("tile")){
							tileId = Integer.parseInt(xml.getAttributeValue(null, "id"));
							while(eventType != XmlPullParser.END_DOCUMENT){
								eventType = xml.next();

								if(eventType == XmlPullParser.START_TAG && xml.getName().equals("property")) {
									if (xml.getAttributeValue(null, "name").equals("resource")) {
										tilesetIds.append(
												tileId,
												context.getResources().getIdentifier(
														xml.getAttributeValue(null, "value"),
														"drawable",
														context.getPackageName()
												)
										);
									} else if (xml.getAttributeValue(null, "name").equals("roads")) {
										tilesRoads.append(tileId, Byte.parseByte(xml.getAttributeValue(null, "value")));
									}
								} else if(eventType == XmlPullParser.END_TAG && xml.getName().equals("tile")){
									break;
								}
							}
						}
						break;
					case XmlPullParser.END_TAG:
						tileId = -1;
						break;
				}
				eventType = xml.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Game loadMap(int res_id){
		XmlPullParser xml = context.getResources().getXml(res_id);

		try {
			int eventType = xml.getEventType();

			Tile[][] tiles = null;
			while(eventType != XmlPullParser.END_DOCUMENT){
				switch(eventType){
					case XmlPullParser.START_TAG:
						if(xml.getName().equals("map")){
							int size_x = Integer.parseInt(xml.getAttributeValue(null, "width"));
							int size_y = Integer.parseInt(xml.getAttributeValue(null, "height"));
							tiles = new Tile[size_x][size_y];
							int tilesetOffset = 0;
							int layerIndex = -1;
							while(eventType != XmlPullParser.END_DOCUMENT){
								eventType = xml.next();
								if(eventType == XmlPullParser.END_TAG && xml.getName().equals("map")){
									break;
								} else if(eventType == XmlPullParser.START_TAG && xml.getName().equals("tileset")){
									tilesetOffset = Integer.parseInt(xml.getAttributeValue(null, "firstgid"));
								} else if(eventType == XmlPullParser.START_TAG && xml.getName().equals("layer")){
									layerIndex++;
									switch(layerIndex){
										case 0: {
											int tileIndex = -1;
											while(eventType != XmlPullParser.END_DOCUMENT){
												eventType = xml.next();
												if(eventType == XmlPullParser.END_TAG && xml.getName().equals("layer")){
													break;
												}
												if(eventType == XmlPullParser.START_TAG && xml.getName().equals("tile")){
													tileIndex++;

													int x = tileIndex%size_x;
													int y = tileIndex/size_x;
													String val = xml.getAttributeValue(null, "gid");
													if(val == null)
														continue;
													int gid = Integer.parseInt(val);
													Integer intVal = tilesetIds.get(gid - tilesetOffset);
													if(intVal == null)
														continue;
													int bitmapId = intVal.intValue();
													tiles[x][y] = new Tile(x, y, getBitmap(bitmapId));

													Byte byteVal = tilesRoads.get(gid - tilesetOffset);
													if(byteVal != null)
														tiles[x][y].setRoads(tilesRoads.get(bitmapId, byteVal));
												}
											}
											break;
										}
										case 1: {
											int tileIndex = -1;
											while(eventType != XmlPullParser.END_DOCUMENT){
												eventType = xml.next();
												if(eventType == XmlPullParser.END_TAG && xml.getName().equals("layer")){
													break;
												}
												if(eventType == XmlPullParser.START_TAG && xml.getName().equals("tile")){
													tileIndex++;

													int x = tileIndex%size_x;
													int y = tileIndex/size_x;
													String val = xml.getAttributeValue(null, "gid");
													if(val == null)
														continue;
													int gid = Integer.parseInt(val);
													Integer intVal = tilesetIds.get(gid - tilesetOffset);
													if(intVal == null)
														continue;
													int bitmapId = intVal.intValue();

													switch(bitmapId){
														case R.drawable.castle:
															tiles[x][y].castle = true;
															tiles[x][y].ownerOnStart = 0;
															break;
													}
												}
											}
											break;
										}
									}
								} else if(eventType == XmlPullParser.START_TAG && xml.getName().equals("objectgroup")){
									while(eventType != XmlPullParser.END_DOCUMENT){
										eventType = xml.next();
										if(eventType == XmlPullParser.END_TAG && xml.getName().equals("objectgroup")){
											break;
										}
										if(eventType == XmlPullParser.START_TAG && xml.getName().equals("object")){
											float x = Float.parseFloat(xml.getAttributeValue(null, "x"));
											float y = Float.parseFloat(xml.getAttributeValue(null, "y"));
											if(xml.getAttributeValue(null, "gid") == null){
												eventType = xml.next();
												if(eventType == XmlPullParser.END_TAG && xml.getName().equals("objectgroup"))
													break;
												if(xml.getName().equals("text")){
													eventType = xml.next();
													if(eventType == XmlPullParser.END_TAG && xml.getName().equals("objectgroup"))
														break;
													if(eventType == XmlPullParser.TEXT) {
														x /= Tile.TILE_SIZE;
														y /= Tile.TILE_SIZE;
														tiles[(int) x][(int) y].ownerOnStart = Integer.parseInt(xml.getText());
													}
												}
											} else {
												//TODO: Načíst okrasné objekty
											}
										}
									}
								}
							}
						}
						break;
					case XmlPullParser.END_TAG:
						if(xml.getName() == "map"){

						}
						break;
				}
				eventType = xml.next();
			}
			if(tiles != null)
				return new Game(tiles);
			else
				return null;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap getBitmap(int res_id){
		return bitmaps.get(res_id);
	}

	public static Bitmap getBitmap(String res_id){
		return getBitmap(context.getResources().getIdentifier(res_id, "drawable", context.getPackageName()));
	}
}
