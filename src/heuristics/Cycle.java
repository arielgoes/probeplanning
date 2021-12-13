package heuristics;
import java.util.ArrayList;

public class Cycle implements Cloneable{
	public ArrayList<Tuple> itemPerCycle;
	public ArrayList<Integer> nodes; //nodes in the cycle;
	public ArrayList<Pair<Integer,Integer>> links; //links in the cycle... sometimes this representations is easier
	public int capacity;
	public int capacity_used;
	public int transportationCost;
	public ArrayList<Integer> pathOverhead;   //for comparison between OptPathPlan and others

	public Cycle() {
		this.itemPerCycle = new ArrayList<Tuple>();
		this.nodes = new ArrayList<Integer>();
		this.links = new ArrayList<Pair<Integer,Integer>>();
	}
	
	public void printCycle() {
		System.out.println("CIRCUIT:");
		System.out.println("path (nodes): " + this.nodes);
		System.out.println("path (links): " + this.links);
		System.out.println("DEVICE-ITEM:");
		System.out.println("device-item: " + this.itemPerCycle);
		
	}

	public void printCycleWithCapacity() {
		System.out.println("---------------------------------");
		System.out.println("CIRCUIT:");
		System.out.println("path (node): " + this.nodes);
		System.out.println("path (links): " + this.links);
		System.out.println("DEVICE-ITEMS:");
		System.out.println("device-items: " + this.itemPerCycle);
		System.out.println("Capacity used: " + this.capacity_used + ". Total Capacity: " + this.capacity);
	}
	
	@Override
	public Cycle clone() throws CloneNotSupportedException {
	   try{
	       Cycle clonedMyClass = (Cycle)super.clone();
	       // if you have custom object, then you need create a new one in here
	       return clonedMyClass ;
	   } catch (CloneNotSupportedException e) {
	       e.printStackTrace();
	       return new Cycle();
	   }

  }

}