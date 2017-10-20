package eu.kotrzena.peasantconquest.game;

import java.util.Comparator;
import java.util.List;

public class InsertionSort {
	public static void sort(List col, Comparator cmp){
		for(int i = 0; i < col.size()-1; i ++){
			int j = i + 1;
			Object element = col.get(j);
			for(;j > 0 && cmp.compare(element, col.get(j-1)) < 0; j--){
				col.set(j, col.get(j-1));
			}
			col.set(j, element);
		}
	}
}
