package main;

import java.util.ArrayList;
import java.util.Random;

public class EulerTrail {
	public int[][] graph;            //the network simulating graph
	public ArrayList<NetworkFlow> Q; //Q contains the list of flows
	public int size;                 //indicates the number of vertex in the graph
	public int MAX;                  //indicates the capacity of the probes
	
	
	public EulerTrail(NetworkInfrastructure infra){
		this.Q = new ArrayList<NetworkFlow>();
		this.size = infra.size;
		this.MAX = 5;
		this.graph = infra.graph;
		OptPathPlan(graph);//the search of flows
	}
	

	public void OptPathPlan(int[][] graph) {
		// G - List of graphs in matrix form
		// graph é uma matriz nula agr
		ArrayList<int[][]> G = graphSplit(graph);
		//No graphs, nothing to work
		if(G.size()==0)
			return;
		// T - list of graphs, in matrix form, without odd vertices
		ArrayList<int[][]> T = graphSelect(G);
		
		if(T.size() != 0){
			if(Q.size() != 0)
				for(int i = 0; i < T.size(); i++)
					FindRelated(T.get(i));
			//**
			else{				
				for(int t = 0; t < T.size(); t++){
					int v = -1;
					for(int i = 0; i < this.size; i++){
						for(int j = 0; j < this.size; j++)
							if(T.get(t)[i][j] == 1) {
								v = i;
								break;
							}
						if(v == -1){
							break;							
						}
					}
					Q.add(EulerCircuit(v, T.get(t)));
				}
			}
		}		
		/*
		 * para todo grafo dentro de G é feito uma busca em profundidade,
		 * até o numero maximo,
		 * começando de um odd vertice
		 */
		for(int S = 0; S < G.size(); S++) {
			ArrayList<Integer> oddnum = Searchodd(G.get(S));
			Q.add(EulerCircuit(oddnum.get(0), G.get(S)));
		}
		
		// retorna as arestas ainda n visitada para matriz graph
		for(int S = 0; S < G.size(); S++){
			for(int i = 0; i < this.size; i++){
				for(int j = 0; j < this.size; j++){					
					if(G.get(S)[i][j] == 1){
						graph[i][j] = 1;													
					}
				}
			}
		}
		OptPathPlan(graph);
		return;
	}

	public ArrayList<int[][]> graphSplit(int[][] graph){
		ArrayList<int[][]> G = new ArrayList<int[][]>();
		// check all vertices
		for(int i = 0; i < this.size; i++) {			
			for(int j = 0; j < this.size; j++) {				
				if(graph[i][j] == 1){
					//use the DFS to find the vertices connected with i - foreach i
					G.add(DepthFirstSearchControl(graph, i));
				}
			}
		}
		return G;
	}
	
	public ArrayList<int[][]> graphSelect(ArrayList<int[][]> G){
		ArrayList<int[][]> T = new ArrayList<int[][]>();
		
		int v;
		for(int S = 0; S < G.size(); S++){
			int aux;
			for(v = 0; v < this.size; v++){
				aux = 0;
				for(int j = 0; j < this.size; j++) {
					if(G.get(S)[v][j] == 1) {
						aux++;											
					}
				}
				if(aux%2==1) {
					break;					
				}
			}if(v == this.size){
				T.add(G.get(S));
				G.remove(G.get(S));
				S--;
			}
		}
		return T;
	}
	
	public ArrayList<Integer> Searchodd(int[][] S) {
		ArrayList<Integer> oddnum = new ArrayList<Integer>();
		Random r = new Random();
		for(int v = 0; v < this.size; v++) {
			int aux = 0;
			for(int j = 0;j < this.size; j++) {
				if(S[v][j] == 1) {
					aux++;
				}
			}
			if(aux%2 == 1){
				if(oddnum.size() != 0){
					oddnum.add(Math.abs(r.nextInt()%oddnum.size()), v);
				}else {
					oddnum.add(v);
				}
			}
		}
		return oddnum;
	}
	
	public void FindRelated(int[][] T){
		int i;
		for(i = 0; i < this.size; i++){
			for(int j = i+1; j < this.size; j++) {
				if(T[i][j] == 1){
					Q.add(EulerCircuit(i, T));
				}
			}
		}
		return;
	}
	
	public ArrayList<Integer> ConectPath(ArrayList<Integer> path, ArrayList<Integer> eulerC, int v){
		ArrayList<Integer> C = new ArrayList<Integer>();
		for(int i = 0; i < v; i++) {
			C.add(path.get(i));
		}
		for(int i = 0; i < eulerC.size(); i++) {
			C.add(eulerC.get(i));
		}
		for(int i = v+1; i < path.size(); i++) {
			C.add(path.get(i));
		}
		return C;
	}
	
	public NetworkFlow EulerCircuit(int v, int[][] matriz) {
		// flow - flow
		NetworkFlow flow = new NetworkFlow(v,-1, MAX);
		
		// c - capacity of flow
		int c = MAX;
		
		// path - the path of flow
		ArrayList<Integer> path = new ArrayList<Integer>();
		
		path.add(v);
		int i = 0;
		
		while(c > 0 && i != size){
			for(i = 0; i < size; i++){
				if(matriz[v][i] != 0){
					matriz[v][i] = 0;
					matriz[i][v] = 0;
					v = i;
					path.add(v);
					c = c-1;
					break;
				}
			}
		}
		flow.setPath(path);
		return flow;
	}
	
	public int[][] DepthFirstSearchControl(int[][] matriz, int v){
		// paths - contains a list of sequences
		ArrayList<ArrayList<Integer>> paths = new ArrayList< ArrayList<Integer> >();
		
		// graph - contains the adjacency matrix of paths - is a matrix size x size
		int[][] graphAux = new int[this.size][this.size];
		
		for(int i = 0; i < this.size; i++) {
			for(int j = 0; j < this.size; j++) {
				graphAux[i][j] = 0;
			}
		}
		graphAux = DepthFirstSearch(v, matriz, graphAux, paths);
		return graphAux;
	}


	private int[][] DepthFirstSearch(int v, int[][] matriz, int[][] graphAux, ArrayList<ArrayList<Integer>> paths) {
		ArrayList<Integer> path = new ArrayList<Integer>();
		
		int i=0;
		path.add(v);
		while(i != this.size){
			for(i = 0;i < this.size; i++){
				if(matriz[v][i] != 0){
					matriz[v][i] = 0;
					matriz[i][v] = 0;
					graphAux[i][v] = 1;
					graphAux[v][i] = 1;
					v = i;
					path.add(v);
					break;
				}
			}
		}
		paths.add(path);
		if(path.size()>1){
			paths.add(path);
			for(i=path.size()-2;i>=0;i--) {
				graphAux = DepthFirstSearch(path.get(i),matriz, graphAux, paths);
			}
		}
		return graphAux;
	}
}
