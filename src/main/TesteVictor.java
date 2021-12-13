package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import heuristics.CARP_Victor;
import heuristics.Cycle;
import heuristics.EdgeRandomization;
import heuristics.OptPathPlan;
import ilog.concert.IloException;

public class TesteVictor {
	
	public static void main(String[] args) throws IloException, IOException, CloneNotSupportedException {

		//Parameters
		//number of routers in the infrastructure
		int networkSize = Integer.parseInt(args[0]);
	
		//available space in a given flow (e.g., # of bytes)
		int capacityProbe = Integer.parseInt(args[1]);
		
		//maxProbes in the solution
		int maxProbes = Integer.parseInt(args[2]);
		 
		//number of telemetry items per router
		int telemetryItemsRouter = Integer.parseInt(args[3]);
		
		//max size of a given telemetry item (in bytes)
		int maxSizeTelemetryItemsRouter = Integer.parseInt(args[4]);
		
		String pathInstance = "/home/lopesvictor/git/hs1.txt";
		
		testModel(capacityProbe, networkSize, telemetryItemsRouter, maxSizeTelemetryItemsRouter, maxProbes);
		/*
		long seed = System.nanoTime();
		NetworkInfrastructure infra = new NetworkInfrastructure(networkSize, pathInstance, telemetryItemsRouter, maxSizeTelemetryItemsRouter, seed);
		infra.generateRndTopology(0.5);
		Statistics st = new Statistics(maxProbes, networkSize, capacityProbe, infra);
		int collector = 0;
		boolean feasible = true;
		long infeasibleTime = System.nanoTime();
		
		//first, verifies if it has a feasible solution
		//this verification ensures that the algorithm can (at least) create a circuit that can collect 
		//the largest item and return to the depot.
		int maximumDistance = Integer.MIN_VALUE;
		for(int i = 0; i < infra.size; i++) {
			int shortPath = infra.getShortestPath(collector, i).size() -1;
			if(shortPath < 0) {
				shortPath = 0;
			}
			if(shortPath > maximumDistance) {
				maximumDistance = shortPath;
			}
		}
		
		int maxItemSize = Integer.MIN_VALUE;
		for(int i = 0; i < infra.sizeTelemetryItems.length; i++) {
			if(infra.sizeTelemetryItems[i] > maxItemSize) {
				maxItemSize = infra.sizeTelemetryItems[i];
			}
		}
		if(maxItemSize > (capacityProbe - (maximumDistance*2))) {
			//Solution not feasible
			feasible = false;
		}
		
		//this verification ensures that every router is connected to at least one other, making it reachble
		for(int i = 0; i < networkSize; i++) {
			int connections = 0;
			for(int j = 0; j < networkSize; j++) {
				if(infra.graph[i][j] == 1) {
					connections++;
				}
			}
			if(connections == 0) {
				feasible = false;
				break;
			}
		}
		
		if(!feasible) {
			double infeasibleTimeFinal = ((System.nanoTime() - infeasibleTime)*0.000000001);
			//heuristic print
			System.out.println("-0" + ";" + infeasibleTimeFinal + ";" 
					+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
					+ ";" + infra.seed);
			//opt print (accessing 'father' infra data)
			System.out.println("-1" + ";" + infeasibleTimeFinal + ";" 
					+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
					+ ";" + infra.seed);
			//Infocom print
			System.out.println("-2" + ";" + infeasibleTimeFinal + ";" 
					+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
					+ ";" + infra.seed);
		}else {
			//victor
			long timeVictor = System.nanoTime();
			CARP_Victor model = new CARP_Victor(infra, capacityProbe, collector);
			model.run2();
			model.convertToCycles();
			model.improvementMethod();
			model.adaptToLinks();
			Hashtable<Integer, Cycle> cycles = new Hashtable<Integer, Cycle>();
			for(int i = 0; i < model.cycles.size(); i++) {
				cycles.put(i, model.cycles.get(i));
			}
			model.time = ((System.nanoTime() - timeVictor)*0.000000001);
			System.out.println("Victor" + st.runCARP(model));
			HashSet<Integer> depotNodes = new HashSet<Integer>();
			depotNodes = model.depotNodesCycles(); //get depot nodes already used at the heuristic
			model = null;
			
			
			//infocom
			long timeOpp = System.nanoTime();
			OptPathPlan pathPlan = new OptPathPlan(infra, capacityProbe, seed, false);
			pathPlan.adaptToLinks();
			pathPlan.addOverhead(collector);
			pathPlan.time = ((System.nanoTime() - timeOpp)*0.000000001);
			System.out.println("OptPathPlan" + st.runOPP(pathPlan, false));
			pathPlan = null;
			
			
			//infocom com ciencia de retorno
			long timeOppAlt = System.nanoTime();
			OptPathPlan pathPlanAlt = new OptPathPlan(infra, capacityProbe, seed, true);
			pathPlanAlt.adaptToLinks();
			pathPlanAlt.addOverhead(collector);
			pathPlanAlt.time = ((System.nanoTime() - timeOppAlt)*0.000000001);
			System.out.println("OptPathPlanReturn" + st.runOPP(pathPlanAlt, false));
			pathPlanAlt = null;
			
			
			//cplex
			long timeOpt = System.nanoTime();
			AlgorithmOpt opt = new AlgorithmOpt(infra, maxProbes);
			ArrayList<Integer> sinks = new ArrayList<Integer>();
			for(Integer dn: depotNodes) {
				sinks.add(dn);
			}
			opt.buildCPPTelemetry(maxProbes, capacityProbe, sinks);
			opt.time = ((System.nanoTime() - timeOpt)*0.000000001);
			System.out.println("Optimal" + st.runOptimal(opt, sinks.get(0)));
			opt = null;
		}
		System.out.println();*/
	}

