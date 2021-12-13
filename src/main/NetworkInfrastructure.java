package main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class NetworkInfrastructure {

	public int[][]graph; //network structure (nodes)
	public int[][]conventionalDCN;
	public int[][]vl2;
	public int[][]fatTree;
	public int[][]dist;
	
	public int size;	 //size of the network
	public String filePath;
	public int[] numTelemetryItemsRouter; //number of telemetry item per router (vertex)
	public int[] sizeTelemetryItems;//size of each telemetry item	
	public int[][] items; //row = routers, col = items
	public int telemetryItemsRouter;	//total number of possible existing items in any router
	public int maxSizeTelemetryItemsRouter; //max possible item size
	public long seed;
	
	public NetworkInfrastructure(int size, String filePath, int telemetryItemsRouter, int maxSizeTelemetryItemsRouter, long seed) {
		
		this.filePath = filePath;
		this.size = size;
		this.telemetryItemsRouter = telemetryItemsRouter;
		this.maxSizeTelemetryItemsRouter = maxSizeTelemetryItemsRouter;
		this.graph = new int[size][size];
		this.dist = new int[size][size];
		this.numTelemetryItemsRouter = new int[size];
		this.items = new int[size][telemetryItemsRouter];
		this.seed = seed;
		
	}
	
	
	
	/**
	 * This method loads network topology without any randomness
	 * @throws FileNotFoundException
	 */
	//read .dat
	
	void loadTopologyDat() throws FileNotFoundException {
		
		File file = new File(filePath); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		
		int i, j;
		
		try {
			String st;
			while( (st = br.readLine())!=null) {
				
				String[] split = st.split("\t");
				i = Integer.parseInt(split[1]);
				j = Integer.parseInt(split[2]);
				this.graph[i][j] = 1;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		for(i = 0; i < this.size; i++){
			
			numTelemetryItemsRouter[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItemsRouter[i]) {
				
				items[i][k] = 1;
				k++;
 				
			}
				
		}
		
		for(j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = maxSizeTelemetryItemsRouter;
			
		}	
	}
	

	void loadTopologyTxt() throws FileNotFoundException {

		File file = new File(filePath); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		
		int i, j;
		
		try {
			String st;
			while( (st = br.readLine())!=null) {
				
				String[] split = st.split(" ");
				i = Integer.parseInt(split[0]);
				j = Integer.parseInt(split[1]);
				this.graph[i][j] = 1;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		for(i = 0; i < this.size; i++){
			
			numTelemetryItemsRouter[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItemsRouter[i]) {
				
				items[i][k] = 1;
				k++;
 				
			}
				
		}
		
		for(j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = maxSizeTelemetryItemsRouter;
			
		}
	}
	
	void loadTopologyTxt(long seed) throws FileNotFoundException {
		
		File file = new File(filePath); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		Random rnd = new Random(seed);
		
		int i, j;
		
		try {
			String st;
			while( (st = br.readLine())!=null) {
				
				String[] split = st.split(" ");
				i = Integer.parseInt(split[0]);
				j = Integer.parseInt(split[1]);
				this.graph[i][j] = 1;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		for(i = 0; i < this.size; i++){
			
			//numTelemetryItems[i] = rnd.nextInt(telemetryItemsRouter) + 1;
			numTelemetryItemsRouter[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItemsRouter[i]) {
				
				items[i][k] = 1;
				k++;
 				
			}
				
		}
		
		int aux = 0;
		
		for(j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = rnd.nextInt(maxSizeTelemetryItemsRouter) + 1;
			
		}
		
		
	}
	
	
	void loadAbilene(long seed) throws FileNotFoundException {
		
		this.graph[0][1] = 1;
		this.graph[1][0] = 1;
		
		this.graph[0][10] = 1;
		this.graph[10][0] = 1;
		
		this.graph[1][2] = 1;
		this.graph[2][1] = 1;
		
		this.graph[1][10] = 1;
		this.graph[10][1] = 1;
		
		this.graph[1][9] = 1;
		this.graph[9][1] = 1;
		
		this.graph[10][9] = 1;
		this.graph[9][10] = 1;
		
		this.graph[2][3] = 1;
		this.graph[3][2] = 1;
		
		this.graph[9][3] = 1;
		this.graph[3][9] = 1;

		this.graph[9][8] = 1;
		this.graph[8][9] = 1;
		
		this.graph[3][4] = 1;
		this.graph[4][3] = 1;
		
		this.graph[8][4] = 1;
		this.graph[4][8] = 1;
		
		this.graph[5][4] = 1;
		this.graph[4][5] = 1;
		
		this.graph[5][8] = 1;
		this.graph[8][5] = 1;
		
		this.graph[7][8] = 1;
		this.graph[8][7] = 1;
		
		this.graph[7][6] = 1;
		this.graph[6][7] = 1;
		
		this.graph[5][6] = 1;
		this.graph[6][5] = 1;

	}
	 
	void loadTopologyDat(long seed) throws FileNotFoundException {
		
		File file = new File(filePath); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		Random rnd = new Random(seed);
		
		int i, j;
		
		try {
			String st;
			while( (st = br.readLine())!=null) {
				
				String[] split = st.split("\t");
				i = Integer.parseInt(split[1]);
				j = Integer.parseInt(split[2]);
				this.graph[i][j] = 1;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		//generete randomly telemetry items
		for(i = 0; i < this.size; i++){
			
			//numTelemetryItems[i] = rnd.nextInt(telemetryItemsRouter) + 1;
			numTelemetryItemsRouter[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItemsRouter[i]) {
				
				//if (rnd.nextDouble() > 0.5 && items[i][l] == 0) {
					items[i][l] = 1;
					k++;
 				//}
				l++;
				l = l % this.telemetryItemsRouter;
				
			}
			
				
		}
		
		for(j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = rnd.nextInt(maxSizeTelemetryItemsRouter) + 1;
			
		}
		
		
	}
	
	void generateRndTopology(double linkProbability) {
		
		//Random rnd = new Random(seed);
		Random rnd = new Random(123);
		
		for(int i = 0; i < size; i++) {
			boolean forceToLink = true;
			for(int j = 0; j < size; j++) {
				if(i != j && forceToLink) {
					this.graph[i][j] = 1;
					this.graph[j][i] = 1;
					forceToLink = false;
				}
				
				if (i != j && rnd.nextDouble() < linkProbability) {
					this.graph[i][j] = 1;
					this.graph[j][i] = 1;
				}
				
			}
			
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		//generete randomly telemetry items
		for(int i = 0; i < this.size; i++){
			
			do {
				numTelemetryItemsRouter[i] = telemetryItemsRouter;//rnd.nextInt(telemetryItemsRouter+1);
				
			}while(numTelemetryItemsRouter[i] == 0);
			
			
			int k = 0;
			int l = 0;
			
			
			
			
			while(k < numTelemetryItemsRouter[i]) {
				
				//if (rnd.nextDouble() > 0.5 && items[i][l] == 0) {
					items[i][l] = 1;
					k++;
 				//}
				l++;
				l = l % this.telemetryItemsRouter;
				
			}
			
				
		}
		
		for(int j = 0; j < this.telemetryItemsRouter; j++) {
			do {
				//sizeTelemetryItems[j] = 2;rnd.nextInt(maxSizeTelemetryItemsRouter);
				sizeTelemetryItems[j] = rnd.nextInt(maxSizeTelemetryItemsRouter);
				//System.out.println("sizeTelemetryItems[" + j + "] = " + sizeTelemetryItems[j]);
			}while(sizeTelemetryItems[j] == 0);
		}
		
	}
	
	
	public void generateConventionalDCNtopology(int core_switches, int height, int k) {
		//'k' must be multiple of 2
		int mx_order = 0;
		for(int i = 1; i <= height * 2; i *= 2) {
			mx_order += core_switches * i;
			//System.out.println("mx_order: " + mx_order);
		}
		
		this.conventionalDCN = new int[mx_order][mx_order];
		 
		int i_index = 1;
		int j_index = 2;
		
		while(i_index < height) {
			for(int i = (int) Math.pow(2, i_index) - 2; i < (int) Math.pow(2, i_index) * 2 - 2; i++) {
				int links = 0;
				for(int j = (int) Math.pow(2, j_index) - 2; j < (int) Math.pow(2, j_index) * 2 - 2; j++) {
					if(links < k && this.conventionalDCN[i][j] != 1) {
						if(i % 2 == 0 && j % 2 == 0) {
							System.out.println("i: " + i + " j: " + j);
							this.conventionalDCN[i][j] = 1;
							this.conventionalDCN[j][i] = 1;
							links++;
						}else if(i % 2 != 0 && j % 2 != 0) {
							System.out.println("i: " + i + " j: " + j);
							this.conventionalDCN[i][j] = 1;
							this.conventionalDCN[j][i] = 1;
							links++;
						}
					}
				}
			}
			i_index++;
			j_index++;
		}
		
		
	}
	
	
	
	public void generateVL2topology(int switches, int stages, int servers) {
		
		int mx_order = (switches/stages) * stages + servers; // matrix order
		
		int size = 0;
		this.vl2 = new int[mx_order][mx_order];
		
		//connect switches to layers
		int offset = 0;
		for(int s = 2; s <= stages; s++) {	
			for(int i = offset; i < switches/stages + offset; i++) {
				for(int j = (switches/stages) + offset; j < (switches/stages) * s; j++) {
					//System.out.println("i: " + i + " j: " + j);
					this.vl2[i][j] = 1;
					this.vl2[i][j] = 1;
				}		
			}
			offset += switches/stages;
		}
		
		//connect 'servers' to the last stage's switches
		int i_offset = offset;
		int server_init = 0;
		int server_end = servers/(switches/stages);
		for(int i = i_offset; i < switches; i++) {
			for(int j = (switches/stages) + offset + server_init; j < (switches/stages) + offset + server_end; j++) {
				//System.out.println("i: " + i + " j: " + j);
				this.vl2[i][j] = 1;
				this.vl2[j][i] = 1;
			}
			server_init += servers/(switches/stages);
			server_end += servers/(switches/stages);
		}
	}
	
	
	public void generateFatTreetopology(int k) {
		
		int core = (int) Math.pow(k/2, 2);
		int pods = k;
		int aggr = k/2;
		int edge = k/2;
		int servers = (int) Math.pow(k, 3)/4;
		
		int[] aggr_index = new int[aggr * pods];
		int[] edge_index = new int[edge * pods];
		int[] server_index = new int[servers];
		
		int size = core;
		
		for(int i = 0; i < aggr * pods; i++) {
			aggr_index[i] = size++;
		}
		for(int i = 0; i < edge * pods; i++) {
			edge_index[i] = size++;
		}
		for(int i = 0; i < servers; i++) {
			server_index[i] = size++;
		}
		
		this.fatTree = new int[size][size];
		
		//map 'core' to 'aggr' switches
		int j = 0;
		for(int i = 0; i < core; i++) {
			if((i + 1) <= core/2) { //until it reaches the half of the pods
				j = 0;
				while(j < (aggr * pods)/2) {
					//System.out.println("i: " + i + " aggr_index[j]: " + aggr_index[j]);
					this.fatTree[i][aggr_index[j]] = 1;
					this.fatTree[aggr_index[j]][i] = 1;	
					j++;	
				}
			}else {
				j = (aggr * pods)/2;
				while(j < (aggr * pods)) {
					//System.out.println("i: " + i + " aggr_index[j]: " + aggr_index[j]);
					this.fatTree[i][aggr_index[j]] = 1;
					this.fatTree[aggr_index[j]][i] = 1;
					j++;
				}
			}
		}
		
		//map 'aggr' to 'edge' switches
		for(int i = 1; i <= pods; i++) {
			for(int a = (k/2) * i - (k/2); a < (k/2) * i; a++) {
				for(int b = (k/2) * i - (k/2); b < (k/2) * i; b++) {
					//System.out.println("aggr_index[a]: " + aggr_index[a] + " edge_index[b]: " + edge_index[b]);
					this.fatTree[aggr_index[a]][edge_index[b]] = 1;
					this.fatTree[b][a] = 1;
				}
			}
		}
		
		//map 'edge' switches to 'servers'
		j = 0;
		for(int i = 0; i < (edge * pods); i++) {
			int count_servers = 0;
			while(true) {
				if((count_servers + 1) <= k/2) {
					//System.out.println("edge_index[i]: " + edge_index[i] + " server_index[j]: " + server_index[j]);
					this.fatTree[edge_index[i]][server_index[j]] = 1;
					this.fatTree[j][edge_index[i]] = 1;
					j++;
					count_servers++;	
				}else {
					break;
					
				}
			}
		}
		
		
		
		
	}
	
	
	public ArrayList<Integer> getShortestPath(int nodeA, int nodeB) {
		
		ArrayList<Integer> shortPath = new ArrayList<Integer>();
		ArrayList<Integer> availableNodes = new ArrayList<Integer>();
		
		if(nodeA == nodeB) return shortPath;
		
		int currentNode = -1;
		int currentAdj = -1;
		
		int dist[] = new int[size];
		int prev[] = new int[size];
		
		int visited[]  = new int[size];
		
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
					if(dist[currentAdj] > dist[currentNode] + this.graph[currentNode][currentAdj]) {
						dist[currentAdj] = dist[currentNode] + this.graph[currentNode][currentAdj];
						prev[currentAdj] = currentNode;
					}
				}
				
			}
			
		}
		
		shortPath = buildPath(nodeA, nodeB, prev);
		//System.out.println("nodeA: " + nodeA + " nodeB: " + nodeB + " shortPath: " + shortPath);
		
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
		
		for(int j = 0; j < this.size; j++) {
			if(this.graph[i][j] == 1){
				adj.add(j);
			}
		}
		
		return adj;
		
	}
	
	public void generateToyTopology(int size) {
		
		this.size = size;
		
		Random rnd = new Random();
		
		for(int i = 0; i < graph.length; i++) {
			for(int j = 0; j < graph[i].length; j++) {
				if(i != j) {
					if(rnd.nextDouble() < 0.5) {
						this.graph[i][j] = 1;
					}else {
						this.graph[i][j] = 0;
					}
					
					do {
						dist[i][j] = 1;//rnd.nextInt(50);
					}while(dist[i][j] == 0);
					
				}
				
				
			}
		}
		
		sizeTelemetryItems = new int[telemetryItemsRouter];
		
		for(int i = 0; i < this.size; i++){
			
			numTelemetryItemsRouter[i] = telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItemsRouter[i]) {
				
				items[i][k] = 1;
				k++;
 				
			}
				
		}
		
		for(int j = 0; j < this.telemetryItemsRouter; j++) {
			
			sizeTelemetryItems[j] = 1;
			
		}	
		
	}



	public void generateCustomTopology() {
		// TODO Auto-generated method stub
				
		this.graph[0][1] = 1;
		this.graph[1][0] = 1;
		this.graph[0][5] = 1;
		this.graph[5][0] = 1;
		this.graph[1][2] = 1;
		this.graph[2][1] = 1;
		this.graph[1][4] = 1;
		this.graph[4][1] = 1;
		this.graph[2][3] = 1;
		this.graph[3][2] = 1;
		this.graph[8][3] = 1;
		this.graph[3][8] = 1;
		this.graph[8][7] = 1;
		this.graph[7][8] = 1;
		this.graph[4][7] = 1;
		this.graph[7][4] = 1;
		this.graph[7][6] = 1;
		this.graph[6][7] = 1;
		this.graph[5][4] = 1;
		this.graph[4][5] = 1;
		
		this.sizeTelemetryItems = new int[this.telemetryItemsRouter]; // size of each telemetry item
		
		//generete randomly telemetry items
		for(int i = 0; i < this.size; i++){
			numTelemetryItemsRouter[i] = this.telemetryItemsRouter;
			
			int k = 0;
			int l = 0;
			
			while(k < numTelemetryItemsRouter[i]) {
				items[i][l] = 1;
				k++;
				l++;
				l = l % this.telemetryItemsRouter;
			}
		}
		
		for(int j = 0; j < this.telemetryItemsRouter; j++) {
				this.sizeTelemetryItems[j] = this.telemetryItemsRouter;	
		}
		
	}
	
	
}
