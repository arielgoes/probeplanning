package heuristics;

import java.util.ArrayList;
import java.util.Comparator;

public class ComparatorCyclesSize implements Comparator{

	@Override
	public int compare(Object o1, Object o2) {
		
		Cycle l1 = (Cycle) o1;
		Cycle l2 = (Cycle) o2;
		
		if(l1.nodes.size() < l2.nodes.size()) return 1;
		else return -1;
		
	}

}
