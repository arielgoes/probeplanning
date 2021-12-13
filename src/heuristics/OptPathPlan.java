package heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import main.NetworkFlow;
import main.NetworkInfrastructure;

public class OptPathPlan {
	public ArrayList<Cycle> Q;      //contains the list of probe paths
	public int size;                //the size of the infrastructure
	public int probeCapacity;       //capacity of the probes
	public NetworkInfrastructure infra;     //the infrastructure 
	public long seed;                       //seed for random generated numbers
	public int[][] infraitems;              //the items in the infrastructure
	public int[][] costs;
	public double time;
	public boolean altVersion;
	public int[][] G;
	
	
	//constructor
	public OptPathPlan(NetworkInfrastructure infra, int probeCapacity, long seed, boolean altVersion) {
		this.Q = new ArrayList<Cycle>();
		this.size = infra.size;
		this.infra = infra;
		this.probeCapacity = probeCapacity;
		this.seed = seed;
		this.costs = this.infra.graph;
		this.altVersion = altVersion;
		
		//copying the list of items from the infrastructure to local object	
		this.infraitems = new int[infra.size][infra.telemetryItemsRouter];
		for(int i = 0; i < infra.size; i++) {
			for(int j = 0; j < infra.telemetryItemsRouter; j++) {
				this.infraitems[i][j] = infra.items[i][j];
			}
		}
		//copying the graph from the infrastructure to local object
		int[][] graph = new int[size][size];
		for(int i=0;i<size;i++)
			for(int j=0;j<size;j++)
				graph[i][j] = infra.graph[i][j];
		
		
		run(graph);    //executes the algorithm
	}
	
	public void run(int[][] graph) {
		ArrayList<int[][]> G = SearchG(graph);  //G contains a list of subgraphs
		if(G.size()==0) {          //if there is no subgraphs, end the algorithm
			this.G = graph;
			return;
		}
		ArrayList<int[][]> T = SearchT(G);   // T contains a list of subgraphs that does not contain odd vertex
		
		if(T.size()!=0) {
			if(Q.size()!=0)
				for(int i=0;i<T.size();i++)
					FindTpath(T.get(i));
			//**
			else
				for(int t=0;t<T.size();t++) {
					int v=-1;
					for(int i=0;i<size;i++) {
						for(int j=0;j<size;j++)
							if(T.get(t)[i][j]==1) {
								v=i;
								break;
							}
						if(v==-1)
							break;
					}
					DepthFirstSearch path = new DepthFirstSearch(v,T.get(t), this.size, this.probeCapacity, this.infra, this.infraitems, this.altVersion);
					Q.add(path.cycle);
				}
		}
		
		/*
		 * para todo grafo dentro de G é feito uma busca em profundidade,
		 * até o numero maximo,
		 * começando de um odd vertice
		*/
		for(int S=0;S<G.size();S++) {
			ArrayList<Integer> oddnum = Searchodd(G.get(S));
			DepthFirstSearch path = new DepthFirstSearch(oddnum.get(0),G.get(S),this.size, this.probeCapacity, this.infra, this.infraitems, this.altVersion);
			Q.add(path.cycle);
			G.remove(S);
			G.add(S, path.graph);
		}
		

		// retorna as arestas ainda n visitada para matriz graph
		for(int S=0;S<G.size();S++)
			for(int i=0;i<size;i++)
				for(int j=0;j<size;j++)
					if(G.get(S)[i][j]==1)
						graph[i][j]=1;
		
		for(int S=0;S<T.size();S++)
			for(int i=0;i<size;i++)
				for(int j=0;j<size;j++)
					if(T.get(S)[i][j]==1)
						graph[i][j]=1;
		
		run(graph);
		this.G = graph;
		return;
	}

	public ArrayList<int[][]> SearchG(int[][] graph){
		ArrayList<int[][]> G = new ArrayList<int[][]>();
		// check all vertices
		for(int i=0;i<size;i++)
			for(int j=0;j<size;j++)
				if(graph[i][j]==1) {
					//use the DFS to find the vertices connected with i - foreach i
					DepthFirstSearch search = new DepthFirstSearch(graph,size,i);
					G.add(search.getgraph());
				}
		return G;
	}
	
	public ArrayList<int[][]> SearchT(ArrayList<int[][]> G){
		ArrayList<int[][]> T = new ArrayList<int[][]>();
		
		int v;
		for(int S=0;S<G.size();S++) {
			int aux;
			for(v=0;v<size;v++) {
				aux = 0;
				for(int j=0;j<size;j++)
					if(G.get(S)[v][j]==1)
						aux++;
				if(aux%2==1)
					break;
			}if(v==size) {
				T.add(G.get(S));
				G.remove(G.get(S));
				S--;
			}
		}return T;
	}
	
	public ArrayList<Integer> Searchodd(int[][] S) {
		ArrayList<Integer> oddnum = new ArrayList<Integer>();
		Random r = new Random(seed);
		for(int v=0;v<size;v++) {
			int aux = 0;
			for(int j=0;j<size;j++) {
				if(S[v][j]==1) {
					aux++;
				}
			}
			if(aux%2==1) {
				if(oddnum.size()!=0) {
					oddnum.add(Math.abs(r.nextInt()%oddnum.size()), v);
				}else {
					oddnum.add(v);
				}
			}
		}
		return oddnum;
	}
	
