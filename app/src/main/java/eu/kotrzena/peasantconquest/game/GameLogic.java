package eu.kotrzena.peasantconquest.game;

import android.graphics.Point;
import android.util.SparseArray;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameLogic {
	public static final float ARMY_SPEED = 0.02f;
	public static final float ARMY_DAMAGE = 0.03f;

	public class Node {
		public int playerId;
		public float unitsCount = 0;
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
		public float unitsCount;
		public int roadId;
		public boolean backDirection;
		public float position;
		public LinkedList<Integer> nextRoadId = new LinkedList<Integer>();
	}

	public Node[] nodes;
	public Road[] roads;
	public final SparseArray<Army> armies = new SparseArray<Army>();
	public int nextArmyId = 0;

	public int sendArmy(int fromNode, int toNode, float unitsPct){
		if(fromNode >= 0 && fromNode < nodes.length && toNode >= 0 && toNode < nodes.length && fromNode != toNode) {
			int units = (int)(unitsPct*nodes[fromNode].unitsCount);
			if(units <= 0)
				return -1;
			if(nodes[fromNode].unitsCount < units || nodes[toNode].playerId < 0)
				return -1;
			Army army = null;

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
				if(nodes[nodeId].playerId == nodes[fromNode].playerId || nodes[nodeId].playerId == -1)
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

			for(int ai = 0; ai < armies.size(); ai++){
				Army army2 = armies.valueAt(ai);
				if(army.roadId == army2.roadId && army.playerId != army2.playerId){
					if(Math.abs(army.position - army2.position) < ARMY_SPEED){
						return -1;
					}
				}
			}

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
		}
		return -1;
	}

	public void update(){
		for(int i = 0; i < nodes.length; i++){
			if(nodes[i].playerId > 0){
				nodes[i].unitsCount += 0.005;
			}
		}

		// Update armies
		synchronized (armies) {
			for (int i = 0; i < armies.size(); i++) {
				GameLogic.Army army = armies.valueAt(i);
				boolean move = true;
				for (int j = 0; j < armies.size(); j++) {
					if (i == j)
						continue;
					GameLogic.Army army2 = armies.valueAt(j);
					if (army.roadId == army2.roadId && Math.abs(army.position - army2.position) < 2 * ARMY_SPEED) {
						if (army.playerId == army2.playerId && army.backDirection == army2.backDirection) {
							// Armies join
							army.unitsCount += army2.unitsCount;
							armies.delete(armies.keyAt(j));
						} else if (army.playerId != army2.playerId) {
							// Armies fight
							army2.unitsCount -= ARMY_DAMAGE;
							move = false;
							if (army.unitsCount < 1)
								armies.delete(armies.keyAt(i));
							if (army2.unitsCount < 1)
								armies.delete(armies.keyAt(j));
						}
					}
				}
				if (army.backDirection && army.position > 0) {
					if (move)
						army.position -= ARMY_SPEED;
				} else if (!army.backDirection && army.position < roads[army.roadId].path.length + 1) {
					if (move)
						army.position += ARMY_SPEED;
				} else {
					// At the end of the road
					int targetNodeId = (army.backDirection ? roads[army.roadId].fromNode : roads[army.roadId].toNode);
					if (!army.nextRoadId.isEmpty()) {
						// Next road
						int nextRoad = army.nextRoadId.poll();
						if (roads[nextRoad].fromNode == targetNodeId) {
							army.backDirection = false;
							army.position = 0;
						} else {
							army.backDirection = true;
							army.position = roads[nextRoad].path.length + 1;
						}
						army.roadId = nextRoad;
					} else {
						// At the target
						Node targetNode = nodes[targetNodeId];
						if (army.playerId == targetNode.playerId) {
							// Join with city
							targetNode.unitsCount += army.unitsCount;
							armies.remove(armies.keyAt(i));
						} else {
							// Attack city
							army.unitsCount -= ARMY_DAMAGE;
							targetNode.unitsCount -= ARMY_DAMAGE;
							if (army.unitsCount < 1)
								armies.remove(armies.keyAt(i));
							else if (targetNode.unitsCount < 1) {
								targetNode.playerId = army.playerId;
								targetNode.unitsCount += army.unitsCount;
								armies.remove(armies.keyAt(i));
							}
						}
					}
				}
			}
		}
	}
}
