package heuristics;


import main.NetworkInfrastructure;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CARP {
	public int[][]graph;
	public int[][]cost; //c_ij
	public int[][]demand; //q_ij;
	public static final int NUM_NODES = 8;
	public static final int W = 4;
	public int lastFeasibleNode = -1;
	public ArrayList<Pair<Integer,Integer>> resultAux;
	public ArrayList<ArrayList<Pair<Integer,Integer>>> resultFirstCriterion;
	public ArrayList<ArrayList<Pair<Integer,Integer>>> resultSecondCriterion;
	public ArrayList<ArrayList<Pair<Integer,Integer>>> resultThirdCriterion;
	public ArrayList<ArrayList<Pair<Integer,Integer>>> resultFourthCriterion;
	public ArrayList<ArrayList<Pair<Integer,Integer>>> resultFifthCriterion;
	public ArrayList<Pair<Integer,Integer>> nonSatisfiedEdges;
	ArrayList<Pair<Integer,Integer>> remainingEdges; //start every node as non visited (globally)
	//Pattern pattern;   //pattern to extract integers from tuples "Pair<Integer,Integer>"


	public int depot = 0;
	
	public CARP() {
		this.graph = new int[NUM_NODES][NUM_NODES];
		this.cost = new int[NUM_NODES][NUM_NODES];
		this.demand = new int[NUM_NODES][NUM_NODES];
		this.resultAux = new ArrayList<Pair<Integer,Integer>>(); //edges
		this.resultFirstCriterion = new ArrayList<ArrayList<Pair<Integer,Integer>>>();
		this.resultSecondCriterion = new ArrayList<ArrayList<Pair<Integer,Integer>>>();
		this.resultThirdCriterion = new ArrayList<ArrayList<Pair<Integer,Integer>>>();
		this.resultFourthCriterion = new ArrayList<ArrayList<Pair<Integer,Integer>>>();
		this.resultFifthCriterion = new ArrayList<ArrayList<Pair<Integer,Integer>>>();
		//this.remainingEdges = new ArrayList<String>();
		this.remainingEdges = new ArrayList<Pair<Integer,Integer>>();
		//this.nonSatisfiedEdges = new ArrayList<String>();
		this.nonSatisfiedEdges = new ArrayList<Pair<Integer,Integer>>();
		//Pattern pattern = Pattern.compile("(?!\\(\\),)(\\d+)");   //pattern to extract integers from tuples "Pair<Integer,Integer>"

		//set NUM_NODES = 4;
		/*this.graph[0][1] = 1;
		this.graph[1][0] = 1;
		this.graph[0][2] = 1;
		this.graph[2][0] = 1;
		this.graph[0][3] = 1;
		this.graph[3][0] = 1;
		this.graph[1][3] = 1;
		this.graph[3][1] = 1;
		this.graph[2][3] = 1;
		this.graph[3][2] = 1;
		
		this.cost[0][1] = 2;
		this.cost[1][0] = 2;
		this.cost[0][2] = 1;
		this.cost[2][0] = 1;
		this.cost[0][3] = 4;
		this.cost[3][0] = 4;
		this.cost[1][3] = 3;
		this.cost[3][1] = 3;
		this.cost[2][3] = 2;
		this.cost[3][2] = 2;
		
		this.demand[0][1] = 2;
		this.demand[1][0] = 2;
		this.demand[0][2] = 1;
		this.demand[2][0] = 1;
		this.demand[0][3] = 1;
		this.demand[3][0] = 1;
		this.demand[1][3] = 2;
		this.demand[3][1] = 2;
		this.demand[2][3] = 2;
		this.demand[3][2] = 2;*/
		
		
		//set NUM_NODES = 8;
		this.graph[0][1] = 1;
		this.graph[1][0] = 1;
		this.graph[0][2] = 1;
		this.graph[2][0] = 1;
		this.graph[1][2] = 1;
		this.graph[2][1] = 1;
		this.graph[2][3] = 1;
		this.graph[3][2] = 1;
		this.graph[2][4] = 1;
		this.graph[4][2] = 1;
		this.graph[2][7] = 1;
		this.graph[7][2] = 1;
		this.graph[3][0] = 1;
		this.graph[0][3] = 1;
		this.graph[4][5] = 1;
		this.graph[5][4] = 1;
		this.graph[5][6] = 1;
		this.graph[6][5] = 1;
		this.graph[6][7] = 1;
		this.graph[7][6] = 1;
		
		this.cost[0][1] = 1;
		this.cost[1][0] = 1;
		this.cost[0][2] = 4;
		this.cost[2][0] = 4;
		this.cost[1][2] = 2;
		this.cost[2][1] = 2;
		this.cost[2][3] = 3;
		this.cost[3][2] = 3;
		this.cost[2][4] = 2;
		this.cost[4][2] = 2;
		this.cost[2][7] = 4;
		this.cost[7][2] = 4;
		this.cost[3][0] = 2;
		this.cost[0][3] = 2;
		this.cost[4][5] = 3;
		this.cost[5][4] = 3;
		this.cost[5][6] = 2;
		this.cost[6][5] = 2;
		this.cost[6][7] = 1;
		this.cost[7][6] = 1;
		
		this.demand[0][1] = 1;
		this.demand[1][0] = 1;
		this.demand[0][2] = 4;
		this.demand[2][0] = 4;
		this.demand[1][2] = 3;
		this.demand[2][1] = 3;
		this.demand[2][3] = 2;
		this.demand[3][2] = 2;
		this.demand[2][4] = 1;
		this.demand[4][2] = 1;
		this.demand[2][7] = 1;
		this.demand[7][2] = 1;
		this.demand[3][0] = 1;
		this.demand[0][3] = 1;
		this.demand[4][5] = 2;
		this.demand[5][4] = 2;
		this.demand[5][6] = 2;
		this.demand[6][5] = 2;
		this.demand[6][7] = 1;
		this.demand[7][6] = 1;
		
		
	}
		
	public void run() {
		short flagCriterion = 0;
		
		//solving by 1st criterion
		/*flagCriterion = 1;
		this.runCriteria(flagCriterion);
		System.out.println("\nEnd_First_Criterion");
		for(int i = 0; i < this.resultFirstCriterion.size(); i++) {
			System.out.printf("path: " + i + ": ");
			System.out.println(this.resultFirstCriterion.get(i));
		}*/

		//solving by 2nd criterion
		flagCriterion = 2;
		this.runCriteria(flagCriterion);
		System.out.println("\nEnd_Second_Criterion");
		for(int i = 0; i < this.resultSecondCriterion.size(); i++) {
			System.out.printf("path: " + i + ": ");
			System.out.println(this.resultSecondCriterion.get(i));
		}
		
		//solving by 3rd criterion
		/*flagCriterion = 3;
		this.runCriteria(flagCriterion);
		System.out.println("\nEnd_Third_Criterion");
		for(int i = 0; i < this.resultThirdCriterion.size(); i++) {
			System.out.printf("path: " + i + ": ");
			System.out.println(this.resultThirdCriterion.get(i));
		}
		
		
		//solving by 4th criterion
		/*flagCriterion = 4;
		this.runCriteria(flagCriterion);
		System.out.println("\nEnd_Fourth_Criterion");
		for(int i = 0; i < this.resultFourthCriterion.size(); i++) {
			System.out.printf("path: " + i + ": ");
			System.out.println(this.resultFourthCriterion.get(i));
		}*/
		
		//solving by 5th criterion
		/*flagCriterion = 5;
		this.runCriteria(flagCriterion);
		System.out.println("\nEnd_Fifth_Criterion");
		for(int i = 0; i < this.resultFifthCriterion.size(); i++) {
			System.out.printf("path: " + i + ": ");
			System.out.println(this.resultFifthCriterion.get(i));
		}*/
		
	}
	
	
	private void runCriteria(short flagCriterion) {
		if(flagCriterion < 0 || flagCriterion > 5) {
			System.out.println("ERROR: INVALID FLAG! EXITING...");
			System.exit(0);
		}
		
		int w = this.W; //vehicle's capacity
		int old_w = w;
		
		//flags
		boolean flagDeviation = false; //deviation from modus operandi
		boolean pathIsEqual = false; //detects repeated path and avoids its insertion into the results
		
		Pair<Integer,Integer> edge = Pair.create(-2,-2);

		
		//start every node as non visited, that is to be visited (globally)
		this.startRemainingEdges(this.remainingEdges);
		
		System.out.println("REMAINING EDGES BEFORE ITERATING...");
		for(int i = 0; i < this.remainingEdges.size(); i++) {
			System.out.println("remainingEdges: " + this.remainingEdges.get(i));
		}


		System.out.println("\nStarting weight(w): " + this.W);
		do {
			
			int index = this.depot;
			int flagDepot = 0;
			boolean visited[][] = new boolean[NUM_NODES][NUM_NODES];
			
			do { //creates a circuit
				if(flagCriterion == 1 && !flagDeviation) {
					edge = this.solveByFirstCriterion(index, visited, flagDepot, flagDeviation); //edge as return	
				}
				else if(flagCriterion == 2 && !flagDeviation) {
					edge = this.solveBySecondCriterion(index, visited, flagDepot, flagDeviation); //edge as return
				}
				/*else if(flagCriterion == 3 && !flagDeviation) {
					edge = this.solveByThirdCriterion(index, visited, flagDepot, flagDeviation); //edge as return
				}
				else if(flagCriterion == 4 && !flagDeviation) {
					edge = this.solveByFourthCriterion(index, visited, flagDepot, flagDeviation); //edge as return
				}
				else if(flagCriterion == 5 && !flagDeviation) {
					if(((double)(old_w - w)) < ((double)(old_w/2D))) {
						edge = this.solveByFourthCriterion(index, visited, flagDepot, flagDeviation); //edge as return
					}else {
						edge = this.solveByThirdCriterion(index, visited, flagDepot, flagDeviation);
					}
					
				}*/
				
				else if (flagDeviation){		
					System.out.println("\nREMAINING EDGES CASE...");
					
					int x1 = -1;
					int x2 = -1;
					
					//find the first remaining edge to create direct shortest path in order to RESTART the criterion
					for(Pair<Integer,Integer> p: this.remainingEdges) {
						if(!this.remainingEdges.isEmpty()) {
							x1 = p.first;
							x2 = p.second;
	
							break;
						}
					}
					
					System.out.format("x1: %d, x2: %d\n", x1, x2);
					
					//create shortest path to the remaining edge
					ArrayList<Integer> shortPath = new ArrayList<Integer>();
					if(x1 != depot) {
						//dijkstra
						shortPath = getShortestPath(depot, x1);
					}else{
						//dijkstra
						shortPath = getShortestPath(depot, x2);
					}

					System.out.println("shortPath: " + shortPath);

					
					for(int k = 0; k < shortPath.size() - 1; k++) {
						int xyz = shortPath.get(k);
						int xyzProx = shortPath.get(k+1);
						visited[xyz][xyzProx] = true;
						visited[xyzProx][xyz] = true;
					}
					
					
					if(flagCriterion == 1) {
						edge = this.solveByFirstCriterion(index, visited, flagDepot, flagDeviation); //edge as return	
					}else if(flagCriterion == 2) {
						edge = this.solveBySecondCriterion(index, visited, flagDepot, flagDeviation); //edge as return
					}/*else if(flagCriterion == 3) {
						edge = this.solveByThirdCriterion(index, visited, flagDepot, flagDeviation); //edge as return
					}else if(flagCriterion == 4) {
						edge = this.solveByFourthCriterion(index, visited, flagDepot, flagDeviation); //edge as return
					}
					}else if(flagCriterion == 4) {
						edge = this.solveByFifthCriterion(index, visited, flagDepot, flagDeviation); //edge as return
					}*/
					
					flagDeviation = false;
				
					
				}
				
				
				System.out.println("\n\n---------Index (node): " + index + "---------");
				System.out.println("edge: " + edge);
				visited[edge.first][edge.second] = true;
				visited[edge.second][edge.first] = true;
				
				
				flagDepot++;
				
				//update 'w' and remove edge;
				if(findEdge(edge)) {
					w -= this.demand[edge.first][edge.second];
					this.remainingEdges.remove(edge);
					Pair<Integer,Integer> edgeRev = Pair.create(edge.second, edge.first);
					this.remainingEdges.remove(edgeRev);
				}
					
				System.out.println("w: " + w);
				this.resultAux.add(edge);	
				
				index = edge.second;
			}while(w >= 0 && edge.second != depot);
			
						
			System.out.println("--------------------------");
		
			
			if(edge.second != depot && w < 0) {
				
				System.out.println("NEGATIVE WEIGHT CASE...");
				Pair<Integer,Integer> auxEdge = Pair.create(-2, -2);
				Pair<Integer,Integer> lastEdge = Pair.create(-2, -2); //to determinate where to start the dijkstra algorithm
				
				while(w < 0) {
					auxEdge = Pair.create(-2,-2);
					auxEdge = this.resultAux.get(this.resultAux.size() - 1); //remove non satisfied edge
					this.nonSatisfiedEdges.add(auxEdge);
					w += this.demand[auxEdge.first][auxEdge.second]; //increase unsatisfied demand
					lastEdge = Pair.create(-2, -2);
					lastEdge = this.resultAux.get(this.resultAux.size() - 1); //set min path back from lastEdge to depot
					this.resultAux.remove(this.resultAux.size() - 1); //remove unsatisfied edge
				}
				
				//dijkstra
				ArrayList<Integer> shortPath = getShortestPath(lastEdge.first, depot);
					
				//add edges from shortest to depot in order to complete a cycle
				for(int k = 0; k < shortPath.size() - 1; k++) {
					auxEdge = Pair.create(-2, -2);
					auxEdge.first = shortPath.get(k);
					auxEdge.second = shortPath.get(k+1);
					this.resultAux.add(auxEdge);
				}
						
				//clear path
				shortPath.clear();
			}
			
			System.out.println("current circuit: ");
			for(int j = 0; j < this.resultAux.size(); j++) {
				System.out.println(this.resultAux.get(j));
			}
			
			
			//if the last path is equals to current one...
			if(w == old_w) {
				pathIsEqual = true;
			}else {
				pathIsEqual = false; 
			}
				
			if(pathIsEqual && this.remainingEdges.isEmpty()){
				System.out.println("RRRRRemainingEdges iter: " + this.remainingEdges);

				this.nonSatisfiedEdges.clear();
				this.resultAux = new ArrayList<Pair<Integer,Integer>>();
				break;
			}
			 
			//remove visited edges (globally)
			
			for(Pair<Integer,Integer> p: this.nonSatisfiedEdges) {
				System.out.println("unsatisfied: " + p);
			}
			
			
			//reinsert unsatisfied edges to remainingEdges;
			for(int i = 0; i < this.nonSatisfiedEdges.size(); i++) {
				if(!findEdge(this.nonSatisfiedEdges.get(i))) {
					this.remainingEdges.add(this.nonSatisfiedEdges.get(i));	
				}
			}
			this.nonSatisfiedEdges.clear();
			

			if(this.remainingEdges.isEmpty()) {
				System.out.println("remainingEdges is empty...");
			}else {
				System.out.println("remainingEdges iter: " + this.remainingEdges);
			}
			
			if(pathIsEqual) {
				this.resultAux = new ArrayList<Pair<Integer,Integer>>();
				flagDeviation = true;
				pathIsEqual = false;
				continue;
			}
			
			if(flagCriterion == 1) {
				this.resultFirstCriterion.add(this.resultAux);	
			}
			else if(flagCriterion == 2) {
				this.resultSecondCriterion.add(this.resultAux);
			}
			/*else if(flagCriterion == 3) {
				this.resultThirdCriterion.add(resultAux);
			}
			else if(flagCriterion == 4) {
				this.resultFourthCriterion.add(resultAux);
			}
			else if(flagCriterion == 5) {
				this.resultFifthCriterion.add(resultAux);
			}*/
			
			this.resultAux = new ArrayList<Pair<Integer,Integer>>();
			w = old_w; //reset weight to start a new cycle
			
		}while(true);	
	}
	
	
	private Pair<Integer,Integer> solveByFirstCriterion(int index, boolean[][] visited, int flagDepot, boolean flagDeviation) {
		System.out.println("flagDeviation: " + flagDeviation);
		//used in order to enable depot as unvisited again and complete a circuit
		if(flagDepot > 1) {
			visited[index][this.depot] = false;
		}
		
		ArrayList<Integer> repeatedLowestCostNodes = new ArrayList<Integer>();
		 
		//search for the lowest cost
		double lowestCost = Double.MAX_VALUE;
		for(int j = 0; j < this.graph.length; j++) {
			if(this.graph[index][j] == 1 && !visited[index][j] && this.cost[index][j] < lowestCost) {
				lowestCost = this.cost[index][j];
			}
		}
		
		System.out.println("lowestCost: " + lowestCost);
		
		
		//Check whether there are any other edge with the same value or not...
		for(int j = 0; j < NUM_NODES; j++) {
			if(this.graph[index][j] == 1 && this.cost[index][j] == lowestCost && !visited[index][j]) {
				repeatedLowestCostNodes.add(j);
			}
		}
		
		//... If not, return the only feasible edge 
		if((repeatedLowestCostNodes.size() - 1) == 0) { //size - 1, because one must exclude the edge already counted
			Pair<Integer,Integer> p = Pair.create(index, repeatedLowestCostNodes.get(0));
			return p;
		}
		
		//... Else: With the lowest nodes in hands, apply first path-scanning 1st criterion, that is: calculate (cost/demand)
		double costDemand[] = new double[NUM_NODES];
		for(int i = 0; i < repeatedLowestCostNodes.size(); i++) {
			int j = repeatedLowestCostNodes.get(i);
			costDemand[j] = this.cost[index][j] / this.demand[index][j];
			System.out.println("costDemand["  + j + "]: " + costDemand[j]);	
		}
		
		//After (cost/demand), return the correct edge;
		double minCost = Double.MAX_VALUE;
		int node_j = -1;
		for(int j = 0; j < NUM_NODES; j++) {
			if(costDemand[j] != 0 && costDemand[j] < minCost) {
				minCost = costDemand[j];
				node_j = j;
			}
		}
		
		//normal case... no deviation
		if(!flagDeviation) {
			int node_j_index = repeatedLowestCostNodes.indexOf(node_j);
			Pair<Integer,Integer> p = Pair.create(index, repeatedLowestCostNodes.get(node_j_index));
			
			return p;
		}
		
		//Else... get the second nearst neighbour
		double secondMinCost = Double.MAX_VALUE;
		for(int j = 0; j < NUM_NODES; j++) {
			if(this.graph[index][j] == 1 && !visited[index][j]) {
				if(this.cost[index][j] > minCost && this.cost[index][j] < secondMinCost) { //get the second lowest cost possible
					secondMinCost = this.cost[index][j];
					node_j = j;
				}
			}
		}
		
		int node_j_index = repeatedLowestCostNodes.indexOf(node_j);
		Pair<Integer,Integer> p = Pair.create(index, repeatedLowestCostNodes.get(node_j_index));
		
		return p;

	}
	
	
	private Pair<Integer,Integer> solveBySecondCriterion(int index, boolean[][] visited, int flagDepot, boolean flagDeviation) {
		System.out.println("flagDeviation: " + flagDeviation);
		//used in order to enable depot as unvisited again and complete a circuit
		if(flagDepot > 1) {
			visited[index][this.depot] = false;
		}
		
		ArrayList<Integer> repeatedHighestCostNodes = new ArrayList<Integer>();
		 
		//search for the highest cost
		double highestCost = Double.MIN_VALUE;
		for(int j = 0; j < this.graph.length; j++) {
			if(this.graph[index][j] == 1 && !visited[index][j] && this.cost[index][j] > highestCost) {
				highestCost = this.cost[index][j];
			}
		}
		
		System.out.println("highestCost: " + highestCost);
		
		//Check whether there are any other edge with the same value or not...
		for(int j = 0; j < NUM_NODES; j++) {
			if(this.graph[index][j] == 1 && this.cost[index][j] == highestCost && !visited[index][j]) {
				repeatedHighestCostNodes.add(j);
				System.out.println("repeated highest: " + repeatedHighestCostNodes);
			}
		}
		
		//... If not, return the only feasible edge 
		if((repeatedHighestCostNodes.size() - 1) == 0) { //size - 1, because one must exclude the edge already counted
			Pair<Integer,Integer> p = Pair.create(index, repeatedHighestCostNodes.get(0));
			return p;
		}
		
		//... Else: With the lowest nodes in hands, apply first path-scanning 1st criterion, that is: calculate (cost/demand)
		double costDemand[] = new double[NUM_NODES];
		for(int i = 0; i < repeatedHighestCostNodes.size(); i++) {
			int j = repeatedHighestCostNodes.get(i);
			costDemand[j] = this.cost[index][j] / this.demand[index][j];
			System.out.println("costDemand["  + j + "]: " + costDemand[j]);	
		}
		
		//After (cost/demand), return the correct edge;
		double maxCost = Double.MIN_VALUE;
		int node_j = -1;
		for(int j = 0; j < NUM_NODES; j++) {
			if(costDemand[j] != 0 && costDemand[j] > maxCost) {
				maxCost = costDemand[j];
				node_j = j;
			}
		}

		
		//normal case... no deviation
		if(!flagDeviation) {
			System.out.println("node_j: " + node_j);
			int node_j_index = repeatedHighestCostNodes.indexOf(node_j);
			Pair<Integer,Integer> p = Pair.create(index, repeatedHighestCostNodes.get(node_j_index));
			
			return p;
		}
		
		//Else... get the second farthest neighbour
		double secondMaxCost = Double.MIN_VALUE;
		for(int j = 0; j < NUM_NODES; j++) {
			if(this.graph[index][j] == 1 && !visited[index][j]) {
				if(this.cost[index][j] > secondMaxCost && this.cost[index][j] < maxCost) { //get the second highest cost possible
					secondMaxCost = this.cost[index][j];
					node_j = j;
				}
			}
		}
		
		int node_j_index = repeatedHighestCostNodes.indexOf(node_j);
		Pair<Integer,Integer> p = Pair.create(index, repeatedHighestCostNodes.get(node_j_index));
		
		return p;
	}
	
	
	private Pair<Integer,Integer> solveByThirdCriterion(int index, boolean[] visited, int flagDepot) {
		
		if(flagDepot > 1) {
			visited[this.depot] = false;
		}
		
		ArrayList<Integer> repeatedLowestCostNodes = new ArrayList<Integer>();

		Pair<Integer,Integer> aux = Pair.create(-2,-2);
		
		//search for the lowest cost
		double lowestCost = Double.MAX_VALUE;
		for(int j = 0; j < this.graph.length; j++) {
			if(this.graph[index][j] == 1 && !visited[j] && this.cost[index][j] < lowestCost) {
				lowestCost = this.cost[index][j];
				aux.first = index;
				aux.second = j;
			}
		}
		
		
		//Check whether there are any other edge with the same value or not...
		for(int j = 0; j < NUM_NODES; j++) {
			if(this.graph[index][j] == 1 && this.cost[index][j] == lowestCost) {
				repeatedLowestCostNodes.add(j);
			}
		}
		
		
		//System.out.println("size of repeatedLowestCostNodes: " + repeatedLowestCostNodes.size());
		
		//... If not, return the only feasible edge 
		if((repeatedLowestCostNodes.size() - 1) == 0) { //size - 1, because one must exclude the edge already counted
			Pair<Integer,Integer> p = Pair.create(index, repeatedLowestCostNodes.get(0));
			return p;
		}
		
		//... Else: With the lowest nodes in hands, apply first path-scanning 3rd criterion, that is: find the shortest path
		int minCostPath = Integer.MAX_VALUE;
		int node_j = -1;
		for(int i = 0; i < repeatedLowestCostNodes.size(); i++) {
			int j = repeatedLowestCostNodes.get(i);
			
			//dijkstra
			ArrayList<Integer> shortPath = getShortestPath(j, depot);
			ArrayList<Pair<Integer,Integer>> minPathAux = new ArrayList<Pair<Integer,Integer>>();
				
			//add edges from shortest to depot in order to complete a cycle
			Pair<Integer,Integer> auxEdge = Pair.create(-2,-2); 
			for(int k = 0; k < shortPath.size() - 1; k++) {
				auxEdge = Pair.create(-2,-2);
				auxEdge.first = shortPath.get(k);
				auxEdge.second = shortPath.get(k+1);
				minPathAux.add(auxEdge);
			}
					
			int sum_j = 0;
			for(int n = 0; n < minPathAux.size(); n++) {
				auxEdge = minPathAux.get(n);
				sum_j += auxEdge.first;
			}
			
			if(sum_j < minCostPath) {
				minCostPath = sum_j;
				node_j = j;
			}
			
			sum_j = 0;
		}
		
		//After, return the correct edge;
		int node_j_index = repeatedLowestCostNodes.indexOf(node_j);
		Pair<Integer,Integer> p = Pair.create(index, repeatedLowestCostNodes.get(node_j_index));

		return p;
	}
	
	
	private Pair<Integer,Integer> solveByFourthCriterion(int index, boolean[] visited, int flagDepot) {
		
		if(flagDepot > 1) {
			visited[this.depot] = false;
		}
		
		ArrayList<Integer> repeatedHighestCostNodes = new ArrayList<Integer>();

		Pair<Integer,Integer> aux = Pair.create(-2,-2);
		
		//search for the highest cost
		double highestCost = Double.MIN_VALUE;
		for(int j = 0; j < this.graph.length; j++) {
			if(this.graph[index][j] == 1 && !visited[j] && this.cost[index][j] > highestCost) {
				highestCost = this.cost[index][j];
				aux.first = index;
				aux.second = j;
			}
		}
		
		
		//Check whether there are any other edge with the same value or not...
		for(int j = 0; j < NUM_NODES; j++) {
			if(this.graph[index][j] == 1 && this.cost[index][j] == highestCost) {
				repeatedHighestCostNodes.add(j);
			}
		}
		
		
		//System.out.println("size of repeatedLowestCostNodes: " + repeatedLowestCostNodes.size());
		
		//... If not, return the only feasible edge 
		if((repeatedHighestCostNodes.size() - 1) == 0) { //size - 1, because one must exclude the edge already counted
			Pair<Integer,Integer> p = Pair.create(index, repeatedHighestCostNodes.get(0));
			return p;
		}
		
		//... Else: With the highest nodes in hands, apply first path-scanning 4rd criterion, that is: find the longest shortest path ??
		int maxCostPath = Integer.MIN_VALUE;
		int node_j = -1;
		for(int i = 0; i < repeatedHighestCostNodes.size(); i++) {
			int j = repeatedHighestCostNodes.get(i);
			
			//dijkstra
			ArrayList<Integer> shortPath = getShortestPath(j, depot);
			ArrayList<Pair<Integer,Integer>> minPathAux = new ArrayList<Pair<Integer,Integer>>();
				
			//add edges from shortest to depot in order to complete a cycle
			Pair<Integer,Integer> auxEdge = Pair.create(-2,-2); 
			for(int k = 0; k < shortPath.size() - 1; k++) {
				auxEdge = Pair.create(-2,-2);
				auxEdge.first = shortPath.get(k);
				auxEdge.second = shortPath.get(k+1);
				minPathAux.add(auxEdge);
			}
					
			int sum_j = 0;
			for(int n = 0; n < minPathAux.size(); n++) {
				auxEdge = minPathAux.get(n);
				sum_j += auxEdge.first;
			}
			
			if(sum_j > maxCostPath) {
				maxCostPath = sum_j;
				node_j = j;
			}
			
			sum_j = 0;
		}
		
		//After, return the correct edge;
		int node_j_index = repeatedHighestCostNodes.indexOf(node_j);
		Pair<Integer,Integer> p = Pair.create(index, repeatedHighestCostNodes.get(node_j_index));

		return p;
	}
	
	
	public ArrayList<Integer> getShortestPath(int nodeA, int nodeB) {
		
		ArrayList<Integer> shortPath = new ArrayList<Integer>();
		ArrayList<Integer> availableNodes = new ArrayList<Integer>();
		
		if(nodeA == nodeB) return shortPath;
		
		int currentNode = -1;
		int currentAdj = -1;
		
		int dist[] = new int[NUM_NODES];
		int prev[] = new int[NUM_NODES];
		
		int visited[]  = new int[NUM_NODES];
		
		//initialize;
		for(int i = 0; i < this.NUM_NODES; i++) {
			dist[i] = Integer.MAX_VALUE;
			prev[i] = -1;
			visited[i] = 0;
			availableNodes.add(i);
			
		}
		
		visited[nodeA] = 1;
		dist[nodeA] = 0;
		
		
		while(!availableNodes.isEmpty()) {
			
			currentNode = getMin(availableNodes, dist, visited);
			if(currentNode == -1) return new ArrayList<Integer>();
			visited[currentNode] = 1;
			
			int k;
			for(k = 0; k < availableNodes.size(); k++) {
				if (availableNodes.get(k) == currentNode) break;
			}
			availableNodes.remove(k);
			
			ArrayList<Integer> adj = getAdj(currentNode);
			
			for(int i = 0; i < adj.size(); i++) {
				
				currentAdj = adj.get(i);
				
				if(visited[currentAdj] == 0) {
					if(dist[currentAdj] > dist[currentNode] + this.cost[currentNode][currentAdj]) {
						dist[currentAdj] = dist[currentNode] + this.cost[currentNode][currentAdj];
						prev[currentAdj] = currentNode;
					}
				}
				
			}
			
		}
		
		shortPath = buildPath(nodeA, nodeB, prev);
		
		return shortPath;
	
	}
	
	ArrayList<Integer> buildPath(int nodeA, int nodeB, int[] prev){
		
		ArrayList<Integer> filePath = new ArrayList<Integer>();
		int currentNode = nodeB;
		
		while(prev[currentNode] != -1) {
			filePath.add(0,currentNode);
			currentNode = prev[currentNode];
		}
		
		filePath.add(0,nodeA);
		
		return filePath;
		
		
	}
	
	int getMin(ArrayList<Integer> availableNodes, int[] dist, int[] visited) {
		
		int min = Integer.MAX_VALUE;
		int index = -1;
		
		for(int i = 0; i < availableNodes.size(); i++) {
			if(dist[availableNodes.get(i)] < min) {
				min = dist[availableNodes.get(i)];
				index = availableNodes.get(i);
			}
		}
		
		return index;
		
	}
	
	ArrayList<Integer> getAdj(int i){
		
		ArrayList<Integer> adj = new ArrayList<Integer>();
		
		for(int j = 0; j < this.NUM_NODES; j++) {
			if(this.graph[i][j] == 1){
				adj.add(j);
			}
		}
		
		return adj;
		
	}
	
	
	public boolean findEdge(Pair<Integer,Integer> edge) {
		if(this.remainingEdges.contains(edge)) {
			return true;
		}
		return false;
	}
	
	
	private void startRemainingEdges(ArrayList<Pair<Integer,Integer>> remainingEdges) {	
		//start every node as non visited, that is to be visited'(globally)
		Pair<Integer,Integer> edge = Pair.create(-2,-2);
		for(int i = 0; i < this.graph.length; i++) {
			for(int j = 0; j < this.graph.length; j++) {	
				if(this.graph[i][j] == 1) {
					edge = Pair.create(i, j);
					remainingEdges.add(edge);
				}		
			}
		}
	}
	
	
}