	public void FindTpath(int[][] T) {
		int i;
		for(i=0;i<size;i++) {
			for(int j=i+1;j<size;j++) {
				if(T[i][j]==1) {
					DepthFirstSearch eulerC = new DepthFirstSearch(i,T,this.size, this.probeCapacity, this.infra, this.infraitems, this.altVersion);
					Q.add(eulerC.cycle);
				}
			}
		}return;
	}
	
	public ArrayList<Integer> ConectPath(ArrayList<Integer> path,ArrayList<Integer> eulerC,int v){
		ArrayList<Integer> C = new ArrayList<Integer>();
		for(int i=0;i<v;i++) {
			C.add(path.get(i));
		}
		for(int i=0;i<eulerC.size();i++) {
			C.add(eulerC.get(i));
		}
		for(int i=v+1;i<path.size();i++) {
			C.add(path.get(i));
		}
		return C;
	}
	
	public HashSet<Integer> depotNodesCycles() {
    	HashSet<Integer> depotNodes = new HashSet<Integer>();
    	for(int i = 0; i < this.Q.size(); i++) {
    		depotNodes.add(this.Q.get(i).nodes.get(0));
		}
    	
    	return depotNodes;
    }
	
	public void addOverhead(int colector) {
		for(Cycle c: this.Q) {
			c.pathOverhead = this.getShortestPath(c.nodes.get(c.nodes.size()-1), colector);
			if(c.pathOverhead.size() > 0) {
				c.transportationCost = c.pathOverhead.size() - 1;
			}else {
				c.transportationCost = 0;
			}
		}
		
	}
	
	public ArrayList<Integer> getShortestPath(int nodeA, int nodeB) {
		ArrayList<Integer> shortPath = new ArrayList<Integer>();
		ArrayList<Integer> availableNodes = new ArrayList<Integer>();
		
		if(nodeA == nodeB) {
			return shortPath;			
		}
		int currentNode = -1;
		int currentAdj = -1;
		int dist[] = new int[this.size];
		int prev[] = new int[this.size];
		int visited[]  = new int[this.size];
		
		//initialize;
		for(int i = 0; i < this.size; i++) {
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
					if(dist[currentAdj] > dist[currentNode] + this.costs[currentNode][currentAdj]) {
						dist[currentAdj] = dist[currentNode] + this.costs[currentNode][currentAdj];
						prev[currentAdj] = currentNode;
					}
				}	
			}	
		}
		shortPath = buildPath(nodeA, nodeB, prev);
		return shortPath;
	}

	public ArrayList<Integer> getAdj(int i){
		ArrayList<Integer> adj = new ArrayList<Integer>();
		for(int j = 0; j < this.size; j++) {
			if(this.infra.graph[i][j] == 1){
				adj.add(j);
			}
		}
		return adj;
	}
	
	public ArrayList<Integer> buildPath(int nodeA, int nodeB, int[] prev){
		ArrayList<Integer> filePath = new ArrayList<Integer>();
		int currentNode = nodeB;
		while(prev[currentNode] != -1) {
			filePath.add(0,currentNode);
			currentNode = prev[currentNode];
		}
		filePath.add(0,nodeA);
		return filePath;
	}
	
	public int getMin(ArrayList<Integer> availableNodes, int[] dist, int[] visited) {	
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
	
	public void adaptToLinks() {
		for(Cycle c: this.Q) {
			for(int i = 1; i < c.nodes.size(); i++) {
				int first = c.nodes.get(i-1);
				int second = c.nodes.get(i);
				Pair<Integer, Integer> p = new Pair<>(first, second);
				c.links.add(p);
			}
		}
	}
	
	
	public void reconstructPaths(ArrayList<Cycle> Q, ArrayList<Integer> failureNodes) {
		//links unsatisfied
		ArrayList<Pair<Integer,Integer>> links_u = new ArrayList<Pair<Integer, Integer>>();
		
		//items unsatisfied
		ArrayList<Tuple> dev_items_u = new ArrayList<Tuple>();
		
		
		//remove path links and items containing the identified failed nodes
		for(int n = 0; n < failureNodes.size(); n++) {
			HashSet<Integer> itemsFromNodes = new HashSet<Integer>();
			
			//select a path
			for(int i = 0; i < Q.size(); i++) {
				int pathSize = Q.get(i).links.size(); //size of the current path given in # of links
				int counter = 0;
				
				//traverse links
				for(int j = 0; j < Q.get(i).links.size(); j++) {
					if(Q.get(i).links.get(j).first == failureNodes.get(n) || Q.get(i).links.get(j).second == failureNodes.get(n)) {
						
						//get nodes from failed link
						itemsFromNodes.add(Q.get(i).links.get(j).first);
						itemsFromNodes.add(Q.get(i).links.get(j).second);
						
						counter++;
						if(!links_u.contains(Q.get(i).links.get(j))) { //avoid duplicates
							links_u.add(Q.get(i).links.get(j)); //unsatisfied link
						}
						
					}
				}
								
				//add items as unsatisfied
				if(counter > 0) {
					for(Integer node : itemsFromNodes) {
						int k = 0;
						for(Tuple di: Q.get(i).itemPerCycle) {
							if(di.getDevice() == node) {
								dev_items_u.add(di);
							}
							k++;
						}
					}
					
					for(Tuple di : dev_items_u) {
						Q.get(i).itemPerCycle.remove(di);
					}
					
				}	
				
				if(counter == pathSize && Q.size() > 0) {
					Q.remove(Q.get(i));
				}

			}
			
		}
		
		//remove links containing failure nodes
		for(int i = 0; i < Q.size(); i++) {
			Q.get(i).links.removeAll(links_u);
		}
		
		//reconstruct
		
		
		
		
		
		
		
	}

}