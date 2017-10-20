package eu.kotrzena.peasantconquest;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lukas on 20.10.17.
 */

public class InsertionSort {
	public static void sort(List col, Comparator cmp){
		for(int i = 0; i < col.size(); i ++){
			int j = i + 1;
			Object element = col.get(j);
			for(;j > 0 && cmp.compare(element, col.get(j-1)) < 0; j--){
				col.set(j, col.get(j-1));
			}
			col.set(j, element);
		}
	}
}
