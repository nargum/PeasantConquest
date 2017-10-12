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
						if(xml.getName() == "tile"){
							tileId = Integer.parseInt(xml.getAttributeValue(null, "id"));
						} else if(tileId != -1 && xml.getName() == "property" && xml.getAttributeValue(null, "name") == "resource"){
							tilesetIds.append(tileId, Integer.parseInt(xml.getAttributeValue(null, "name")));
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

	public static Bitmap getBitmap(int res_id){
		return bitmaps.get(res_id);
	}

	public static Bitmap getBitmap(String res_id){
		return getBitmap(context.getResources().getIdentifier(res_id, "drawable", context.getPackageName()));
	}
}
