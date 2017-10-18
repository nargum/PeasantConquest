package eu.kotrzena.peasantconquest;

import android.graphics.Point;
import android.os.Debug;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameLogic {
	public static final float ARMY_SPEED = 0.02f;

	public class Node {
		public int playerId;
		public int unitsCount = 10;
		public Point position;
		public int roads[];
	}
	public class Road {
		public Point[] path;
		public int fromNode = -1;
		public int toNode = -1;
	}
	public class Army {
		public int playerId;
		public int unitsCount;
		public int roadId;
		public boolean backDirection;
		public float position;
		public LinkedList<Integer> nextRoadId = new LinkedList<Integer>();
	}

	public Node[] nodes;
	public Road[] roads;
	public SparseArray<Army> armies = new SparseArray<Army>();
	public int nextArmyId = 0;

	public int sendArmy(int fromNode, int toNode, int units){
		if(fromNode >= 0 && fromNode < nodes.length && toNode >= 0 && toNode < nodes.length && fromNode != toNode) {
			if(nodes[fromNode].unitsCount < units)
				return -1;
			Army army = null;
			List<List<Integer>> paths = new LinkedList<List<Integer>>();

			Queue<Integer> nodeQueue = new ConcurrentLinkedQueue<Integer>();
			nodeQueue.add(fromNode);
			class VisitedNode {
				public int fromNode;
				public int road;
				public int length;
				public VisitedNode(int f, int r, int l){
					fromNode = f;
					road = r;
					length = l;
				}
			}
			SparseArray<VisitedNode> visitedNodes = new SparseArray<>();
			visitedNodes.append(
				fromNode,
				new VisitedNode(-1, -1, 0)
			);

			while(!nodeQueue.isEmpty()){
				int nodeId = nodeQueue.poll();
				for(int ri = 0; ri < nodes[nodeId].roads.length; ri++){
					int rIndex = nodes[nodeId].roads[ri];
					int nextNode;
					if(roads[rIndex].fromNode == nodeId)
						nextNode = roads[rIndex].toNode;
					else if(roads[rIndex].toNode == nodeId)
						nextNode = roads[rIndex].fromNode;
					else
						continue;
					VisitedNode visitedNode = visitedNodes.get(nextNode);
					int length = visitedNodes.get(nodeId).length + roads[rIndex].path.length + 1;
					if(visitedNode == null){
						visitedNode = new VisitedNode(nodeId, rIndex, length);
						visitedNodes.append(nextNode, visitedNode);
						nodeQueue.add(nextNode);
					} else {
						if(visitedNode.length > length){
							visitedNodes.setValueAt(nextNode, new VisitedNode(nodeId, rIndex, length));
						}
					}
				}
			}

			VisitedNode toNodeData = visitedNodes.get(toNode);
			if(toNodeData == null)
				return -1;

			army = new Army();
			while(toNodeData.fromNode != fromNode){
				army.nextRoadId.push(toNodeData.road);
				toNodeData = visitedNodes.get(toNodeData.fromNode);
			}
			army.roadId = toNodeData.road;

			Road road = roads[army.roadId];
			if(road.toNode == fromNode) {
				army.backDirection = true;
				army.position = road.path.length + 1;
			} else if(road.fromNode == fromNode) {
				army.backDirection = false;
				army.position = 0;
			}

			army.playerId = nodes[fromNode].playerId;
			army.unitsCount = units;
			armies.append(nextArmyId++, army);

			nodes[fromNode].unitsCount -= units;

			return nextArmyId - 1;

			//TODO: Sestavit cestu ten for za tim je už zbytečný
			/*
			for (int ri = 0; ri < roads.length; ri++) {
				Road road = roads[ri];
				if(road.fromNode == toNode && road.toNode == fromNode) {
					army = new Army();
					army.roadId = ri;
					army.backDirection = true;
					army.position = road.path.length + 1;
					break;
				} else if(road.fromNode == fromNode && road.toNode == toNode) {
					army = new Army();
					army.roadId = ri;
					army.backDirection = false;
					army.position = 0;
					break;
				} else {

					//TODO: Najít cestu když se jde přes další uzel
				}
			}
			if(army != null){
				army.playerId = nodes[fromNode].playerId;
				army.unitsCount = units;
				armies.append(nextArmyId++, army);

				nodes[fromNode].unitsCount -= units;

				return nextArmyId - 1;
			}*/
		}
		return -1;
	}

	public void update(){
		for(int i = 0; i < armies.size(); i++) {
			GameLogic.Army army = armies.valueAt(i);
			for(int j = 0; j < armies.size(); j++) {
				if(i == j)
					continue;
				GameLogic.Army army2 = armies.valueAt(j);
				if(army.roadId == army2.roadId && Math.abs(army.position - army2.position) < ARMY_SPEED) {
					if(army.playerId == army2.playerId && army.backDirection == army2.backDirection){
						//TODO: Smazat jednu armádu a jednotky nacpat do druhé
					} else if(army.playerId != army2.playerId){
						//TODO: Boj, počet jednotek změnit na float?
					}
				}
			}
			if(army.backDirection) {
				if (army.position > 0)
					army.position -= ARMY_SPEED;
				else {
					if(!army.nextRoadId.isEmpty()){
						int nextRoad = army.nextRoadId.poll();
						if(roads[nextRoad].fromNode == roads[army.roadId].fromNode) {
							army.backDirection = false;
							army.position = 0;
						} else {
							army.backDirection = true;
							army.position = roads[nextRoad].path.length + 1;
						}
						army.roadId = nextRoad;
					} else {
						nodes[roads[army.roadId].fromNode].unitsCount += army.unitsCount;
						armies.remove(armies.keyAt(i));
					}
				}
			} else {
				if (army.position < roads[army.roadId].path.length+1)
					army.position += ARMY_SPEED;
				else {
					if(!army.nextRoadId.isEmpty()){
						int nextRoad = army.nextRoadId.poll();
						if(roads[nextRoad].fromNode == roads[army.roadId].toNode) {
							army.backDirection = false;
							army.position = 0;
						} else {
							army.backDirection = true;
							army.position = roads[nextRoad].path.length + 1;
						}
						army.roadId = nextRoad;
					} else {
						nodes[roads[army.roadId].toNode].unitsCount += army.unitsCount;
						armies.remove(armies.keyAt(i));
					}
				}
			}
		}
	}
}
