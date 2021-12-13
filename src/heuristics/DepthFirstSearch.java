package heuristics;

import java.util.ArrayList;
import java.util.Random;
import main.NetworkInfrastructure;

public class DepthFirstSearch {

	ArrayList< ArrayList<Integer> > paths;
	Cycle cycle;
	ArrayList<Integer> path;
	int[][] graph;
	
	public DepthFirstSearch() {
		
	}
	
	public DepthFirstSearch(NetworkInfrastructure infra){
	
		paths = new ArrayList< ArrayList<Integer> >();

		Random r = new Random();
		int v = Math.abs(r.nextInt() % infra.size);
		Search(v,infra);
		
	}
	
	//run through the whole matrix
	public DepthFirstSearch(int[][] matriz,int size,int v){
		// paths - contains a list of sequences
		paths = new ArrayList< ArrayList<Integer> >();
		
		// graph - contains the adjacency matrix of paths - is a matrix size x size
		this.graph = new int[size][size];
		for(int i=0;i<size;i++) {
			for(int j=0;j<size;j++) {
				this.graph[i][j]=0;
			}
		}
		Search(v,matriz,size);
	}
	
	public DepthFirstSearch(int v,int[][] matriz,int size){
		
		path = new ArrayList<Integer>();
		this.graph = new int[size][size];
		for(int i=0;i<size;i++)
			for(int j=0;j<size;j++)
				this.graph[i][j]=0;
		
		int i=0;
		path.add(v);
		i=0;
		while(i!=size){
			for(i=0;i<size;i++){
				if(matriz[v][i]!=0){
					matriz[v][i]=0;
					matriz[i][v]=0;
					this.graph[i][v]=1;
					this.graph[v][i]=1;
					v=i;
					path.add(v);
					break;
				}
			}
		}
	}
	
	public DepthFirstSearch(int v,int[][] matriz,int size,int capacityProbe, NetworkInfrastructure infra, int[][] items, boolean altVersion){
		// flow - flow
		cycle = new Cycle();
		cycle.capacity = capacityProbe;
		
		// c - capacity of flow
		int remainingCapacity = capacityProbe;
		
		// path - the path of flow
		path = new ArrayList<Integer>();
		path.add(v);
		int i=-1;
		int ant_v = -1;
		if(!altVersion) {			
			while(i!=size){
				for(int x = 0; x < infra.telemetryItemsRouter; x++) {
					if(items[v][x] == 1) {
						if(remainingCapacity - infra.sizeTelemetryItems[x] >= 0) {
							remainingCapacity -= infra.sizeTelemetryItems[x];
							items[v][x] = 0;
							Tuple t = new Tuple(v, x);
							cycle.itemPerCycle.add(t);
						}
					}
				}
				if(remainingCapacity <= 0) {
					break;
				}
				for(i = 0; i < size; i++){
					if(matriz[v][i] != 0 && i != ant_v){
						boolean visitado = true;
						for(int j = 0; j < infra.telemetryItemsRouter; j++) {
							if(items[v][j] == 1) {
								visitado = false;
							}
						}
						for(int j = 0; j < infra.telemetryItemsRouter; j++) {
							if(items[i][j] == 1) {
								visitado = false;
							}
						}
						if(visitado) {						
							matriz[v][i] = 0;
							matriz[i][v] = 0;
						}
						ant_v = v;
						v = i;
						path.add(v);
						remainingCapacity -= 1;
						break;
					}
				}
			}
		}else {
			while(i!=size){
				for(int x = 0; x < infra.telemetryItemsRouter; x++) {
					if(items[v][x] == 1) {
						if(remainingCapacity - infra.sizeTelemetryItems[x] >= 0) {
							remainingCapacity -= infra.sizeTelemetryItems[x];
							items[v][x] = 0;
							Tuple t = new Tuple(v, x);
							cycle.itemPerCycle.add(t);
						}
					}
				}
				if(remainingCapacity <= 2) {
					break;
				}
				for(i = 0; i < size; i++){
					if(matriz[v][i] != 0 && i != ant_v){
						boolean visitado = true;
						for(int j = 0; j < infra.telemetryItemsRouter; j++) {
							if(items[v][j] == 1) {
								visitado = false;
								break;
							}
							if(items[i][j] == 1) {
								visitado = false;
								break;
							}
						}
						if(visitado) {						
							matriz[v][i] = 0;
							matriz[i][v] = 0;
						}
						ant_v = v;
						v = i;
						path.add(v);
						remainingCapacity -= 2;
						break;
					}
				}
			}
			//add return to collector path to cycle
			int hopsToReturn = path.size()-2;
			for(int j = hopsToReturn; j >= 0; j--) {
				path.add(path.get(j));
			}
		}
		cycle.nodes = path;
		cycle.capacity_used = cycle.capacity - remainingCapacity;
		this.graph = matriz;
	}
	
	public void Search(int v,NetworkInfrastructure infra){
		
		ArrayList<Integer> path = new ArrayList<Integer>();
		
		int i=0;
		System.out.println(infra.size);
		path.add(v);
		while(i!=infra.size){
			for(i=0;i<infra.size;i++){
				if(infra.graph[v][i]!=0){
					infra.graph[v][i]=0;
					infra.graph[i][v]=0;
					v=i;
					path.add(v);
					break;
				}
			}
		}if(path.size()>1){
			paths.add(path);
			for(i=path.size()-2;i>=0;i--) {
				Search(path.get(i),infra);
			}
		}
		
	}

	public void Search(int v,int[][] matriz,int size){
		ArrayList<Integer> path = new ArrayList<Integer>();
		
		int i=0;
		path.add(v);
		while(i!=size){
			for(i=0;i<size;i++){
				if(matriz[v][i]!=0){
					matriz[v][i]=0;
					matriz[i][v]=0;
					this.graph[i][v]=1;
					this.graph[v][i]=1;
					v=i;
					path.add(v);
					break;
				}
			}
		}paths.add(path);
		if(path.size()>1){
			paths.add(path);
			for(i=path.size()-2;i>=0;i--) {
				Search(path.get(i),matriz,size);
			}
		}
	}
	
	public ArrayList<Integer>getPath(){
		return path;
	}
	public ArrayList< ArrayList<Integer> > getPaths(){
		return paths;
	}
	
	public int[][] getgraph(){
		return graph;
	}
	
	public int getgraph(int i,int j){
		return graph[i][j];
	}
}