	@SuppressWarnings("unused")
	private static void testModel(int capacityProbe, int networkSize, int telemetryItemsRouter, int maxSizeTelemetryItemsRouter, int maxProbes) throws CloneNotSupportedException {
		String pathInstance = "/home/lopesvictor/git/hs1.txt";
		long seed = System.nanoTime();
		NetworkInfrastructure infra = new NetworkInfrastructure(networkSize, pathInstance, telemetryItemsRouter, maxSizeTelemetryItemsRouter,(long) seed);
		int bestSolution = Integer.MAX_VALUE;
		for(int i = 0; i < 1; i++) {
			System.out.println("--------------------------------------------------");
			seed = System.nanoTime();
			//long seed = 80892918240283L;
			int collector = 0;
			
			infra.generateRndTopology(0.5);
			//victor
			long timeVictor = System.nanoTime();
			CARP_Victor model = new CARP_Victor(infra, capacityProbe, collector);
			model.run2();
			model.convertToCycles();
			model.improvementMethod();
			model.adaptToLinks();
			model.time = ((System.nanoTime() - timeVictor)*0.000000001);
			
			
			/*EdgeRandomization model = new EdgeRandomization(infra, capacityProbe, (long) seed, maxProbes);
			model.runER();
			//model.improvementMethod();
			model.adaptToLinks();*/
			
			if(model.cycles.size() < bestSolution) {
				bestSolution = model.cycles.size();
			}
			System.out.println("Solução atual: " + model.cycles.size() + ". Melhor Solução: " + bestSolution);
			
			
			//check 1, se a capacidade do ciclo passa da capacidade maxima
			for(Cycle c: model.cycles) {
				if(c.capacity_used > c.capacity) {
					System.out.println("Capacity overflow. Total Capacity:" + c.capacity + ". Capacity Used: " + c.capacity_used + ". Seed: " + seed);
				}
			}
			
			//check 2, se a capacidade usada está coerente com os itens e arestas do sistema
			for(Cycle c: model.cycles) {
				int capacityUsed = 0;
				capacityUsed += c.links.size();
				for(int j = 0; j < c.itemPerCycle.size(); j++) {
					capacityUsed += infra.sizeTelemetryItems[c.itemPerCycle.get(j).item]; 
				}
				if(capacityUsed == c.capacity_used) {
					//System.out.println("Capacity Ok");
				}else {
					System.out.println("Capacity not Ok. Capacity Used on cycle: " + c.capacity_used + ". Real Capacity Used: " + capacityUsed);
				}
			}
			
			//check 3, se todos os itens foram coletados
			int[][] itemsCollected = new int[networkSize][telemetryItemsRouter];
			for(Cycle c: model.cycles) {
				for(int j = 0; j < c.itemPerCycle.size(); j++) {
					if(itemsCollected[c.itemPerCycle.get(j).device][c.itemPerCycle.get(j).item] == 0) {
						itemsCollected[c.itemPerCycle.get(j).device][c.itemPerCycle.get(j).item] = 1;
					}else {
						System.out.println("Coletando um item mais de uma vez. Item: (" + c.itemPerCycle.get(j).device + "," + c.itemPerCycle.get(j).item + "). Seed: " + seed);
					}
				}
			}
			for(int j = 0; j < networkSize; j++) {
				for(int k = 0; k < telemetryItemsRouter; k++) {
					if(itemsCollected[j][k] == 0) {
						System.out.println("Não coletou item (" + j + "," + k + "). Seed: " + seed);
					}
				}
			}
			
			//check 4 se todas as arestas estão sendo coletadas
			int[][] arestas = new int[networkSize][networkSize];
			for(Cycle c: model.cycles) {
				for(int j = 1; j < c.nodes.size(); j++) {
					arestas[c.nodes.get(j-1)][c.nodes.get(j)] = 1;
					arestas[c.nodes.get(j)][c.nodes.get(j-1)] = 1;
				}
			}
			for(int j = 0; j < networkSize; j++) {
				for(int k = 0; k < networkSize; k++) {
					if(infra.graph[j][k] == 0 && arestas[j][k] == 1) {
						System.out.println("Passou por aresta inexistente (" + j + "," + k + "). Seed: " + seed);
					}
					if(arestas[j][k] == 0 && infra.graph[j][k] == 1) {
						System.out.println("Não passou pela aresta (" + j + "," + k + "). Seed: " + seed);
					}
				}
			}
			
			/*Statistics st = new Statistics(maxProbes, networkSize, capacityProbe, infra);
			System.out.println("Victor" + st.runCARP(model));*/
		}
	}		
}
