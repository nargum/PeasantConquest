package eu.kotrzena.peasantconquest;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class GameLogic {
	public class Node {
		public int playerId;
		public int unitsCount;
		public Point position;
		public int[] roads;
	}
	public class Road {
		public Point[] path;
		public int fromNode;
		public int toNode;
	}
	public class Army {
		public int playerId;
		public int unitsCount;
		public int roadId;
		public boolean direction;
		public int[] nextRoadId;
	}

	public Node[] nodes;
	public Road[] roads;
	public List<Army> armies;
}
