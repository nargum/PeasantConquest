package eu.kotrzena.peasantconquest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;

import java.lang.reflect.Field;

public class Bitmaps {
	private static SparseArray<Bitmap> bitmaps = new SparseArray<Bitmap>();

	public static void init(Context context){
		if(bitmaps.size() > 0)
			return;
		Field[] fields = R.drawable.class.getDeclaredFields();
		final R.drawable drawableResources = new R.drawable();
		for (Field f : fields) {
			try {
				int res_id = f.getInt(drawableResources);
				bitmaps.append(res_id, BitmapFactory.decodeResource(context.getResources(), res_id));
			} catch (IllegalAccessException e) {
				Log.w("WARN", "Bitmaps: Not found " + f.toString());
			} catch (IllegalArgumentException e){
				Log.w("WARN", "Bitmaps: Not primitive field " + f.toString());
			}
		}
	}

	public static Bitmap getBitmap(int res_id){
		return bitmaps.get(res_id);
	}
}
