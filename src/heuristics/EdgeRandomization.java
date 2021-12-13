package heuristics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;


import main.NetworkInfrastructure;

public class EdgeRandomization {
	public NetworkInfrastructure infra;
	public long seed;
	public int capacityProbe; //probe total capacity
	public int maxProbes; //max cap of generated probes
	public int[] nodeDemand; //number of non-collected items per device
	public ArrayList<ArrayList<Pair<Integer,Integer>>> cyclesFinal; //(node, node)
	public ArrayList<ArrayList<Pair<Integer,Integer>>> deviceItemFinal; //(dev, item)
	public ArrayList<Cycle> cycles;
	public int [][]costShortestPath;
	public int totalItems; //tells total amount of items at network's device
	
	
	public EdgeRandomization(NetworkInfrastructure infra, int capacityProbe, long seed, int maxProbes) {
		this.seed = seed;
		this.infra = infra;
		this.capacityProbe = capacityProbe;
		this.nodeDemand = new int[this.infra.size];
		this.cyclesFinal = new ArrayList<ArrayList<Pair<Integer,Integer>>>();
		this.deviceItemFinal = new ArrayList<ArrayList<Pair<Integer,Integer>>>();
		this.maxProbes = maxProbes; 
		this.cycles = new ArrayList<Cycle>();
		this.costShortestPath = new int[this.infra.size][this.infra.size];
		this.totalItems = 0;
		
		
		//start node demand by telling how many items each router has
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				if(this.infra.items[i][j] == 1) {
					this.nodeDemand[i] += 1;
					this.totalItems += 1;  
				}
			}
		}
		
		
		//start shortest path all for all
				int count = 0;
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						ArrayList<Integer> shortPath = new ArrayList<Integer>();
						shortPath = this.infra.getShortestPath(i, j);
						//System.out.println("nodeA " + i + " nodeB: " + j + " shortPath: " + shortPath);
						if(shortPath.size() > 0) {
							
							if(i == j) {
								this.costShortestPath[i][j] = Integer.MAX_VALUE;
							}else if(i > j) {
								continue;
							}else{
								this.costShortestPath[i][j] = shortPath.size() - 1;
								this.costShortestPath[j][i] = shortPath.size() - 1;
							}
						}else {
							count++;
							this.costShortestPath[i][j] = Integer.MAX_VALUE;
						}
					}
				}
				
				//print cost distance
				/*System.out.println("COST-DISTANCE");
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						System.out.printf("[%d][%d]=%d ", i, j, this.costShortestPath[i][j]);
					}
					System.out.println("");
				}*/
		
		
	}
	
	
	
	public Hashtable<Integer, Cycle> runER() {
		//long startTime = System.nanoTime();
	
		//start remaining edges
		ArrayList<Pair<Integer,Integer>> remainingEdges = new ArrayList<Pair<Integer,Integer>>();
		startRemainingEdges(remainingEdges);
		
		/*System.out.println("REMAINING EDGES BEFORE ITERATING...");
		for(int i = 0; i < remainingEdges.size(); i++) {
			System.out.println("remainingEdges: " + remainingEdges.get(i));
		}*/
		
		boolean collected[][] = new boolean[this.infra.size][this.infra.telemetryItemsRouter];
		
		int w = this.capacityProbe; //vehicle's capacity
		int old_w = w;
		
		//System.out.println("initial w: " + w);
		Random rnd = new Random(this.seed);
		int fixedEdgeCost = 1;
		
		
		Pair<Integer, Integer> edge = Pair.create(-2, -2); //allocate memory to 'edge'
		ArrayList<Pair<Integer,Integer>> resultAux = new ArrayList<Pair<Integer,Integer>>();
		ArrayList<Pair<Integer,Integer>> deviceItemAux = new ArrayList<Pair<Integer,Integer>>();
		
		//start procedure
		int collectedItems = 0;
		
		do {
			int nodeA = -1;
			int nodeB = -1;
			int lastNode = -1;
			collectedItems = 0;
			
			//System.out.println("\nNEW CYCLE\n");
			boolean flagHasEdge = false;
			
			if(!remainingEdges.isEmpty()) {
				edge = Pair.create(nodeA, nodeB);
				while(!findEdge(edge, remainingEdges)) {
					nodeA = remainingEdges.get(rnd.nextInt(remainingEdges.size())).first;
					nodeB = remainingEdges.get(rnd.nextInt(remainingEdges.size())).second;
					edge = Pair.create(nodeA, nodeB);
				}
				//System.out.println("(if) nodeA: " + nodeA + " nodeB: " + nodeB);
				flagHasEdge = true;
			}else {
				nodeA = this.chooseCycleStart();		
				//System.out.println("(else) nodeA: " + nodeA + " nodeB: " + nodeB);
				flagHasEdge = false;
			}
			
			boolean[][] visited = new boolean[this.infra.size][this.infra.size];
			int depot = nodeA;
			//System.out.println("depot: " + depot);
			
			do {
				if(flagHasEdge) {
					//System.out.println("(if)----------------nodeA " + nodeA + "----------------");
					//System.out.println("(if) edge: " + edge);
					flagHasEdge = false;
				}else {
					edge = createEdge(nodeA, lastNode, w, depot, visited, remainingEdges);
					//System.out.println("depot: " + depot);
					//System.out.println("(else)----------------nodeA " + nodeA + "----------------");
					//System.out.println("(else) edge: " + edge);	
				}
				
				if(edge.first >= 0 && edge.second >= 0) {
					
					w -= fixedEdgeCost; //upforward cost
					
					//DEPOT CASE
					if(edge.first == depot) {
						w -= fixedEdgeCost;
					}
										
					visited[edge.first][edge.second] = true;
					visited[edge.second][edge.first] = true;
						
					//iterate over telemetry items and collect it whenever possible (first node)
					for(int j = 0; j < infra.telemetryItemsRouter; j++) {
						if(infra.items[edge.first][j] == 1 && !collected[edge.first][j]) {
							if(edge.first == depot && (w - infra.sizeTelemetryItems[j] >= 0)) {
								
								w -= infra.sizeTelemetryItems[j];
								this.nodeDemand[edge.first] -= 1; //it was able to collect an item. Decrease an item from node demand
								Pair di = Pair.create(edge.first, j);
								deviceItemAux.add(di);
								collected[edge.first][j] = true;
								//System.out.println("w (edge.first): " + w);
								
							}else if(edge.first != depot && 
									(w - this.costShortestPath[edge.first][depot] - infra.sizeTelemetryItems[j] >= 0) && 
									!collected[edge.first][j]) {
								
								w -= infra.sizeTelemetryItems[j];
								this.nodeDemand[edge.first] -= 1; //it was able to collect an item. Decrease an item from node demand
								Pair di = Pair.create(edge.first, j);
								deviceItemAux.add(di);
								collected[edge.first][j] = true;
								//System.out.println("w (edge.first): " + w);
							}						 
						}
					}
						
					//iterate over telemetry items and collect it whenever possible (second node)
					for(int j = 0; j < infra.telemetryItemsRouter; j++) {
						if(infra.items[edge.second][j] == 1 && 
						  (w - this.costShortestPath[edge.second][depot] - infra.sizeTelemetryItems[j] >= 0) &&
						  !collected[edge.second][j]) {
							
							w -= infra.sizeTelemetryItems[j];
							this.nodeDemand[edge.second] -= 1; //it was able to collect an item. Decrease an item from node demand
							Pair di = Pair.create(edge.second, j);
							deviceItemAux.add(di);
							collected[edge.second][j] = true;
							//System.out.println("w (edge.second): " + w);
						}
						
					}
						
					//System.out.println("NODE-DEMAND (inner-loop)");
					/*for(int k = 0; k < this.nodeDemand.length; k++) {
						System.out.printf("%d ", this.nodeDemand[k]);
					}*/
					
					if(this.nodeDemand[edge.first] <= 0) {
						remainingEdges.remove(edge);
					}
					
					if(this.nodeDemand[edge.second] <= 0) {
						Pair<Integer,Integer> edgeRev = Pair.create(edge.second, edge.first);
						remainingEdges.remove(edgeRev);
					}
					
					//System.out.println("\nw (inner-loop): " + w);
					resultAux.add(edge);
					lastNode = edge.first;
					nodeA = edge.second;
				}else {
					//System.out.println("SKIPPING UNFEASIBLE EDGE AND RETURNING TO DEPOT");
					break;	
				}
 			}while(w > this.capacityProbe/2 && !remainingEdges.isEmpty());
			//while(w > this.costShortestPath[edge.second][depot] + 1 && !remainingEdges.isEmpty());

			 
			//when w <= this.W/2... do:
			//System.out.println("LESS THAN (OR EQUAL) CASE...");			
			ArrayList<Integer> shortPath = new ArrayList<Integer>();
			
			//System.out.println("edge.first: " + edge.first + ", edge.second: " + edge.second);
			//System.out.println("remainingEdges: " + remainingEdges);
			
			//if both nodes are feasible
			if(edge.first >= 0 && edge.second >= 0) {
				shortPath = infra.getShortestPath(edge.second, depot);
			}else { //if only the first is feasible
				shortPath = infra.getShortestPath(edge.first, depot);
			}
			
			//create pairs from shortest path
			for(int k = 0; k < shortPath.size() - 1; k++) {
				int node1 = shortPath.get(k);
				int node2 = shortPath.get(k+1);
				Pair<Integer,Integer> p = Pair.create(node1, node2);
				resultAux.add(p); //add to current circuit
				
				//if the edge contains the 'depot' it's already 
				if(node1 != depot && node2 != depot) {
					w -= fixedEdgeCost;	
				}
								
				//... yet not satisfied
				if(findEdge(edge, remainingEdges)) {

					//iterate over telemetry items and collect it whenever possible (first node)
					for(int j = 0; j < infra.telemetryItemsRouter; j++) {
						if(infra.items[node1][j] == 1 && !collected[node1][j]) {
							if(w - infra.sizeTelemetryItems[j] >= 0) {
								w -= infra.sizeTelemetryItems[j];
								this.nodeDemand[node1] -= 1; //it was able to collect an item. Decrease an item from node demand
								Pair di = Pair.create(node1, j);
								deviceItemAux.add(di);
								collected[node1][j] = true;	
							}						 
						}
					}

					//iterate over telemetry items and collect it whenever possible (second node)
					for(int j = 0; j < infra.telemetryItemsRouter; j++) {
						if(infra.items[node2][j] == 1 && !collected[node2][j]) {
							if(w - infra.sizeTelemetryItems[j] >= 0) {
								w -= infra.sizeTelemetryItems[j];
								this.nodeDemand[node2] -= 1; //it was able to collect an item. Decrease an item from node demand
								Pair di = Pair.create(node2, j);
								deviceItemAux.add(di);
								collected[node2][j] = true;

							}
						}
					}
					

					//remove satisfied edge iff both nodes are satisfied
					if(this.nodeDemand[node1] <= 0) {
						remainingEdges.remove(edge);
					}
					
					if(this.nodeDemand[node2] <= 0) {
						Pair<Integer,Integer> edgeRev = Pair.create(node2, node1);
						remainingEdges.remove(edgeRev);
					}
					
				}
				
			}
			
			//System.out.println("NODE-DEMAND (outter-loop)");
			/*for(int k = 0; k < this.nodeDemand.length; k++) {
				System.out.printf("%d ", this.nodeDemand[k]);
			}*/
			
			shortPath = new ArrayList<Integer>();
			
			//System.out.println("current circuit generated: " + resultAux);
			if(!resultAux.isEmpty()) {
				this.cyclesFinal.add(resultAux);
				this.deviceItemFinal.add(deviceItemAux);
			}
			
			//System.out.println("current path: " + resultAux);
			
			resultAux = new ArrayList<Pair<Integer,Integer>>();
			deviceItemAux = new ArrayList<Pair<Integer,Integer>>();

			//System.out.println("remainingEdges (outter-loop): " + remainingEdges);
			//System.out.println("w post (outter-loop): " + w);
			w = old_w;
			
			for(int i = 0; i < this.infra.size; i++) {
				for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
					if(collected[i][j]) {
						//System.out.printf("(%d,%d) collected ", i, j);
						collectedItems++;
					}
				}
				//System.out.println("");
			}
			//System.out.println("items collected: " + collectedItems + ", totalItems: " + this.totalItems);
			//System.out.println("IIIIIIIII: " + remainingEdges);
			
		}while(collectedItems < this.totalItems || !remainingEdges.isEmpty());
		//while(test < 1);
		 
		Hashtable <Integer, Cycle> cyclesER = new Hashtable <Integer, Cycle>();
		cyclesER = this.convertToCycle(); //convert arraylist to Cycle class object
		
		return cyclesER;
	}
	
	
	public int chooseCycleStart() {
		int bestNode = 0;
		int bestValue = Integer.MIN_VALUE;
		for(int i = 0; i < this.nodeDemand.length; i++) {
			if(nodeDemand[i] > bestValue) {
				bestValue = nodeDemand[i];
				bestNode = i;
			}
		}
		return bestNode;
	}
	
	//array format
	public void printCircuitsArray() {
		System.out.println("\nCYCLES (ArrayList):");
		for(int i = 0; i < this.cyclesFinal.size(); i++) {
			System.out.printf("path " + i + ": " + this.cyclesFinal.get(i) + "\n");
		}
	}
	
	//array format
	public void printDeviceItemArray() {
		System.out.println("\nDEVICE-ITEM (ArrayList)");
		for(int i = 0; i < this.deviceItemFinal.size(); i++) {
			int auxTotal = 0;
			System.out.println("device-item " + i + ": " + this.deviceItemFinal.get(i));
			for(int j = 0; j < this.deviceItemFinal.get(i).size(); j++) {
				auxTotal+= this.infra.sizeTelemetryItems[this.deviceItemFinal.get(i).get(j).second];
				//System.out.printf("%d ", this.infra.sizeTelemetryItems[this.deviceItemFinal.get(i).get(j).second]);
			}
			//System.out.println("total used: " + auxTotal + ", probe capacity: " + this.capacityProbe);
		}
	}
	
	//cycle format
	public void printCircuitsCycle(){
		System.out.println("\nCYCLES (Cycle):");
		for(int i = 0; i < this.cycles.size(); i++) {
			System.out.printf("path " + i + ": " + this.cycles.get(i).nodes + "\n");
		}
	}
	
	//cycle format
	public void printDeviceItemCycle() {
		System.out.println("\nDEVICE-ITEM (Cycle)");
		for(int i = 0; i < this.cycles.size(); i++) {
			System.out.println("device-items: " + i + ": " + this.cycles.get(i).itemPerCycle);
		}
	}

	
	public void startRemainingEdges(ArrayList<Pair<Integer,Integer>> remainingEdges) {	
		//start every node as non visited, that is to be visited'(globally)
		Pair<Integer,Integer> edge = Pair.create(-2,-2);
		for(int i = 0; i < this.infra.graph.length; i++) {
			for(int j = 0; j < this.infra.graph.length; j++) {	
				if(this.infra.graph[i][j] == 1) {
					edge = Pair.create(i, j);
					remainingEdges.add(edge);
				}		
			}
		}
	}
	
	
	public Pair<Integer,Integer> createEdge(int node1, int lastNode, double w, int depot, boolean[][] visited, ArrayList<Pair<Integer,Integer>> remainingEdges) {
		int node2 = -1;
		int chanceTotal = 0;
		
		ArrayList<Integer> neighboursList = new ArrayList<Integer>();
		Map<Integer, Integer> chance = new HashMap<Integer, Integer>(); // (node, path size)
		
		//search for neighbours... avoid loop on random node selection
		for(int j = 0; j < this.infra.size; j++) {
			if(node1 != j && this.infra.graph[node1][j] == 1 &&
					!visited[node1][j] && j != lastNode && j != depot) {
				
				neighboursList.add(j);

				//System.out.println("this.costShortestPath[" + j + "][" + depot + "]: " + this.costShortestPath[j][depot]);
				chance.put(j, this.costShortestPath[j][depot]);
				chanceTotal += this.costShortestPath[j][depot];
			}	
		}
		
		if(chance.size() > 1) {
			//normalize probability values
			for(Integer key : chance.keySet()) {
				chance.replace(key, chanceTotal - chance.get(key));
			}
			chanceTotal = 0;
			
			for(Integer key : chance.keySet()) {
				chanceTotal += chance.get(key);
			}
			chance = this.sortByComparator(chance, false); //order -> false = DESC, true = ASC;
		}
		
		//if there is at least one feasible neighbour
		if(!neighboursList.isEmpty()) {
			node2 = probabilityNode(chance, chanceTotal);
			
			Pair<Integer, Integer> p = Pair.create(node1, node2);
			visited[node1][node2] = true;
			return p;
			
		}else {
			Pair<Integer, Integer> pair = Pair.create(node1, -2);
			for(Pair<Integer,Integer> p: remainingEdges) {
				if(neighboursList.contains(p.first)) { // if there is still a neighbour where it can force and "jump over"
					pair = Pair.create(node1, p.first);
					visited[node1][p.first] = true;
					break;
				}
			}
			
			// if pair is (node, -2) at the end, it means it completed a circuit and there is no remaining edge to the current node,...
			// ... so, one have to choose how to treat this issue.
			return pair;
		}
	}

	
	public int probabilityNode(Map<Integer, Integer> chance, int chanceTotal) {
		int subTotal = 0;
		int selectedNode = -1;
		int choice = this.getRandomNumberInRange(0, chanceTotal);
		
		for(Integer key : chance.keySet()) {
			subTotal += chance.get(key);
			//System.out.println("(for) key: " + key + " choice: " + choice + " subTotal: " + subTotal + " chanceTotal: " + chanceTotal);
						
			if(choice <= subTotal) {
				 selectedNode = key;
				 //System.out.println("selectedNode: " + selectedNode);
				 return selectedNode;
			}
			
		}
		
		return selectedNode;
	}
	
	
	public Map<Integer, Integer> sortByComparator(Map<Integer, Integer> unsortMap, final boolean order){
		
		//order -> false = DESC, true = ASC;
        List<Entry<Integer, Integer>> list = new LinkedList<Entry<Integer, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<Integer, Integer>>(){
            public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2){
                if (order){
                    return o1.getValue().compareTo(o2.getValue());
                }
                else{
                    return o2.getValue().compareTo(o1.getValue());
                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<Integer, Integer> sortedMap = new LinkedHashMap<Integer, Integer>();
        
        for (Entry<Integer, Integer> entry : list){
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
	
	public static void printMap(Map<Integer, Integer> map){
        for (Entry<Integer, Integer> entry : map.entrySet()){
            System.out.println("Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
    }
	
	
	public int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}

		Random r = new Random(this.seed);
		return r.nextInt((max - min) + 1) + min;
	}
	

	public boolean findEdge(Pair<Integer,Integer> edge, ArrayList<Pair<Integer,Integer>> remainingEdges) {		
		return (remainingEdges.contains(edge)) ? true : false;
	}
	
	
	//conversion
	public Hashtable<Integer, Cycle> convertToCycle() {
		Hashtable<Integer, Cycle> cyclesER = new Hashtable<Integer, Cycle>();
		
		for(int i = 0; i < this.cyclesFinal.size(); i++) {
			int j = 1;
			ArrayList<Integer> path = new ArrayList<Integer>();
			Cycle c = new Cycle();
			c.transportationCost = 0;
			
			int capacityUsed = 0;
			
			for(Pair<Integer, Integer> p: this.deviceItemFinal.get(i)) {
				capacityUsed += this.infra.sizeTelemetryItems[p.second];
			}
			
			
			for(Pair<Integer,Integer> p: this.cyclesFinal.get(i)) {
				capacityUsed++;
				c.links.add(p);
				if(j != this.cyclesFinal.get(i).size()) {
					path.add(p.first);
				}else {
					path.add(p.first);
					path.add(p.second);
				}
				
				j++;
			}
			c.capacity_used = capacityUsed;
			c.capacity = this.capacityProbe;
			c.nodes = path;
			
			
			ArrayList<Tuple> devicesItems = new ArrayList<Tuple>(); 
			for(int k = 0; k < this.deviceItemFinal.get(i).size(); k++) {
				Pair<Integer,Integer> p = Pair.create(-2,-2);
				p = this.deviceItemFinal.get(i).get(k);
				Tuple t = new Tuple(p.first, p.second);
				devicesItems.add(t);
			}
		
			c.itemPerCycle = devicesItems;
			cyclesER.put(i, c);
			cycles.add(c);
		}
	
		return cyclesER;
	}
	
	
	public void printDeviceItemInfra() {
		//System.out.println("\n\nDEMAND INFRA:");
		for(int i = 0; i < infra.size; i++) {
			//System.out.println("router: " + i);
			
			//System.out.printf("items: ");
			for(int j = 0; j < infra.telemetryItemsRouter; j++) {
				if(infra.items[i][j] == 1) {
					//System.out.printf("%d ", j);
				}
			}
			//System.out.println();
			
			//System.out.printf("demand: ");
			for(int j = 0; j < infra.telemetryItemsRouter; j++) {
				if(infra.items[i][j] == 1) {
					//System.out.printf("%d ", infra.sizeTelemetryItems[j]);
				}
			}
			//System.out.println();
			//System.out.println();
			
		}

	}
	
	
	public List<List<Integer>> subsets(int[] nums) {
		List<List<Integer>> list = new ArrayList<>();
		subsetsHelper(list, new ArrayList<>(), nums, 0);
		return list;
	}
	
	private void subsetsHelper(List<List<Integer>> list , List<Integer> resultList, int [] nums, int start){
		list.add(new ArrayList<>(resultList));
		for(int i = start; i < nums.length; i++){
          
			resultList.add(nums[i]);
			subsetsHelper(list, resultList, nums, i + 1);
			resultList.remove(resultList.size() - 1);
		}
	}

	private ArrayList<Integer> notInQ(List<Integer> list) {
		
		int[] nodes = new int[this.infra.size];
		for(int i = 0; i < this.infra.size; i++) {
			nodes[i] = i;
		}
		
		for(int i = 0; i < list.size(); i++) {
			nodes[list.get(i)] = -1; 
		}
		
		ArrayList<Integer> nodesNotInQ = new ArrayList<Integer>();
		
		for(int i = 0; i < this.infra.size; i++) {
			if(nodes[i] != -1) nodesNotInQ.add(i);
		}
		
		return nodesNotInQ;
			
	}

	public List<List<Integer>> combination(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(n, k, 0, result, new ArrayList<>());
        return result;
    }

    public void backtrack(int n, int k, int startIndex, List<List<Integer>> result,
                          List<Integer> partialList) {
        if (k == partialList.size()) {
            result.add(new ArrayList<>(partialList));
            return;
        }
        for (int i = startIndex; i < n; i++) {
            partialList.add(i);
            backtrack(n, k, i + 1, result, partialList);
            partialList.remove(partialList.size() - 1);
        }
    }
    
    public int calculateCapacity(Cycle c) {
		int capacity_used = 0;
		for(Tuple t: c.itemPerCycle) {
			capacity_used += infra.sizeTelemetryItems[t.item];
		}
		
		/*for(Pair<Integer,Integer> p: c.links) {
			capacity_used++;
		}*/
		
		capacity_used += c.nodes.size() - 1;
		
		return capacity_used;
	}
    
    public HashSet<Integer> getCollectors(){
    	HashSet<Integer> depotNodes = new HashSet<Integer>();
    	for(int i = 0; i < this.cycles.size(); i++) {
    		depotNodes.add(this.cycles.get(i).nodes.get(0));
		}
    	
    	return depotNodes;
    }
    
    public void adaptToLinks() {
    	
		for(Cycle c: this.cycles) {
			c.links.clear();
			for(int i = 1; i < c.nodes.size(); i++) {
				int first = c.nodes.get(i-1);
				int second = c.nodes.get(i);
				Pair p = new Pair(first, second);
				c.links.add(p);
			}
			c.capacity_used = this.calculateCapacity(c);
		}
	}
      
    public void improvementMethod() throws CloneNotSupportedException {                                                                
		ArrayList<Cycle> already_checked_cycles = new ArrayList<Cycle>();              //lista que guarda os ciclos que já foram checados
		while(true) {			                                                       //loop principal que executa até ter checado todos os ciclos
			ArrayList<Cycle> cycles_aux = new ArrayList<Cycle>(this.cycles);                                      //altero ciclos sempre no cycles_aux, se eu perceber que não posso excluir um ciclo do cycle_aux eu só retorno para o loot, que irá utilizar a lista cycles que não havia sido alterada
			int value_least_used = Integer.MAX_VALUE;                                  //usado para saber qual foi o ciclo menos utilizado
			Cycle leastUsed = new Cycle();                                             //guarda o ciclo menos utilizado, não faço alterações nesse ciclo
			Cycle leastUsed_aux = new Cycle();                                         //guarda o ciclo menos utilizado, pelos mesmos motivos do cycles_aux eu só altero essa variável
			boolean find = false;                                                      //é true se eu encontrei algum ciclo que ainda não foi checado e false caso contrário
			for(Cycle c : cycles_aux) {                                                //procura por ciclos com o mínimo de uso
				if(!already_checked_cycles.contains(c)) {					
					if(c.capacity_used < value_least_used) {
						find = true;                                                   //encontrei ao menos um ciclo
						value_least_used = c.capacity_used;
						leastUsed = c.clone();
						leastUsed_aux = c;
					}
				}
			}
			if(find == false || leastUsed.capacity_used + 2 >= leastUsed.capacity) {     //só utiliza ciclos que tenham um capacidade não usada relevante(tem que achar algum valor que represente isso, por exemplo o tamanho do menor item, mas por enquanto deixa assim)
				break;                                                                  //como eu sei que esse é o menos utilizado, todos outros ciclos terão mais capacidade utilizada que esse, então eu saio do loop principal, termiando o algoritmo
			}
			
			if(!leastUsed.itemPerCycle.isEmpty()) {                                     //se o ciclo foi usado para coletar algum item
				ArrayList<Tuple> removeTuples = new ArrayList<Tuple>();                 //guarda os items que eu retirei desse ciclo
				for(Tuple t : leastUsed.itemPerCycle) {                                 //itero sobre todos os itens do ciclo
					boolean found = false;                                              //true se eu encontrei um ciclo que passa pelo mesmo nodo e que tem capacidade para coletar o item, falso caso contrário 
					for(int i = 0; i < cycles_aux.size(); i++) {                        //itera sobre todos os outros ciclos
						if(cycles_aux.get(i).nodes.contains(t.device)) {				//se for encontrado um ciclo com capacidade sobrando que passe pelo mesmo nodo que o ciclo atual
							if(cycles_aux.get(i).capacity_used + this.infra.sizeTelemetryItems[t.item] <= cycles_aux.get(i).capacity && !cycles_aux.get(i).equals(leastUsed_aux)) {
								found = true;                                           //encontrei um ciclo que pode coletar o item
								removeTuples.add(t);                                    //adiciono esse item na lista de itens removidos do ciclo
								cycles_aux.get(i).itemPerCycle.add(t);                  //adiciono esse item no ciclo
								cycles_aux.get(i).capacity_used += this.infra.sizeTelemetryItems[t.item];   //adiciono na capacidade
								break;                                                  //não preciso mais procurar em outros ciclos
							}
						}
					}
					if(!found) {                                                        //procura por um ciclo que passe perto de um nodo utilizado pelo pathAtual, e o utiliza para coletar esse item
						boolean found_dijkstra = false;                                 //true se encontrou um ciclo que pode criar um caminho para "buscar" o item
						int min_cost = Integer.MAX_VALUE;                               //armazena qual o custo mínimo para buscar o item
						int choosed_cycle = -1;                                         //armazena o ciclo escolhido
						int choosed_device_index = -1;                                  //armazena o nodo mais próximo do item (ciclo [1,2,3,4,5] foi verificado que o menor caminho para buscar o item (6, 0) é [2,6,2], por exemplo)
						int choosed_device_next = -1;                                   //armazena qual o proximo nodo, depois do escolhido, para garantir que o ciclo continue de maneira correta
						for(int c = 0; c < cycles_aux.size(); c++) {                    //itera sobre todos os ciclos
							if(cycles_aux.get(c) == leastUsed_aux) {                    //ignora o ciclo least_used, pois é dele que está sendo retirado o item
								continue;
							}
							for(int i = 0; i < cycles_aux.get(c).nodes.size()-1; i++) { //itera sobre todos os nodos do ciclo
								int cost_dijkstra_ida = infra.getShortestPath(cycles_aux.get(c).nodes.get(i), t.device).size() - 1;   //calcula o tamanho do caminho de ida
								int cost_dijkstra_volta = infra.getShortestPath(t.device, cycles_aux.get(c).nodes.get(i)).size() - 1; //calcula o tamanho do caminho de volta(é o mesmo do caminho de ida)
								if(cost_dijkstra_ida < 0) {
									cost_dijkstra_ida = 0;
								}
								if(cost_dijkstra_volta < 0) {
									cost_dijkstra_volta = 0;
								}
								int cost_node = cost_dijkstra_ida + cost_dijkstra_volta + infra.sizeTelemetryItems[t.item];           //calcula o caminho total para coletar o item partindo do nodo i
								if(cost_node + cycles_aux.get(c).capacity_used <= cycles_aux.get(c).capacity) {                       //se o ciclo "c" tiver capacidade suficiente para coletar esse item
									if(cost_node < min_cost) {                                                                        //se o custo para coletar o item for menor que eu encontrei 
										found_dijkstra = true;                                                                        //considero que encontrei pelo menos um ciclo que pode coletar o item
										min_cost = cost_node;                                                                         //atualizo o min_cost 
										choosed_cycle = c;                                                                            //atualizo o choosed_cycle
										choosed_device_index = i;                                                                     //atualizo o choosed_device
										choosed_device_next = i+1;                                                                    //atualizo o choosed_device_next
									}
								}
							}
						}
						if(!found_dijkstra) {							                 //se não foi encontrado um ciclo que possa coletar esse item
							already_checked_cycles.add(leastUsed);                   //coloco esse ciclo no already_checked, uma vez que não consegui coletar um dos itens dele
							continue;
						}else {                                                          //se encontrei um ciclo que pode coletar o item
							removeTuples.add(t);                                         //adiciono o item em questão na lista de tuplas a serem removidas
							cycles_aux.get(choosed_cycle).itemPerCycle.add(t);           //adiciono o item no ciclo escolhido
							cycles_aux.get(choosed_cycle).capacity_used += min_cost;     //adiciono o custo de coletar o item
							ArrayList<Integer> caminho_ida = infra.getShortestPath(cycles_aux.get(choosed_cycle).nodes.get(choosed_device_index), t.device);      //armazena o caminho de ida
							for(int j = 1; j < caminho_ida.size();j++) {                                                                                          //adiciona o caminho de ida na lista nodes do ciclo
								cycles_aux.get(choosed_cycle).nodes.add(choosed_device_index+j, caminho_ida.get(j));
							}
							ArrayList<Integer> caminho_volta = infra.getShortestPath(t.device, cycles_aux.get(choosed_cycle).nodes.get(choosed_device_index));    //armazena o caminho de volta
							for(int j = 1; j < caminho_ida.size();j++) {                                                                                          //adiciona o caminho de volta na lista nodes do ciclo
								cycles_aux.get(choosed_cycle).nodes.add(choosed_device_next+caminho_ida.size()-1, caminho_volta.get(j));
							}
						}
					}
				}
				
				for(Tuple t: removeTuples) {                                              //para cada item armazenado em removeTuples
					leastUsed.itemPerCycle.remove(t);                                     //remove o item do ciclo least_used
					leastUsed.capacity_used = this.calculateCapacity(leastUsed);          //recalcula a capacidade utilizada do ciclo least_used
				}
				cycles.remove(leastUsed_aux);                                         //atualiza os ciclos do sistema
				cycles.add((Cycle) leastUsed);
			}else {                                                                       //caso onde um circuito não coleta itens, apenas visita arestas
				boolean canDeleteCycle = true;                                            //se uma das arestas não puder ser removida, essa variável será falsa
				for(int i = 0; i < leastUsed.nodes.size()-1; i++) {                       //itera sobre todas as arestas do ciclo
					if(!canDeleteCycle) {                                                 //se eu não puder remover uma das arestas do ciclo, eu considero que não posso deletar esse ciclo da solução
						break;
					}
					int node = leastUsed.nodes.get(i);                                    //node armazena o nodo atual
					int prox_node = leastUsed.nodes.get(i+1);                             //prox_node armazena o nodo depois de node
					boolean found = false;                                                //found será true se eu encontrar outro ciclo que passe pela mesma aresta, do contrário continuará false
					for(Cycle c: cycles_aux) {                                            //itero sobre todos os ciclos
						if(found) {                                                       //se encontrar um ciclo que passe pela mesma aresta não há mais necessidade de verificar outros ciclos
							break;
						}
						if(c.equals(leastUsed_aux)) {                                         //desconsidero o ciclo least_used, já que é dele que estamos tentando retirar a aresta
							continue;
						}
						for(int k = 0; k < c.nodes.size()-1; k++) {                       //itero sobre os nodos do ciclo
							if(c.nodes.get(k) == node && c.nodes.get(k+1) == prox_node) { //se o ciclo tiver essa aresta
								found = true;                                             //marco como encontrado
								break;                                                    //não preciso mais procurar nesse ciclo
							}
							if(c.nodes.get(k) == prox_node && c.nodes.get(k+1) == node) { //se o ciclo tiver a aresta ivertida
								found = true;                                             //marco como encontrado
								break;                                                    //não preciso mais procurar nesse ciclo
							}
						}
					}
					if(found) {                                                               //se já encontrei outro ciclo que passe pela mesma aresta
						canDeleteCycle = true;                                                //marco esse ciclo como um que pode ser deleta (para essa aresta, se alguma aresta marcar essa flag como false não tem como reverter e o ciclo não será deletado)
					}else {                                                                   //se eu não encontro um ciclo que já passe por essa aresta, preciso fazer um ciclo passar por ela
						for(Cycle c: cycles_aux) {                                            //itero sobre todos os ciclos
							if(c.capacity - c.capacity_used <= 2 || c.equals(leastUsed_aux)){ //se o ciclo possui uma capacidade mínima sobrando eu desconsidero
								canDeleteCycle = false;
								continue;
							}
							int c_prev_node = -1;                                         //c_prev_node armazena o node de onde vai partir o caminho que passará pela aresta
							int c_capacity_used = c.capacity_used;                        //c_capacity_used armazena a capacidade utilizada pelo ciclo
							for(int k = 0; k < c.nodes.size()-1; k++) {                   //itera sobre os nodes do ciclo
								if(c.nodes.get(k) == node) {                              //caso o ciclo passe pela exata aresta que está sendo procurada
									c_prev_node = c.nodes.get(k);                         //define esse nodo como ponto de partida
									break;                                                //para de procurar
								}
								if(c.nodes.get(k) == prox_node) {                         //se a aresta for (5,3), ele pode não achar o nodo 5, mas encontrar o nodo 3, que também serve para o mesmo propósito 
									c_prev_node = c.nodes.get(k);                         //define esse nodo como ponto de partida
									int aux = node;                                       //como a aresta (3,5) e (5,3) são a mesma para o propósito do algorimto, ele faz um swap dos valores de node e prox_node
									node = prox_node;
									prox_node = aux;
									break;                                                //para de procurar
								}
							}
							if(c_prev_node != -1) {                                                              // caso ele tenha encontrado o ponto inicial
								ArrayList<Integer> caminhoIda = infra.getShortestPath(node, prox_node);          //define o caminho de ida até o prox_node
								ArrayList<Integer> caminhoVolta = infra.getShortestPath(prox_node, node);        //define o caminho de volta do prox_node até o node
								for(int j = 1; j < caminhoVolta.size(); j++) {                                   //junta os dois caminhos
									caminhoIda.add(caminhoVolta.get(j));
								}
								int aditional_cost = caminhoIda.size() - 1;                                      //contabiliza ida e volta (agora caminhoIda tem todo o caminho, ida e volta)
								if(c_capacity_used + aditional_cost <= c.capacity) {                             //verifica se o ciclo possui a capacidade 
									int k = c.nodes.indexOf(node);                                               
									for(int l = 1; l < caminhoIda.size(); l++) {
										cycles_aux.get(cycles_aux.indexOf(c)).nodes.add(k+l, caminhoIda.get(l)); //conta louca que funciona pra colocar o caminho gerado na posição correta dentro da lista nodes do ciclo
									}
									c.capacity_used += aditional_cost;                                           //adiciona o custo no ciclo
									canDeleteCycle = true;                                                       //ainda posso deletar esse ciclo
									break;                                                                       //não preciso continuar verificando em outros ciclos
								}else {                                                                          //caso não possua a capacidade sobrando
									continue;                                                                    //continuo procurando em outros ciclos
								}
							}else {                                                                              //caso não tenha um ciclo que passe por um dos nodos da aresta
								canDeleteCycle = false;                                                          //não implementei nada para criar esse caminho, pode ser algo que melhore o algoritmo
								continue;                                                                        //simplesmente digo que não posso deletar esse ciclo, o continue levará para um "if" que vai parar a execução
								 
							}
						}
					}
				}
				if(canDeleteCycle) {                                    //se eu consegui atrelar todas as arestas do leastUsed em outros ciclos
					cycles_aux.remove(leastUsed_aux);                       //removo o leastUsed do cycles_aux
					cycles = cycles_aux;
				}else {
					already_checked_cycles.add(leastUsed_aux);          //adiciono o leastUsed_aux (leastUsed sem nenhuma modificação) na lista de ciclos já checados
					continue;                                           //não atualizo o cycles, pois o cycles_aux possui alterações que não foram feitas de verdade, apenas passo para a próxima
					
				}
			}
		}
	}

	private boolean verifyCycle(Cycle c, ArrayList<Cycle> already_checked_cycles) {
		boolean different = true;
		for(Cycle checked: already_checked_cycles) {
			if(!different) {
				break;
			}
			if(c.capacity_used != checked.capacity_used) {
				continue;
			}else {
				if(c.itemPerCycle.isEmpty() && checked.itemPerCycle.isEmpty()) {
					if(!c.nodes.equals(checked.nodes)) {
						different = false;
						break;
					}
				}else {
					//verifico os itens
					if(c.itemPerCycle.size() != checked.itemPerCycle.size()) {
						continue;
					}else {
						for(int i = 0; i < c.itemPerCycle.size(); i++) {
							if(c.itemPerCycle.get(i).device == checked.itemPerCycle.get(i).device) {
								if(c.itemPerCycle.get(i).item == checked.itemPerCycle.get(i).item) {
									different = false;
								}else {
									break;
								}
							}else {
								break;
							}
						}
					}
				}
			}
		}
		return different;
	}

}
